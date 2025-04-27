package com.example.flashcards;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance(); // Get Firestore instance
        auth = FirebaseAuth.getInstance(); // Get FirebaseAuth instance
    }

    // Add a flashcard
    public void addFlashcard(String question, String answer, String category) {
        String userId = auth.getCurrentUser().getUid(); // Get the current user ID
        CollectionReference flashcardsRef = db.collection("users")
                .document(userId)
                .collection("flashcards");

        // Create a map to store flashcard data
        Map<String, Object> flashcard = new HashMap<>();
        flashcard.put("question", question);
        flashcard.put("answer", answer);
        flashcard.put("category", category);
        flashcard.put("createdAt", System.currentTimeMillis()); // Timestamp when card is created

        // Add the flashcard to Firestore
        flashcardsRef.add(flashcard)
                .addOnSuccessListener(documentReference -> {
                    // Flashcard successfully added
                    System.out.println("Flashcard added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Error while adding flashcard
                    System.out.println("Error adding flashcard: " + e.getMessage());
                });
    }

    // Get all flashcards for the current user
    public void getFlashcards() {
        String userId = auth.getCurrentUser().getUid(); // Get the current user ID
        CollectionReference flashcardsRef = db.collection("users")
                .document(userId)
                .collection("flashcards");

        // Fetch all flashcards, ordered by creation date
        flashcardsRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String question = document.getString("question");
                        String answer = document.getString("answer");
                        String category = document.getString("category");

                        // Log flashcard data (you can update UI here)
                        System.out.println("Question: " + question);
                        System.out.println("Answer: " + answer);
                        System.out.println("Category: " + category);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching flashcards
                    System.out.println("Error getting flashcards: " + e.getMessage());
                });
    }
}
