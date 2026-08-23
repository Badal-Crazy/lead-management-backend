package com.example.backend.repository;

import com.example.backend.model.User;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CsvUserRepository {
    
    private static final String CSV_FILE = "users.csv";
    private static final String[] HEADER = {"username", "password", "email", "phone", "role", "enabled", "approved"};

    public CsvUserRepository() {
        initializeCsvFile();
    }

    private void initializeCsvFile() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                writer.writeNext(HEADER);
                
                // Super Admin
                writer.writeNext(new String[]{"superadmin", "super123", "super@admin.com", "9999999999", "ROLE_SUPER_ADMIN", "true", "true"});
                // Admin
                writer.writeNext(new String[]{"admin", "admin123", "admin@example.com", "8888888888", "ROLE_ADMIN", "true", "true"});
                // Agent
                writer.writeNext(new String[]{"agent", "agent123", "agent@example.com", "7777777777", "ROLE_AGENT", "true", "true"});
                
                System.out.println("✅ users.csv created with default users!");
                System.out.println("Super Admin: superadmin / super123");
                System.out.println("Admin: admin / admin123");
                System.out.println("Agent: agent / agent123");
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize CSV file", e);
            }
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> records = reader.readAll();
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length >= 7) {
                    User user = new User();
                    user.setUsername(record[0].trim());
                    user.setPassword(record[1].trim());
                    user.setEmail(record[2].trim());
                    user.setPhone(record[3].trim());
                    user.setRole(record[4].trim());
                    user.setEnabled(Boolean.parseBoolean(record[5].trim()));
                    user.setApproved(Boolean.parseBoolean(record[6].trim()));
                    users.add(user);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
        return users;
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public void save(User user) {
        List<User> users = findAll();
        users.removeIf(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));
        users.add(user);
        saveAll(users);
    }

    public void saveAll(List<User> users) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeNext(HEADER);
            for (User user : users) {
                writer.writeNext(new String[]{
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getPhone() != null ? user.getPhone() : "",
                    user.getRole(),
                    String.valueOf(user.isEnabled()),
                    String.valueOf(user.isApproved())
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save to CSV file", e);
        }
    }

    public void deleteByUsername(String username) {
        List<User> users = findAll();
        users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        saveAll(users);
    }
}
