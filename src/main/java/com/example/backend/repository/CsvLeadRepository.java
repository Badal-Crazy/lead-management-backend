package com.example.backend.repository;

import com.example.backend.model.Lead;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class CsvLeadRepository {
    
    private static final String CSV_FILE = "leads.csv";
    private static final String[] HEADER = {
        "id", "dnd", "phoneNumber", "name", "userId", "agreementNumber",
        "amountToPitch", "os", "latePaymentFee", "settlementAmount",
        "overdueInterest", "waiverAmount", "overduePrincipal", "initialInterest",
        "disbursementDate", "initialDueDate", "bucketRange", "dpd",
        "paymentLinkWaiver", "paymentLinkSettlement", "currentCity", "currentState",
        "productName", "lastPaidDate", "lastPaidSum", "altPhoneNumber",
        "allocationMonthYear", "lenderProcessName", "status", "assignedTo",
        "createdAt", "updatedAt", "uploadName"
    };
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    private final Map<Long, Lead> idCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> phoneCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> nameCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> userIdCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> agreementCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> lenderCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> monthCache = new ConcurrentHashMap<>();
    private final Map<String, List<Lead>> uploadNameCache = new ConcurrentHashMap<>();
    private final List<Lead> allLeads = new ArrayList<>();
    private volatile boolean isLoaded = false;
    private final Object lock = new Object();

    public CsvLeadRepository() {
        initializeCsvFile();
        loadCache();
    }

    private void initializeCsvFile() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                writer.writeNext(HEADER);
                System.out.println("✅ leads.csv created with uploadName column!");
            } catch (IOException e) {
                System.err.println("❌ Failed to create leads.csv: " + e.getMessage());
            }
        } else {
            // Check if uploadName column exists, if not, rebuild
            try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
                String[] existingHeader = reader.readNext();
                if (existingHeader == null || existingHeader.length < HEADER.length) {
                    System.out.println("🔄 Rebuilding CSV with new header (uploadName column)...");
                    // Read existing data
                    List<Lead> existingLeads = loadAllLeads();
                    // Rewrite with new header
                    saveAll(existingLeads);
                } else {
                    System.out.println("✅ CSV already has uploadName column");
                }
            } catch (IOException | CsvException e) {
                System.err.println("❌ Error checking CSV header: " + e.getMessage());
            }
        }
    }

    private void loadCache() {
        synchronized (lock) {
            try {
                long startTime = System.currentTimeMillis();
                List<Lead> leads = loadAllLeads();
                
                idCache.clear();
                phoneCache.clear();
                nameCache.clear();
                userIdCache.clear();
                agreementCache.clear();
                lenderCache.clear();
                monthCache.clear();
                uploadNameCache.clear();
                allLeads.clear();
                
                for (Lead lead : leads) {
                    if (lead.getId() != null) {
                        idCache.put(lead.getId(), lead);
                    }
                    
                    if (lead.getPhoneNumber() != null && !lead.getPhoneNumber().isEmpty()) {
                        phoneCache.computeIfAbsent(lead.getPhoneNumber(), k -> new ArrayList<>()).add(lead);
                    }
                    if (lead.getAltPhoneNumber() != null && !lead.getAltPhoneNumber().isEmpty()) {
                        phoneCache.computeIfAbsent(lead.getAltPhoneNumber(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getName() != null && !lead.getName().isEmpty()) {
                        nameCache.computeIfAbsent(lead.getName().toLowerCase(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getUserId() != null && !lead.getUserId().isEmpty()) {
                        userIdCache.computeIfAbsent(lead.getUserId().toLowerCase(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getAgreementNumber() != null && !lead.getAgreementNumber().isEmpty()) {
                        agreementCache.computeIfAbsent(lead.getAgreementNumber(), k -> new ArrayList<>()).add(lead);
                        agreementCache.computeIfAbsent(lead.getAgreementNumber().toLowerCase(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getLenderProcessName() != null && !lead.getLenderProcessName().isEmpty()) {
                        lenderCache.computeIfAbsent(lead.getLenderProcessName().toLowerCase(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getAllocationMonthYear() != null && !lead.getAllocationMonthYear().isEmpty()) {
                        monthCache.computeIfAbsent(lead.getAllocationMonthYear(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    if (lead.getUploadName() != null && !lead.getUploadName().isEmpty()) {
                        System.out.println("📝 Caching lead with uploadName: " + lead.getUploadName() + " (ID: " + lead.getId() + ")");
                        uploadNameCache.computeIfAbsent(lead.getUploadName(), k -> new ArrayList<>()).add(lead);
                    }
                    
                    allLeads.add(lead);
                }
                
                long maxId = leads.stream().mapToLong(Lead::getId).max().orElse(0);
                idGenerator.set(maxId + 1);
                
                isLoaded = true;
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ Cache loaded with " + leads.size() + " leads in " + duration + "ms");
                System.out.println("📊 uploadName cache size: " + uploadNameCache.size());
                
            } catch (Exception e) {
                System.err.println("❌ Failed to load cache: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<Lead> loadAllLeads() {
        List<Lead> leads = new ArrayList<>();
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            return leads;
        }
        
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> records = reader.readAll();
            if (records.isEmpty()) {
                return leads;
            }
            
            // Check header length
            String[] header = records.get(0);
            boolean hasUploadName = header.length >= 33;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length < 32) continue;
                
                try {
                    Lead lead = new Lead();
                    lead.setId(parseLongSafe(record[0]));
                    lead.setDnd(getSafeValue(record, 1));
                    lead.setPhoneNumber(getSafeValue(record, 2));
                    lead.setName(getSafeValue(record, 3));
                    lead.setUserId(getSafeValue(record, 4));
                    lead.setAgreementNumber(getSafeValue(record, 5));
                    lead.setAmountToPitch(parseDoubleSafe(record[6]));
                    lead.setOs(parseDoubleSafe(record[7]));
                    lead.setLatePaymentFee(parseDoubleSafe(record[8]));
                    lead.setSettlementAmount(parseDoubleSafe(record[9]));
                    lead.setOverdueInterest(parseDoubleSafe(record[10]));
                    lead.setWaiverAmount(parseDoubleSafe(record[11]));
                    lead.setOverduePrincipal(parseDoubleSafe(record[12]));
                    lead.setInitialInterest(parseDoubleSafe(record[13]));
                    lead.setDisbursementDate(parseDateSafe(record[14]));
                    lead.setInitialDueDate(parseDateSafe(record[15]));
                    lead.setBucketRange(getSafeValue(record, 16));
                    lead.setDpd(parseIntegerSafe(record[17]));
                    lead.setPaymentLinkWaiver(getSafeValue(record, 18));
                    lead.setPaymentLinkSettlement(getSafeValue(record, 19));
                    lead.setCurrentCity(getSafeValue(record, 20));
                    lead.setCurrentState(getSafeValue(record, 21));
                    lead.setProductName(getSafeValue(record, 22));
                    lead.setLastPaidDate(parseDateSafe(record[23]));
                    lead.setLastPaidSum(parseDoubleSafe(record[24]));
                    lead.setAltPhoneNumber(getSafeValue(record, 25));
                    lead.setAllocationMonthYear(getSafeValue(record, 26));
                    lead.setLenderProcessName(getSafeValue(record, 27));
                    lead.setStatus(getSafeValue(record, 28));
                    lead.setAssignedTo(getSafeValue(record, 29));
                    lead.setCreatedAt(parseDateTimeSafe(record[30]));
                    lead.setUpdatedAt(parseDateTimeSafe(record[31]));
                    // Upload name is at index 32 if it exists
                    lead.setUploadName(hasUploadName && record.length > 32 ? getSafeValue(record, 32) : "");
                    leads.add(lead);
                } catch (Exception e) {
                    System.err.println("❌ Error parsing lead row " + i + ": " + e.getMessage());
                }
            }
        } catch (IOException | CsvException e) {
            System.err.println("❌ Error reading leads.csv: " + e.getMessage());
        }
        return leads;
    }

    public List<Lead> findAll() {
        if (!isLoaded) {
            loadCache();
        }
        return new ArrayList<>(allLeads);
    }

    public Optional<Lead> findById(Long id) {
        if (!isLoaded) {
            loadCache();
        }
        return Optional.ofNullable(idCache.get(id));
    }

    public List<Lead> findByUploadName(String uploadName) {
        if (!isLoaded) {
            loadCache();
        }
        System.out.println("🔍 Looking for uploadName: " + uploadName);
        System.out.println("📊 uploadName cache keys: " + uploadNameCache.keySet());
        List<Lead> result = uploadNameCache.getOrDefault(uploadName, new ArrayList<>());
        System.out.println("✅ Found " + result.size() + " leads for uploadName: " + uploadName);
        return result;
    }

    public List<Lead> findByStatus(String status) {
        if (!isLoaded) {
            loadCache();
        }
        return allLeads.stream()
                .filter(lead -> status.equalsIgnoreCase(lead.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Lead> search(String query, String type) {
        if (!isLoaded) {
            loadCache();
        }
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(allLeads);
        }
        
        String searchQuery = query.trim();
        String searchQueryLower = searchQuery.toLowerCase();
        long startTime = System.currentTimeMillis();
        List<Lead> results = new ArrayList<>();
        
        System.out.println("🔍 Searching for: '" + searchQuery + "' with type: '" + type + "'");
        
        try {
            if ("name".equalsIgnoreCase(type)) {
                for (Map.Entry<String, List<Lead>> entry : nameCache.entrySet()) {
                    if (entry.getKey().contains(searchQueryLower)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else if ("phone".equalsIgnoreCase(type)) {
                if (phoneCache.containsKey(searchQuery)) {
                    results.addAll(phoneCache.get(searchQuery));
                }
                for (Map.Entry<String, List<Lead>> entry : phoneCache.entrySet()) {
                    if (entry.getKey().contains(searchQuery)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else if ("userId".equalsIgnoreCase(type)) {
                for (Map.Entry<String, List<Lead>> entry : userIdCache.entrySet()) {
                    if (entry.getKey().contains(searchQueryLower)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else if ("agreement".equalsIgnoreCase(type)) {
                for (Map.Entry<String, List<Lead>> entry : agreementCache.entrySet()) {
                    if (entry.getKey().contains(searchQueryLower)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else if ("lender".equalsIgnoreCase(type)) {
                for (Map.Entry<String, List<Lead>> entry : lenderCache.entrySet()) {
                    if (entry.getKey().contains(searchQueryLower)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else if ("month".equalsIgnoreCase(type)) {
                for (Map.Entry<String, List<Lead>> entry : monthCache.entrySet()) {
                    if (entry.getKey().toLowerCase().contains(searchQueryLower)) {
                        results.addAll(entry.getValue());
                    }
                }
            } else {
                for (Lead lead : allLeads) {
                    boolean match = false;
                    if (lead.getName() != null && lead.getName().toLowerCase().contains(searchQueryLower)) {
                        match = true;
                    } else if (lead.getPhoneNumber() != null && lead.getPhoneNumber().contains(searchQuery)) {
                        match = true;
                    } else if (lead.getUserId() != null && lead.getUserId().toLowerCase().contains(searchQueryLower)) {
                        match = true;
                    } else if (lead.getAgreementNumber() != null && lead.getAgreementNumber().toLowerCase().contains(searchQueryLower)) {
                        match = true;
                    } else if (lead.getLenderProcessName() != null && lead.getLenderProcessName().toLowerCase().contains(searchQueryLower)) {
                        match = true;
                    } else if (lead.getAllocationMonthYear() != null && lead.getAllocationMonthYear().toLowerCase().contains(searchQueryLower)) {
                        match = true;
                    }
                    if (match) {
                        results.add(lead);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
        
        List<Lead> uniqueResults = results.stream().distinct().collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("🔍 Search completed in " + duration + "ms, found " + uniqueResults.size() + " results");
        return uniqueResults;
    }

    public synchronized Lead save(Lead lead) {
        List<Lead> leads = findAll();
        if (lead.getId() == null) {
            lead.setId(idGenerator.getAndIncrement());
            lead.setCreatedAt(LocalDateTime.now());
            leads.add(lead);
        } else {
            leads.removeIf(l -> l.getId().equals(lead.getId()));
            lead.setUpdatedAt(LocalDateTime.now());
            leads.add(lead);
        }
        saveAll(leads);
        loadCache();
        return lead;
    }

    public synchronized void saveAllLeads(List<Lead> leads) {
        if (leads == null || leads.isEmpty()) return;
        
        List<Lead> existingLeads = findAll();
        for (Lead lead : leads) {
            if (lead.getId() == null) {
                lead.setId(idGenerator.getAndIncrement());
                lead.setCreatedAt(LocalDateTime.now());
                existingLeads.add(lead);
            } else {
                existingLeads.removeIf(l -> l.getId().equals(lead.getId()));
                lead.setUpdatedAt(LocalDateTime.now());
                existingLeads.add(lead);
            }
        }
        saveAll(existingLeads);
        loadCache();
    }

    public void deleteByUploadName(String uploadName) {
        List<Lead> leads = findAll();
        int before = leads.size();
        leads.removeIf(lead -> uploadName.equals(lead.getUploadName()));
        int after = leads.size();
        System.out.println("🗑️ Deleted " + (before - after) + " leads with uploadName: " + uploadName);
        saveAll(leads);
        loadCache();
    }

    public synchronized void saveAll(List<Lead> leads) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeNext(HEADER);
            for (Lead lead : leads) {
                writer.writeNext(new String[]{
                    String.valueOf(lead.getId()),
                    lead.getDnd() != null ? lead.getDnd() : "",
                    lead.getPhoneNumber() != null ? lead.getPhoneNumber() : "",
                    lead.getName() != null ? lead.getName() : "",
                    lead.getUserId() != null ? lead.getUserId() : "",
                    lead.getAgreementNumber() != null ? lead.getAgreementNumber() : "",
                    String.valueOf(lead.getAmountToPitch() != null ? lead.getAmountToPitch() : 0.0),
                    String.valueOf(lead.getOs() != null ? lead.getOs() : 0.0),
                    String.valueOf(lead.getLatePaymentFee() != null ? lead.getLatePaymentFee() : 0.0),
                    String.valueOf(lead.getSettlementAmount() != null ? lead.getSettlementAmount() : 0.0),
                    String.valueOf(lead.getOverdueInterest() != null ? lead.getOverdueInterest() : 0.0),
                    String.valueOf(lead.getWaiverAmount() != null ? lead.getWaiverAmount() : 0.0),
                    String.valueOf(lead.getOverduePrincipal() != null ? lead.getOverduePrincipal() : 0.0),
                    String.valueOf(lead.getInitialInterest() != null ? lead.getInitialInterest() : 0.0),
                    lead.getDisbursementDate() != null ? lead.getDisbursementDate().toString() : "",
                    lead.getInitialDueDate() != null ? lead.getInitialDueDate().toString() : "",
                    lead.getBucketRange() != null ? lead.getBucketRange() : "",
                    String.valueOf(lead.getDpd() != null ? lead.getDpd() : 0),
                    lead.getPaymentLinkWaiver() != null ? lead.getPaymentLinkWaiver() : "",
                    lead.getPaymentLinkSettlement() != null ? lead.getPaymentLinkSettlement() : "",
                    lead.getCurrentCity() != null ? lead.getCurrentCity() : "",
                    lead.getCurrentState() != null ? lead.getCurrentState() : "",
                    lead.getProductName() != null ? lead.getProductName() : "",
                    lead.getLastPaidDate() != null ? lead.getLastPaidDate().toString() : "",
                    String.valueOf(lead.getLastPaidSum() != null ? lead.getLastPaidSum() : 0.0),
                    lead.getAltPhoneNumber() != null ? lead.getAltPhoneNumber() : "",
                    lead.getAllocationMonthYear() != null ? lead.getAllocationMonthYear() : "",
                    lead.getLenderProcessName() != null ? lead.getLenderProcessName() : "",
                    lead.getStatus() != null ? lead.getStatus() : "Pending",
                    lead.getAssignedTo() != null ? lead.getAssignedTo() : "",
                    lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : LocalDateTime.now().toString(),
                    lead.getUpdatedAt() != null ? lead.getUpdatedAt().toString() : LocalDateTime.now().toString(),
                    lead.getUploadName() != null ? lead.getUploadName() : ""
                });
            }
            System.out.println("✅ Saved " + leads.size() + " leads to CSV");
        } catch (IOException e) {
            System.err.println("❌ Error saving leads.csv: " + e.getMessage());
            throw new RuntimeException("Failed to save leads", e);
        }
    }

    public void deleteById(Long id) {
        List<Lead> leads = findAll();
        leads.removeIf(l -> l.getId().equals(id));
        saveAll(leads);
        loadCache();
    }

    private String getSafeValue(String[] record, int index) {
        if (record.length <= index || record[index] == null) return "";
        String value = record[index].trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            String cleaned = value.trim().replace(",", "").replace("$", "");
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

    private LocalDateTime parseDateTimeSafe(String value) {
        if (value == null || value.trim().isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
