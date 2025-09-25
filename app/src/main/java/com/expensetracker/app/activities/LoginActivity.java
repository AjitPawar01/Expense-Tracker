package com.expensetracker.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.utils.FirebaseHelper;
import com.expensetracker.app.models.User;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword, etUsername;
    private Button btnLogin, btnRegister, btnSwitchMode;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "LoginActivity started");

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getEmail());
            navigateToMainActivity();
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnSwitchMode = findViewById(R.id.btnSwitchMode);
        progressBar = findViewById(R.id.progressBar);

        updateUIForMode();

        Log.d(TAG, "Views initialized");
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                loginUser();
            } else {
                registerUser();
            }
        });

        btnSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIForMode();
            Log.d(TAG, "Switched to " + (isLoginMode ? "Login" : "Register") + " mode");
        });
    }

    private void updateUIForMode() {
        if (isLoginMode) {
            etUsername.setVisibility(View.GONE);
            btnLogin.setText("Login");
            btnSwitchMode.setText("Don't have an account? Register");
        } else {
            etUsername.setVisibility(View.VISIBLE);
            btnLogin.setText("Register");
            btnSwitchMode.setText("Already have an account? Login");
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Attempting login for email: " + email);

        if (!validateLoginInput(email, password)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Login successful for: " + user.getEmail());
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Log.e(TAG, "Login failed: " + errorMessage);
                        Toast.makeText(this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        Log.d(TAG, "Attempting registration for email: " + email);

        if (!validateRegistrationInput(email, password, username)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Firebase user created successfully: " + firebaseUser.getUid());

                            // Create user document in Firestore
                            createUserProfile(firebaseUser, username, email);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        Toast.makeText(this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserProfile(FirebaseUser firebaseUser, String username, String email) {
        Log.d(TAG, "Creating user profile for: " + email);

        // Determine user role based on email
        String role = determineUserRole(email);

        // Get company ID from email domain
        String companyId = getCompanyIdFromEmail(email);

        Log.d(TAG, "Assigned role: " + role + ", Company ID: " + companyId);

        User user = new User(
                firebaseUser.getUid(),
                username,
                email,
                role,
                true,
                System.currentTimeMillis(),
                0.0,
                companyId
        );

        FirebaseHelper.getInstance().createUser(user, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                Log.d(TAG, "User profile created successfully");
                Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                Log.e(TAG, "Failed to create user profile: " + error);
                Toast.makeText(LoginActivity.this, "Error creating user profile: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String determineUserRole(String email) {
        // Admin emails (you can customize this list)
        if (email.contains("admin@")) {
            return "ADMIN";
        }

        // All other users are regular users
        return "USER";
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);

            // Convert domain to valid company ID
            // Example: company.com -> company_com
            String companyId = domain.replace(".", "_").toLowerCase();

            Log.d(TAG, "Extracted company ID: " + companyId + " from email: " + email);
            return companyId;
        }

        // Default company for invalid emails
        return "default_company";
    }

    private boolean validateLoginInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            Log.w(TAG, "Login validation failed: Email is empty");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            Log.w(TAG, "Login validation failed: Password is empty");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            Log.w(TAG, "Login validation failed: Invalid email format");
            return false;
        }

        Log.d(TAG, "Login input validation passed");
        return true;
    }

    private boolean validateRegistrationInput(String email, String password, String username) {
        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            Log.w(TAG, "Registration validation failed: Username is empty");
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            Log.w(TAG, "Registration validation failed: Email is empty");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            Log.w(TAG, "Registration validation failed: Invalid email format");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            Log.w(TAG, "Registration validation failed: Password is empty");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            Log.w(TAG, "Registration validation failed: Password too short");
            return false;
        }

        Log.d(TAG, "Registration input validation passed");
        return true;
    }

    private void navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LoginActivity destroyed");
    }
}