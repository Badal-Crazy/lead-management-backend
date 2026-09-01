// src/main/java/com/example/backend/repository/CsvDispositionRepository.java
package com.example.backend.repository;

import com.example.backend.model.Disposition;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CsvDispositionRepository {
    
    private static final String CSV_FILE = "dispositions.csv";
    private static final String[] HEADER = {
        "id", "agreementNumber", "leadId", "leadName", "leadPhone", 
        "dispositionStatus", "callDate", "callTime", "amount", 
        "paymentDate", "paymentAmount", "notes", "disposedBy", "createdAt"
    };
    private final AtomicLong idGenerator = new AtomicLong(1);

    public CsvDispositionRepository() {
        initializeCsvFile();
    }

    private void initializeCsvFile() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                writer.writeNext(HEADER);
                System.out.println("✅ dispositions.csv created with agreementNumber column!");
            } catch (IOException e) {
                System.err.println("❌ Failed to create dispositions.csv: " + e.getMessage());
            }
        } else {
            // Check if agreementNumber column exists
            try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
                String[] existingHeader = reader.readNext();
                if (existingHeader == null || existingHeader.length < 14) {
                    System.out.println("🔄 Rebuilding CSV with new header (agreementNumber column)...");
                    List<Disposition> existingDispositions = findAll();
                    saveAll(existingDispositions);
                } else {
                    System.out.println("✅ CSV already has agreementNumber column");
                }
            } catch (IOException | CsvException e) {
                System.err.println("❌ Error checking CSV header: " + e.getMessage());
            }
        }
    }

    public List<Disposition> findAll() {
        List<Disposition> dispositions = new ArrayList<>();
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            return dispositions;
        }
        
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> records = reader.readAll();
            if (records.isEmpty()) {
                return dispositions;
            }
            
            // Check header to determine columns
            String[] header = records.get(0);
            boolean hasAgreementNumber = header.length >= 2 && "agreementNumber".equals(header[1]);
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                int minLength = hasAgreementNumber ? 14 : 13;
                if (record.length < minLength) continue;
                
                try {
                    Disposition disposition = new Disposition();
                    int idx = 0;
                    disposition.setId(parseLongSafe(record[idx++]));
                    
                    if (hasAgreementNumber) {
                        disposition.setAgreementNumber(record[idx++]);
                    }
                    
                    disposition.setLeadId(parseLongSafe(record[idx++]));
                    disposition.setLeadName(record[idx++]);
                    disposition.setLeadPhone(record[idx++]);
                    disposition.setDispositionStatus(record[idx++]);
                    disposition.setCallDate(parseDate(record[idx++]));
                    disposition.setCallTime(parseTime(record[idx++]));
                    disposition.setAmount(parseDoubleSafe(record[idx++]));
                    disposition.setPaymentDate(parseDate(record[idx++]));
                    disposition.setPaymentAmount(parseDoubleSafe(record[idx++]));
                    disposition.setNotes(record[idx++]);
                    disposition.setDisposedBy(record[idx++]);
                    disposition.setCreatedAt(parseDateTime(record[idx++]));
                    dispositions.add(disposition);
                } catch (Exception e) {
                    System.err.println("❌ Error parsing disposition row " + i + ": " + e.getMessage());
                }
            }
        } catch (IOException | CsvException e) {
            System.err.println("❌ Error reading dispositions.csv: " + e.getMessage());
        }
        return dispositions;
    }

    public Optional<Disposition> findById(Long id) {
        return findAll().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst();
    }

    public List<Disposition> findByLeadId(Long leadId) {
        return findAll().stream()
                .filter(d -> leadId.equals(d.getLeadId()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Disposition> findByAgreementNumber(String agreementNumber) {
        if (agreementNumber == null || agreementNumber.isEmpty()) {
            return new ArrayList<>();
        }
        return findAll().stream()
                .filter(d -> agreementNumber.equals(d.getAgreementNumber()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Disposition> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return findAll().stream()
                .filter(d -> {
                    if (d.getCreatedAt() == null) return false;
                    LocalDate createdDate = d.getCreatedAt().toLocalDate();
                    return (startDate == null || !createdDate.isBefore(startDate)) &&
                           (endDate == null || !createdDate.isAfter(endDate));
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public synchronized Disposition save(Disposition disposition) {
        List<Disposition> dispositions = findAll();
        if (disposition.getId() == null) {
            // Find max ID
            long maxId = dispositions.stream().mapToLong(Disposition::getId).max().orElse(0);
            idGenerator.set(maxId + 1);
            disposition.setId(idGenerator.getAndIncrement());
            disposition.setCreatedAt(LocalDateTime.now());
            dispositions.add(disposition);
        } else {
            dispositions.removeIf(d -> d.getId().equals(disposition.getId()));
            dispositions.add(disposition);
        }
        saveAll(dispositions);
        return disposition;
    }

    public synchronized void saveAll(List<Disposition> dispositions) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeNext(HEADER);
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            for (Disposition disposition : dispositions) {
                writer.writeNext(new String[]{
                    String.valueOf(disposition.getId()),
                    disposition.getAgreementNumber() != null ? disposition.getAgreementNumber() : "",
                    String.valueOf(disposition.getLeadId() != null ? disposition.getLeadId() : 0),
                    disposition.getLeadName() != null ? disposition.getLeadName() : "",
                    disposition.getLeadPhone() != null ? disposition.getLeadPhone() : "",
                    disposition.getDispositionStatus() != null ? disposition.getDispositionStatus() : "",
                    disposition.getCallDate() != null ? disposition.getCallDate().format(dateFormatter) : "",
                    disposition.getCallTime() != null ? disposition.getCallTime().format(timeFormatter) : "",
                    String.valueOf(disposition.getAmount() != null ? disposition.getAmount() : 0.0),
                    disposition.getPaymentDate() != null ? disposition.getPaymentDate().format(dateFormatter) : "",
                    String.valueOf(disposition.getPaymentAmount() != null ? disposition.getPaymentAmount() : 0.0),
                    disposition.getNotes() != null ? disposition.getNotes() : "",
                    disposition.getDisposedBy() != null ? disposition.getDisposedBy() : "",
                    disposition.getCreatedAt() != null ? disposition.getCreatedAt().format(dateTimeFormatter) : LocalDateTime.now().format(dateTimeFormatter)
                });
            }
            System.out.println("✅ Saved " + dispositions.size() + " dispositions to CSV");
        } catch (IOException e) {
            System.err.println("❌ Error saving dispositions.csv: " + e.getMessage());
            throw new RuntimeException("Failed to save dispositions", e);
        }
    }

    public void deleteById(Long id) {
        List<Disposition> dispositions = findAll();
        dispositions.removeIf(d -> d.getId().equals(id));
        saveAll(dispositions);
    }

    // Helper methods
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
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
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}