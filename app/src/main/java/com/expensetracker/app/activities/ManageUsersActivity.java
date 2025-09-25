package com.expensetracker.app.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensetracker.app.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.adapters.UserAdapter;
import com.expensetracker.app.models.User;
import com.expensetracker.app.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private FirebaseUser currentUser;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        // Check if current user is admin
        checkAdminAccess();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadUsers();
    }

    private void checkAdminAccess() {
        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                if (!"ADMIN".equals(user.getRole())) {
                    Toast.makeText(ManageUsersActivity.this,
                            "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ManageUsersActivity.this,
                        "Error verifying admin access", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        users = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Users");
        }
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(users, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void loadUsers() {
        FirebaseHelper.getInstance().getAllUsers(new FirebaseHelper.OnUserListListener() {
            @Override
            public void onSuccess(List<User> userList) {
                users.clear();
                users.addAll(userList);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ManageUsersActivity.this,
                        "Error loading users: " + error, Toast.LENGTH_SHORT).show();
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
        String[] options = {"View Details", "Toggle Status", "View Transactions"};

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
                            viewUserTransactions(user);
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
        details.append("Current Balance: ₹").append(String.format("%.2f", user.getCurrentBalance()));

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
                    FirebaseHelper.getInstance().updateUserStatus(user.getUid(), newStatus,
                            new FirebaseHelper.OnCompleteListener() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(ManageUsersActivity.this,
                                            "User status updated successfully", Toast.LENGTH_SHORT).show();
                                    loadUsers(); // Refresh the list
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(ManageUsersActivity.this,
                                            "Error updating user status: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void viewUserTransactions(User user) {
        // Show all transactions for this user
        FirebaseHelper.getInstance().getAllTransactionsForCompany(user.getUid(),
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> transactions) {
                        if (transactions.isEmpty()) {
                            Toast.makeText(ManageUsersActivity.this,
                                    "No transactions found for this user", Toast.LENGTH_SHORT).show();
                        } else {
                            showTransactionsDialog(user.getUsername(), transactions);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(ManageUsersActivity.this,
                                "Error loading user transactions: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTransactionsDialog(String username, List<Transaction> transactions) {
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
            } else {
                totalExpense += transaction.getAmount();
            }
        }

        transactionList.append("\n--- Summary ---\n");
        transactionList.append("Total Income: ₹").append(String.format("%.2f", totalIncome)).append("\n");
        transactionList.append("Total Expense: ₹").append(String.format("%.2f", totalExpense)).append("\n");
        transactionList.append("Current Balance: ₹").append(String.format("%.2f", totalIncome - totalExpense));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(username + "'s Transactions")
                .setMessage(transactionList.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}