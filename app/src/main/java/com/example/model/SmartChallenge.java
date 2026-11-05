package com.example.model;

import java.io.Serializable;
import java.util.List;

public class SmartChallenge implements Serializable {
    private String id;
    private String question;
    private String difficulty; // e.g. "Conceptual Application", "Advanced Mechanism", "Synthesis"
    private List<String> options;
    private int correctIndex;
    private String idealAnswerKeywords;
    private String detailedAnalysis;

    public SmartChallenge(String id, String question, String difficulty, List<String> options, int correctIndex, String idealAnswerKeywords, String detailedAnalysis) {
        this.id = id;
        this.question = question;
        this.difficulty = difficulty;
        this.options = options;
        this.correctIndex = correctIndex;
        this.idealAnswerKeywords = idealAnswerKeywords;
        this.detailedAnalysis = detailedAnalysis;
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public String getDifficulty() { return difficulty; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getIdealAnswerKeywords() { return idealAnswerKeywords; }
    public String getDetailedAnalysis() { return detailedAnalysis; }
}
