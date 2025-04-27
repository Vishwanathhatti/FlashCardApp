package com.example.flashcards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private Context context;
    private List<Flashcard> flashcardList;
    private OnFlashcardClickListener listener;
    private RecyclerView recyclerView;

    public interface OnFlashcardClickListener {
        void onFlashcardClick(int position);
        void onFlashcardLongClick(int position);
    }

    public FlashcardAdapter(Context context, List<Flashcard> flashcardList, OnFlashcardClickListener listener) {
        this.context = context;
        this.flashcardList = flashcardList;
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flashcard, parent, false);
        return new FlashcardViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        Flashcard flashcard = flashcardList.get(position);
        holder.bind(flashcard);
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    public void flipCard(int position) {
        FlashcardViewHolder holder = (FlashcardViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            holder.flipCard();
        }
    }

    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;
        View cardFront, cardBack;
        boolean isFrontVisible = true;
        private OnFlashcardClickListener listener;
        private boolean isAnimating = false;

        public FlashcardViewHolder(@NonNull View itemView, OnFlashcardClickListener listener) {
            super(itemView);
            this.listener = listener;
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            cardFront = itemView.findViewById(R.id.cardFront);
            cardBack = itemView.findViewById(R.id.cardBack);

            itemView.setOnClickListener(v -> {
                if (!isAnimating) {
                    flipCard();
                    if (this.listener != null) {
                        this.listener.onFlashcardClick(getAdapterPosition());
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (this.listener != null) {
                    this.listener.onFlashcardLongClick(getAdapterPosition());
                    return true;
                }
                return false;
            });
        }

        public void bind(Flashcard flashcard) {
            tvQuestion.setText(flashcard.getQuestion());
            tvAnswer.setText(flashcard.getAnswer());
            resetToFront();
        }

        private void resetToFront() {
            cardFront.setVisibility(View.VISIBLE);
            cardBack.setVisibility(View.GONE);
            cardFront.setRotationY(0f);
            cardBack.setRotationY(0f);
            isFrontVisible = true;
            isAnimating = false;
        }

        public void flipCard() {
            if (isAnimating) return;

            isAnimating = true;
            float scale = itemView.getContext().getResources().getDisplayMetrics().density;
            cardFront.setCameraDistance(8000 * scale);
            cardBack.setCameraDistance(8000 * scale);

            if (isFrontVisible) {
                // Flip from front to back
                cardFront.animate()
                        .rotationY(90f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            cardFront.setVisibility(View.GONE);
                            cardFront.setRotationY(0f);
                            cardBack.setVisibility(View.VISIBLE);
                            cardBack.setRotationY(-90f);
                            cardBack.animate()
                                    .rotationY(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> {
                                        isFrontVisible = false;
                                        isAnimating = false;
                                    })
                                    .start();
                        })
                        .start();
            } else {
                // Flip from back to front
                cardBack.animate()
                        .rotationY(90f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            cardBack.setVisibility(View.GONE);
                            cardBack.setRotationY(0f);
                            cardFront.setVisibility(View.VISIBLE);
                            cardFront.setRotationY(-90f);
                            cardFront.animate()
                                    .rotationY(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> {
                                        isFrontVisible = true;
                                        isAnimating = false;
                                    })
                                    .start();
                        })
                        .start();
            }
        }
    }
}