package com.example.backend.controller;

import com.example.backend.model.Disposition;
import com.example.backend.model.Lead;
import com.example.backend.repository.CsvDispositionRepository;
import com.example.backend.repository.CsvLeadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispositions")
@CrossOrigin(origins = "http://localhost:5173")
public class DispositionController {

    private final CsvDispositionRepository dispositionRepository;
    private final CsvLeadRepository leadRepository;

    public DispositionController(CsvDispositionRepository dispositionRepository, CsvLeadRepository leadRepository) {
        this.dispositionRepository = dispositionRepository;
        this.leadRepository = leadRepository;
    }

    @PostMapping
    public ResponseEntity<?> createDisposition(@RequestBody Disposition disposition) {
        try {
            System.out.println("📝 Creating disposition for lead: " + disposition.getLeadId());
            
            var leadOpt = leadRepository.findById(disposition.getLeadId());
            if (leadOpt.isPresent()) {
                Lead lead = leadOpt.get();
                disposition.setLeadName(lead.getName());
                disposition.setLeadPhone(lead.getPhoneNumber());
                
                lead.setStatus("Disposed");
                leadRepository.save(lead);
            }

            Disposition saved = dispositionRepository.save(disposition);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            System.err.println("❌ Error creating disposition: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create disposition: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllDispositions() {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllDispositionsAlt() {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<?> getDispositionsByLead(@PathVariable Long leadId) {
        try {
            List<Disposition> dispositions = dispositionRepository.findByLeadId(leadId);
            return ResponseEntity.ok(dispositions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyDispositions(@RequestParam String username) {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/team")
    public ResponseEntity<?> getTeamDispositions() {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch team dispositions"));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getDispositionSummary() {
        try {
            List<Disposition> dispositions = dispositionRepository.findAll();
            Map<String, Object> summary = new HashMap<>();
            summary.put("total", dispositions.size());
            
            Map<String, Long> statusCount = new HashMap<>();
            for (Disposition d : dispositions) {
                String status = d.getDispositionStatus() != null ? d.getDispositionStatus() : "Unknown";
                statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
            }
            summary.put("byStatus", statusCount);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch summary"));
        }
    }

    @GetMapping("/summary/all")
    public ResponseEntity<?> getAllDispositionSummary() {
        try {
            List<Disposition> dispositions = dispositionRepository.findAll();
            Map<String, Object> summary = new HashMap<>();
            summary.put("total", dispositions.size());
            
            Map<String, Long> statusCount = new HashMap<>();
            for (Disposition d : dispositions) {
                String status = d.getDispositionStatus() != null ? d.getDispositionStatus() : "Unknown";
                statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
            }
            summary.put("byStatus", statusCount);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch summary"));
        }
    }

    @GetMapping("/my/summary")
    public ResponseEntity<?> getMyDispositionSummary() {
        try {
            List<Disposition> dispositions = dispositionRepository.findAll();
            Map<String, Object> summary = new HashMap<>();
            summary.put("total", dispositions.size());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch summary"));
        }
    }

    @GetMapping("/my/date-wise")
    public ResponseEntity<?> getMyDateWiseDispositions(@RequestParam String date) {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getAgentDispositions(@PathVariable String agentId) {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch agent dispositions"));
        }
    }

    @GetMapping("/date-wise")
    public ResponseEntity<?> getDateWiseDispositions(@RequestParam String date) {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<?> getAdminDispositions(@PathVariable String adminId) {
        try {
            return ResponseEntity.ok(dispositionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch admin dispositions"));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadDispositions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
            
            List<Disposition> dispositions = dispositionRepository.findByDateRange(start, end);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Lead ID,Lead Name,Lead Phone,Disposition Status,Call Date,Call Time,Amount,Payment Date,Payment Amount,Remarks,Disposed By,Date\n");
            
            for (Disposition d : dispositions) {
                csv.append(d.getLeadId()).append(",")
                   .append(d.getLeadName() != null ? d.getLeadName() : "").append(",")
                   .append(d.getLeadPhone() != null ? d.getLeadPhone() : "").append(",")
                   .append(d.getDispositionStatus() != null ? d.getDispositionStatus() : "").append(",")
                   .append(d.getCallDate() != null ? d.getCallDate() : "").append(",")
                   .append(d.getCallTime() != null ? d.getCallTime() : "").append(",")
                   .append(d.getAmount() != null ? d.getAmount() : 0).append(",")
                   .append(d.getPaymentDate() != null ? d.getPaymentDate() : "").append(",")
                   .append(d.getPaymentAmount() != null ? d.getPaymentAmount() : 0).append(",")
                   .append(d.getNotes() != null ? d.getNotes().replace(",", ";") : "").append(",")
                   .append(d.getDisposedBy() != null ? d.getDisposedBy() : "").append(",")
                   .append(d.getCreatedAt() != null ? d.getCreatedAt() : "").append("\n");
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=dispositions.csv")
                    .body(csv.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download dispositions: " + e.getMessage()));
        }
    }
}
