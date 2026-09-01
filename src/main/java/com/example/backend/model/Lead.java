// src/main/java/com/example/backend/model/Lead.java
package com.example.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Lead {
    
    private Long id;
    private String dnd;
    private String phoneNumber;
    private String name;
    private String userId;
    private String agreementNumber;
    private Double amountToPitch;
    private Double os;
    private Double latePaymentFee;
    private Double settlementAmount;
    private Double overdueInterest;
    private Double waiverAmount;
    private Double overduePrincipal;
    private Double initialInterest;
    private LocalDate disbursementDate;
    private LocalDate initialDueDate;
    private String bucketRange;
    private Integer dpd;
    private String paymentLinkWaiver;
    private String paymentLinkSettlement;
    private String currentCity;
    private String currentState;
    private String productName;
    private LocalDate lastPaidDate;
    private Double lastPaidSum;
    private String altPhoneNumber;
    private String allocationMonthYear;
    private String lenderProcessName;
    private String status;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String uploadName;
    private String ptpDate;  // Added for PTP functionality

    // Default constructor
    public Lead() {}

    // Constructor with essential fields
    public Lead(String name, String phoneNumber, String agreementNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.agreementNumber = agreementNumber;
        this.status = "Pending";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDnd() {
        return dnd;
    }

    public void setDnd(String dnd) {
        this.dnd = dnd;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public Double getAmountToPitch() {
        return amountToPitch;
    }

    public void setAmountToPitch(Double amountToPitch) {
        this.amountToPitch = amountToPitch;
    }

    public Double getOs() {
        return os;
    }

    public void setOs(Double os) {
        this.os = os;
    }

    public Double getLatePaymentFee() {
        return latePaymentFee;
    }

    public void setLatePaymentFee(Double latePaymentFee) {
        this.latePaymentFee = latePaymentFee;
    }

    public Double getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(Double settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public Double getOverdueInterest() {
        return overdueInterest;
    }

    public void setOverdueInterest(Double overdueInterest) {
        this.overdueInterest = overdueInterest;
    }

    public Double getWaiverAmount() {
        return waiverAmount;
    }

    public void setWaiverAmount(Double waiverAmount) {
        this.waiverAmount = waiverAmount;
    }

    public Double getOverduePrincipal() {
        return overduePrincipal;
    }

    public void setOverduePrincipal(Double overduePrincipal) {
        this.overduePrincipal = overduePrincipal;
    }

    public Double getInitialInterest() {
        return initialInterest;
    }

    public void setInitialInterest(Double initialInterest) {
        this.initialInterest = initialInterest;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public LocalDate getInitialDueDate() {
        return initialDueDate;
    }

    public void setInitialDueDate(LocalDate initialDueDate) {
        this.initialDueDate = initialDueDate;
    }

    public String getBucketRange() {
        return bucketRange;
    }

    public void setBucketRange(String bucketRange) {
        this.bucketRange = bucketRange;
    }

    public Integer getDpd() {
        return dpd;
    }

    public void setDpd(Integer dpd) {
        this.dpd = dpd;
    }

    public String getPaymentLinkWaiver() {
        return paymentLinkWaiver;
    }

    public void setPaymentLinkWaiver(String paymentLinkWaiver) {
        this.paymentLinkWaiver = paymentLinkWaiver;
    }

    public String getPaymentLinkSettlement() {
        return paymentLinkSettlement;
    }

    public void setPaymentLinkSettlement(String paymentLinkSettlement) {
        this.paymentLinkSettlement = paymentLinkSettlement;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LocalDate getLastPaidDate() {
        return lastPaidDate;
    }

    public void setLastPaidDate(LocalDate lastPaidDate) {
        this.lastPaidDate = lastPaidDate;
    }

    public Double getLastPaidSum() {
        return lastPaidSum;
    }

    public void setLastPaidSum(Double lastPaidSum) {
        this.lastPaidSum = lastPaidSum;
    }

    public String getAltPhoneNumber() {
        return altPhoneNumber;
    }

    public void setAltPhoneNumber(String altPhoneNumber) {
        this.altPhoneNumber = altPhoneNumber;
    }

    public String getAllocationMonthYear() {
        return allocationMonthYear;
    }

    public void setAllocationMonthYear(String allocationMonthYear) {
        this.allocationMonthYear = allocationMonthYear;
    }

    public String getLenderProcessName() {
        return lenderProcessName;
    }

    public void setLenderProcessName(String lenderProcessName) {
        this.lenderProcessName = lenderProcessName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUploadName() {
        return uploadName;
    }

    public void setUploadName(String uploadName) {
        this.uploadName = uploadName;
    }

    public String getPtpDate() {
        return ptpDate;
    }

    public void setPtpDate(String ptpDate) {
        this.ptpDate = ptpDate;
    }

    @Override
    public String toString() {
        return "Lead{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", agreementNumber='" + agreementNumber + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}