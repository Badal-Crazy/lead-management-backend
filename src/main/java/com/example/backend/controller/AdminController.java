package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.CsvUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final CsvUserRepository userRepository;

    public AdminController(CsvUserRepository userRepository) {
        this.userRepository = userRepository;
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
            if ("ROLE_SUPER_ADMIN".equals(userRole)) {
                if (username.equals("superadmin")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Cannot delete Super Admin"));
                }
                // Super Admin can delete anyone else
                userRepository.deleteByUsername(username);
                System.out.println("✅ Super Admin deleted user: " + username);
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            }

            // Admin can only delete AGENTS
            if ("ROLE_ADMIN".equals(userRole)) {
                if (!"ROLE_AGENT".equals(targetRole)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Admins can only delete agents"));
                }
                userRepository.deleteByUsername(username);
                System.out.println("✅ Admin deleted agent: " + username);
                return ResponseEntity.ok(Map.of("message", "Agent deleted successfully"));
            }

            // Agents cannot delete anyone
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
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", users.size());
            stats.put("pendingApprovals", users.stream().filter(u -> !u.isApproved()).count());
            stats.put("activeUsers", users.stream().filter(u -> u.isEnabled()).count());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent activity"));
        }
    }
}
