package com.example.data;

import com.example.model.AudioSummary;
import com.example.model.Flashcard;
import com.example.model.QuizQuestion;
import com.example.model.SmartChallenge;
import com.example.model.StudyKit;

import java.util.ArrayList;
import java.util.List;

public class StudyRepository {
    private static StudyRepository instance;
    private final List<StudyKit> studyKits = new ArrayList<>();
    private String selectedKitId = "";

    private StudyRepository() {
        // Starts empty: user creates/uploads their own reviewers & study kits
    }

    public static synchronized StudyRepository getInstance() {
        if (instance == null) {
            instance = new StudyRepository();
        }
        return instance;
    }

    public List<StudyKit> getStudyKits() { return studyKits; }

    public StudyKit getSelectedKit() {
        for (StudyKit kit : studyKits) {
            if (kit.getId().equals(selectedKitId)) return kit;
        }
        return studyKits.isEmpty() ? null : studyKits.get(0);
    }

    public void setSelectedKitId(String id) { this.selectedKitId = id; }

    public void addKit(StudyKit kit) {
        studyKits.add(0, kit);
        selectedKitId = kit.getId();
    }

    public void updateFlashcardMastery(String kitId, int cardIndex, boolean mastered) {
        StudyKit kit = getSelectedKit();
        if (kit != null && cardIndex >= 0 && cardIndex < kit.getFlashcards().size()) {
            Flashcard card = kit.getFlashcards().get(cardIndex);
            card.setMasteryLevel(mastered ? 2 : 1);
            card.incrementReviewCount();

            // Recompute kit mastery %
            int masteredCount = 0;
            for (Flashcard c : kit.getFlashcards()) {
                if (c.getMasteryLevel() == 2) masteredCount++;
            }
            int newPct = (masteredCount * 100) / kit.getFlashcards().size();
            kit.setMasteryPercentage(newPct);
        }
    }
}
