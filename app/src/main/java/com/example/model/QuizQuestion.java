package com.example.model;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    private String id;
    private String question;
    private List<String> options;
    private int correctIndex;
    private String explanation;

    public QuizQuestion(String id, String question, List<String> options, int correctIndex, String explanation) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
}
