package com.example.flashcards;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerBtn;
    private TextView loginLinkBtn;
    private FirebaseAuth auth;
    private View rootView;
    private GoogleSignInClient googleSignInClient;
    private ProgressBar progressBar;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        confirmPasswordInput = findViewById(R.id.confirmPassword);
        registerBtn = findViewById(R.id.registerBtn);
        loginLinkBtn = findViewById(R.id.loginLinkBtn);
        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progressBar);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up Google Sign-In button
        com.google.android.gms.common.SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
// You can access the child TextView and set the text.
        TextView textView = (TextView) googleSignInButton.getChildAt(0); // The text view inside the SignInButton
        textView.setText("Continue with Google");  // Set custom text here
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Register button click listener
        registerBtn.setOnClickListener(v -> {
            // Get input values
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Validate inputs
            if (name.isEmpty()) {
                showErrorSnackbar("Please enter your name");
                nameInput.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                showErrorSnackbar("Please enter your email");
                emailInput.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                showErrorSnackbar("Please enter a password");
                passwordInput.requestFocus();
                return;
            }

            if (confirmPassword.isEmpty()) {
                showErrorSnackbar("Please confirm your password");
                confirmPasswordInput.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                showErrorSnackbar("Passwords don't match");
                confirmPasswordInput.requestFocus();
                return;
            }

            if (password.length() < 6) {
                showErrorSnackbar("Password must be at least 6 characters");
                passwordInput.requestFocus();
                return;
            }

            // All validations passed - proceed with registration
            registerUser(name, email, password);
        });

        // Login link click listener
        loginLinkBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                showErrorSnackbar(getString(R.string.google_sign_in_failed) + ": " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        showSuccessSnackbar("Google sign in successful!");

                        // Navigate to main activity after delay
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(this, FlashcardListActivity.class));
                            finish();
                        }, 1500);
                    } else {
                        // If sign in fails
                        showErrorSnackbar("Authentication failed");
                    }
                });
    }

    private void registerUser(String name, String email, String password) {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        registerBtn.setEnabled(false);
        registerBtn.setText("Creating account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Registration success
                        showSuccessSnackbar("Registration successful!");

                        // Navigate to main activity after delay
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(this, FlashcardListActivity.class));
                            finish();
                        }, 1500);
                    } else {
                        // Registration failed
                        registerBtn.setEnabled(true);
                        registerBtn.setText("Register");

                        // Handle specific errors
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("email address is badly formatted")) {
                                errorMessage = "Invalid email format";
                            } else if (error.contains("already in use")) {
                                errorMessage = "Email already registered";
                            } else if (error.contains("password is invalid")) {
                                errorMessage = "Password is too weak";
                            }
                        }
                        showErrorSnackbar(errorMessage);
                    }
                });
    }

    private void showSuccessSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void showErrorSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.RED);
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
}
