package com.example.backend.controller;

import com.example.backend.model.UploadRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = "http://localhost:5173")
public class UploadController {

    // In-memory store for upload records (replace with database in production)
    private final Map<String, UploadRecord> uploadStore = new ConcurrentHashMap<>();

    @PostMapping("/leads")
    public ResponseEntity<?> uploadLeads(@RequestParam("file") MultipartFile file) {
        try {
            String uploadId = UUID.randomUUID().toString();
            
            UploadRecord record = new UploadRecord();
            record.setId(uploadId);
            record.setFileName(file.getOriginalFilename());
            record.setFileSize(file.getSize());
            record.setStatus("PROCESSING");
            record.setProgress(0);
            record.setUploadedAt(LocalDateTime.now());
            
            uploadStore.put(uploadId, record);
            
            // Simulate processing (in real app, process async)
            // For demo, we'll update progress
            new Thread(() -> {
                try {
                    for (int i = 0; i <= 100; i += 10) {
                        Thread.sleep(200);
                        record.setProgress(i);
                        if (i == 100) {
                            record.setStatus("COMPLETED");
                            record.setTotalRows(100);
                            record.setSuccessRows(95);
                            record.setErrorRows(5);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return ResponseEntity.ok(Map.of(
                "uploadId", uploadId,
                "message", "Upload started successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUploadHistory() {
        try {
            List<UploadRecord> records = new ArrayList<>(uploadStore.values());
            records.sort((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()));
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch history: " + e.getMessage()));
        }
    }

    @GetMapping("/{uploadId}/progress")
    public ResponseEntity<?> getUploadProgress(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.get(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "progress", record.getProgress(),
            "status", record.getStatus()
        ));
    }

    @GetMapping("/{uploadId}/status")
    public ResponseEntity<?> getUploadStatus(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.get(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{uploadId}/result")
    public ResponseEntity<?> getUploadResult(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.get(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "totalRows", record.getTotalRows(),
            "successRows", record.getSuccessRows(),
            "errorRows", record.getErrorRows(),
            "status", record.getStatus()
        ));
    }

    @GetMapping("/{uploadId}/errors")
    public ResponseEntity<?> getUploadErrors(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.get(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record.getErrors() != null ? record.getErrors() : new ArrayList<>());
    }

    @PostMapping("/{uploadId}/cancel")
    public ResponseEntity<?> cancelUpload(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.remove(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        record.setStatus("CANCELLED");
        return ResponseEntity.ok(Map.of("message", "Upload cancelled successfully"));
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<?> deleteUpload(@PathVariable String uploadId) {
        UploadRecord record = uploadStore.remove(uploadId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Upload record deleted successfully"));
    }
}
