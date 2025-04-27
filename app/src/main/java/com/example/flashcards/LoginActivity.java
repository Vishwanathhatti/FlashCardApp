package com.example.flashcards;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginBtn, registerBtn;
    private TextView forgotPasswordLink;
    private FirebaseAuth auth;
    private View rootView;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);
//        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        rootView = findViewById(android.R.id.content);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up Google Sign-In button
        com.google.android.gms.common.SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        TextView textView = (TextView) googleSignInButton.getChildAt(0); // The text view inside the SignInButton
        textView.setText("Continue with Google");  // Set custom text here
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Login button click listener
        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Validate inputs
            if (email.isEmpty()) {
                showErrorSnackbar("Please enter your email");
                emailInput.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                showErrorSnackbar("Please enter your password");
                passwordInput.requestFocus();
                return;
            }

            // All validations passed - proceed with login
            loginUser(email, password);
        });

        // Register button click listener
        registerBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

//        // Forgot password click listener
//        forgotPasswordLink.setOnClickListener(v -> {
//            // Implement your forgot password logic here
//            showSuccessSnackbar("Password reset link will be sent to your email");
//        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }, 1500);
                    } else {
                        // If sign in fails
                        showErrorSnackbar("Authentication failed");
                    }
                });
    }

    private void loginUser(String email, String password) {
        // Show loading state
        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Login success
                        showSuccessSnackbar("Login successful!");

                        // Navigate to main activity after delay
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(this, FlashcardListActivity.class));
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }, 1500);
                    } else {
                        // Login failed
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");

                        // Handle specific errors
                        String errorMessage = "Login failed";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("email address is badly formatted")) {
                                errorMessage = "Invalid email format";
                            } else if (error.contains("no user record")) {
                                errorMessage = "Account not found";
                            } else if (error.contains("password is invalid")) {
                                errorMessage = "Incorrect password";
                            } else if (error.contains("too many requests")) {
                                errorMessage = "Too many attempts. Try again later";
                            } else if (error.contains("network error")) {
                                errorMessage = "No internet connection";
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