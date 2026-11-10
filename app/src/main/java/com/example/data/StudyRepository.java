package com.example.data;

import android.content.Context;

import com.example.model.Flashcard;
import com.example.model.StudyKit;

import java.util.ArrayList;
import java.util.List;

public class StudyRepository {
    private static StudyRepository instance;
    private final List<StudyKit> studyKits = new ArrayList<>();
    private String selectedKitId = "";
    private StudyDatabaseHelper dbHelper;

    private StudyRepository() {}

    public static synchronized StudyRepository getInstance() {
        if (instance == null) {
            instance = new StudyRepository();
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (dbHelper == null) {
            dbHelper = new StudyDatabaseHelper(context.getApplicationContext());
            studyKits.clear();
            studyKits.addAll(dbHelper.getAllKits());
            if (!studyKits.isEmpty() && selectedKitId.isEmpty()) {
                selectedKitId = studyKits.get(0).getId();
            }
        }
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
        if (dbHelper != null) {
            dbHelper.insertOrUpdateKit(kit);
        }
    }

    public void updateKit(StudyKit kit) {
        if (dbHelper != null) {
            dbHelper.insertOrUpdateKit(kit);
        }
    }

    public void deleteKit(String kitId) {
        StudyKit toRemove = null;
        for (StudyKit kit : studyKits) {
            if (kit.getId().equals(kitId)) {
                toRemove = kit;
                break;
            }
        }
        if (toRemove != null) {
            studyKits.remove(toRemove);
            if (selectedKitId.equals(kitId)) {
                selectedKitId = studyKits.isEmpty() ? "" : studyKits.get(0).getId();
            }
            if (dbHelper != null) {
                dbHelper.deleteKit(kitId);
            }
        }
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

            if (dbHelper != null) {
                dbHelper.insertOrUpdateKit(kit);
            }
        }
    }
}
