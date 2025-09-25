package com.expensetracker.app.models;

public class Transaction {
    private String id;
    private String userId;
    private String companyId; // New field for company identification
    private String type; // "INCOME" or "EXPENSE"
    private double amount;
    private String category;
    private String description;
    private String date; // Format: YYYY-MM-DD
    private long timestamp;
    private double openingBalance;
    private double closingBalance;

    public Transaction() {
        // Default constructor required for Firebase
    }

    public Transaction(String userId, String companyId, String type, double amount, String category,
                       String description, String date, long timestamp,
                       double openingBalance, double closingBalance) {
        this.userId = userId;
        this.companyId = companyId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.timestamp = timestamp;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
    }

    // Legacy constructor for backward compatibility
    public Transaction(String userId, String type, double amount, String category,
                       String description, String date, long timestamp,
                       double openingBalance, double closingBalance) {
        this.userId = userId;
        this.companyId = "default"; // Default company for existing data
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.timestamp = timestamp;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public double getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(double closingBalance) {
        this.closingBalance = closingBalance;
    }
}