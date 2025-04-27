package com.example.flashcards;


public class Flashcard {
    private String id;     // Firestore document ID
    private String question;
    private String answer;

    public Flashcard() {
        // Needed for Firestore
    }

    public Flashcard(String id, String question, String answer) {
        this.id = id;
        this.question = question;
        this.answer = answer;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}

