package com.example.backend.controller;

import com.example.backend.model.Lead;
import com.example.backend.model.Disposition;
import com.example.backend.model.User;
import com.example.backend.repository.CsvDispositionRepository;
import com.example.backend.repository.CsvLeadRepository;
import com.example.backend.repository.CsvUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "${cors.allowed.origins}")
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

    @GetMapping("/agent")
    public ResponseEntity<?> getAgentDashboard(@RequestParam(required = false) String agentId) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get all leads and dispositions
            List<Lead> allLeads = leadRepository.findAll();
            List<Disposition> allDispositions = dispositionRepository.findAll();
            
            // Filter leads for this agent if agentId is provided
            List<Lead> agentLeads = allLeads;
            if (agentId != null && !agentId.isEmpty()) {
                agentLeads = allLeads.stream()
                    .filter(l -> agentId.equals(l.getAssignedTo()))
                    .collect(Collectors.toList());
            }

            // Total Leads
            response.put("totalLeads", agentLeads.size());

            // Total Dispositions - filter by disposedBy (agent username/id)
            long totalDispositions = allDispositions.stream()
                .filter(d -> agentId == null || agentId.isEmpty() || agentId.equals(d.getDisposedBy()))
                .count();
            response.put("totalDispositions", totalDispositions);

            // Total PTP (Promise to Pay) - leads with PTP status
            long totalPTP = agentLeads.stream()
                .filter(l -> "PTP".equalsIgnoreCase(l.getStatus()))
                .count();
            response.put("totalPTP", totalPTP);

            // Total Collection - sum of payment amounts from dispositions
            double totalCollection = allDispositions.stream()
                .filter(d -> agentId == null || agentId.isEmpty() || agentId.equals(d.getDisposedBy()))
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            response.put("totalCollection", totalCollection);

            // Today's date
            LocalDate today = LocalDate.now();

            // Today's PTP
            List<Lead> todayPTPLeads = agentLeads.stream()
                .filter(l -> "PTP".equalsIgnoreCase(l.getStatus()))
                .filter(l -> {
                    LocalDateTime ptpDateTime = parsePtpDate(l);
                    if (ptpDateTime != null) {
                        return ptpDateTime.toLocalDate().equals(today);
                    }
                    return false;
                })
                .collect(Collectors.toList());
            response.put("todayPTP", todayPTPLeads.size());

            // Today's PTP Leads with details
            List<Map<String, Object>> ptpLeadsDetails = todayPTPLeads.stream()
                .map(lead -> {
                    Map<String, Object> leadDetails = new HashMap<>();
                    leadDetails.put("id", lead.getId());
                    leadDetails.put("name", lead.getName());
                    leadDetails.put("phoneNumber", lead.getPhoneNumber());
                    // Use amountToPitch or os as the amount
                    Double amount = lead.getAmountToPitch() != null ? lead.getAmountToPitch() : 
                                   (lead.getOs() != null ? lead.getOs() : 0.0);
                    leadDetails.put("amount", amount);
                    leadDetails.put("customerDetails", getCustomerDetails(lead));
                    return leadDetails;
                })
                .collect(Collectors.toList());
            response.put("todayPTPLeads", ptpLeadsDetails);

            // Today's Collection - filter by callDate
            List<Disposition> todayDispositions = allDispositions.stream()
                .filter(d -> agentId == null || agentId.isEmpty() || agentId.equals(d.getDisposedBy()))
                .filter(d -> d.getCallDate() != null && d.getCallDate().equals(today))
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .collect(Collectors.toList());
            double todayCollection = todayDispositions.stream()
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            response.put("todayCollection", todayCollection);

            // Today's Collection Leads
            List<Map<String, Object>> collectionLeadsDetails = todayDispositions.stream()
                .map(disposition -> {
                    Map<String, Object> leadDetails = new HashMap<>();
                    leadDetails.put("id", disposition.getLeadId());
                    leadDetails.put("name", disposition.getLeadName());
                    leadDetails.put("phoneNumber", disposition.getLeadPhone());
                    leadDetails.put("amount", disposition.getPaymentAmount());
                    leadDetails.put("customerDetails", "Payment collected on " + 
                        (disposition.getCallDate() != null ? 
                            disposition.getCallDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : 
                            "N/A"));
                    return leadDetails;
                })
                .collect(Collectors.toList());
            response.put("todayCollectionLeads", collectionLeadsDetails);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch agent dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminDashboard() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            List<User> allUsers = userRepository.findAll();
            List<Lead> allLeads = leadRepository.findAll();
            List<Disposition> allDispositions = dispositionRepository.findAll();

            // Total Agents
            List<User> agents = allUsers.stream()
                .filter(u -> "AGENT".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
            response.put("totalAgents", agents.size());

            // Active/Inactive Agents
            long activeAgents = agents.stream()
                .filter(User::isEnabled)
                .count();
            long inactiveAgents = agents.size() - activeAgents;
            response.put("activeAgents", activeAgents);
            response.put("inactiveAgents", inactiveAgents);

            // Total Dispositions
            response.put("totalDispositions", allDispositions.size());

            // Today's Dispositions
            LocalDate today = LocalDate.now();
            long todayDispositions = allDispositions.stream()
                .filter(d -> d.getCallDate() != null && d.getCallDate().equals(today))
                .count();
            response.put("todayDispositions", todayDispositions);

            // Total Collection - sum of payment amounts from Paid/Part Paid dispositions
            double totalCollection = allDispositions.stream()
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            response.put("totalCollection", totalCollection);

            // Today's Collection
            double todayCollection = allDispositions.stream()
                .filter(d -> d.getCallDate() != null && d.getCallDate().equals(today))
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            response.put("todayCollection", todayCollection);

            // Agent Wise Data
            List<Map<String, Object>> agentWiseData = agents.stream()
                .map(agent -> {
                    Map<String, Object> agentData = new HashMap<>();
                    agentData.put("id", agent.getId());
                    agentData.put("name", agent.getUsername());
                    agentData.put("email", agent.getEmail());
                    
                    List<Lead> agentLeads = allLeads.stream()
                        .filter(l -> {
                            String assignedTo = l.getAssignedTo();
                            return assignedTo != null && assignedTo.equals(agent.getId());
                        })
                        .collect(Collectors.toList());
                    agentData.put("totalLeads", agentLeads.size());
                    
                    long agentDispositions = allDispositions.stream()
                        .filter(d -> {
                            String disposedBy = d.getDisposedBy();
                            return disposedBy != null && disposedBy.equals(agent.getId());
                        })
                        .count();
                    agentData.put("dispositions", agentDispositions);
                    
                    long agentPTP = agentLeads.stream()
                        .filter(l -> "PTP".equalsIgnoreCase(l.getStatus()))
                        .count();
                    agentData.put("ptp", agentPTP);
                    
                    double agentCollection = allDispositions.stream()
                        .filter(d -> {
                            String disposedBy = d.getDisposedBy();
                            return disposedBy != null && disposedBy.equals(agent.getId());
                        })
                        .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                        .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                        .sum();
                    agentData.put("collection", agentCollection);
                    
                    agentData.put("isActive", agent.isEnabled());
                    agentData.put("isApproved", agent.isApproved());
                    
                    return agentData;
                })
                .collect(Collectors.toList());
            response.put("agentWiseData", agentWiseData);

            // Upcoming PTP/Stocks
            List<Map<String, Object>> upcomingPTP = allLeads.stream()
                .filter(l -> "PTP".equalsIgnoreCase(l.getStatus()))
                .filter(l -> {
                    LocalDateTime ptpDateTime = parsePtpDate(l);
                    return ptpDateTime != null && ptpDateTime.toLocalDate().isAfter(today);
                })
                .sorted((l1, l2) -> {
                    LocalDateTime d1 = parsePtpDate(l1);
                    LocalDateTime d2 = parsePtpDate(l2);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2);
                })
                .limit(10)
                .map(lead -> {
                    Map<String, Object> ptp = new HashMap<>();
                    ptp.put("customerName", lead.getName());
                    ptp.put("phoneNumber", lead.getPhoneNumber());
                    Double amount = lead.getAmountToPitch() != null ? lead.getAmountToPitch() : 
                                   (lead.getOs() != null ? lead.getOs() : 0.0);
                    ptp.put("amount", amount);
                    LocalDateTime ptpDateTime = parsePtpDate(lead);
                    ptp.put("date", ptpDateTime != null ? ptpDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "N/A");
                    ptp.put("notes", "PTP scheduled for " + lead.getName());
                    return ptp;
                })
                .collect(Collectors.toList());
            response.put("upcomingPTP", upcomingPTP);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch admin dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            var leads = leadRepository.findAll();
            var dispositions = dispositionRepository.findAll();
            var users = userRepository.findAll();

            Map<String, Object> stats = new HashMap<>();
            
            // User stats
            stats.put("totalUsers", users.size());
            stats.put("totalAgents", users.stream().filter(u -> "AGENT".equalsIgnoreCase(u.getRole())).count());
            stats.put("totalAdmins", users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count());
            
            // Lead stats
            stats.put("totalLeads", leads.size());
            stats.put("pendingLeads", leads.stream().filter(l -> "Pending".equalsIgnoreCase(l.getStatus())).count());
            stats.put("disposedLeads", leads.stream().filter(l -> "Disposed".equalsIgnoreCase(l.getStatus())).count());
            stats.put("ptpLeads", leads.stream().filter(l -> "PTP".equalsIgnoreCase(l.getStatus())).count());
            
            // Disposition stats
            stats.put("totalDispositions", dispositions.size());
            
            // Calculate pending approvals (users not approved)
            long pendingApprovals = users.stream()
                .filter(u -> !u.isApproved())
                .count();
            stats.put("pendingApprovals", pendingApprovals);

            // Calculate total collection from dispositions
            double totalCollection = dispositions.stream()
                .filter(d -> "Paid".equalsIgnoreCase(d.getDispositionStatus()) || "Part Paid".equalsIgnoreCase(d.getDispositionStatus()))
                .mapToDouble(d -> d.getPaymentAmount() != null ? d.getPaymentAmount() : 0.0)
                .sum();
            stats.put("totalCollection", totalCollection);

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
            
            // Add dispositions as activities
            var dispositions = dispositionRepository.findAll();
            int count = Math.min(5, dispositions.size());
            for (int i = dispositions.size() - 1; i >= dispositions.size() - count && i >= 0; i--) {
                var d = dispositions.get(i);
                Map<String, Object> activity = new HashMap<>();
                String status = d.getDispositionStatus() != null ? d.getDispositionStatus() : "completed";
                activity.put("message", "Disposition " + status + " for " + d.getLeadName());
                activity.put("icon", "fa-check-circle");
                activity.put("time", d.getCreatedAt() != null ? 
                    d.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : 
                    new Date().toString());
                activities.add(activity);
            }

            // Add lead activities
            var leads = leadRepository.findAll();
            int leadCount = Math.min(3, leads.size());
            for (int i = leads.size() - 1; i >= leads.size() - leadCount && i >= 0; i--) {
                var l = leads.get(i);
                Map<String, Object> activity = new HashMap<>();
                String status = l.getStatus() != null ? l.getStatus().toLowerCase() : "created";
                activity.put("message", "Lead " + l.getName() + " was " + status);
                activity.put("icon", "fa-user-plus");
                activity.put("time", l.getCreatedAt() != null ? 
                    l.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : 
                    new Date().toString());
                activities.add(activity);
            }

            // Sort by time (most recent first)
            activities.sort((a, b) -> {
                String timeA = a.get("time").toString();
                String timeB = b.get("time").toString();
                return timeB.compareTo(timeA);
            });

            // Limit to 5 activities
            if (activities.size() > 5) {
                activities = activities.subList(0, 5);
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

    // Helper method to parse PTP date from Lead object
    private LocalDateTime parsePtpDate(Lead lead) {
        try {
            String ptpDateStr = null;
            
            // Try to get ptpDate using getter
            try {
                java.lang.reflect.Method method = lead.getClass().getMethod("getPtpDate");
                Object result = method.invoke(lead);
                if (result != null) {
                    ptpDateStr = result.toString();
                }
            } catch (Exception e) {
                // Try to access field directly
                try {
                    java.lang.reflect.Field field = lead.getClass().getDeclaredField("ptpDate");
                    field.setAccessible(true);
                    Object result = field.get(lead);
                    if (result != null) {
                        ptpDateStr = result.toString();
                    }
                } catch (Exception ex) {
                    // No ptpDate field, return null
                    return null;
                }
            }
            
            if (ptpDateStr == null || ptpDateStr.isEmpty()) {
                return null;
            }
            
            // Try to parse with various formats
            try {
                return LocalDateTime.parse(ptpDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse(ptpDateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ex) {
                    try {
                        return LocalDateTime.parse(ptpDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception exc) {
                        try {
                            return LocalDateTime.parse(ptpDateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                        } catch (Exception excc) {
                            try {
                                LocalDate date = LocalDate.parse(ptpDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                return date.atStartOfDay();
                            } catch (Exception exccc) {
                                try {
                                    LocalDate date = LocalDate.parse(ptpDateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                    return date.atStartOfDay();
                                } catch (Exception excccc) {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    // Helper method to get customer details
    private String getCustomerDetails(Lead lead) {
        StringBuilder details = new StringBuilder();
        try {
            // Get phone number
            if (lead.getPhoneNumber() != null && !lead.getPhoneNumber().isEmpty()) {
                details.append("Phone: ").append(lead.getPhoneNumber()).append(", ");
            }
            
            // Get agreement number
            if (lead.getAgreementNumber() != null && !lead.getAgreementNumber().isEmpty()) {
                details.append("Agreement: ").append(lead.getAgreementNumber()).append(", ");
            }
            
            // Get status
            if (lead.getStatus() != null && !lead.getStatus().isEmpty()) {
                details.append("Status: ").append(lead.getStatus());
            }
            
            // Get amount
            Double amount = lead.getAmountToPitch() != null ? lead.getAmountToPitch() : 
                           (lead.getOs() != null ? lead.getOs() : null);
            if (amount != null) {
                details.append(", Amount: ₹").append(amount);
            }
            
            // Get lender process name
            if (lead.getLenderProcessName() != null && !lead.getLenderProcessName().isEmpty()) {
                details.append(", Lender: ").append(lead.getLenderProcessName());
            }
            
        } catch (Exception e) {
            // If anything fails, return basic info
            try {
                String name = lead.getName();
                if (name != null) {
                    return "Customer: " + name;
                }
            } catch (Exception ex) {
                return "Customer details not available";
            }
        }
        return details.toString();
    }
}