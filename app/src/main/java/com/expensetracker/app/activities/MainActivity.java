package com.expensetracker.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.utils.FirebaseHelper;
import com.expensetracker.app.utils.BalanceCalculator;
import com.expensetracker.app.utils.FirebaseDebugHelper;
import com.expensetracker.app.models.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView tvWelcome, tvDate, tvOpeningBalance, tvClosingBalance, tvTotalIncome, tvTotalExpense;
    private Button btnAddTransaction, btnSearchTransactions, btnManageUsers;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private User userProfile;
    private String todayDate;
    private String userCompanyId; // ADDED: Store user's company ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity onCreate started");

        // Initialize Firebase debug helper
        FirebaseDebugHelper.initializeFirebase();
        FirebaseDebugHelper.enableFirestoreLogging();
        FirebaseDebugHelper.testFirebaseConnection();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getEmail() : "null"));

        if (currentUser == null) {
            Log.d(TAG, "No current user, navigating to login");
            navigateToLogin();
            return;
        }

        // ADDED: Get company ID from user email
        userCompanyId = getCompanyIdFromEmail(currentUser.getEmail());
        Log.d(TAG, "User company ID: " + userCompanyId);

        initViews();
        setupToolbar();
        setupClickListeners();
        setTodayDate();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume");
        if (currentUser != null && areViewsInitialized() && userCompanyId != null) {
            loadDailySummary();
        }
    }

    private boolean areViewsInitialized() {
        return tvOpeningBalance != null && tvClosingBalance != null &&
                tvTotalIncome != null && tvTotalExpense != null;
    }

    private void initViews() {
        Log.d(TAG, "Initializing views...");

        tvWelcome = findViewById(R.id.tvWelcome);
        tvDate = findViewById(R.id.tvDate);
        tvOpeningBalance = findViewById(R.id.tvOpeningBalance);
        tvClosingBalance = findViewById(R.id.tvClosingBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        btnAddTransaction = findViewById(R.id.btnAddTransaction);
        btnSearchTransactions = findViewById(R.id.btnSearchTransactions);
        btnManageUsers = findViewById(R.id.btnManageUsers);

        // Debug: Check which views are null
        Log.d(TAG, "Views initialized:");
        Log.d(TAG, "tvOpeningBalance: " + (tvOpeningBalance != null ? "OK" : "NULL"));
        Log.d(TAG, "tvClosingBalance: " + (tvClosingBalance != null ? "OK" : "NULL"));
        Log.d(TAG, "tvTotalIncome: " + (tvTotalIncome != null ? "OK" : "NULL"));
        Log.d(TAG, "tvTotalExpense: " + (tvTotalExpense != null ? "OK" : "NULL"));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Expense Tracker");
            }
        }
    }

    private void setupClickListeners() {
        if (btnAddTransaction != null) {
            btnAddTransaction.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddTransactionActivity.class);
                startActivity(intent);
            });
        }

        if (btnSearchTransactions != null) {
            btnSearchTransactions.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchTransactionActivity.class);
                startActivity(intent);
            });
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                Intent intent = new Intent(this, ManageUsersActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setTodayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDate = dateFormat.format(new Date());

        if (tvDate != null) {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvDate.setText("Today: " + displayFormat.format(new Date()));
        }

        Log.d(TAG, "Today's date set to: " + todayDate);
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot load user profile: currentUser is null");
            return;
        }

        Log.d(TAG, "Loading user profile for: " + currentUser.getUid());

        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User profile loaded successfully: " + user.getUsername());
                userProfile = user;

                // UPDATED: Use company ID from user profile if available
                if (user.getCompanyId() != null) {
                    userCompanyId = user.getCompanyId();
                    Log.d(TAG, "Updated company ID from profile: " + userCompanyId);
                }

                updateWelcomeMessage();
                updateUIForUserRole();
                loadDailySummary();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load user profile: " + error);
                Toast.makeText(MainActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                // Try to create user profile
                createUserProfileFromAuth();
            }
        });
    }

    private void createUserProfileFromAuth() {
        if (currentUser == null) return;

        Log.d(TAG, "Creating user profile from auth data");

        String username = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
        String email = currentUser.getEmail();
        String role = email != null && (email.equals("admin@expensetracker.com") || email.contains("admin@")) ? "ADMIN" : "USER";

        // Get company ID from email
        String companyId = getCompanyIdFromEmail(email);

        // Use the new constructor with companyId parameter
        User user = new User(currentUser.getUid(), username, email, role,
                true, System.currentTimeMillis(), 0.0, companyId);

        FirebaseHelper.getInstance().createUser(user, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "User profile created successfully");
                userProfile = user;
                userCompanyId = companyId; // UPDATED: Set company ID
                updateWelcomeMessage();
                updateUIForUserRole();
                loadDailySummary();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to create user profile: " + error);
                Toast.makeText(MainActivity.this, "Error creating profile: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            // Convert domain to valid company ID
            String companyId = domain.replace(".", "_").toLowerCase();
            Log.d(TAG, "Extracted company ID: " + companyId + " from email: " + email);
            return companyId;
        }
        return "default_company";
    }

    private void updateWelcomeMessage() {
        if (tvWelcome != null && userProfile != null) {
            tvWelcome.setText("Welcome, " + userProfile.getUsername());
            Log.d(TAG, "Welcome message updated for: " + userProfile.getUsername());
        }
    }

    private void updateUIForUserRole() {
        if (btnManageUsers != null && userProfile != null) {
            if ("ADMIN".equals(userProfile.getRole())) {
                btnManageUsers.setVisibility(View.VISIBLE);
                Log.d(TAG, "Admin UI enabled");
            } else {
                btnManageUsers.setVisibility(View.GONE);
                Log.d(TAG, "Regular user UI enabled");
            }
        }
    }

    private void loadDailySummary() {
        if (userCompanyId == null || todayDate == null) {
            Log.e(TAG, "Cannot load daily summary: userCompanyId or todayDate is null");
            Log.e(TAG, "userCompanyId: " + userCompanyId + ", todayDate: " + todayDate);
            return;
        }

        Log.d(TAG, "Loading daily summary for company: " + userCompanyId + ", date: " + todayDate);

        // UPDATED: Use company-based balance calculation
        BalanceCalculator.getDailySummary(userCompanyId, todayDate,
                new BalanceCalculator.OnDailySummaryListener() {
                    @Override
                    public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                        Log.d(TAG, "Daily summary calculated successfully for company: " + userCompanyId);
                        Log.d(TAG, "Opening: " + summary.openingBalance + ", Closing: " + summary.closingBalance);
                        Log.d(TAG, "Income: " + summary.totalIncome + ", Expense: " + summary.totalExpense);
                        updateSummaryUI(summary);
                    }

                    @Override
                    public void onSummaryError(String error) {
                        Log.e(TAG, "Daily summary error: " + error);
                        // Show default values
                        BalanceCalculator.DailySummary defaultSummary = new BalanceCalculator.DailySummary();
                        updateSummaryUI(defaultSummary);
                        Toast.makeText(MainActivity.this, "Error loading summary: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSummaryUI(BalanceCalculator.DailySummary summary) {
        try {
            if (summary == null) {
                Log.w(TAG, "Summary is null, using default values");
                summary = new BalanceCalculator.DailySummary();
            }

            Log.d(TAG, "Updating summary UI with values:");
            Log.d(TAG, "Opening: ₹" + summary.openingBalance);
            Log.d(TAG, "Closing: ₹" + summary.closingBalance);
            Log.d(TAG, "Income: ₹" + summary.totalIncome);
            Log.d(TAG, "Expense: ₹" + summary.totalExpense);

            if (tvOpeningBalance != null) {
                String openingText = String.format(Locale.getDefault(), "₹%.2f", summary.openingBalance);
                tvOpeningBalance.setText(openingText);
                Log.d(TAG, "Set opening balance: " + openingText);
            }

            if (tvClosingBalance != null) {
                String closingText = String.format(Locale.getDefault(), "₹%.2f", summary.closingBalance);
                tvClosingBalance.setText(closingText);
                Log.d(TAG, "Set closing balance: " + closingText);
            }

            if (tvTotalIncome != null) {
                String incomeText = String.format(Locale.getDefault(), "₹%.2f", summary.totalIncome);
                tvTotalIncome.setText(incomeText);
                Log.d(TAG, "Set total income: " + incomeText);
            }

            if (tvTotalExpense != null) {
                String expenseText = String.format(Locale.getDefault(), "₹%.2f", summary.totalExpense);
                tvTotalExpense.setText(expenseText);
                Log.d(TAG, "Set total expense: " + expenseText);
            }

            Log.d(TAG, "Summary UI updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating summary UI: " + e.getMessage());
            Toast.makeText(this, "Error updating summary: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Log.d(TAG, "Logging out user");
        mAuth.signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Log.d(TAG, "Navigating to login screen");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}