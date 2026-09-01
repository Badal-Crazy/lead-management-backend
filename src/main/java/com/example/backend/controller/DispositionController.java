// src/main/java/com/example/backend/controller/DispositionController.java
package com.example.backend.controller;

import com.example.backend.model.Disposition;
import com.example.backend.model.Lead;
import com.example.backend.repository.CsvDispositionRepository;
import com.example.backend.repository.CsvLeadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispositions")
@CrossOrigin(origins = "http://localhost:5173")
public class DispositionController {

    private final CsvDispositionRepository dispositionRepository;
    private final CsvLeadRepository leadRepository;

    public DispositionController(CsvDispositionRepository dispositionRepository, 
                                 CsvLeadRepository leadRepository) {
        this.dispositionRepository = dispositionRepository;
        this.leadRepository = leadRepository;
    }

    @PostMapping
    public ResponseEntity<?> createDisposition(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("📝 Creating disposition...");
            System.out.println("📝 Request data: " + request);
            
            Disposition disposition = new Disposition();
            
            // Set agreement number
            String agreementNumber = (String) request.get("agreementNumber");
            if (agreementNumber != null && !agreementNumber.isEmpty()) {
                disposition.setAgreementNumber(agreementNumber);
            }
            
            // Set lead fields
            Object leadIdObj = request.get("leadId");
            if (leadIdObj != null) {
                disposition.setLeadId(Long.valueOf(leadIdObj.toString()));
            }
            
            disposition.setLeadName((String) request.get("leadName"));
            disposition.setLeadPhone((String) request.get("leadPhone"));
            disposition.setDispositionStatus((String) request.get("dispositionStatus"));
            
            // Parse dates
            String callDateStr = (String) request.get("callDate");
            if (callDateStr != null && !callDateStr.isEmpty()) {
                disposition.setCallDate(LocalDate.parse(callDateStr));
            }
            
            String callTimeStr = (String) request.get("callTime");
            if (callTimeStr != null && !callTimeStr.isEmpty()) {
                disposition.setCallTime(LocalTime.parse(callTimeStr));
            }
            
            // Set amounts
            Object amountObj = request.get("amount");
            if (amountObj != null) {
                disposition.setAmount(Double.valueOf(amountObj.toString()));
            }
            
            String paymentDateStr = (String) request.get("paymentDate");
            if (paymentDateStr != null && !paymentDateStr.isEmpty()) {
                disposition.setPaymentDate(LocalDate.parse(paymentDateStr));
            }
            
            Object paymentAmountObj = request.get("paymentAmount");
            if (paymentAmountObj != null) {
                disposition.setPaymentAmount(Double.valueOf(paymentAmountObj.toString()));
            }
            
            disposition.setNotes((String) request.get("notes"));
            disposition.setDisposedBy((String) request.get("disposedBy"));
            disposition.setCreatedAt(LocalDateTime.now());
            
            // Find lead by agreement number and update status
            if (agreementNumber != null && !agreementNumber.isEmpty()) {
                List<Lead> leads = leadRepository.search(agreementNumber, "agreement");
                if (leads != null && !leads.isEmpty()) {
                    Lead lead = leads.get(0);
                    System.out.println("✅ Found lead: " + lead.getName() + " (ID: " + lead.getId() + ")");
                    lead.setStatus("Disposed");
                    leadRepository.save(lead);
                    System.out.println("✅ Lead status updated to Disposed");
                } else {
                    System.out.println("⚠️ No lead found for agreement: " + agreementNumber);
                }
            }
            
            // Save disposition
            Disposition saved = dispositionRepository.save(disposition);
            System.out.println("✅ Disposition saved with ID: " + saved.getId());
            
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
            List<Disposition> dispositions = dispositionRepository.findAll();
            return ResponseEntity.ok(dispositions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllDispositionsAlt() {
        try {
            List<Disposition> dispositions = dispositionRepository.findAll();
            return ResponseEntity.ok(dispositions);
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

    @GetMapping("/agreement/{agreementNumber}")
    public ResponseEntity<?> getDispositionsByAgreement(@PathVariable String agreementNumber) {
        try {
            List<Disposition> dispositions = dispositionRepository.findByAgreementNumber(agreementNumber);
            return ResponseEntity.ok(dispositions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dispositions: " + e.getMessage()));
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
            csv.append("Agreement Number,Lead ID,Lead Name,Lead Phone,Disposition Status,Call Date,Call Time,Amount,Payment Date,Payment Amount,Remarks,Disposed By,Date\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            for (Disposition d : dispositions) {
                csv.append(d.getAgreementNumber() != null ? d.getAgreementNumber() : "").append(",")
                   .append(d.getLeadId() != null ? d.getLeadId() : "").append(",")
                   .append(d.getLeadName() != null ? d.getLeadName() : "").append(",")
                   .append(d.getLeadPhone() != null ? d.getLeadPhone() : "").append(",")
                   .append(d.getDispositionStatus() != null ? d.getDispositionStatus() : "").append(",")
                   .append(d.getCallDate() != null ? d.getCallDate().format(formatter) : "").append(",")
                   .append(d.getCallTime() != null ? d.getCallTime().format(timeFormatter) : "").append(",")
                   .append(d.getAmount() != null ? d.getAmount() : 0).append(",")
                   .append(d.getPaymentDate() != null ? d.getPaymentDate().format(formatter) : "").append(",")
                   .append(d.getPaymentAmount() != null ? d.getPaymentAmount() : 0).append(",")
                   .append(d.getNotes() != null ? d.getNotes().replace(",", ";") : "").append(",")
                   .append(d.getDisposedBy() != null ? d.getDisposedBy() : "").append(",")
                   .append(d.getCreatedAt() != null ? d.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "").append("\n");
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