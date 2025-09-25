package com.expensetracker.app.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String role; // "ADMIN" or "USER"
    private boolean active;
    private long createdAt;
    private double currentBalance;
    private String companyId; // New field for company identification

    public User() {
        // Default constructor required for Firebase
    }

    public User(String uid, String username, String email, String role,
                boolean active, long createdAt, double currentBalance, String companyId) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.currentBalance = currentBalance;
        this.companyId = companyId;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
}