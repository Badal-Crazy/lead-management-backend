package com.example.backend.controller;

import com.example.backend.model.Lead;
import com.example.backend.repository.CsvLeadRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "${cors.allowed.origins}")
public class LeadController {

    private final CsvLeadRepository leadRepository;

    public LeadController(CsvLeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody Lead lead) {
        try {
            System.out.println("📝 Creating lead: " + lead.getName());
            lead.setStatus("Pending");
            Lead saved = leadRepository.save(lead);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            System.err.println("❌ Error creating lead: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create lead: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLeadById(@PathVariable Long id) {
        try {
            var leadOpt = leadRepository.findById(id);
            if (leadOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(leadOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch lead: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLeads(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "allocationMonthYear", required = false) String allocationMonthYear,
            @RequestParam(value = "lenderProcessName", required = false) String lenderProcessName,
            @RequestParam(value = "uploadName", required = false) String uploadName) {
        
        System.out.println("========================================");
        System.out.println("📤 CSV UPLOAD STARTED");
        System.out.println("File name: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize() + " bytes (" + (file.getSize() / 1024 / 1024) + " MB)");
        System.out.println("Allocation Month: " + allocationMonthYear);
        System.out.println("Lender Process: " + lenderProcessName);
        System.out.println("Upload Name: " + uploadName);
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Use the mapping values for all leads
            String finalAllocationMonth = allocationMonthYear != null ? allocationMonthYear : "";
            String finalLenderProcess = lenderProcessName != null ? lenderProcessName : "";
            String finalUploadName = uploadName != null ? uploadName : "";

            int batchSize = 5000000;
            List<Lead> batch = new ArrayList<>(batchSize);
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();

            try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                List<String[]> records = reader.readAll();
                System.out.println("📊 Found " + records.size() + " rows in CSV");
                
                if (records.size() <= 1) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "CSV file is empty or has only headers"));
                }

                String[] headers = records.get(0);
                Map<String, Integer> columnMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim().toLowerCase();
                    if (header.startsWith("\"") && header.endsWith("\"")) {
                        header = header.substring(1, header.length() - 1);
                    }
                    columnMap.put(header, i);
                }

                int totalRows = records.size() - 1;
                System.out.println("📊 Processing " + totalRows + " rows...");

                for (int i = 1; i < records.size(); i++) {
                    String[] record = records.get(i);
                    try {
                        String phoneNumber = getValueByColumn(record, columnMap, "phone number");
                        String name = getValueByColumn(record, columnMap, "name");
                        
                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                            errorCount++;
                            if (errors.size() < 100) errors.add("Row " + i + ": Phone Number is required");
                            continue;
                        }
                        if (name == null || name.isEmpty()) {
                            errorCount++;
                            if (errors.size() < 100) errors.add("Row " + i + ": Name is required");
                            continue;
                        }

                        Lead lead = new Lead();
                        lead.setPhoneNumber(phoneNumber);
                        lead.setName(name);
                        lead.setDnd(getValueByColumn(record, columnMap, "dnd"));
                        lead.setUserId(getValueByColumn(record, columnMap, "user id"));
                        lead.setAgreementNumber(getValueByColumn(record, columnMap, "agreementnumber"));
                        lead.setAmountToPitch(parseDoubleSafe(getValueByColumn(record, columnMap, "amount to be pitched(os)")));
                        lead.setOs(parseDoubleSafe(getValueByColumn(record, columnMap, "os")));
                        lead.setLatePaymentFee(parseDoubleSafe(getValueByColumn(record, columnMap, "late payment fee")));
                        lead.setSettlementAmount(parseDoubleSafe(getValueByColumn(record, columnMap, "settlement amount")));
                        lead.setOverdueInterest(parseDoubleSafe(getValueByColumn(record, columnMap, "overdue_interest")));
                        lead.setWaiverAmount(parseDoubleSafe(getValueByColumn(record, columnMap, "waiver amount")));
                        lead.setOverduePrincipal(parseDoubleSafe(getValueByColumn(record, columnMap, "overdue_principal")));
                        lead.setInitialInterest(parseDoubleSafe(getValueByColumn(record, columnMap, "initial_interest")));
                        lead.setDisbursementDate(parseDateSafe(getValueByColumn(record, columnMap, "disbursementdate")));
                        lead.setInitialDueDate(parseDateSafe(getValueByColumn(record, columnMap, "initialduedate")));
                        lead.setBucketRange(getValueByColumn(record, columnMap, "bucket_range"));
                        lead.setDpd(parseIntegerSafe(getValueByColumn(record, columnMap, "dpd")));
                        lead.setPaymentLinkWaiver(getValueByColumn(record, columnMap, "payment link(waiver)"));
                        lead.setPaymentLinkSettlement(getValueByColumn(record, columnMap, "payment link settlement"));
                        lead.setCurrentCity(getValueByColumn(record, columnMap, "current_city"));
                        lead.setCurrentState(getValueByColumn(record, columnMap, "current_state"));
                        lead.setProductName(getValueByColumn(record, columnMap, "productname"));
                        lead.setLastPaidDate(parseDateSafe(getValueByColumn(record, columnMap, "last_paid_date")));
                        lead.setLastPaidSum(parseDoubleSafe(getValueByColumn(record, columnMap, "last_paid_sum")));
                        lead.setAltPhoneNumber(getValueByColumn(record, columnMap, "alt phone number"));
                        
                        // Apply mapping values to all leads
                        lead.setAllocationMonthYear(finalAllocationMonth);
                        lead.setLenderProcessName(finalLenderProcess);
                        lead.setUploadName(finalUploadName);
                        
                        lead.setStatus("Pending");
                        lead.setCreatedAt(LocalDateTime.now());
                        lead.setUpdatedAt(LocalDateTime.now());

                        batch.add(lead);
                        successCount++;

                        if (batch.size() >= batchSize) {
                            leadRepository.saveAllLeads(batch);
                            System.out.println("✅ Saved " + successCount + " leads so far...");
                            batch.clear();
                        }
                        
                    } catch (Exception e) {
                        errorCount++;
                        if (errors.size() < 100) errors.add("Row " + i + ": " + e.getMessage());
                    }
                }

                if (!batch.isEmpty()) {
                    leadRepository.saveAllLeads(batch);
                    System.out.println("✅ Saved final batch of " + batch.size() + " leads");
                }
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("========================================");
            System.out.println("✅ Upload complete in " + duration + " seconds!");
            System.out.println("Success: " + successCount + " leads");
            System.out.println("Errors: " + errorCount);
            System.out.println("========================================");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "CSV processed successfully");
            response.put("count", successCount);
            response.put("errors", errors.size() > 0 ? errors : new ArrayList<>());
            response.put("totalRows", successCount + errorCount);
            response.put("success", true);
            response.put("duration", duration + "s");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload CSV: " + e.getMessage()));
        }
    }

    @DeleteMapping("/upload/{uploadName}")
    public ResponseEntity<?> deleteLeadsByUploadName(@PathVariable String uploadName) {
        try {
            System.out.println("🗑️ Deleting leads with upload name: " + uploadName);
            
            // URL decode the upload name
            String decodedUploadName = java.net.URLDecoder.decode(uploadName, "UTF-8");
            System.out.println("Decoded upload name: " + decodedUploadName);
            
            List<Lead> leads = leadRepository.findByUploadName(decodedUploadName);
            System.out.println("Found " + leads.size() + " leads to delete");
            
            if (leads.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No leads found with upload name: " + decodedUploadName));
            }
            
            leadRepository.deleteByUploadName(decodedUploadName);
            
            return ResponseEntity.ok(Map.of(
                "message", "Deleted " + leads.size() + " leads",
                "count", leads.size()
            ));
        } catch (Exception e) {
            System.err.println("❌ Error deleting leads: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete leads: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLead(@PathVariable Long id) {
        try {
            System.out.println("🗑️ Deleting lead with ID: " + id);
            var leadOpt = leadRepository.findById(id);
            if (leadOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            leadRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Lead deleted successfully"));
        } catch (Exception e) {
            System.err.println("❌ Error deleting lead: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete lead: " + e.getMessage()));
        }
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<?> bulkDeleteLeads(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("leadIds");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No lead IDs provided"));
            }
            
            int deletedCount = 0;
            for (Long id : ids) {
                var leadOpt = leadRepository.findById(id);
                if (leadOpt.isPresent()) {
                    leadRepository.deleteById(id);
                    deletedCount++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Deleted " + deletedCount + " leads",
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            System.err.println("❌ Error bulk deleting leads: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete leads: " + e.getMessage()));
        }
    }

    private String getValueByColumn(String[] record, Map<String, Integer> columnMap, String columnName) {
        String key = columnName.toLowerCase().trim();
        if (columnMap.containsKey(key)) {
            int index = columnMap.get(key);
            if (index < record.length) {
                String value = record[index];
                if (value == null) return "";
                value = value.trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return "";
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            String cleaned = value.trim().replace(",", "").replace("$", "").replace(" ", "");
            if (cleaned.isEmpty()) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer parseIntegerSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDate parseDateSafe(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllLeads() {
        try {
            return ResponseEntity.ok(leadRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch leads"));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeads() {
        try {
            return ResponseEntity.ok(leadRepository.findByStatus("Pending"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch pending leads"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchLeads(
            @RequestParam String q,
            @RequestParam(defaultValue = "name") String type) {
        try {
            long startTime = System.currentTimeMillis();
            List<Lead> results = leadRepository.search(q, type);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("count", results.size());
            response.put("duration", duration + "ms");
            response.put("message", "Search completed in " + duration + "ms");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search leads: " + e.getMessage()));
        }
    }
}
