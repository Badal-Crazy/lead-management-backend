package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.model.Lead;
import com.example.backend.model.Disposition;
import com.example.backend.repository.CsvUserRepository;
import com.example.backend.repository.CsvLeadRepository;
import com.example.backend.repository.CsvDispositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final CsvUserRepository userRepository;
    private final CsvLeadRepository leadRepository;
    private final CsvDispositionRepository dispositionRepository;

    private static final DateTimeFormatter DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM = 
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT_DD_MM_YYYY = 
        DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public AdminController(CsvUserRepository userRepository,
                           CsvLeadRepository leadRepository,
                           CsvDispositionRepository dispositionRepository) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.dispositionRepository = dispositionRepository;
    }

    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        try {
            List<User> pendingUsers = userRepository.findAll().stream()
                    .filter(u -> !u.isApproved())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(pendingUsers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch pending users: " + e.getMessage()));
        }
    }

    @PostMapping("/approve-user/{username}")
    public ResponseEntity<?> approveUser(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            System.out.println("📝 Approving user: " + username);
            
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + username));
            }

            User user = userOpt.get();
            String action = request.get("action");

            if ("approve".equals(action)) {
                user.setApproved(true);
                user.setEnabled(true);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "User approved successfully"));
            } else {
                userRepository.deleteByUsername(username);
                return ResponseEntity.ok(Map.of("message", "User rejected and deleted"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error approving user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to approve user: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            // Log user details for debugging
            System.out.println("=== ALL USERS ===");
            users.forEach(u -> {
                System.out.println("User: " + u.getUsername() + 
                    ", Role: '" + u.getRole() + 
                    "', Enabled: " + u.isEnabled() +
                    ", Approved: " + u.isApproved());
            });
            
            users.forEach(u -> u.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(
            @PathVariable String username,
            @RequestHeader("X-User-Role") String userRole) {
        try {
            System.out.println("🗑️ Delete request for user: " + username);
            System.out.println("👤 Requested by role: " + userRole);
            
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + username));
            }

            User userToDelete = userOpt.get();
            String targetRole = userToDelete.getRole();

            // Super Admin can delete anyone except themselves
            if ("ROLE_SUPER_ADMIN".equals(userRole) || "SUPERADMIN".equals(userRole)) {
                if (username.equals("superadmin")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Cannot delete Super Admin"));
                }
                userRepository.deleteByUsername(username);
                System.out.println("✅ Super Admin deleted user: " + username);
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            }

            // Admin can only delete AGENTS
            if ("ROLE_ADMIN".equals(userRole) || "ADMIN".equals(userRole)) {
                if (!"ROLE_AGENT".equals(targetRole) && !"AGENT".equals(targetRole)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Admins can only delete agents"));
                }
                userRepository.deleteByUsername(username);
                System.out.println("✅ Admin deleted agent: " + username);
                return ResponseEntity.ok(Map.of("message", "Agent deleted successfully"));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to delete users"));

        } catch (Exception e) {
            System.err.println("❌ Error deleting user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already exists"));
            }

            user.setApproved(true);
            user.setEnabled(true);
            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setRole(request.get("role"));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "User role updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update role: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{username}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable String username, @RequestBody Map<String, Boolean> request) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setEnabled(request.get("enabled"));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "User status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to toggle user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{username}/password")
    public ResponseEntity<?> changePassword(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setPassword(request.get("password"));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            List<User> users = userRepository.findAll();
            List<Lead> leads = leadRepository.findAll();
            List<Disposition> dispositions = dispositionRepository.findAll();
            
            // Log all users for debugging
            System.out.println("=== STATS CALCULATION ===");
            System.out.println("Total users: " + users.size());
            users.forEach(u -> {
                System.out.println("  - " + u.getUsername() + ": role='" + u.getRole() + "'");
            });
            
            Map<String, Object> stats = new HashMap<>();
            
            // User stats - FIXED ROLE MATCHING
            long totalUsers = users.size();
            
            // Count ADMINs (including SUPERADMIN)
            long totalAdmins = users.stream()
                .filter(u -> {
                    String role = u.getRole();
                    if (role == null) return false;
                    String upperRole = role.toUpperCase();
                    return upperRole.equals("ADMIN") || 
                           upperRole.equals("SUPERADMIN") ||
                           upperRole.equals("ROLE_ADMIN") || 
                           upperRole.equals("ROLE_SUPERADMIN");
                })
                .count();
            
            // Count AGENTs
            long totalAgents = users.stream()
                .filter(u -> {
                    String role = u.getRole();
                    if (role == null) return false;
                    String upperRole = role.toUpperCase();
                    return upperRole.equals("AGENT") || 
                           upperRole.equals("ROLE_AGENT");
                })
                .count();
            
            long activeUsers = users.stream()
                .filter(User::isEnabled)
                .count();
            
            long pendingApprovals = users.stream()
                .filter(u -> !u.isApproved())
                .count();
            
            System.out.println("Admins counted: " + totalAdmins);
            System.out.println("Agents counted: " + totalAgents);
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalAdmins", totalAdmins);
            stats.put("totalAgents", totalAgents);
            stats.put("activeUsers", activeUsers);
            stats.put("pendingApprovals", pendingApprovals);
            
            // Lead stats
            long totalLeads = leads.size();
            long pendingLeads = leads.stream()
                .filter(l -> l.getStatus() != null && l.getStatus().equalsIgnoreCase("Pending"))
                .count();
            long disposedLeads = leads.stream()
                .filter(l -> l.getStatus() != null && l.getStatus().equalsIgnoreCase("Disposed"))
                .count();
            long ptpLeads = leads.stream()
                .filter(l -> l.getStatus() != null && l.getStatus().equalsIgnoreCase("PTP"))
                .count();
            
            stats.put("totalLeads", totalLeads);
            stats.put("pendingLeads", pendingLeads);
            stats.put("disposedLeads", disposedLeads);
            stats.put("ptpLeads", ptpLeads);
            
            // Disposition stats
            stats.put("totalDispositions", dispositions.size());
            
            // Collection stats
            double totalCollection = dispositions.stream()
                .filter(d -> d.getDispositionStatus() != null)
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || 
                            "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            stats.put("totalCollection", totalCollection);
            
            System.out.println("📊 Stats result: " + stats);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();
            
            // Get dispositions
            var dispositions = dispositionRepository.findAll();
            System.out.println("📋 Found " + dispositions.size() + " dispositions for recent activity");
            
            // Add dispositions as activities (most recent first)
            if (!dispositions.isEmpty()) {
                List<Disposition> sortedDispositions = new ArrayList<>(dispositions);
                sortedDispositions.sort((d1, d2) -> {
                    LocalDateTime d1Time = d1.getCreatedAt() != null ? d1.getCreatedAt() : 
                                          (d1.getCallDate() != null ? d1.getCallDate().atStartOfDay() : null);
                    LocalDateTime d2Time = d2.getCreatedAt() != null ? d2.getCreatedAt() : 
                                          (d2.getCallDate() != null ? d2.getCallDate().atStartOfDay() : null);
                    if (d1Time == null && d2Time == null) return 0;
                    if (d1Time == null) return 1;
                    if (d2Time == null) return -1;
                    return d2Time.compareTo(d1Time);
                });

                int count = Math.min(5, sortedDispositions.size());
                for (int i = 0; i < count; i++) {
                    var d = sortedDispositions.get(i);
                    Map<String, Object> activity = new HashMap<>();
                    String status = d.getDispositionStatus() != null ? d.getDispositionStatus() : "Completed";
                    String leadName = d.getLeadName() != null ? d.getLeadName() : "Unknown Lead";
                    activity.put("message", "Disposition " + status + " for " + leadName);
                    activity.put("icon", "fa-check-circle");
                    activity.put("type", "disposition");
                    
                    String timeStr;
                    if (d.getCreatedAt() != null) {
                        timeStr = d.getCreatedAt().format(DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM);
                    } else if (d.getCallDate() != null) {
                        timeStr = d.getCallDate().format(DATE_FORMAT_DD_MM_YYYY);
                    } else {
                        timeStr = LocalDateTime.now().format(DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM);
                    }
                    activity.put("time", timeStr);
                    activities.add(activity);
                }
            }

            // Add lead activities if we have less than 5 activities
            if (activities.size() < 5) {
                var leads = leadRepository.findAll();
                System.out.println("📋 Found " + leads.size() + " leads for recent activity");
                
                if (!leads.isEmpty()) {
                    List<Lead> sortedLeads = new ArrayList<>(leads);
                    sortedLeads.sort((l1, l2) -> {
                        LocalDateTime d1 = l1.getCreatedAt() != null ? l1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime d2 = l2.getCreatedAt() != null ? l2.getCreatedAt() : LocalDateTime.MIN;
                        return d2.compareTo(d1);
                    });

                    int remaining = Math.min(5 - activities.size(), sortedLeads.size());
                    for (int i = 0; i < remaining; i++) {
                        var l = sortedLeads.get(i);
                        Map<String, Object> activity = new HashMap<>();
                        String status = l.getStatus() != null ? l.getStatus().toLowerCase() : "created";
                        String leadName = l.getName() != null ? l.getName() : "Unknown Lead";
                        activity.put("message", "Lead " + leadName + " was " + status);
                        activity.put("icon", "fa-user-plus");
                        activity.put("type", "lead");
                        
                        String timeStr;
                        if (l.getCreatedAt() != null) {
                            timeStr = l.getCreatedAt().format(DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM);
                        } else {
                            timeStr = LocalDateTime.now().format(DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM);
                        }
                        activity.put("time", timeStr);
                        activities.add(activity);
                    }
                }
            }

            // If still no activities, add a welcome message
            if (activities.isEmpty()) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("message", "Welcome to Admin Dashboard!");
                activity.put("icon", "fa-star");
                activity.put("type", "welcome");
                activity.put("time", LocalDateTime.now().format(DATE_TIME_FORMAT_DD_MM_YYYY_HH_MM));
                activities.add(activity);
            }

            System.out.println("✅ Returning " + activities.size() + " activities");
            return ResponseEntity.ok(activities);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching recent activity: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent activity: " + e.getMessage()));
        }
    }
}