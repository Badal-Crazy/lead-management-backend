package com.example.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Disposition {
    private Long id;
    private Long leadId;
    private String leadName;
    private String leadPhone;
    private String dispositionStatus; // PTP, Paid, Part Paid, CB, RTP, RNR, LM
    private LocalDate callDate;
    private LocalTime callTime;
    private Double amount;
    private LocalDate paymentDate;
    private Double paymentAmount;
    private String notes;
    private String disposedBy;
    private LocalDateTime createdAt;

    public Disposition() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public String getLeadPhone() { return leadPhone; }
    public void setLeadPhone(String leadPhone) { this.leadPhone = leadPhone; }

    public String getDispositionStatus() { return dispositionStatus; }
    public void setDispositionStatus(String dispositionStatus) { this.dispositionStatus = dispositionStatus; }

    public LocalDate getCallDate() { return callDate; }
    public void setCallDate(LocalDate callDate) { this.callDate = callDate; }

    public LocalTime getCallTime() { return callTime; }
    public void setCallTime(LocalTime callTime) { this.callTime = callTime; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDisposedBy() { return disposedBy; }
    public void setDisposedBy(String disposedBy) { this.disposedBy = disposedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
