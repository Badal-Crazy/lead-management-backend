package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.CsvUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final CsvUserRepository userRepository;

    public AuthController(CsvUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        System.out.println("📝 Signup attempt: " + user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
        }

        user.setPassword(user.getPassword());
        user.setApproved(false);
        user.setEnabled(false);
        user.setRole("AGENT");
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Registration successful. Waiting for admin approval.",
            "status", "PENDING_APPROVAL"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        System.out.println("========================================");
        System.out.println("🔐 LOGIN ATTEMPT");
        System.out.println("Username: '" + username + "'");
        System.out.println("Password: '" + password + "'");
        System.out.println("========================================");

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("❌ User NOT found: " + username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        System.out.println("✅ User found: " + user.getUsername());
        System.out.println("Role: " + user.getRole());
        System.out.println("Enabled: " + user.isEnabled());
        System.out.println("Approved: " + user.isApproved());
        System.out.println("Stored password: '" + user.getPassword() + "'");
        System.out.println("Provided password: '" + password + "'");

        if (!user.isEnabled()) {
            System.out.println("❌ User is disabled");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account is disabled"));
        }

        if (!user.isApproved()) {
            System.out.println("❌ User is not approved");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account pending approval"));
        }

        if (!user.getPassword().equals(password)) {
            System.out.println("❌ Password does NOT match!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        System.out.println("✅ Login SUCCESS!");
        System.out.println("========================================");

        user.setLastLogin(LocalDateTime.now());
        user.setLoggedIn(true);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("enabled", user.isEnabled());
        response.put("approved", user.isApproved());
        response.put("email", user.getEmail() != null ? user.getEmail() : "");
        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> request) {
        String username = request != null ? request.get("username") : null;
        
        if (username != null) {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLoggedIn(false);
                user.setLastLogout(LocalDateTime.now());
                userRepository.save(user);
            }
        }
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/force-logout")
    public ResponseEntity<?> forceLogout(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setLoggedIn(false);
        user.setLastLogout(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User logged out forcefully"));
    }

    @GetMapping("/session")
    public ResponseEntity<?> getSession(@RequestParam(required = false) String username) {
        // If no username provided, try to get from request
        if (username == null) {
            return ResponseEntity.ok(Map.of(
                "isLoggedIn", false,
                "message", "No active session"
            ));
        }
        
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "isLoggedIn", false,
                "message", "User not found"
            ));
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("isLoggedIn", user.isLoggedIn());
        response.put("lastLogin", user.getLastLogin());
        response.put("enabled", user.isEnabled());
        response.put("approved", user.isApproved());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken() {
        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return ResponseEntity.ok(Map.of("message", "Password reset link sent to " + email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
