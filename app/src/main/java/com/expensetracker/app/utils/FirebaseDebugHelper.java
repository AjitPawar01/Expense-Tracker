package com.expensetracker.app.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FirebaseDebugHelper {
    private static final String TAG = "FirebaseDebug";

    public static void initializeFirebase() {
        try {
            // Enable Firestore offline persistence
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            db.setFirestoreSettings(settings);

            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage());
        }
    }

    public static void testFirebaseConnection() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d(TAG, "Testing Firebase connection...");
        Log.d(TAG, "Auth instance: " + (auth != null ? "OK" : "NULL"));
        Log.d(TAG, "Firestore instance: " + (db != null ? "OK" : "NULL"));
        Log.d(TAG, "Current user: " + (auth.getCurrentUser() != null ?
                auth.getCurrentUser().getEmail() : "NULL"));

        // Test Firestore connection
        db.collection("test")
                .document("connection_test")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firestore connection: SUCCESS");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore connection: FAILED - " + e.getMessage());
                });
    }

    public static void enableFirestoreLogging() {
        FirebaseFirestore.setLoggingEnabled(true);
    }
}