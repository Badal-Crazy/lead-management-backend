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
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CsvDispositionRepository {
    
    private static final String CSV_FILE = "dispositions.csv";
    private static final String[] HEADER = {
        "id", "leadId", "leadName", "leadPhone", "dispositionStatus",
        "callDate", "callTime", "amount", "paymentDate", "paymentAmount",
        "notes", "disposedBy", "createdAt"
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
                System.out.println("✅ dispositions.csv created!");
            } catch (IOException e) {
                System.err.println("❌ Failed to create dispositions.csv: " + e.getMessage());
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
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length < 13) continue;
                
                try {
                    Disposition disposition = new Disposition();
                    disposition.setId(Long.parseLong(record[0].trim()));
                    disposition.setLeadId(Long.parseLong(record[1].trim()));
                    disposition.setLeadName(record[2].trim());
                    disposition.setLeadPhone(record[3].trim());
                    disposition.setDispositionStatus(record[4].trim());
                    disposition.setCallDate(parseDate(record[5]));
                    disposition.setCallTime(parseTime(record[6]));
                    disposition.setAmount(parseDouble(record[7]));
                    disposition.setPaymentDate(parseDate(record[8]));
                    disposition.setPaymentAmount(parseDouble(record[9]));
                    disposition.setNotes(record[10].trim());
                    disposition.setDisposedBy(record[11].trim());
                    disposition.setCreatedAt(LocalDateTime.parse(record[12].trim()));
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

    public List<Disposition> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return findAll().stream()
                .filter(d -> {
                    LocalDate createdDate = d.getCreatedAt().toLocalDate();
                    return (startDate == null || !createdDate.isBefore(startDate)) &&
                           (endDate == null || !createdDate.isAfter(endDate));
                })
                .toList();
    }

    public List<Disposition> findByLeadId(Long leadId) {
        return findAll().stream()
                .filter(d -> d.getLeadId().equals(leadId))
                .toList();
    }

    public Disposition save(Disposition disposition) {
        List<Disposition> dispositions = findAll();
        if (disposition.getId() == null) {
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

    public void saveAll(List<Disposition> dispositions) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeNext(HEADER);
            for (Disposition disposition : dispositions) {
                writer.writeNext(new String[]{
                    String.valueOf(disposition.getId()),
                    String.valueOf(disposition.getLeadId()),
                    disposition.getLeadName() != null ? disposition.getLeadName() : "",
                    disposition.getLeadPhone() != null ? disposition.getLeadPhone() : "",
                    disposition.getDispositionStatus() != null ? disposition.getDispositionStatus() : "",
                    disposition.getCallDate() != null ? disposition.getCallDate().toString() : "",
                    disposition.getCallTime() != null ? disposition.getCallTime().toString() : "",
                    String.valueOf(disposition.getAmount() != null ? disposition.getAmount() : 0.0),
                    disposition.getPaymentDate() != null ? disposition.getPaymentDate().toString() : "",
                    String.valueOf(disposition.getPaymentAmount() != null ? disposition.getPaymentAmount() : 0.0),
                    disposition.getNotes() != null ? disposition.getNotes() : "",
                    disposition.getDisposedBy() != null ? disposition.getDisposedBy() : "",
                    disposition.getCreatedAt().toString()
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save to CSV file", e);
        }
    }

    public void deleteById(Long id) {
        List<Disposition> dispositions = findAll();
        dispositions.removeIf(d -> d.getId().equals(id));
        saveAll(dispositions);
    }

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

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
