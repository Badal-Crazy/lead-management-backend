// src/main/java/com/example/backend/model/Disposition.java
package com.example.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Disposition {
    
    private Long id;
    private String agreementNumber;
    private Long leadId;
    private String leadName;
    private String leadPhone;
    private String dispositionStatus;
    private LocalDate callDate;
    private LocalTime callTime;
    private Double amount;
    private LocalDate paymentDate;
    private Double paymentAmount;
    private String notes;
    private String disposedBy;
    private LocalDateTime createdAt;

    // Default constructor
    public Disposition() {}

    // Constructor with fields
    public Disposition(String agreementNumber, Long leadId, String leadName, String leadPhone, 
                       String dispositionStatus, LocalDate callDate, LocalTime callTime, 
                       Double amount, LocalDate paymentDate, Double paymentAmount, 
                       String notes, String disposedBy) {
        this.agreementNumber = agreementNumber;
        this.leadId = leadId;
        this.leadName = leadName;
        this.leadPhone = leadPhone;
        this.dispositionStatus = dispositionStatus;
        this.callDate = callDate;
        this.callTime = callTime;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentAmount = paymentAmount;
        this.notes = notes;
        this.disposedBy = disposedBy;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public String getLeadPhone() {
        return leadPhone;
    }

    public void setLeadPhone(String leadPhone) {
        this.leadPhone = leadPhone;
    }

    public String getDispositionStatus() {
        return dispositionStatus;
    }

    public void setDispositionStatus(String dispositionStatus) {
        this.dispositionStatus = dispositionStatus;
    }

    public LocalDate getCallDate() {
        return callDate;
    }

    public void setCallDate(LocalDate callDate) {
        this.callDate = callDate;
    }

    public LocalTime getCallTime() {
        return callTime;
    }

    public void setCallTime(LocalTime callTime) {
        this.callTime = callTime;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDisposedBy() {
        return disposedBy;
    }

    public void setDisposedBy(String disposedBy) {
        this.disposedBy = disposedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Disposition{" +
                "id=" + id +
                ", agreementNumber='" + agreementNumber + '\'' +
                ", leadId=" + leadId +
                ", leadName='" + leadName + '\'' +
                ", leadPhone='" + leadPhone + '\'' +
                ", dispositionStatus='" + dispositionStatus + '\'' +
                ", callDate=" + callDate +
                ", callTime=" + callTime +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentAmount=" + paymentAmount +
                ", disposedBy='" + disposedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}