package com.expensetracker.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.expensetracker.app.R;
import com.expensetracker.app.models.User;
import com.expensetracker.app.utils.FirebaseHelper;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView ivProfileImage;
    private TextView tvUsername, tvEmail, tvRole, tvCompany, tvPasswordStatus;
    private Button btnChangeImage, btnChangePassword, btnLogout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private User userProfile;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "ProfileActivity started");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        if (currentUser == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupClickListeners();
        loadUserProfile();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        tvCompany = findViewById(R.id.tvCompany);
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
    }

    private void setupClickListeners() {
        btnChangeImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                userProfile = user;
                updateUI();
                loadProfileImage();
                checkPasswordChangeStatus();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading profile: " + error);
                Toast.makeText(ProfileActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        if (userProfile == null) return;

        tvUsername.setText(userProfile.getUsername());
        tvEmail.setText(userProfile.getEmail());
        tvRole.setText(userProfile.getRole());

        if ("ADMIN".equals(userProfile.getRole())) {
            tvRole.setBackgroundResource(R.drawable.bg_admin_tag);
        } else {
            tvRole.setBackgroundResource(R.drawable.bg_user_tag);
        }

        String companyId = userProfile.getCompanyId();
        if (companyId != null) {
            tvCompany.setText(companyId.replace("_", "."));
        }
    }

    private void loadProfileImage() {
        StorageReference profileImageRef = storageReference.child(currentUser.getUid() + ".jpg");

        profileImageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "Profile image found, loading...");
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.ic_account)
                            .error(R.drawable.ic_account)
                            .into(ivProfileImage);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "No profile image found, using default");
                    ivProfileImage.setImageResource(R.drawable.ic_account);
                });
    }

    private void checkPasswordChangeStatus() {
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean passwordChanged = documentSnapshot.getBoolean("passwordChanged");
                        if (passwordChanged != null && passwordChanged) {
                            tvPasswordStatus.setText("Password has already been changed");
                            tvPasswordStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                            btnChangePassword.setEnabled(false);
                            btnChangePassword.setText("PASSWORD ALREADY CHANGED");
                        } else {
                            tvPasswordStatus.setText("You can change your password one time only");
                            tvPasswordStatus.setTextColor(getResources().getColor(R.color.warning_color));
                            btnChangePassword.setEnabled(true);
                        }
                    }
                });
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (selectedImageUri == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnChangeImage.setEnabled(false);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();

            StorageReference imageRef = storageReference.child(currentUser.getUid() + ".jpg");

            imageRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Profile image uploaded successfully");
                        Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show();
                        loadProfileImage();
                        progressBar.setVisibility(View.GONE);
                        btnChangeImage.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error uploading image: " + e.getMessage());
                        Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnChangeImage.setEnabled(true);
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnChangeImage.setEnabled(true);
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);

        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        builder.setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword, dialog);
            }
        });
    }

    private boolean validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Enter current password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Enter new password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        progressBar.setVisibility(View.VISIBLE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Update password
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                // Mark password as changed in Firestore
                                db.collection("users")
                                        .document(currentUser.getUid())
                                        .update("passwordChanged", true)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Log.d(TAG, "Password changed successfully");
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                            checkPasswordChangeStatus();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating password status: " + e.getMessage());
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(this, "Password changed but status update failed", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating password: " + e.getMessage());
                                progressBar.setVisibility(View.GONE);
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(this, "Error updating password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Re-authentication failed: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
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