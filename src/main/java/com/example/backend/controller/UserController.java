package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.CsvUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final CsvUserRepository userRepository;

    public UserController(CsvUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = userRepository.findAll();
            users.forEach(u -> u.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            users.forEach(u -> u.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch all users: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        try {
            var userOpt = userRepository.findByUsername(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable String userId) {
        try {
            var userOpt = userRepository.findByUsername(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            user.setApproved(true);
            user.setEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to approve user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable String userId) {
        try {
            userRepository.deleteByUsername(userId);
            return ResponseEntity.ok(Map.of("message", "User rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reject user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String userId) {
        try {
            var userOpt = userRepository.findByUsername(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            user.setEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to activate user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String userId) {
        try {
            var userOpt = userRepository.findByUsername(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            user.setEnabled(false);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate user: " + e.getMessage()));
        }
    }
}
