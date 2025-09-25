package com.expensetracker.app.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.models.User;
import com.expensetracker.app.models.Company;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // Company operations
    public void createCompany(Company company, OnCompleteListener listener) {
        Log.d(TAG, "Creating company: " + company.getCompanyName());

        Map<String, Object> companyMap = new HashMap<>();
        companyMap.put("companyName", company.getCompanyName());
        companyMap.put("currentBalance", company.getCurrentBalance());
        companyMap.put("createdAt", company.getCreatedAt());
        companyMap.put("createdBy", company.getCreatedBy());

        db.collection("companies")
                .document(company.getCompanyId())
                .set(companyMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Company created successfully");
                    listener.onSuccess("Company created successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating company: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    public void getCompany(String companyId, OnCompanyListener listener) {
        Log.d(TAG, "Getting company: " + companyId);

        db.collection("companies")
                .document(companyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Company company = document.toObject(Company.class);
                            company.setCompanyId(document.getId());
                            Log.d(TAG, "Company retrieved: " + company.getCompanyName());
                            listener.onSuccess(company);
                        } else {
                            Log.w(TAG, "Company not found: " + companyId);
                            listener.onFailure("Company not found");
                        }
                    } else {
                        Log.e(TAG, "Error getting company: " + task.getException());
                        listener.onFailure("Error getting company: " + task.getException());
                    }
                });
    }

    public void updateCompanyBalance(String companyId, double balance) {
        Log.d(TAG, "Updating company balance: " + companyId + " to " + balance);

        db.collection("companies")
                .document(companyId)
                .update("currentBalance", balance)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Company balance updated successfully"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating company balance: " + e.getMessage()));
    }

    // Transaction operations (updated for company balance)
    public void addTransaction(Transaction transaction, OnCompleteListener listener) {
        Log.d(TAG, "Adding transaction: " + transaction.getType() + " - " + transaction.getAmount());

        Map<String, Object> transactionMap = new HashMap<>();
        transactionMap.put("userId", transaction.getUserId());
        transactionMap.put("companyId", transaction.getCompanyId()); // FIXED: Now includes companyId
        transactionMap.put("type", transaction.getType());
        transactionMap.put("amount", transaction.getAmount());
        transactionMap.put("category", transaction.getCategory());
        transactionMap.put("description", transaction.getDescription());
        transactionMap.put("date", transaction.getDate());
        transactionMap.put("timestamp", transaction.getTimestamp());
        transactionMap.put("openingBalance", transaction.getOpeningBalance());
        transactionMap.put("closingBalance", transaction.getClosingBalance());

        db.collection("transactions")
                .add(transactionMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction added with ID: " + documentReference.getId());
                    // Update company balance instead of user balance
                    updateCompanyBalance(transaction.getCompanyId(), transaction.getClosingBalance());
                    listener.onSuccess("Transaction added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding transaction: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    public void updateTransaction(String transactionId, Transaction transaction, OnCompleteListener listener) {
        Log.d(TAG, "Updating transaction: " + transactionId);

        Map<String, Object> transactionMap = new HashMap<>();
        transactionMap.put("companyId", transaction.getCompanyId()); // FIXED: Include companyId
        transactionMap.put("type", transaction.getType());
        transactionMap.put("amount", transaction.getAmount());
        transactionMap.put("category", transaction.getCategory());
        transactionMap.put("description", transaction.getDescription());
        transactionMap.put("date", transaction.getDate());
        transactionMap.put("timestamp", transaction.getTimestamp());
        transactionMap.put("openingBalance", transaction.getOpeningBalance());
        transactionMap.put("closingBalance", transaction.getClosingBalance());

        db.collection("transactions")
                .document(transactionId)
                .update(transactionMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction updated successfully");
                    // Recalculate all balances after update
                    recalculateAllBalances(transaction.getCompanyId(), listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating transaction: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    public void deleteTransaction(String transactionId, String companyId, OnCompleteListener listener) {
        Log.d(TAG, "Deleting transaction: " + transactionId);

        db.collection("transactions")
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction deleted successfully");
                    // Recalculate all balances after deletion
                    recalculateAllBalances(companyId, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting transaction: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // FIXED: Get transactions by company instead of user
    public void getTransactionsByDate(String companyId, String date, OnTransactionListListener listener) {
        Log.d(TAG, "Getting transactions for companyId: " + companyId + ", date: " + date);

        db.collection("transactions")
                .whereEqualTo("companyId", companyId) // FIXED: Query by companyId
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        }

                        // Sort by timestamp in memory
                        Collections.sort(transactions, (t1, t2) ->
                                Long.compare(t1.getTimestamp(), t2.getTimestamp()));

                        Log.d(TAG, "Retrieved " + transactions.size() + " transactions for " + date);
                        listener.onSuccess(transactions);
                    } else {
                        Log.e(TAG, "Error getting transactions: " + task.getException());
                        listener.onFailure("Error getting transactions: " + task.getException());
                    }
                });
    }

    public void getLastTransactionBefore(String companyId, String date, OnTransactionListener listener) {
        Log.d(TAG, "Getting last transaction before date: " + date + " for company: " + companyId);

        db.collection("transactions")
                .whereEqualTo("companyId", companyId) // FIXED: Query by companyId
                .whereLessThan("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        }

                        // Sort by date and timestamp descending to get the latest
                        Collections.sort(transactions, (t1, t2) -> {
                            int dateCompare = t2.getDate().compareTo(t1.getDate()); // Descending
                            if (dateCompare == 0) {
                                return Long.compare(t2.getTimestamp(), t1.getTimestamp()); // Descending
                            }
                            return dateCompare;
                        });

                        Transaction lastTransaction = transactions.get(0);
                        Log.d(TAG, "Found last transaction before " + date + ": " +
                                lastTransaction.getDate() + " with closing balance: " +
                                lastTransaction.getClosingBalance());

                        listener.onSuccess(lastTransaction);
                    } else {
                        Log.d(TAG, "No transactions found before date: " + date);
                        listener.onSuccess(null); // No previous transaction
                    }
                });
    }

    public void getAllTransactionsForCompany(String companyId, OnTransactionListListener listener) {
        Log.d(TAG, "Getting all transactions for company: " + companyId);

        db.collection("transactions")
                .whereEqualTo("companyId", companyId) // FIXED: Query by companyId
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        }

                        // Sort by date and timestamp ascending
                        Collections.sort(transactions, (t1, t2) -> {
                            int dateCompare = t1.getDate().compareTo(t2.getDate());
                            if (dateCompare == 0) {
                                return Long.compare(t1.getTimestamp(), t2.getTimestamp());
                            }
                            return dateCompare;
                        });

                        Log.d(TAG, "Retrieved " + transactions.size() + " transactions for company");
                        listener.onSuccess(transactions);
                    } else {
                        Log.e(TAG, "Error getting all transactions: " + task.getException());
                        listener.onFailure("Error getting transactions: " + task.getException());
                    }
                });
    }

    public void getTransactionById(String transactionId, OnTransactionListener listener) {
        Log.d(TAG, "Getting transaction by ID: " + transactionId);

        db.collection("transactions")
                .document(transactionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            Log.d(TAG, "Transaction retrieved: " + transaction.getCategory());
                            listener.onSuccess(transaction);
                        } else {
                            Log.w(TAG, "Transaction not found: " + transactionId);
                            listener.onFailure("Transaction not found");
                        }
                    } else {
                        Log.e(TAG, "Error getting transaction: " + task.getException());
                        listener.onFailure("Error getting transaction: " + task.getException());
                    }
                });
    }

    // User operations (FIXED to include companyId)
    public void createUser(User user, OnCompleteListener listener) {
        Log.d(TAG, "Creating user: " + user.getEmail());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("active", user.isActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("currentBalance", user.getCurrentBalance());
        userMap.put("companyId", user.getCompanyId()); // FIXED: Now includes companyId

        db.collection("users")
                .document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created successfully");

                    // Create company if it doesn't exist (for admin users)
                    if ("ADMIN".equals(user.getRole())) {
                        createCompanyIfNotExists(user.getCompanyId(), user.getUid());
                    }

                    listener.onSuccess("User created successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    private void createCompanyIfNotExists(String companyId, String createdBy) {
        getCompany(companyId, new OnCompanyListener() {
            @Override
            public void onSuccess(Company company) {
                Log.d(TAG, "Company already exists: " + companyId);
            }

            @Override
            public void onFailure(String error) {
                // Company doesn't exist, create it
                String companyName = companyId.replace("_", " ").toUpperCase() + " CORP";
                Company newCompany = new Company(companyId, companyName, 0.0,
                        System.currentTimeMillis(), createdBy);

                createCompany(newCompany, new OnCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "New company created: " + companyId);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to create company: " + error);
                    }
                });
            }
        });
    }

    public void getUser(String uid, OnUserListener listener) {
        Log.d(TAG, "Getting user: " + uid);

        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            user.setUid(document.getId());
                            Log.d(TAG, "User retrieved: " + user.getUsername());
                            listener.onSuccess(user);
                        } else {
                            Log.w(TAG, "User not found: " + uid);
                            listener.onFailure("User not found");
                        }
                    } else {
                        Log.e(TAG, "Error getting user: " + task.getException());
                        listener.onFailure("Error getting user: " + task.getException());
                    }
                });
    }

    public void getAllUsers(OnUserListListener listener) {
        Log.d(TAG, "Getting all users");

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUid(document.getId());
                            users.add(user);
                        }

                        // Sort by creation date descending
                        Collections.sort(users, (u1, u2) ->
                                Long.compare(u2.getCreatedAt(), u1.getCreatedAt()));

                        Log.d(TAG, "Retrieved " + users.size() + " users");
                        listener.onSuccess(users);
                    } else {
                        Log.e(TAG, "Error getting users: " + task.getException());
                        listener.onFailure("Error getting users: " + task.getException());
                    }
                });
    }

    public void updateUserStatus(String uid, boolean isActive, OnCompleteListener listener) {
        Log.d(TAG, "Updating user status: " + uid + " to " + isActive);

        db.collection("users")
                .document(uid)
                .update("active", isActive)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User status updated successfully");
                    listener.onSuccess("User status updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user status: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    public void updateTransactionBalances(Transaction transaction) {
        if (transaction.getId() == null || transaction.getId().isEmpty()) {
            Log.w(TAG, "Cannot update transaction balances: transaction ID is null or empty");
            return;
        }

        Log.d(TAG, "Updating transaction balances for ID: " + transaction.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("openingBalance", transaction.getOpeningBalance());
        updates.put("closingBalance", transaction.getClosingBalance());

        db.collection("transactions")
                .document(transaction.getId())
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Transaction balances updated successfully"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating transaction balances: " + e.getMessage()));
    }

    private void recalculateAllBalances(String companyId, OnCompleteListener listener) {
        Log.d(TAG, "Starting balance recalculation for company: " + companyId);

        getAllTransactionsForCompany(companyId, new OnTransactionListListener() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                double runningBalance = 0.0;

                for (Transaction transaction : transactions) {
                    transaction.setOpeningBalance(runningBalance);

                    if ("INCOME".equals(transaction.getType())) {
                        runningBalance += transaction.getAmount();
                    } else if ("EXPENSE".equals(transaction.getType())) {
                        runningBalance -= transaction.getAmount();
                    }

                    transaction.setClosingBalance(runningBalance);

                    // Update in Firebase
                    updateTransactionBalances(transaction);
                }

                updateCompanyBalance(companyId, runningBalance);
                Log.d(TAG, "Balance recalculation completed. Final balance: " + runningBalance);
                listener.onSuccess("Balances recalculated successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error during balance recalculation: " + error);
                listener.onFailure(error);
            }
        });
    }

    // Interfaces
    public interface OnCompleteListener {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface OnTransactionListener {
        void onSuccess(Transaction transaction);
        void onFailure(String error);
    }

    public interface OnTransactionListListener {
        void onSuccess(List<Transaction> transactions);
        void onFailure(String error);
    }

    public interface OnUserListener {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public interface OnUserListListener {
        void onSuccess(List<User> users);
        void onFailure(String error);
    }

    public interface OnCompanyListener {
        void onSuccess(Company company);
        void onFailure(String error);
    }
}