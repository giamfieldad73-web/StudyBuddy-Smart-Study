package com.example.model;

import java.io.Serializable;

public class Flashcard implements Serializable {
    private String id;
    private String question;
    private String answer;
    private String hint;
    private int masteryLevel; // 0=New, 1=Learning, 2=Mastered
    private int reviewCount;

    public Flashcard(String id, String question, String answer, String hint, int masteryLevel, int reviewCount) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.hint = hint;
        this.masteryLevel = masteryLevel;
        this.reviewCount = reviewCount;
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getHint() { return hint; }
    public int getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }
    public int getReviewCount() { return reviewCount; }
    public void incrementReviewCount() { this.reviewCount++; }
}
