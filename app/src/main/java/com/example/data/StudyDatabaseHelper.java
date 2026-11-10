package com.example.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.model.AudioSummary;
import com.example.model.Flashcard;
import com.example.model.QuizQuestion;
import com.example.model.SmartChallenge;
import com.example.model.StudyKit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Helper for permanent persistence of Study Kits, Flashcards,
 * Quiz Questions, Smart Challenges, Audio Summaries, and User Progress.
 * Data is permanently stored on disk until explicitly deleted by the user.
 */
public class StudyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_buddy_db.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_KITS = "study_kits";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_SUBJECT = "subject";
    private static final String COL_COLOR_HEX = "color_hex";
    private static final String COL_SUMMARY = "summary";
    private static final String COL_FULL_DOC_TEXT = "full_document_text";
    private static final String COL_SOURCE_FILE_NAME = "source_file_name";
    private static final String COL_KEY_CONCEPTS_JSON = "key_concepts_json";
    private static final String COL_FLASHCARDS_JSON = "flashcards_json";
    private static final String COL_QUIZ_JSON = "quiz_questions_json";
    private static final String COL_SMART_JSON = "smart_challenges_json";
    private static final String COL_AUDIO_JSON = "audio_summary_json";
    private static final String COL_MASTERY_PCT = "mastery_percentage";

    public StudyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSql = "CREATE TABLE " + TABLE_KITS + " ("
            + COL_ID + " TEXT PRIMARY KEY, "
            + COL_TITLE + " TEXT, "
            + COL_SUBJECT + " TEXT, "
            + COL_COLOR_HEX + " TEXT, "
            + COL_SUMMARY + " TEXT, "
            + COL_FULL_DOC_TEXT + " TEXT, "
            + COL_SOURCE_FILE_NAME + " TEXT, "
            + COL_KEY_CONCEPTS_JSON + " TEXT, "
            + COL_FLASHCARDS_JSON + " TEXT, "
            + COL_QUIZ_JSON + " TEXT, "
            + COL_SMART_JSON + " TEXT, "
            + COL_AUDIO_JSON + " TEXT, "
            + COL_MASTERY_PCT + " INTEGER"
            + ");";
        db.execSQL(createTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KITS);
        onCreate(db);
    }

    public void insertOrUpdateKit(StudyKit kit) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, kit.getId());
        cv.put(COL_TITLE, kit.getTitle());
        cv.put(COL_SUBJECT, kit.getSubject());
        cv.put(COL_COLOR_HEX, kit.getColorHex());
        cv.put(COL_SUMMARY, kit.getSummary());
        cv.put(COL_FULL_DOC_TEXT, kit.getFullDocumentText());
        cv.put(COL_SOURCE_FILE_NAME, kit.getSourceFileName());
        cv.put(COL_KEY_CONCEPTS_JSON, serializeKeyConcepts(kit.getKeyConcepts()));
        cv.put(COL_FLASHCARDS_JSON, serializeFlashcards(kit.getFlashcards()));
        cv.put(COL_QUIZ_JSON, serializeQuizQuestions(kit.getQuizQuestions()));
        cv.put(COL_SMART_JSON, serializeSmartChallenges(kit.getSmartChallenges()));
        cv.put(COL_AUDIO_JSON, serializeAudioSummary(kit.getAudioSummary()));
        cv.put(COL_MASTERY_PCT, kit.getMasteryPercentage());

        db.insertWithOnConflict(TABLE_KITS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<StudyKit> getAllKits() {
        List<StudyKit> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_KITS, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                try {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                    String subject = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUBJECT));
                    String colorHex = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLOR_HEX));
                    String summary = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUMMARY));
                    String docText = cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_DOC_TEXT));
                    String fileName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_FILE_NAME));
                    String keyConceptsJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_KEY_CONCEPTS_JSON));
                    String flashcardsJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_FLASHCARDS_JSON));
                    String quizJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUIZ_JSON));
                    String smartJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_SMART_JSON));
                    String audioJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_AUDIO_JSON));
                    int masteryPct = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MASTERY_PCT));

                    List<String> keyConcepts = deserializeKeyConcepts(keyConceptsJson);
                    List<Flashcard> flashcards = deserializeFlashcards(flashcardsJson);
                    List<QuizQuestion> quizQuestions = deserializeQuizQuestions(quizJson);
                    List<SmartChallenge> smartChallenges = deserializeSmartChallenges(smartJson);
                    AudioSummary audioSummary = deserializeAudioSummary(audioJson);

                    StudyKit kit = new StudyKit(
                        id, title, subject, colorHex, summary,
                        docText, fileName, keyConcepts, flashcards,
                        quizQuestions, smartChallenges, audioSummary, masteryPct
                    );
                    list.add(kit);
                } catch (Exception ignored) {}
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void deleteKit(String kitId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_KITS, COL_ID + " = ?", new String[]{kitId});
    }

    public void deleteAllKits() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_KITS, null, null);
    }

    // JSON Serializers
    private String serializeKeyConcepts(List<String> concepts) {
        if (concepts == null) return "[]";
        return new JSONArray(concepts).toString();
    }

    private List<String> deserializeKeyConcepts(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String serializeFlashcards(List<Flashcard> flashcards) {
        JSONArray arr = new JSONArray();
        if (flashcards != null) {
            for (Flashcard f : flashcards) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id", f.getId());
                    obj.put("question", f.getQuestion());
                    obj.put("answer", f.getAnswer());
                    obj.put("hint", f.getHint());
                    obj.put("masteryLevel", f.getMasteryLevel());
                    obj.put("reviewCount", f.getReviewCount());
                    arr.put(obj);
                } catch (Exception ignored) {}
            }
        }
        return arr.toString();
    }

    private List<Flashcard> deserializeFlashcards(String json) {
        List<Flashcard> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new Flashcard(
                    obj.optString("id"),
                    obj.optString("question"),
                    obj.optString("answer"),
                    obj.optString("hint"),
                    obj.optInt("masteryLevel"),
                    obj.optInt("reviewCount")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String serializeQuizQuestions(List<QuizQuestion> quiz) {
        JSONArray arr = new JSONArray();
        if (quiz != null) {
            for (QuizQuestion q : quiz) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id", q.getId());
                    obj.put("question", q.getQuestion());
                    obj.put("options", new JSONArray(q.getOptions()));
                    obj.put("correctAnswerIndex", q.getCorrectIndex());
                    obj.put("explanation", q.getExplanation());
                    arr.put(obj);
                } catch (Exception ignored) {}
            }
        }
        return arr.toString();
    }

    private List<QuizQuestion> deserializeQuizQuestions(String json) {
        List<QuizQuestion> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                JSONArray optsArr = obj.optJSONArray("options");
                List<String> options = new ArrayList<>();
                if (optsArr != null) {
                    for (int j = 0; j < optsArr.length(); j++) {
                        options.add(optsArr.getString(j));
                    }
                }
                list.add(new QuizQuestion(
                    obj.optString("id"),
                    obj.optString("question"),
                    options,
                    obj.optInt("correctAnswerIndex"),
                    obj.optString("explanation")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String serializeSmartChallenges(List<SmartChallenge> challenges) {
        JSONArray arr = new JSONArray();
        if (challenges != null) {
            for (SmartChallenge sc : challenges) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id", sc.getId());
                    obj.put("question", sc.getQuestion());
                    obj.put("difficulty", sc.getDifficulty());
                    obj.put("options", new JSONArray(sc.getOptions()));
                    obj.put("correctIndex", sc.getCorrectIndex());
                    obj.put("idealAnswerKeywords", sc.getIdealAnswerKeywords());
                    obj.put("detailedAnalysis", sc.getDetailedAnalysis());
                    arr.put(obj);
                } catch (Exception ignored) {}
            }
        }
        return arr.toString();
    }

    private List<SmartChallenge> deserializeSmartChallenges(String json) {
        List<SmartChallenge> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                JSONArray optsArr = obj.optJSONArray("options");
                List<String> options = new ArrayList<>();
                if (optsArr != null) {
                    for (int j = 0; j < optsArr.length(); j++) {
                        options.add(optsArr.getString(j));
                    }
                }
                list.add(new SmartChallenge(
                    obj.optString("id"),
                    obj.optString("question"),
                    obj.optString("difficulty"),
                    options,
                    obj.optInt("correctIndex"),
                    obj.optString("idealAnswerKeywords"),
                    obj.optString("detailedAnalysis")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String serializeAudioSummary(AudioSummary audio) {
        if (audio == null) return "{}";
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", audio.getId());
            obj.put("title", audio.getTitle());
            obj.put("speakerRole", audio.getSpeakerRole());
            obj.put("durationText", audio.getDurationText());
            obj.put("audioScript", audio.getAudioScript());
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private AudioSummary deserializeAudioSummary(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(json);
            return new AudioSummary(
                obj.optString("id"),
                obj.optString("title"),
                obj.optString("speakerRole"),
                obj.optString("durationText"),
                obj.optString("audioScript")
            );
        } catch (Exception e) {
            return null;
        }
    }
}
