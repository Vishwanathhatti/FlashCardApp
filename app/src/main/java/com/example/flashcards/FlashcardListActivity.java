package com.example.flashcards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FlashcardListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<Flashcard> flashcardList;
    private List<Flashcard> allFlashcards;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FloatingActionButton fabAdd;
    private TextInputEditText searchInput;
    private ImageButton clearSearchButton;
    private TextView emptyStateView;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_list);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Flashcards");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        searchInput = findViewById(R.id.searchInput);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        flashcardList = new ArrayList<>();
        allFlashcards = new ArrayList<>();
        adapter = new FlashcardAdapter(this, flashcardList, new FlashcardAdapter.OnFlashcardClickListener() {
            @Override
            public void onFlashcardClick(int position) {
                adapter.flipCard(position);
            }

            @Override
            public void onFlashcardLongClick(int position) {
                showFlashcardOptionsDialog(position);
            }
        });
        recyclerView.setAdapter(adapter);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set click listener for FAB with animation
        fabAdd.setOnClickListener(v -> {
            v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        startActivity(new Intent(FlashcardListActivity.this, FlashcardFormActivity.class));
                    }).start();
        });

        // Setup search functionality
        setupSearch();

        // Hide keyboard when touching outside
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
    }

    private void setupSearch() {
        // Clear button visibility and functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search request if it exists
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // Delay search to avoid excessive filtering while typing
                searchRunnable = () -> filterFlashcards(s.toString());
                searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear button click listener
        clearSearchButton.setOnClickListener(v -> {
            searchInput.setText("");
            filterFlashcards("");
            hideKeyboard();
        });

        // Handle search action from keyboard
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void filterFlashcards(String query) {
        List<Flashcard> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            filteredList.addAll(allFlashcards);
        } else {
            String lowercaseQuery = query.toLowerCase().trim();
            for (Flashcard flashcard : allFlashcards) {
                if (flashcard.getQuestion().toLowerCase().contains(lowercaseQuery) ||
                        flashcard.getAnswer().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(flashcard);
                }
            }
        }

        flashcardList.clear();
        flashcardList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (allFlashcards.isEmpty()) {
            emptyStateView.setText("No flashcards found. Create one!");
            emptyStateView.setVisibility(View.VISIBLE);
        } else if (flashcardList.isEmpty()) {
            emptyStateView.setText("No matching flashcards found");
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashcards();
    }

    private void loadFlashcards() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("flashcards")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading flashcards", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allFlashcards.clear();
                    flashcardList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Flashcard flashcard = doc.toObject(Flashcard.class);
                        if (flashcard != null) {
                            flashcard.setId(doc.getId());
                            allFlashcards.add(flashcard);
                            flashcardList.add(flashcard);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void showFlashcardOptionsDialog(int position) {
        Flashcard flashcard = flashcardList.get(position);

        String[] options = {"Edit", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle("Flashcard Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit option
                        Intent intent = new Intent(this, FlashcardFormActivity.class);
                        intent.putExtra("flashcardId", flashcard.getId());
                        intent.putExtra("question", flashcard.getQuestion());
                        intent.putExtra("answer", flashcard.getAnswer());
                        startActivity(intent);
                    } else {
                        // Delete option
                        confirmDeleteFlashcard(flashcard);
                    }
                })
                .show();
    }

    private void confirmDeleteFlashcard(Flashcard flashcard) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this flashcard?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFlashcard(flashcard))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFlashcard(Flashcard flashcard) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("flashcards")
                .document(flashcard.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int position = flashcardList.indexOf(flashcard);
                    if (position != -1) {
                        flashcardList.remove(position);
                        allFlashcards.remove(flashcard);
                        adapter.notifyItemRemoved(position);
                        updateEmptyState();
                    }
                    Toast.makeText(this, "Flashcard deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete flashcard", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_flashcard_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Logout the user
            mAuth.signOut();
            Intent intent = new Intent(FlashcardListActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending search callbacks when activity is destroyed
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}