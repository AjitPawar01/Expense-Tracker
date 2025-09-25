package com.expensetracker.app.models;

public class Company {
    private String companyId;
    private String companyName;
    private double currentBalance;
    private long createdAt;
    private String createdBy; // Admin who created the company

    public Company() {
        // Default constructor required for Firebase
    }

    public Company(String companyId, String companyName, double currentBalance,
                   long createdAt, String createdBy) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.currentBalance = currentBalance;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}