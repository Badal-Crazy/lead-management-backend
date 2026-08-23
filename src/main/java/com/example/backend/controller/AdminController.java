package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.model.Lead;
import com.example.backend.model.Disposition;
import com.example.backend.repository.CsvUserRepository;
import com.example.backend.repository.CsvLeadRepository;
import com.example.backend.repository.CsvDispositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final CsvUserRepository userRepository;
    private final CsvLeadRepository leadRepository;
    private final CsvDispositionRepository dispositionRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(CsvUserRepository userRepository,
                          CsvLeadRepository leadRepository,
                          CsvDispositionRepository dispositionRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.dispositionRepository = dispositionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            List<User> users = userRepository.findAll();
            List<Lead> leads = leadRepository.findAll();
            List<Disposition> dispositions = dispositionRepository.findAll();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", users.size());
            stats.put("totalAdmins", users.stream().filter(u -> u.getRole().equals("ROLE_ADMIN")).count());
            stats.put("totalAgents", users.stream().filter(u -> u.getRole().equals("ROLE_AGENT")).count());
            stats.put("totalLeads", leads.size());
            stats.put("pendingLeads", leads.stream().filter(l -> "Pending".equalsIgnoreCase(l.getStatus())).count());
            stats.put("disposedLeads", leads.stream().filter(l -> "Disposed".equalsIgnoreCase(l.getStatus())).count());
            stats.put("totalDispositions", dispositions.size());
            stats.put("pendingApprovals", users.stream().filter(u -> !u.isApproved()).count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();
            List<Disposition> dispositions = dispositionRepository.findAll();
            
            int count = Math.min(5, dispositions.size());
            for (int i = dispositions.size() - 1; i >= dispositions.size() - count && i >= 0; i--) {
                Disposition d = dispositions.get(i);
                Map<String, Object> activity = new HashMap<>();
                activity.put("message", "Disposition completed for " + d.getLeadName());
                activity.put("icon", "fa-check-circle");
                activity.put("time", d.getCreatedAt() != null ? d.getCreatedAt().toString() : new Date().toString());
                activities.add(activity);
            }

            if (activities.isEmpty()) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("message", "Welcome to the Admin Dashboard!");
                activity.put("icon", "fa-star");
                activity.put("time", new Date().toString());
                activities.add(activity);
            }

            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent activity: " + e.getMessage()));
        }
    }

    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        try {
            List<User> pendingUsers = userRepository.findAll().stream()
                    .filter(u -> !u.isApproved())
                    .toList();
            return ResponseEntity.ok(pendingUsers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch pending users"));
        }
    }

    @PostMapping("/approve-user/{username}")
    public ResponseEntity<?> approveUser(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            String action = request.get("action");

            if ("approve".equals(action)) {
                user.setApproved(true);
                user.setEnabled(true);
            } else {
                userRepository.deleteByUsername(username);
                return ResponseEntity.ok(Map.of("message", "User rejected and deleted"));
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to approve user: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            users.forEach(u -> u.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users"));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setApproved(true);
            user.setEnabled(true);
            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User created successfully"));
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

    @PutMapping("/users/{username}/password")
    public ResponseEntity<?> updatePassword(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            // Super Admin can change any user's password without old password
            user.setPassword(passwordEncoder.encode(request.get("password")));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update password: " + e.getMessage()));
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

    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            if (username.equals("superadmin")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete super admin"));
            }
            userRepository.deleteByUsername(username);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }
}
