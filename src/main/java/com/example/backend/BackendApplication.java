package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.backend")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 Lead Management System Started!");
        System.out.println("========================================");
        System.out.println("📊 Default Users:");
        System.out.println("   Super Admin: admin / admin123");
        System.out.println("   Admin: admin2 / admin123");
        System.out.println("   Agent: agent / agent123");
        System.out.println("========================================");
        System.out.println("📁 CSV Files Created:");
        System.out.println("   - users.csv");
        System.out.println("   - leads.csv");
        System.out.println("   - dispositions.csv");
        System.out.println("========================================");
        System.out.println("🌐 API Endpoints:");
        System.out.println("   - http://localhost:8080/api/auth/login");
        System.out.println("   - http://localhost:8080/api/auth/register");
        System.out.println("   - http://localhost:8080/api/leads");
        System.out.println("   - http://localhost:8080/api/dispositions");
        System.out.println("   - http://localhost:8080/api/admin/stats");
        System.out.println("========================================");
    }
}