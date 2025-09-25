package com.expensetracker.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensetracker.app.R;
import com.expensetracker.app.adapters.TransactionAdapter;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.models.User;
import com.expensetracker.app.utils.BalanceCalculator;
import com.expensetracker.app.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchTransactionActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {
    private EditText etSearchDate;
    private Button btnDatePicker, btnSearch;
    private TextView tvOpeningBalance, tvClosingBalance, tvTotalIncome, tvTotalExpense;
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;

    private FirebaseUser currentUser;
    private User userProfile;
    private Calendar selectedDate;
    private List<Transaction> transactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_transaction);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadUserProfile();
        setDefaultDate();
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
    }

    private void loadUserProfile() {
        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                userProfile = user;
                transactionAdapter.setUserRole(user.getRole());
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private void setDefaultDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
        searchTransactions(); // Search for today's transactions by default
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etSearchDate.setText(dateFormat.format(selectedDate.getTime()));
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

        // Show loading state
        btnSearch.setEnabled(false);
        btnSearch.setText("Searching...");

        // Get daily summary first
        BalanceCalculator.getDailySummary(currentUser.getUid(), searchDate,
                new BalanceCalculator.OnDailySummaryListener() {
                    @Override
                    public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                        updateSummaryUI(summary);
                        loadTransactionsForDate(searchDate);
                    }

                    @Override
                    public void onSummaryError(String error) {
                        btnSearch.setEnabled(true);
                        btnSearch.setText("SEARCH");
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error loading summary: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTransactionsForDate(String date) {
        FirebaseHelper.getInstance().getTransactionsByDate(currentUser.getUid(), date,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> transactionList) {
                        btnSearch.setEnabled(true);
                        btnSearch.setText("SEARCH");

                        transactions.clear();
                        transactions.addAll(transactionList);
                        transactionAdapter.notifyDataSetChanged();

                        if (transactionList.isEmpty()) {
                            Toast.makeText(SearchTransactionActivity.this,
                                    "No transactions found for this date", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        btnSearch.setEnabled(true);
                        btnSearch.setText("SEARCH");
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error loading transactions: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSummaryUI(BalanceCalculator.DailySummary summary) {
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
        // Implementation would show AlertDialog with options
        Toast.makeText(this, "Admin options for transaction", Toast.LENGTH_SHORT).show();
    }

    private void deleteTransaction(Transaction transaction) {
        FirebaseHelper.getInstance().deleteTransaction(transaction.getId(), currentUser.getUid(),
                new FirebaseHelper.OnCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(SearchTransactionActivity.this,
                                "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                        searchTransactions(); // Refresh the list
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(SearchTransactionActivity.this,
                                "Error deleting transaction: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
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
        if (!etSearchDate.getText().toString().isEmpty()) {
            searchTransactions();
        }
    }
}