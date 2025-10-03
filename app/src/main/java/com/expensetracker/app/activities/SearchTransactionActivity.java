package com.expensetracker.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.adapters.TransactionAdapter;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.models.User;
import com.expensetracker.app.utils.FirebaseHelper;
import com.expensetracker.app.utils.BalanceCalculator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchTransactionActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {
    private static final String TAG = "SearchTransactionActivity";

    private EditText etSearchDate;
    private Button btnDatePicker, btnSearch;
    private TextView tvOpeningBalance, tvClosingBalance, tvTotalIncome, tvTotalExpense;
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;

    private FirebaseUser currentUser;
    private User userProfile;
    private Calendar selectedDate;
    private List<Transaction> transactions;
    private String userCompanyId; // ADDED: Store user's company ID

    private ChipGroup chipGroupDates;
    private Chip chipToday, chipYesterday, chipThisWeek, chipThisMonth;
    private CardView summaryCard;
    private LinearLayout transactionsHeader, emptyState;
    private FrameLayout loadingOverlay;
    private TextView tvTransactionCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_transaction);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        Log.d(TAG, "SearchTransactionActivity started for user: " + currentUser.getEmail());

        // ADDED: Get company ID from email
        userCompanyId = getCompanyIdFromEmail(currentUser.getEmail());
        Log.d(TAG, "User company ID: " + userCompanyId);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadUserProfile();
        setDefaultDate();
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            return domain.replace(".", "_").toLowerCase();
        }
        return "default_company";
    }

    private void initViews() {
        etSearchDate = findViewById(R.id.etSearchDate);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnSearch = findViewById(R.id.btnSearch);
        tvOpeningBalance = findViewById(R.id.tvOpeningBalance);
        tvClosingBalance = findViewById(R.id.tvClosingBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        rvTransactions = findViewById(R.id.rvTransactions);

        selectedDate = Calendar.getInstance();
        transactions = new ArrayList<>();

        chipGroupDates = findViewById(R.id.chipGroupDates);
        chipToday = findViewById(R.id.chipToday);
        chipYesterday = findViewById(R.id.chipYesterday);
        chipThisWeek = findViewById(R.id.chipThisWeek);
        chipThisMonth = findViewById(R.id.chipThisMonth);
        summaryCard = findViewById(R.id.summaryCard);
        transactionsHeader = findViewById(R.id.transactionsHeader);
        emptyState = findViewById(R.id.emptyState);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Transactions");
        }
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(transactions, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void setupClickListeners() {
        btnDatePicker.setOnClickListener(v -> showDatePicker());
        btnSearch.setOnClickListener(v -> searchTransactions());
        chipToday.setOnClickListener(v -> setDateToToday());
        chipYesterday.setOnClickListener(v -> setDateToYesterday());
        chipThisWeek.setOnClickListener(v -> Toast.makeText(this, "Week view coming soon", Toast.LENGTH_SHORT).show());
        chipThisMonth.setOnClickListener(v -> Toast.makeText(this, "Month view coming soon", Toast.LENGTH_SHORT).show());
    }

    private void loadUserProfile() {
        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                userProfile = user;
                transactionAdapter.setUserRole(user.getRole());

                // ADDED: Update company ID from user profile if available
                if (user.getCompanyId() != null) {
                    userCompanyId = user.getCompanyId();
                    Log.d(TAG, "Updated company ID from profile: " + userCompanyId);
                }

                // Trigger initial search after profile loads
                searchTransactions();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading user profile: " + error);
                Toast.makeText(SearchTransactionActivity.this,
                        "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDefaultDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
        Log.d(TAG, "Default search date set to: " + dateFormat.format(selectedDate.getTime()));
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
                    Log.d(TAG, "Date selected: " + dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void searchTransactions() {
        String searchDate = etSearchDate.getText().toString().trim();

        if (searchDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userCompanyId == null) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Searching transactions for company: " + userCompanyId + ", date: " + searchDate);

        showLoading();

        BalanceCalculator.getDailySummary(userCompanyId, searchDate,
                new BalanceCalculator.OnDailySummaryListener() {
                    @Override
                    public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                        Log.d(TAG, "Daily summary loaded for " + searchDate);
                        updateSummaryUI(summary);
                        loadTransactionsForDate(searchDate);
                    }

                    @Override
                    public void onSummaryError(String error) {
                        Log.e(TAG, "Error loading summary: " + error);
                        hideLoading();
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error loading summary: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTransactionsForDate(String date) {
        Log.d(TAG, "Loading transactions for company: " + userCompanyId + ", date: " + date);

        FirebaseHelper.getInstance().getTransactionsByDate(userCompanyId, date,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> transactionList) {
                        hideLoading();

                        Log.d(TAG, "Loaded " + transactionList.size() + " transactions for " + date);

                        transactions.clear();
                        transactions.addAll(transactionList);
                        transactionAdapter.notifyDataSetChanged();

                        if (transactionList.isEmpty()) {
                            showEmptyState();
                            Toast.makeText(SearchTransactionActivity.this,
                                    "No transactions found for this date", Toast.LENGTH_SHORT).show();
                        } else {
                            showResults(transactionList.size());
                            Toast.makeText(SearchTransactionActivity.this,
                                    "Found " + transactionList.size() + " transactions", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading transactions: " + error);
                        hideLoading();
                        showEmptyState();
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error loading transactions: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSummaryUI(BalanceCalculator.DailySummary summary) {
        if (summary == null) {
            Log.w(TAG, "Summary is null, using default values");
            summary = new BalanceCalculator.DailySummary();
        }

        Log.d(TAG, "Updating summary UI:");
        Log.d(TAG, "Opening: ₹" + summary.openingBalance);
        Log.d(TAG, "Closing: ₹" + summary.closingBalance);
        Log.d(TAG, "Income: ₹" + summary.totalIncome);
        Log.d(TAG, "Expense: ₹" + summary.totalExpense);

        tvOpeningBalance.setText(String.format(Locale.getDefault(), "₹%.2f", summary.openingBalance));
        tvClosingBalance.setText(String.format(Locale.getDefault(), "₹%.2f", summary.closingBalance));
        tvTotalIncome.setText(String.format(Locale.getDefault(), "₹%.2f", summary.totalIncome));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "₹%.2f", summary.totalExpense));
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        // Handle transaction click - could show details or edit
        if (userProfile != null && "ADMIN".equals(userProfile.getRole())) {
            showTransactionOptions(transaction);
        }
    }

    @Override
    public void onEditClick(Transaction transaction) {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        intent.putExtra("transaction_id", transaction.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        // Show confirmation dialog and delete
        deleteTransaction(transaction);
    }

    private void showTransactionOptions(Transaction transaction) {
        // Show dialog with edit/delete options for admin
        Toast.makeText(this, "Admin options for transaction", Toast.LENGTH_SHORT).show();
    }

    private void deleteTransaction(Transaction transaction) {
        Log.d(TAG, "Deleting transaction: " + transaction.getId());

        // UPDATED: Pass company ID instead of user ID for deletion
        FirebaseHelper.getInstance().deleteTransaction(transaction.getId(), userCompanyId,
                new FirebaseHelper.OnCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Transaction deleted successfully");
                        Toast.makeText(SearchTransactionActivity.this,
                                "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                        searchTransactions(); // Refresh the list
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error deleting transaction: " + error);
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error deleting transaction: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setDateToToday() {
        selectedDate = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
        searchTransactions();
    }

    private void setDateToYesterday() {
        selectedDate = Calendar.getInstance();
        selectedDate.add(Calendar.DAY_OF_MONTH, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
        searchTransactions();
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
        btnSearch.setEnabled(true);
    }

    private void showResults(int transactionCount) {
        emptyState.setVisibility(View.GONE);
        summaryCard.setVisibility(View.VISIBLE);
        transactionsHeader.setVisibility(View.VISIBLE);

        String countText = transactionCount + " Transaction" + (transactionCount != 1 ? "s" : "");
        tvTransactionCount.setText(countText);
    }

    private void showEmptyState() {
        summaryCard.setVisibility(View.GONE);
        transactionsHeader.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh transactions when returning from edit
        if (!etSearchDate.getText().toString().isEmpty() && userCompanyId != null) {
            searchTransactions();
        }
    }
}