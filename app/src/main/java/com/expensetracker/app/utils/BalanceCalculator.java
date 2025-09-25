package com.expensetracker.app.utils;

import android.util.Log;
import com.expensetracker.app.models.Transaction;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class BalanceCalculator {
    private static final String TAG = "BalanceCalculator";

    /**
     * Calculate opening balance for a new transaction (UPDATED for company-based)
     * Logic: Previous day's closing balance becomes today's opening balance
     */
    public static void calculateOpeningBalance(String companyId, String date,
                                               OnBalanceCalculatedListener listener) {
        Log.d(TAG, "Calculating opening balance for company: " + companyId + ", date: " + date);

        FirebaseHelper.getInstance().getLastTransactionBefore(companyId, date,
                new FirebaseHelper.OnTransactionListener() {
                    @Override
                    public void onSuccess(Transaction lastTransaction) {
                        double openingBalance = 0.0;
                        if (lastTransaction != null) {
                            openingBalance = lastTransaction.getClosingBalance();
                            Log.d(TAG, "Found previous transaction with closing balance: " + openingBalance);
                        } else {
                            Log.d(TAG, "No previous transactions found, opening balance = 0.0");
                        }
                        listener.onBalanceCalculated(openingBalance);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error calculating opening balance: " + error);
                        listener.onError(error);
                    }
                });
    }

    /**
     * Calculate closing balance after adding a new transaction
     * Fixed logic: Income adds to balance, Expense subtracts from balance
     */
    public static double calculateClosingBalance(double openingBalance,
                                                 String transactionType,
                                                 double amount) {
        double closingBalance = openingBalance;

        Log.d(TAG, "Calculating closing balance:");
        Log.d(TAG, "Opening Balance: " + openingBalance);
        Log.d(TAG, "Transaction Type: " + transactionType);
        Log.d(TAG, "Amount: " + amount);

        if ("INCOME".equals(transactionType)) {
            closingBalance = openingBalance + amount;
            Log.d(TAG, "INCOME: " + openingBalance + " + " + amount + " = " + closingBalance);
        } else if ("EXPENSE".equals(transactionType)) {
            closingBalance = openingBalance - amount;
            Log.d(TAG, "EXPENSE: " + openingBalance + " - " + amount + " = " + closingBalance);
        }

        return closingBalance;
    }

    /**
     * Recalculate all balances for a company when a past transaction is modified (UPDATED)
     */
    public static void recalculateAllBalancesFromDate(String companyId, String fromDate,
                                                      OnRecalculationCompleteListener listener) {
        Log.d(TAG, "Recalculating all balances from date: " + fromDate + " for company: " + companyId);

        FirebaseHelper.getInstance().getAllTransactionsForCompany(companyId,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> allTransactions) {
                        Log.d(TAG, "Retrieved " + allTransactions.size() + " transactions for recalculation");

                        // Sort transactions by date and timestamp
                        Collections.sort(allTransactions, new Comparator<Transaction>() {
                            @Override
                            public int compare(Transaction t1, Transaction t2) {
                                int dateCompare = t1.getDate().compareTo(t2.getDate());
                                if (dateCompare == 0) {
                                    return Long.compare(t1.getTimestamp(), t2.getTimestamp());
                                }
                                return dateCompare;
                            }
                        });

                        // Recalculate all balances from the beginning
                        double runningBalance = 0.0;

                        for (Transaction transaction : allTransactions) {
                            // Set opening balance
                            transaction.setOpeningBalance(runningBalance);

                            // Calculate new closing balance
                            if ("INCOME".equals(transaction.getType())) {
                                runningBalance += transaction.getAmount();
                            } else if ("EXPENSE".equals(transaction.getType())) {
                                runningBalance -= transaction.getAmount();
                            }

                            transaction.setClosingBalance(runningBalance);

                            Log.d(TAG, "Updated transaction " + transaction.getId() +
                                    " - Opening: " + transaction.getOpeningBalance() +
                                    ", Closing: " + transaction.getClosingBalance());

                            // Update in Firebase
                            updateTransactionBalances(transaction);
                        }

                        // Update company's current balance
                        FirebaseHelper.getInstance().updateCompanyBalance(companyId, runningBalance);

                        listener.onRecalculationComplete("Balances recalculated successfully");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error during recalculation: " + error);
                        listener.onRecalculationError(error);
                    }
                });
    }

    /**
     * Get daily summary for a specific date with corrected balance calculations (UPDATED for company)
     */
    public static void getDailySummary(String companyId, String date,
                                       OnDailySummaryListener listener) {
        Log.d(TAG, "Getting daily summary for date: " + date + ", company: " + companyId);

        // First get opening balance from previous day
        FirebaseHelper.getInstance().getLastTransactionBefore(companyId, date,
                new FirebaseHelper.OnTransactionListener() {
                    @Override
                    public void onSuccess(Transaction lastTransaction) {
                        double openingBalance = 0.0;
                        if (lastTransaction != null) {
                            openingBalance = lastTransaction.getClosingBalance();
                            Log.d(TAG, "Opening balance from previous transaction: " + openingBalance);
                        }

                        final double finalOpeningBalance = openingBalance;

                        // Now get transactions for this specific date
                        FirebaseHelper.getInstance().getTransactionsByDate(companyId, date,
                                new FirebaseHelper.OnTransactionListListener() {
                                    @Override
                                    public void onSuccess(List<Transaction> transactions) {
                                        DailySummary summary = new DailySummary();
                                        summary.openingBalance = finalOpeningBalance;

                                        Log.d(TAG, "Found " + transactions.size() + " transactions for " + date);

                                        if (transactions.isEmpty()) {
                                            // No transactions for this date, closing = opening
                                            summary.closingBalance = finalOpeningBalance;
                                        } else {
                                            // Sort transactions by timestamp
                                            Collections.sort(transactions, (t1, t2) ->
                                                    Long.compare(t1.getTimestamp(), t2.getTimestamp()));

                                            double runningBalance = finalOpeningBalance;

                                            for (Transaction transaction : transactions) {
                                                if ("INCOME".equals(transaction.getType())) {
                                                    summary.totalIncome += transaction.getAmount();
                                                    runningBalance += transaction.getAmount();
                                                } else if ("EXPENSE".equals(transaction.getType())) {
                                                    summary.totalExpense += transaction.getAmount();
                                                    runningBalance -= transaction.getAmount();
                                                }
                                            }

                                            summary.closingBalance = runningBalance;
                                        }

                                        Log.d(TAG, "Summary calculated - Opening: " + summary.openingBalance +
                                                ", Closing: " + summary.closingBalance +
                                                ", Income: " + summary.totalIncome +
                                                ", Expense: " + summary.totalExpense);

                                        listener.onSummaryCalculated(summary);
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Error getting transactions for summary: " + error);
                                        listener.onSummaryError(error);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error getting opening balance for summary: " + error);
                        // Still try to get summary with 0 opening balance
                        FirebaseHelper.getInstance().getTransactionsByDate(companyId, date,
                                new FirebaseHelper.OnTransactionListListener() {
                                    @Override
                                    public void onSuccess(List<Transaction> transactions) {
                                        DailySummary summary = new DailySummary();
                                        // Opening balance = 0 if no previous transactions

                                        double runningBalance = 0.0;
                                        for (Transaction transaction : transactions) {
                                            if ("INCOME".equals(transaction.getType())) {
                                                summary.totalIncome += transaction.getAmount();
                                                runningBalance += transaction.getAmount();
                                            } else if ("EXPENSE".equals(transaction.getType())) {
                                                summary.totalExpense += transaction.getAmount();
                                                runningBalance -= transaction.getAmount();
                                            }
                                        }

                                        summary.closingBalance = runningBalance;
                                        listener.onSummaryCalculated(summary);
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        listener.onSummaryError(error);
                                    }
                                });
                    }
                });
    }

    private static void updateTransactionBalances(Transaction transaction) {
        // Update transaction balances in Firebase
        FirebaseHelper.getInstance().updateTransactionBalances(transaction);
    }

    // Data classes
    public static class DailySummary {
        public double openingBalance = 0.0;
        public double closingBalance = 0.0;
        public double totalIncome = 0.0;
        public double totalExpense = 0.0;

        @Override
        public String toString() {
            return "DailySummary{" +
                    "openingBalance=" + openingBalance +
                    ", closingBalance=" + closingBalance +
                    ", totalIncome=" + totalIncome +
                    ", totalExpense=" + totalExpense +
                    '}';
        }
    }

    // Interfaces
    public interface OnBalanceCalculatedListener {
        void onBalanceCalculated(double openingBalance);
        void onError(String error);
    }

    public interface OnRecalculationCompleteListener {
        void onRecalculationComplete(String message);
        void onRecalculationError(String error);
    }

    public interface OnDailySummaryListener {
        void onSummaryCalculated(DailySummary summary);
        void onSummaryError(String error);
    }
}