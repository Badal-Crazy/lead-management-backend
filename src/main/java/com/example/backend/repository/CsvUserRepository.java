// src/main/java/com/example/backend/repository/CsvUserRepository.java
package com.example.backend.repository;

import com.example.backend.model.User;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CsvUserRepository {
    
    private static final String CSV_FILE = "users.csv";
    private static final String[] HEADER = {
        "id", "username", "password", "email", "phone", "role", 
        "enabled", "approved", "adminId", "teamId", 
        "createdAt", "updatedAt", "lastLogin", "lastLogout", "loggedIn", 
        "resetToken", "resetTokenExpiry"
    };
    
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    private final Map<String, User> usernameCache = new ConcurrentHashMap<>();
    private final Map<String, User> emailCache = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private volatile boolean isLoaded = false;

    public CsvUserRepository() {
        initializeAndLoad();
    }

    private void initializeAndLoad() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            createDefaultUsers();
        } else {
            // Check if the file has the new header format
            try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
                String[] header = reader.readNext();
                if (header != null && header.length > 0) {
                    // Check if this is the old format (no id column)
                    if (!"id".equals(header[0]) && !"id".equalsIgnoreCase(header[0].replace("\"", ""))) {
                        System.out.println("🔄 Old CSV format detected. Migrating to new format...");
                        migrateOldFormat();
                    } else {
                        System.out.println("✅ CSV file already in new format");
                    }
                }
            } catch (IOException | CsvException e) {
                System.err.println("❌ Error reading CSV header: " + e.getMessage());
            }
        }
        loadCache();
    }

    private void createDefaultUsers() {
        System.out.println("📝 Creating users.csv with default users...");
        List<User> defaultUsers = new ArrayList<>();
        
        User admin = new User("admin", "admin123", "admin@example.com", "ADMIN");
        admin.setApproved(true);
        admin.setEnabled(true);
        admin.setId("1");
        defaultUsers.add(admin);
        
        User agent = new User("agent", "agent123", "agent@example.com", "AGENT");
        agent.setApproved(true);
        agent.setEnabled(true);
        agent.setId("2");
        defaultUsers.add(agent);
        
        User superAdmin = new User("superadmin", "super123", "super@admin.com", "SUPER_ADMIN");
        superAdmin.setApproved(true);
        superAdmin.setEnabled(true);
        superAdmin.setId("3");
        defaultUsers.add(superAdmin);
        
        saveAll(defaultUsers);
        System.out.println("✅ Default users created successfully!");
    }

    private void migrateOldFormat() {
        try {
            List<User> existingUsers = loadOldFormatUsers();
            if (!existingUsers.isEmpty()) {
                System.out.println("📊 Found " + existingUsers.size() + " users in old format. Migrating...");
                // Assign IDs to users without IDs
                long idCounter = 1;
                for (User user : existingUsers) {
                    if (user.getId() == null || user.getId().isEmpty()) {
                        user.setId(String.valueOf(idCounter++));
                    }
                    if (user.getCreatedAt() == null) {
                        user.setCreatedAt(LocalDateTime.now());
                    }
                    if (user.getUpdatedAt() == null) {
                        user.setUpdatedAt(LocalDateTime.now());
                    }
                }
                // Backup old file
                File oldFile = new File(CSV_FILE);
                File backupFile = new File("users_backup.csv");
                if (oldFile.renameTo(backupFile)) {
                    System.out.println("📦 Old users.csv backed up to users_backup.csv");
                }
                // Save in new format
                saveAll(existingUsers);
                System.out.println("✅ Migration complete! " + existingUsers.size() + " users migrated.");
            }
        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<User> loadOldFormatUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(CSV_FILE);
        if (!file.exists()) return users;
        
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> records = reader.readAll();
            if (records.isEmpty() || records.size() <= 1) return users;
            
            // Read header to determine column mapping
            String[] header = records.get(0);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                String col = header[i].replace("\"", "").trim().toLowerCase();
                columnIndex.put(col, i);
            }
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                try {
                    User user = new User();
                    
                    // Map columns based on header
                    if (columnIndex.containsKey("username")) {
                        user.setUsername(record[columnIndex.get("username")].replace("\"", "").trim());
                    }
                    if (columnIndex.containsKey("password")) {
                        user.setPassword(record[columnIndex.get("password")].replace("\"", "").trim());
                    }
                    if (columnIndex.containsKey("email")) {
                        user.setEmail(record[columnIndex.get("email")].replace("\"", "").trim());
                    }
                    if (columnIndex.containsKey("phone")) {
                        user.setPhone(record[columnIndex.get("phone")].replace("\"", "").trim());
                    }
                    if (columnIndex.containsKey("role")) {
                        String role = record[columnIndex.get("role")].replace("\"", "").trim();
                        // Handle ROLE_ prefix
                        if (role.startsWith("ROLE_")) {
                            role = role.substring(5);
                        }
                        user.setRole(role);
                    }
                    if (columnIndex.containsKey("enabled")) {
                        user.setEnabled(Boolean.parseBoolean(record[columnIndex.get("enabled")].replace("\"", "").trim()));
                    }
                    if (columnIndex.containsKey("approved")) {
                        user.setApproved(Boolean.parseBoolean(record[columnIndex.get("approved")].replace("\"", "").trim()));
                    }
                    
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    users.add(user);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing row " + i + ": " + e.getMessage());
                }
            }
        } catch (IOException | CsvException e) {
            System.err.println("❌ Error reading old CSV: " + e.getMessage());
        }
        return users;
    }

    private void loadCache() {
        synchronized (this) {
            try {
                List<User> users = loadAllUsers();
                userCache.clear();
                usernameCache.clear();
                emailCache.clear();
                
                for (User user : users) {
                    if (user.getId() != null) {
                        userCache.put(user.getId(), user);
                    }
                    if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                        usernameCache.put(user.getUsername().toLowerCase(), user);
                    }
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailCache.put(user.getEmail().toLowerCase(), user);
                    }
                }
                isLoaded = true;
                System.out.println("✅ Loaded " + users.size() + " users into cache");
                System.out.println("📊 Users: " + users.stream().map(User::getUsername).toList());
            } catch (Exception e) {
                System.err.println("❌ Failed to load user cache: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<User> loadAllUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            System.out.println("⚠️ users.csv not found!");
            return users;
        }
        
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> records = reader.readAll();
            System.out.println("📊 Found " + records.size() + " records in users.csv (including header)");
            
            if (records.isEmpty() || records.size() <= 1) {
                System.out.println("⚠️ No user data found in users.csv");
                return users;
            }
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length < 7) {
                    System.out.println("⚠️ Skipping row " + i + " - insufficient columns: " + record.length);
                    continue;
                }
                
                try {
                    User user = new User();
                    
                    // Handle both old and new format
                    int idx = 0;
                    // If first column is id (numeric), it's the new format
                    boolean isNewFormat = record[0].trim().matches("\\d+");
                    
                    if (isNewFormat && record.length >= 16) {
                        // New format with all columns
                        user.setId(record[0].trim());
                        user.setUsername(record[1].trim());
                        user.setPassword(record[2].trim());
                        user.setEmail(record[3].trim());
                        user.setPhone(record[4].trim());
                        String role = record[5].trim();
                        if (role.startsWith("ROLE_")) {
                            role = role.substring(5);
                        }
                        user.setRole(role);
                        user.setEnabled(Boolean.parseBoolean(record[6].trim()));
                        user.setApproved(Boolean.parseBoolean(record[7].trim()));
                        if (record.length > 8) user.setAdminId(record[8].trim());
                        if (record.length > 9) user.setTeamId(record[9].trim());
                        if (record.length > 10 && !record[10].isEmpty()) {
                            user.setCreatedAt(LocalDateTime.parse(record[10].trim()));
                        }
                        if (record.length > 11 && !record[11].isEmpty()) {
                            user.setUpdatedAt(LocalDateTime.parse(record[11].trim()));
                        }
                    } else {
                        // Old format: username,password,email,phone,role,enabled,approved
                        user.setUsername(record[0].trim());
                        user.setPassword(record[1].trim());
                        user.setEmail(record[2].trim());
                        user.setPhone(record[3].trim());
                        String role = record[4].trim();
                        if (role.startsWith("ROLE_")) {
                            role = role.substring(5);
                        }
                        user.setRole(role);
                        user.setEnabled(Boolean.parseBoolean(record[5].trim()));
                        user.setApproved(Boolean.parseBoolean(record[6].trim()));
                        user.setCreatedAt(LocalDateTime.now());
                        user.setUpdatedAt(LocalDateTime.now());
                        // Generate ID
                        user.setId(String.valueOf(idGenerator.getAndIncrement()));
                    }
                    
                    users.add(user);
                    System.out.println("✅ Loaded user: " + user.getUsername() + " (Role: " + user.getRole() + ")");
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing user row " + i + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException | CsvException e) {
            System.err.println("❌ Error reading users.csv: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("📊 Total users loaded: " + users.size());
        return users;
    }

    public List<User> findAll() {
        if (!isLoaded) {
            loadCache();
        }
        List<User> users = new ArrayList<>(userCache.values());
        System.out.println("📊 findAll() returning " + users.size() + " users");
        return users;
    }

    public Optional<User> findById(String id) {
        if (!isLoaded) {
            loadCache();
        }
        return Optional.ofNullable(userCache.get(id));
    }

    public Optional<User> findByUsername(String username) {
        if (!isLoaded) {
            loadCache();
        }
        if (username == null) return Optional.empty();
        User user = usernameCache.get(username.toLowerCase());
        System.out.println("🔍 findByUsername: " + username + " -> " + (user != null ? user.getUsername() : "not found"));
        return Optional.ofNullable(user);
    }

    public Optional<User> findByEmail(String email) {
        if (!isLoaded) {
            loadCache();
        }
        if (email == null) return Optional.empty();
        return Optional.ofNullable(emailCache.get(email.toLowerCase()));
    }

    public List<User> findByRole(String role) {
        if (!isLoaded) {
            loadCache();
        }
        String roleUpper = role.toUpperCase();
        if (!roleUpper.startsWith("ROLE_")) {
            roleUpper = "ROLE_" + roleUpper;
        }
        final String finalRole = roleUpper;
        return userCache.values().stream()
                .filter(u -> finalRole.equalsIgnoreCase(u.getRole()) || 
                            role.equalsIgnoreCase(u.getRole()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<User> findLoggedInUsers() {
        if (!isLoaded) {
            loadCache();
        }
        return userCache.values().stream()
                .filter(User::isLoggedIn)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public synchronized User save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(String.valueOf(idGenerator.getAndIncrement()));
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        
        // Update caches
        if (user.getId() != null) {
            userCache.put(user.getId(), user);
        }
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            usernameCache.put(user.getUsername().toLowerCase(), user);
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            emailCache.put(user.getEmail().toLowerCase(), user);
        }
        
        saveAll(new ArrayList<>(userCache.values()));
        System.out.println("✅ Saved user: " + user.getUsername() + " (Role: " + user.getRole() + ")");
        return user;
    }

    public synchronized void saveAll(List<User> users) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeNext(HEADER);
            for (User user : users) {
                writer.writeNext(new String[]{
                    user.getId() != null ? user.getId() : "",
                    user.getUsername() != null ? user.getUsername() : "",
                    user.getPassword() != null ? user.getPassword() : "",
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getPhone() != null ? user.getPhone() : "",
                    user.getRole() != null ? user.getRole() : "",
                    String.valueOf(user.isEnabled()),
                    String.valueOf(user.isApproved()),
                    user.getAdminId() != null ? user.getAdminId() : "",
                    user.getTeamId() != null ? user.getTeamId() : "",
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString(),
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : LocalDateTime.now().toString(),
                    user.getLastLogin() != null ? user.getLastLogin().toString() : "",
                    user.getLastLogout() != null ? user.getLastLogout().toString() : "",
                    String.valueOf(user.isLoggedIn()),
                    user.getResetToken() != null ? user.getResetToken() : "",
                    user.getResetTokenExpiry() != null ? user.getResetTokenExpiry().toString() : ""
                });
            }
            System.out.println("✅ Saved " + users.size() + " users to CSV");
        } catch (IOException e) {
            System.err.println("❌ Error saving users.csv: " + e.getMessage());
            throw new RuntimeException("Failed to save users", e);
        }
    }

    public void deleteById(String id) {
        if (id == null) return;
        User user = userCache.remove(id);
        if (user != null) {
            if (user.getUsername() != null) {
                usernameCache.remove(user.getUsername().toLowerCase());
            }
            if (user.getEmail() != null) {
                emailCache.remove(user.getEmail().toLowerCase());
            }
            saveAll(new ArrayList<>(userCache.values()));
        }
    }

    public void deleteByUsername(String username) {
        if (username == null) return;
        User user = usernameCache.remove(username.toLowerCase());
        if (user != null) {
            if (user.getId() != null) {
                userCache.remove(user.getId());
            }
            if (user.getEmail() != null) {
                emailCache.remove(user.getEmail().toLowerCase());
            }
            saveAll(new ArrayList<>(userCache.values()));
        }
    }

    public void updateLastLogin(String username) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            user.setLoggedIn(true);
            save(user);
        }
    }

    public void updateLogout(String username) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogout(LocalDateTime.now());
            user.setLoggedIn(false);
            save(user);
        }
    }
}