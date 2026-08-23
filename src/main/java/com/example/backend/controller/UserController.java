package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.CsvUserRepository;
import com.example.backend.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final CsvUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserController(CsvUserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        User currentUser = userOpt.get();
        List<User> users;

        // Admin sees users under them (agents)
        if (currentUser.getRole().equals("ADMIN")) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getAdminId() != null && u.getAdminId().equals(currentUser.getId()))
                    .toList();
        } else if (currentUser.getRole().equals("AGENT")) {
            // Agent sees only themselves
            users = List.of(currentUser);
        } else {
            // Super Admin sees all
            users = userRepository.findAll();
        }

        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getRole().equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestHeader("Authorization") String authHeader, @RequestBody User user) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        var currentUserOpt = userRepository.findByUsername(username);
        if (currentUserOpt.isEmpty() || !currentUserOpt.get().getRole().equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setApproved(true);
        user.setEnabled(true);
        userRepository.save(user);

        user.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody User user) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existing = userOpt.get();
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getRole() != null) existing.setRole(user.getRole());
        if (user.getTeamId() != null) existing.setTeamId(user.getTeamId());
        if (user.getAdminId() != null) existing.setAdminId(user.getAdminId());

        userRepository.save(existing);
        existing.setPassword(null);
        return ResponseEntity.ok(existing);
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable String userId) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setApproved(true);
        user.setEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User approved successfully"));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable String userId) {
        userRepository.deleteByUsername(userId);
        return ResponseEntity.ok(Map.of("message", "User rejected and deleted"));
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String userId) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String userId) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setEnabled(false);
        user.setLoggedIn(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }

    @GetMapping("/{userId}/login-status")
    public ResponseEntity<?> getLoginStatus(@PathVariable String userId) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "isLoggedIn", user.isLoggedIn(),
            "lastLogin", user.getLastLogin(),
            "lastLogout", user.getLastLogout()
        ));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId.equals("superadmin")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete super admin"));
        }
        userRepository.deleteByUsername(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable String userId, @RequestBody Map<String, String> request) {
        var userOpt = userRepository.findByUsername(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRole(request.get("role"));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User role updated successfully"));
    }

    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractUsername(token);
    }
}
