package com.expensetracker.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.utils.FirebaseHelper;
import com.expensetracker.app.utils.BalanceCalculator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {
    private static final String TAG = "AddTransactionActivity";

    private RadioGroup rgTransactionType;
    private RadioButton rbIncome, rbExpense;
    private EditText etAmount, etCategory, etDescription, etDate;
    private Button btnSave, btnSelectDate;
    private ProgressBar progressBar;

    private FirebaseUser currentUser;
    private Calendar selectedDate;
    private String transactionId; // For editing existing transactions
    private Transaction existingTransaction; // Store existing transaction data
    private boolean isEditMode = false;
    private String userCompanyId = "default_company"; // Will be set from user profile
    private MaterialButton btnIncome, btnExpense;
    private boolean isIncomeSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        Log.d(TAG, "AddTransactionActivity started");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No current user found, finishing activity");
            finish();
            return;
        }

        Log.d(TAG, "Current user: " + currentUser.getEmail());

        // Get company ID from email
        userCompanyId = getCompanyIdFromEmail(currentUser.getEmail());

        initViews();
        setupToggleButtons();
        setupToolbar();
        setupClickListeners();
        setDefaultDate();

        // Check if this is edit mode
        transactionId = getIntent().getStringExtra("transaction_id");
        if (transactionId != null && !transactionId.isEmpty()) {
            isEditMode = true;
            loadTransactionForEdit();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Transaction");
            }
            Log.d(TAG, "Edit mode for transaction: " + transactionId);
        }
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            return domain.replace(".", "_").toLowerCase();
        }
        return "default_company";
    }

    private void initViews() {
        rgTransactionType = findViewById(R.id.rgTransactionType);
        rbIncome = findViewById(R.id.rbIncome);
        rbExpense = findViewById(R.id.rbExpense);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        btnSave = findViewById(R.id.btnSave);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        progressBar = findViewById(R.id.progressBar);

        selectedDate = Calendar.getInstance();

        // Validate views
        if (etAmount == null) Log.e(TAG, "etAmount is null");
        if (etCategory == null) Log.e(TAG, "etCategory is null");
        if (etDate == null) Log.e(TAG, "etDate is null");
        if (btnSave == null) Log.e(TAG, "btnSave is null");
        if (btnSelectDate == null) Log.e(TAG, "btnSelectDate is null");
        if (progressBar == null) Log.e(TAG, "progressBar is null");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Add Transaction");
            }
        }
    }

    private void setupClickListeners() {
        if (btnSelectDate != null) {
            btnSelectDate.setOnClickListener(v -> showDatePicker());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (isEditMode) {
                    updateTransaction();
                } else {
                    saveTransaction();
                }
            });
        }
    }

    private void setDefaultDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(selectedDate.getTime());

        if (etDate != null) {
            etDate.setText(todayDate);
        }

        Log.d(TAG, "Default date set to: " + todayDate);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String selectedDateStr = dateFormat.format(selectedDate.getTime());
                    etDate.setText(selectedDateStr);
                    Log.d(TAG, "Date selected: " + selectedDateStr);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadTransactionForEdit() {
        Log.d(TAG, "Loading transaction for edit: " + transactionId);

        if (btnSave != null) {
            btnSave.setText("Update Transaction");
            btnSave.setEnabled(false);
        }

        // Get transaction from Firebase
        FirebaseHelper.getInstance().getTransactionById(transactionId, new FirebaseHelper.OnTransactionListener() {
            @Override
            public void onSuccess(Transaction transaction) {
                Log.d(TAG, "Transaction loaded successfully");
                existingTransaction = transaction;
                populateFields(transaction);

                if (btnSave != null) {
                    btnSave.setEnabled(true);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load transaction: " + error);
                Toast.makeText(AddTransactionActivity.this,
                        "Failed to load transaction: " + error, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void populateFields(Transaction transaction) {
        Log.d(TAG, "Populating fields with transaction data");

        if (etAmount != null) {
            etAmount.setText(String.valueOf(transaction.getAmount()));
        }

        if (etCategory != null) {
            etCategory.setText(transaction.getCategory());
        }

        if (etDescription != null) {
            etDescription.setText(transaction.getDescription());
        }

        if (etDate != null) {
            etDate.setText(transaction.getDate());

            // Update selectedDate calendar
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate.setTime(dateFormat.parse(transaction.getDate()));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
            }
        }

        // Set transaction type
        if (rgTransactionType != null && rbIncome != null && rbExpense != null) {
            if ("INCOME".equals(transaction.getType())) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }
        }
    }

    private void saveTransaction() {
        Log.d(TAG, "Starting to save transaction");

        if (!validateInput()) {
            Log.w(TAG, "Input validation failed");
            return;
        }

        String type = rbIncome != null && rbIncome.isChecked() ? "INCOME" : "EXPENSE";
        double amount = Double.parseDouble(etAmount.getText().toString().trim());
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        Log.d(TAG, "Transaction details:");
        Log.d(TAG, "Type: " + type);
        Log.d(TAG, "Amount: " + amount);
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Date: " + date);
        Log.d(TAG, "Company ID: " + userCompanyId);

        // Show loading state
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
        }

        // CRITICAL FIX: Use company ID for balance calculation instead of user ID
        BalanceCalculator.calculateOpeningBalance(userCompanyId, date, // CHANGED: userCompanyId instead of currentUser.getUid()
                new BalanceCalculator.OnBalanceCalculatedListener() {
                    @Override
                    public void onBalanceCalculated(double openingBalance) {
                        Log.d(TAG, "Opening balance calculated: ₹" + openingBalance);

                        // Calculate closing balance
                        double closingBalance = BalanceCalculator.calculateClosingBalance(
                                openingBalance, type, amount);

                        Log.d(TAG, "Closing balance calculated: ₹" + closingBalance);

                        // Create transaction object with company ID
                        Transaction transaction = new Transaction(
                                currentUser.getUid(),
                                userCompanyId, // IMPORTANT: Include company ID
                                type,
                                amount,
                                category,
                                description,
                                date,
                                System.currentTimeMillis(),
                                openingBalance,
                                closingBalance
                        );

                        // Save to Firebase
                        FirebaseHelper.getInstance().addTransaction(transaction,
                                new FirebaseHelper.OnCompleteListener() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Log.d(TAG, "Transaction saved to Firebase successfully");

                                        // CRITICAL FIX: Use company ID for recalculation instead of user ID
                                        BalanceCalculator.recalculateAllBalancesFromDate(
                                                userCompanyId, date, // CHANGED: userCompanyId instead of currentUser.getUid()
                                                new BalanceCalculator.OnRecalculationCompleteListener() {
                                                    @Override
                                                    public void onRecalculationComplete(String message) {
                                                        Log.d(TAG, "Balance recalculation completed successfully");
                                                        hideLoadingAndFinish("Transaction saved successfully");
                                                    }

                                                    @Override
                                                    public void onRecalculationError(String error) {
                                                        Log.e(TAG, "Balance recalculation failed: " + error);
                                                        hideLoadingAndFinish("Transaction saved but balance update failed");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Failed to save transaction: " + error);
                                        hideLoadingAndShowError("Failed to save transaction: " + error);
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to calculate opening balance: " + error);
                        hideLoadingAndShowError("Error calculating balance: " + error);
                    }
                });
    }

    private void updateTransaction() {
        Log.d(TAG, "Starting to update transaction: " + transactionId);

        if (!validateInput()) {
            Log.w(TAG, "Input validation failed");
            return;
        }

        if (existingTransaction == null) {
            Toast.makeText(this, "Error: No existing transaction data", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = rbIncome != null && rbIncome.isChecked() ? "INCOME" : "EXPENSE";
        double amount = Double.parseDouble(etAmount.getText().toString().trim());
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        Log.d(TAG, "Updated transaction details:");
        Log.d(TAG, "Type: " + type);
        Log.d(TAG, "Amount: " + amount);
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Date: " + date);

        // Show loading state
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Updating...");
        }

        // Calculate new opening balance for the updated date
        BalanceCalculator.calculateOpeningBalance(userCompanyId, date, // CHANGED: userCompanyId instead of currentUser.getUid()
                new BalanceCalculator.OnBalanceCalculatedListener() {
                    @Override
                    public void onBalanceCalculated(double openingBalance) {
                        Log.d(TAG, "Opening balance calculated: ₹" + openingBalance);

                        // Calculate closing balance
                        double closingBalance = BalanceCalculator.calculateClosingBalance(
                                openingBalance, type, amount);

                        Log.d(TAG, "Closing balance calculated: ₹" + closingBalance);

                        // Create transaction object with company ID
                        Transaction transaction = new Transaction(
                                currentUser.getUid(),
                                userCompanyId, // IMPORTANT: Include company ID
                                type,
                                amount,
                                category,
                                description,
                                date,
                                System.currentTimeMillis(),
                                openingBalance,
                                closingBalance
                        );

                        // Save to Firebase
                        FirebaseHelper.getInstance().addTransaction(transaction,
                                new FirebaseHelper.OnCompleteListener() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Log.d(TAG, "Transaction saved to Firebase successfully");

                                        // CRITICAL FIX: Use company ID for recalculation instead of user ID
                                        BalanceCalculator.recalculateAllBalancesFromDate(
                                                userCompanyId, date, // CHANGED: userCompanyId instead of currentUser.getUid()
                                                new BalanceCalculator.OnRecalculationCompleteListener() {
                                                    @Override
                                                    public void onRecalculationComplete(String message) {
                                                        Log.d(TAG, "Balance recalculation completed successfully");
                                                        hideLoadingAndFinish("Transaction saved successfully");
                                                    }

                                                    @Override
                                                    public void onRecalculationError(String error) {
                                                        Log.e(TAG, "Balance recalculation failed: " + error);
                                                        hideLoadingAndFinish("Transaction saved but balance update failed");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Failed to save transaction: " + error);
                                        hideLoadingAndShowError("Failed to save transaction: " + error);
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to calculate opening balance: " + error);
                        hideLoadingAndShowError("Error calculating balance: " + error);
                    }
                });
    }

    private void hideLoadingAndFinish(String message) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText(isEditMode ? "Update Transaction" : "SAVE TRANSACTION");
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void hideLoadingAndShowError(String error) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText(isEditMode ? "Update Transaction" : "SAVE TRANSACTION");
        }

        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private boolean validateInput() {
        Log.d(TAG, "Validating input...");

        // Validate amount
        if (etAmount == null) {
            Toast.makeText(this, "Amount field not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            Log.w(TAG, "Amount is empty");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                etAmount.requestFocus();
                Log.w(TAG, "Amount is not positive: " + amount);
                return false;
            }
            Log.d(TAG, "Amount validation passed: " + amount);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount format");
            etAmount.requestFocus();
            Log.w(TAG, "Invalid amount format: " + amountStr);
            return false;
        }

        // Validate category
        if (etCategory == null) {
            Toast.makeText(this, "Category field not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        String category = etCategory.getText().toString().trim();
        if (category.isEmpty()) {
            etCategory.setError("Category is required");
            etCategory.requestFocus();
            Log.w(TAG, "Category is empty");
            return false;
        }
        Log.d(TAG, "Category validation passed: " + category);

        // Validate date
        if (etDate == null) {
            Toast.makeText(this, "Date field not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        String date = etDate.getText().toString().trim();
        if (date.isEmpty()) {
            etDate.setError("Date is required");
            etDate.requestFocus();
            Log.w(TAG, "Date is empty");
            return false;
        }
        Log.d(TAG, "Date validation passed: " + date);

        // Validate transaction type
        if (rgTransactionType == null) {
            Toast.makeText(this, "Transaction type selection not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        int selectedId = rgTransactionType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select transaction type (Income or Expense)",
                    Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No transaction type selected");
            return false;
        }

        String type = rbIncome != null && rbIncome.isChecked() ? "INCOME" : "EXPENSE";
        Log.d(TAG, "Transaction type validation passed: " + type);

        Log.d(TAG, "All input validation passed");
        return true;
    }

    private void setupToggleButtons() {
        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);

        // Set initial state (Expense selected by default)
        selectExpense();

        btnIncome.setOnClickListener(v -> selectIncome());
        btnExpense.setOnClickListener(v -> selectExpense());

        // Make etDate clickable
        if (etDate != null) {
            etDate.setOnClickListener(v -> showDatePicker());
        }
    }

    private void selectIncome() {
        isIncomeSelected = true;

        // Update button states
        btnIncome.setBackgroundColor(getResources().getColor(R.color.income_color));
        btnIncome.setTextColor(Color.WHITE);
        btnIncome.setIconTintResource(android.R.color.white);

        btnExpense.setBackgroundColor(Color.TRANSPARENT);
        btnExpense.setTextColor(getResources().getColor(R.color.text_primary));
        btnExpense.setIconTintResource(R.color.expense_color);

        // Update hidden radio button
        if (rbIncome != null) {
            rbIncome.setChecked(true);
        }

        Log.d(TAG, "Income selected");
    }

    private void selectExpense() {
        isIncomeSelected = false;

        // Update button states
        btnExpense.setBackgroundColor(getResources().getColor(R.color.expense_color));
        btnExpense.setTextColor(Color.WHITE);
        btnExpense.setIconTintResource(android.R.color.white);

        btnIncome.setBackgroundColor(Color.TRANSPARENT);
        btnIncome.setTextColor(getResources().getColor(R.color.text_primary));
        btnIncome.setIconTintResource(R.color.income_color);

        // Update hidden radio button
        if (rbExpense != null) {
            rbExpense.setChecked(true);
        }

        Log.d(TAG, "Expense selected");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "Back button pressed");
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AddTransactionActivity destroyed");
    }
}