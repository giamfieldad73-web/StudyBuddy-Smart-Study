package com.example.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StudyKit implements Serializable {
    private String id;
    private String title;
    private String subject;
    private String colorHex;
    private String summary;
    private String fullDocumentText; // Raw or generated document text
    private String sourceFileName;   // e.g. "Lecture_Notes_Ch4.txt"
    private List<String> keyConcepts;
    private List<Flashcard> flashcards;
    private List<QuizQuestion> quizQuestions;
    private List<SmartChallenge> smartChallenges;
    private AudioSummary audioSummary;
    private int masteryPercentage;

    public StudyKit(String id, String title, String subject, String colorHex, String summary,
                    String fullDocumentText, String sourceFileName,
                    List<String> keyConcepts, List<Flashcard> flashcards,
                    List<QuizQuestion> quizQuestions, List<SmartChallenge> smartChallenges,
                    AudioSummary audioSummary, int masteryPercentage) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.colorHex = colorHex;
        this.summary = summary;
        this.fullDocumentText = fullDocumentText != null ? fullDocumentText : "";
        this.sourceFileName = sourceFileName != null ? sourceFileName : "";
        this.keyConcepts = keyConcepts;
        this.flashcards = flashcards;
        this.quizQuestions = quizQuestions;
        this.smartChallenges = smartChallenges != null ? smartChallenges : new ArrayList<>();
        this.audioSummary = audioSummary;
        this.masteryPercentage = masteryPercentage;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getColorHex() { return colorHex; }
    public String getSummary() { return summary; }
    public String getFullDocumentText() { return fullDocumentText; }
    public void setFullDocumentText(String fullDocumentText) { this.fullDocumentText = fullDocumentText; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public List<Flashcard> getFlashcards() { return flashcards; }
    public List<QuizQuestion> getQuizQuestions() { return quizQuestions; }
    public List<SmartChallenge> getSmartChallenges() { return smartChallenges; }
    public void setSmartChallenges(List<SmartChallenge> smartChallenges) { this.smartChallenges = smartChallenges; }
    public AudioSummary getAudioSummary() { return audioSummary; }
    public int getMasteryPercentage() { return masteryPercentage; }
    public void setMasteryPercentage(int masteryPercentage) { this.masteryPercentage = masteryPercentage; }
}
