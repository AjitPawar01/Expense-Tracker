package com.expensetracker.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.adapters.UserAdapter;
import com.expensetracker.app.models.User;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private static final String TAG = "ManageUsersActivity";

    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private FirebaseUser currentUser;
    private List<User> users;
    private String adminCompanyId;
    private TextView tvTotalUsers, tvActiveUsers, tvAdminCount;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipActiveOnly, chipAdmins, chipRegularUsers;
    private LinearLayout emptyState;
    private FrameLayout loadingOverlay;
    private List<User> allUsersList = new ArrayList<>();
    private ExtendedFloatingActionButton fabAddUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        Log.d(TAG, "ManageUsersActivity started");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No current user found, finishing activity");
            finish();
            return;
        }

        Log.d(TAG, "ManageUsersActivity started by: " + currentUser.getEmail());

        // Get admin's company ID from email
        adminCompanyId = getCompanyIdFromEmail(currentUser.getEmail());
        Log.d(TAG, "Admin company ID: " + adminCompanyId);

        initViews();
        setupToolbar();
        setupFilterChips();
        setupRecyclerView();
        setupFab();

        // Check admin access and load users
        checkAdminAccess();
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            String companyId = domain.replace(".", "_").toLowerCase();
            Log.d(TAG, "Extracted company ID: " + companyId + " from email: " + email);
            return companyId;
        }
        return "default_company";
    }

    private void checkAdminAccess() {
        Log.d(TAG, "Checking admin access for user: " + currentUser.getUid());

        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User profile loaded - Role: " + user.getRole());

                if (!"ADMIN".equals(user.getRole())) {
                    Toast.makeText(ManageUsersActivity.this,
                            "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // Update admin company ID from user profile if available
                if (user.getCompanyId() != null) {
                    adminCompanyId = user.getCompanyId();
                    Log.d(TAG, "Updated admin company ID from profile: " + adminCompanyId);
                }

                // Load users after admin verification
                loadUsers();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error verifying admin access: " + error);
                Toast.makeText(ManageUsersActivity.this,
                        "Error verifying admin access: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        fabAddUser = findViewById(R.id.fabAddUser); // This will now work correctly
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvAdminCount = findViewById(R.id.tvAdminCount);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipActiveOnly = findViewById(R.id.chipActiveOnly);
        chipAdmins = findViewById(R.id.chipAdmins);
        chipRegularUsers = findViewById(R.id.chipRegularUsers);
        emptyState = findViewById(R.id.emptyState);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        users = new ArrayList<>();

        if (rvUsers == null) {
            Log.e(TAG, "rvUsers is null - check layout file");
        }
        if (fabAddUser == null) {
            Log.e(TAG, "fabAddUser is null - check layout file");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Manage Users");
            }
        }
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> filterUsers("all"));
        chipActiveOnly.setOnClickListener(v -> filterUsers("active"));
        chipAdmins.setOnClickListener(v -> filterUsers("admins"));
        chipRegularUsers.setOnClickListener(v -> filterUsers("users"));
    }

    private void filterUsers(String filterType) {
        List<User> filteredList = new ArrayList<>();

        for (User user : allUsersList) {
            switch (filterType) {
                case "all":
                    filteredList.add(user);
                    break;
                case "active":
                    if (user.isActive()) {
                        filteredList.add(user);
                    }
                    break;
                case "admins":
                    if ("ADMIN".equals(user.getRole())) {
                        filteredList.add(user);
                    }
                    break;
                case "users":
                    if ("USER".equals(user.getRole())) {
                        filteredList.add(user);
                    }
                    break;
            }
        }

        users.clear();
        users.addAll(filteredList);
        userAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats() {
        int totalUsers = allUsersList.size();
        int activeUsers = 0;
        int adminCount = 0;

        for (User user : allUsersList) {
            if (user.isActive()) {
                activeUsers++;
            }
            if ("ADMIN".equals(user.getRole())) {
                adminCount++;
            }
        }

        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvActiveUsers.setText(String.valueOf(activeUsers));
        tvAdminCount.setText(String.valueOf(adminCount));
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(users, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
        Log.d(TAG, "RecyclerView setup completed");
    }

    private void setupFab() {
        if (fabAddUser != null) {
            fabAddUser.setOnClickListener(v -> showAddUserDialog());
        }
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null);

        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        builder.setView(dialogView)
                .setTitle("Add New User")
                .setPositiveButton("Create", null) // Set to null initially to override later
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismiss on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateUserInput(username, email, password)) {
                createNewUser(username, email, password, dialog);
            }
        });
    }

    private boolean validateUserInput(String username, String email, String password) {
        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if email belongs to the same company
        String emailCompanyId = getCompanyIdFromEmail(email);
        if (!adminCompanyId.equals(emailCompanyId)) {
            Toast.makeText(this, "Email must belong to your company domain", Toast.LENGTH_LONG).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createNewUser(String username, String email, String password, AlertDialog dialog) {
        Log.d(TAG, "Creating new user with email: " + email);

        // Show loading state
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Creating...");

        // Create Firebase Authentication user
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String uid = task.getResult().getUser().getUid();
                        Log.d(TAG, "Firebase Auth user created with UID: " + uid);

                        // Create user profile with USER role (hardcoded)
                        String userCompanyId = getCompanyIdFromEmail(email);
                        User newUser = new User(
                                uid,
                                username,
                                email,
                                "USER", // Always USER role
                                true, // Active by default
                                System.currentTimeMillis(),
                                0.0, // Initial balance
                                userCompanyId
                        );

                        // Save to Firestore
                        FirebaseHelper.getInstance().createUser(newUser, new FirebaseHelper.OnCompleteListener() {
                            @Override
                            public void onSuccess(String message) {
                                Log.d(TAG, "User profile created successfully");

                                // Sign out the newly created user and sign back in as admin
                                FirebaseAuth.getInstance().signOut();
                                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                                        currentUser.getEmail(),
                                        "" // Admin needs to re-authenticate
                                );

                                runOnUiThread(() -> {
                                    Toast.makeText(ManageUsersActivity.this,
                                            "User created successfully", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadUsers(); // Refresh the list
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Failed to create user profile: " + error);
                                runOnUiThread(() -> {
                                    Toast.makeText(ManageUsersActivity.this,
                                            "Failed to create user profile: " + error,
                                            Toast.LENGTH_LONG).show();
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Create");
                                });
                            }
                        });
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to create Firebase Auth user: " + errorMessage);

                        runOnUiThread(() -> {
                            Toast.makeText(ManageUsersActivity.this,
                                    "Failed to create user: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Create");
                        });
                    }
                });
    }

    private void loadUsers() {
        Log.d(TAG, "Loading users for company: " + adminCompanyId);
        showLoading();

        FirebaseHelper.getInstance().getAllUsers(new FirebaseHelper.OnUserListListener() {
            @Override
            public void onSuccess(List<User> allUsers) {
                Log.d(TAG, "Retrieved " + allUsers.size() + " total users from Firebase");

                // Filter users by company ID
                List<User> companyUsers = new ArrayList<>();

                for (User user : allUsers) {
                    String userCompanyId = user.getCompanyId();

                    // Handle users without companyId (legacy data)
                    if (userCompanyId == null || userCompanyId.isEmpty()) {
                        userCompanyId = getCompanyIdFromEmail(user.getEmail());
                        Log.d(TAG, "User " + user.getEmail() + " missing companyId, derived: " + userCompanyId);
                    }

                    Log.d(TAG, "Checking user: " + user.getEmail() +
                            ", UserCompanyId: " + userCompanyId +
                            ", AdminCompanyId: " + adminCompanyId);

                    // Include user if they belong to the same company as admin
                    if (adminCompanyId != null && adminCompanyId.equals(userCompanyId)) {
                        companyUsers.add(user);
                        Log.d(TAG, "Added user to company list: " + user.getEmail());
                    }
                }

                Log.d(TAG, "Filtered to " + companyUsers.size() + " users in company: " + adminCompanyId);

                // Update UI on main thread
                runOnUiThread(() -> {
                    hideLoading();
                    allUsersList.clear();
                    allUsersList.addAll(companyUsers);

                    users.clear();
                    users.addAll(companyUsers);
                    userAdapter.notifyDataSetChanged();

                    updateStats();

                    if (companyUsers.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvUsers.setVisibility(View.GONE);
                        Toast.makeText(ManageUsersActivity.this,
                                "No users found in your company", Toast.LENGTH_SHORT).show();
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvUsers.setVisibility(View.VISIBLE);
                        Toast.makeText(ManageUsersActivity.this,
                                "Found " + companyUsers.size() + " users in company",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading users: " + error);
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(ManageUsersActivity.this,
                            "Error loading users: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onUserClick(User user) {
        showUserOptionsDialog(user);
    }

    @Override
    public void onToggleStatus(User user) {
        toggleUserStatus(user);
    }

    private void showUserOptionsDialog(User user) {
        String[] options = {"View Details", "Toggle Status", "View Company Transactions", "View Company Summary"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User: " + user.getUsername())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showUserDetails(user);
                            break;
                        case 1:
                            toggleUserStatus(user);
                            break;
                        case 2:
                            viewCompanyTransactions(user);
                            break;
                        case 3:
                            viewCompanySummary(user);
                            break;
                    }
                })
                .show();
    }

    private void showUserDetails(User user) {
        StringBuilder details = new StringBuilder();
        details.append("Username: ").append(user.getUsername()).append("\n");
        details.append("Email: ").append(user.getEmail()).append("\n");
        details.append("Role: ").append(user.getRole()).append("\n");
        details.append("Status: ").append(user.isActive() ? "Active" : "Inactive").append("\n");

        String userCompanyId = user.getCompanyId();
        if (userCompanyId == null) {
            userCompanyId = getCompanyIdFromEmail(user.getEmail());
        }
        details.append("Company ID: ").append(userCompanyId).append("\n");
        details.append("Individual Balance: ₹").append(String.format("%.2f", user.getCurrentBalance()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void toggleUserStatus(User user) {
        // Don't allow admin to deactivate themselves
        if (user.getUid().equals(currentUser.getUid())) {
            Toast.makeText(this, "Cannot change your own status", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newStatus = !user.isActive();
        String action = newStatus ? "activate" : "deactivate";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Action")
                .setMessage("Are you sure you want to " + action + " user: " + user.getUsername() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d(TAG, "Toggling user status: " + user.getUid() + " to " + newStatus);

                    FirebaseHelper.getInstance().updateUserStatus(user.getUid(), newStatus,
                            new FirebaseHelper.OnCompleteListener() {
                                @Override
                                public void onSuccess(String message) {
                                    Log.d(TAG, "User status updated successfully");
                                    Toast.makeText(ManageUsersActivity.this,
                                            "User status updated successfully", Toast.LENGTH_SHORT).show();
                                    loadUsers(); // Refresh the list
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e(TAG, "Error updating user status: " + error);
                                    Toast.makeText(ManageUsersActivity.this,
                                            "Error updating user status: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void viewCompanyTransactions(User user) {
        String userCompanyId = user.getCompanyId();
        if (userCompanyId == null) {
            userCompanyId = getCompanyIdFromEmail(user.getEmail());
        }

        Log.d(TAG, "Viewing transactions for company: " + userCompanyId);

        String finalUserCompanyId = userCompanyId;
        FirebaseHelper.getInstance().getAllTransactionsForCompany(userCompanyId,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> transactions) {
                        Log.d(TAG, "Retrieved " + transactions.size() + " transactions for company");

                        if (transactions.isEmpty()) {
                            Toast.makeText(ManageUsersActivity.this,
                                    "No transactions found for this company", Toast.LENGTH_SHORT).show();
                        } else {
                            showTransactionsDialog("Company Transactions (" +
                                    finalUserCompanyId.replace("_", ".") + ")", transactions);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading company transactions: " + error);
                        Toast.makeText(ManageUsersActivity.this,
                                "Error loading company transactions: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void viewCompanySummary(User user) {
        String userCompanyId = user.getCompanyId();
        if (userCompanyId == null) {
            userCompanyId = getCompanyIdFromEmail(user.getEmail());
        }

        Log.d(TAG, "Getting company summary for: " + userCompanyId);

        String finalUserCompanyId = userCompanyId;
        FirebaseHelper.getInstance().getAllTransactionsForCompany(userCompanyId,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> transactions) {
                        StringBuilder summary = new StringBuilder();
                        double totalIncome = 0, totalExpense = 0;
                        int incomeCount = 0, expenseCount = 0;

                        for (Transaction transaction : transactions) {
                            if ("INCOME".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                                incomeCount++;
                            } else if ("EXPENSE".equals(transaction.getType())) {
                                totalExpense += transaction.getAmount();
                                expenseCount++;
                            }
                        }

                        double currentBalance = totalIncome - totalExpense;

                        summary.append("Company: ").append(finalUserCompanyId.replace("_", ".")).append("\n\n");
                        summary.append("Total Transactions: ").append(transactions.size()).append("\n");
                        summary.append("Income Transactions: ").append(incomeCount).append("\n");
                        summary.append("Expense Transactions: ").append(expenseCount).append("\n\n");
                        summary.append("Total Income: ₹").append(String.format("%.2f", totalIncome)).append("\n");
                        summary.append("Total Expense: ₹").append(String.format("%.2f", totalExpense)).append("\n");
                        summary.append("Current Balance: ₹").append(String.format("%.2f", currentBalance)).append("\n\n");
                        summary.append("Users in Company: ").append(users.size());

                        AlertDialog.Builder builder = new AlertDialog.Builder(ManageUsersActivity.this);
                        builder.setTitle("Company Summary")
                                .setMessage(summary.toString())
                                .setPositiveButton("OK", null)
                                .show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading company summary: " + error);
                        Toast.makeText(ManageUsersActivity.this,
                                "Error loading company summary: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTransactionsDialog(String title, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage("No transactions found.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        StringBuilder transactionList = new StringBuilder();
        double totalIncome = 0, totalExpense = 0;

        for (Transaction transaction : transactions) {
            transactionList.append(transaction.getDate())
                    .append(" - ")
                    .append(transaction.getType())
                    .append(": ₹")
                    .append(String.format("%.2f", transaction.getAmount()))
                    .append(" (")
                    .append(transaction.getCategory())
                    .append(")\n");

            if ("INCOME".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            } else if ("EXPENSE".equals(transaction.getType())) {
                totalExpense += transaction.getAmount();
            }
        }

        transactionList.append("\n--- Summary ---\n");
        transactionList.append("Total Income: ₹").append(String.format("%.2f", totalIncome)).append("\n");
        transactionList.append("Total Expense: ₹").append(String.format("%.2f", totalExpense)).append("\n");
        transactionList.append("Current Balance: ₹").append(String.format("%.2f", totalIncome - totalExpense));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(transactionList.toString())
                .setPositiveButton("OK", null)
                .show();
    }



    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ManageUsersActivity destroyed");
    }
}