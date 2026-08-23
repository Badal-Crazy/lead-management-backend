package com.example.backend.controller;

import com.example.backend.repository.CsvDispositionRepository;
import com.example.backend.repository.CsvLeadRepository;
import com.example.backend.repository.CsvUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {

    private final CsvLeadRepository leadRepository;
    private final CsvDispositionRepository dispositionRepository;
    private final CsvUserRepository userRepository;

    public DashboardController(CsvLeadRepository leadRepository,
                               CsvDispositionRepository dispositionRepository,
                               CsvUserRepository userRepository) {
        this.leadRepository = leadRepository;
        this.dispositionRepository = dispositionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            var leads = leadRepository.findAll();
            var dispositions = dispositionRepository.findAll();
            var users = userRepository.findAll();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLeads", leads.size());
            stats.put("pendingLeads", leads.stream().filter(l -> "Pending".equalsIgnoreCase(l.getStatus())).count());
            stats.put("disposedLeads", leads.stream().filter(l -> "Disposed".equalsIgnoreCase(l.getStatus())).count());
            stats.put("totalAgents", users.stream().filter(u -> "ROLE_AGENT".equals(u.getRole())).count());
            stats.put("totalDispositions", dispositions.size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();
            var dispositions = dispositionRepository.findAll();
            
            int count = Math.min(5, dispositions.size());
            for (int i = dispositions.size() - 1; i >= dispositions.size() - count && i >= 0; i--) {
                var d = dispositions.get(i);
                Map<String, Object> activity = new HashMap<>();
                activity.put("message", "Disposition completed for " + d.getLeadName());
                activity.put("icon", "fa-check-circle");
                activity.put("time", d.getCreatedAt() != null ? d.getCreatedAt().toString() : new Date().toString());
                activities.add(activity);
            }

            if (activities.isEmpty()) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("message", "Welcome to your Dashboard!");
                activity.put("icon", "fa-star");
                activity.put("time", new Date().toString());
                activities.add(activity);
            }

            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch recent activity: " + e.getMessage()));
        }
    }
}
