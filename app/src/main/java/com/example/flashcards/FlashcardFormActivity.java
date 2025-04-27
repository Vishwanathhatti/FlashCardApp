package com.example.flashcards;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FlashcardFormActivity extends AppCompatActivity {

    private TextInputEditText etQuestion, etAnswer;
    private TextInputLayout questionLayout, answerLayout;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String flashcardId;  // null if creating new flashcard

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_form);

        // Initialize Material Design components
        questionLayout = findViewById(R.id.questionLayout);
        answerLayout = findViewById(R.id.answerLayout);
        etQuestion = findViewById(R.id.etQuestion);
        etAnswer = findViewById(R.id.etAnswer);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check if we are editing an existing flashcard
        flashcardId = getIntent().getStringExtra("flashcardId");
        if (flashcardId != null) {
            // Set title for edit mode
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Flashcard");
            }

            etQuestion.setText(getIntent().getStringExtra("question"));
            etAnswer.setText(getIntent().getStringExtra("answer"));
            btnSave.setText("Update Flashcard");
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Create New Flashcard");
            }
        }

        btnSave.setOnClickListener(v -> saveFlashcard());
    }

    private void saveFlashcard() {
        String question = etQuestion.getText().toString().trim();
        String answer = etAnswer.getText().toString().trim();

        // Reset errors
        questionLayout.setError(null);
        answerLayout.setError(null);

        // Validate inputs with Material Design error handling
        if (TextUtils.isEmpty(question)) {
            questionLayout.setError("Question is required");
            return;
        }

        if (TextUtils.isEmpty(answer)) {
            answerLayout.setError("Answer is required");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> flashcard = new HashMap<>();
        flashcard.put("question", question);
        flashcard.put("answer", answer);
        flashcard.put("userId", userId);

        // Add a loading state to the button
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (flashcardId == null) {
            // Create new flashcard
            db.collection("users")
                    .document(userId)
                    .collection("flashcards")
                    .add(flashcard)
                    .addOnSuccessListener(documentReference -> {
                        showSuccessToast("Flashcard created successfully");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showErrorToast("Failed to create flashcard");
                        resetButtonState();
                    });
        } else {
            // Update existing flashcard
            db.collection("users")
                    .document(userId)
                    .collection("flashcards")
                    .document(flashcardId)
                    .set(flashcard)
                    .addOnSuccessListener(aVoid -> {
                        showSuccessToast("Flashcard updated successfully");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showErrorToast("Failed to update flashcard");
                        resetButtonState();
                    });
        }
    }

    private void showSuccessToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void resetButtonState() {
        btnSave.setEnabled(true);
        btnSave.setText(flashcardId == null ? "Save Flashcard" : "Update Flashcard");
    }
}