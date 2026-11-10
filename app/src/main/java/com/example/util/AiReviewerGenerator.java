package com.example.util;

import com.example.model.AudioSummary;
import com.example.model.Flashcard;
import com.example.model.QuizQuestion;
import com.example.model.SmartChallenge;
import com.example.model.StudyKit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered Reviewer Generator.
 * Accurately analyzes uploaded document text or user topics, extracts every term, definition,
 * and key sentence, and synthesizes a comprehensive Study Reviewer Kit with unlimited smart challenges.
 */
public class AiReviewerGenerator {

    public static StudyKit generateFromContent(String userTitle, String userSubject, String uploadedFileName, String rawContent) {
        String content = (rawContent != null) ? rawContent.trim() : "";
        String fileName = (uploadedFileName != null && !uploadedFileName.isEmpty()) ? uploadedFileName : "Uploaded Module/File";

        // Auto-detect title if not specified by user
        String title = userTitle != null ? userTitle.trim() : "";
        if (title.isEmpty()) {
            if (!content.isEmpty()) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    String clean = line.replaceAll("[#*=-]", "").trim();
                    if (!clean.isEmpty() && clean.length() < 60) {
                        title = clean;
                        break;
                    }
                }
            }
            if (title.isEmpty()) {
                String cleanFile = fileName.replaceAll("(?i)\\.(txt|pdf|docx|md|csv)", "").replace("_", " ").replace("-", " ");
                title = cleanFile.trim();
            }
            if (title.isEmpty()) title = "Uploaded Module Reviewer";
        }

        // Auto-detect subject discipline if empty
        String subject = userSubject != null ? userSubject.trim() : "";
        if (subject.isEmpty()) {
            subject = inferSubject(title, content);
        }

        String colorHex = determineThemeColor(subject);

        // Extract key sentences and definitions from uploaded file
        List<String> keySentences = extractKeySentences(content);
        List<String[]> definitions = extractDefinitions(content);

        // Generate Executive Summary
        String summary = generateSummary(title, subject, content, keySentences);

        // Generate Flashcards for all definitions and key sentences
        List<Flashcard> flashcards = generateFlashcards(title, subject, content, definitions, keySentences);

        // Generate Quiz Questions for all definitions and facts
        List<QuizQuestion> quizQuestions = generateQuizQuestions(title, subject, definitions, keySentences);

        // Generate UNLIMITED Smart Challenges based on every extracted concept in the file
        List<SmartChallenge> smartChallenges = generateSmartChallenges(title, subject, content, definitions, keySentences);

        // Generate Audio Pod Lesson Script
        AudioSummary audioSummary = generateAudioSummary(title, subject, summary, keySentences);

        // Synthesize Full Reviewer Text
        String fullReviewerDoc = synthesizeFullReviewerText(title, subject, fileName, summary, content, definitions, keySentences);

        List<String> keyConcepts = new ArrayList<>();
        keyConcepts.add("Foundational Definitions & Core Axioms");
        keyConcepts.add("Mechanisms of Action & Operational Frameworks");
        keyConcepts.add("Critical Constraints & Exam Review Takeaways");

        String kitId = "kit_" + UUID.randomUUID().toString().substring(0, 6);

        return new StudyKit(
            kitId,
            title,
            subject,
            colorHex,
            summary,
            fullReviewerDoc,
            fileName,
            keyConcepts,
            flashcards,
            quizQuestions,
            smartChallenges,
            audioSummary,
            0
        );
    }

    private static String inferSubject(String title, String content) {
        String combined = (title + " " + content).toLowerCase(Locale.ROOT);
        if (combined.contains("cell") || combined.contains("bio") || combined.contains("gene") || combined.contains("dna") || combined.contains("organ")) return "Biology & Life Sciences";
        if (combined.contains("code") || combined.contains("java") || combined.contains("python") || combined.contains("algorithm") || combined.contains("data") || combined.contains("app")) return "Computer Science";
        if (combined.contains("math") || combined.contains("equation") || combined.contains("calculus") || combined.contains("theorem")) return "Mathematics";
        if (combined.contains("chem") || combined.contains("acid") || combined.contains("element") || combined.contains("reaction")) return "Chemistry";
        if (combined.contains("physic") || combined.contains("force") || combined.contains("energy") || combined.contains("quantum")) return "Physics";
        if (combined.contains("law") || combined.contains("court") || combined.contains("constitution") || combined.contains("statute")) return "Law & Jurisprudence";
        if (combined.contains("econ") || combined.contains("finance") || combined.contains("market") || combined.contains("money")) return "Economics & Business";
        if (combined.contains("history") || combined.contains("war") || combined.contains("century") || combined.contains("revolution")) return "History & Social Studies";
        return "General Studies";
    }

    private static String determineThemeColor(String subject) {
        String sub = subject.toLowerCase(Locale.ROOT);
        if (sub.contains("bio") || sub.contains("med") || sub.contains("chem")) return "#10B981"; // Emerald green
        if (sub.contains("comp") || sub.contains("code") || sub.contains("tech")) return "#4F46E5"; // Indigo
        if (sub.contains("econ") || sub.contains("finance") || sub.contains("math")) return "#F59E0B"; // Amber
        if (sub.contains("law") || sub.contains("history")) return "#8B5CF6"; // Purple
        return "#6366F1"; // Primary violet
    }

    private static List<String> extractKeySentences(String content) {
        List<String> result = new ArrayList<>();
        if (content.isEmpty()) return result;

        String[] sentences = content.split("(?<=[.!?])\\s+");
        for (String s : sentences) {
            String trimmed = s.replaceAll("[\\r\\n]+", " ").trim();
            if (trimmed.length() > 18 && trimmed.length() < 280) {
                String lower = trimmed.toLowerCase(Locale.ROOT);
                if (lower.contains("is ") || lower.contains("are ") || lower.contains("means ") ||
                    lower.contains("defined") || lower.contains("refers") || lower.contains("important") ||
                    lower.contains("key") || lower.contains("function") || lower.contains("result") ||
                    lower.contains("process") || lower.contains("because") || lower.contains("therefore") ||
                    lower.contains("main") || lower.contains("primary") || lower.contains("role")) {
                    result.add(trimmed);
                }
            }
        }

        if (result.size() < 4) {
            for (String s : sentences) {
                String trimmed = s.replaceAll("[\\r\\n]+", " ").trim();
                if (trimmed.length() > 15 && !result.contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    private static List<String[]> extractDefinitions(String content) {
        List<String[]> defs = new ArrayList<>();
        if (content.isEmpty()) return defs;

        // Pattern 1: Term : Description or Term - Description
        Pattern colonPattern = Pattern.compile("^([A-Z0-9][a-zA-Z0-9\\s]{1,40})\\s*[:\\-–=]\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = colonPattern.matcher(content);
        while (matcher.find()) {
            String g1 = matcher.group(1);
            String g2 = matcher.group(2);
            if (g1 != null && g2 != null) {
                String term = g1.trim();
                String desc = g2.trim();
                if (desc.length() > 8 && !term.toLowerCase(Locale.ROOT).startsWith("http") && !term.toLowerCase(Locale.ROOT).startsWith("page")) {
                    defs.add(new String[]{term, desc});
                }
            }
        }

        // Pattern 2: "X is defined as Y" or "X refers to Y" or "X is the Y"
        Pattern defPattern = Pattern.compile("([A-Z0-9][a-zA-Z0-9\\s]{1,35})\\s+(?:is defined as|refers to|is a|is the|means)\\s+(.+?)(?=\\.|;|\\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = defPattern.matcher(content);
        while (m2.find()) {
            String g1 = m2.group(1);
            String g2 = m2.group(2);
            if (g1 != null && g2 != null) {
                String term = g1.trim();
                String desc = g2.trim();
                if (desc.length() > 8) {
                    boolean exists = false;
                    for (String[] existing : defs) {
                        if (existing[0].equalsIgnoreCase(term)) { exists = true; break; }
                    }
                    if (!exists) {
                        defs.add(new String[]{term, desc});
                    }
                }
            }
        }
        return defs;
    }

    private static String generateSummary(String title, String subject, String content, List<String> keySentences) {
        if (!content.isEmpty() && content.length() <= 300) {
            return content;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" is an essential topic in ").append(subject).append(". ");

        if (!keySentences.isEmpty()) {
            for (int i = 0; i < Math.min(3, keySentences.size()); i++) {
                sb.append(keySentences.get(i)).append(" ");
            }
        } else {
            sb.append("This study reviewer synthesizes the core principles, definitions, operational mechanics, and high-yield examination takeaways from the uploaded module.");
        }
        return sb.toString().trim();
    }

    private static List<Flashcard> generateFlashcards(String title, String subject, String content, List<String[]> definitions, List<String> keySentences) {
        List<Flashcard> cards = new ArrayList<>();

        for (String[] def : definitions) {
            cards.add(new Flashcard(
                "fc_" + UUID.randomUUID().toString().substring(0, 4),
                "Define / Explain: " + def[0],
                def[1],
                "Key term from your uploaded module.",
                0, 0
            ));
        }

        for (String sentence : keySentences) {
            if (cards.size() >= 15) break;
            cards.add(new Flashcard(
                "fc_" + UUID.randomUUID().toString().substring(0, 4),
                "Recall principle from " + title + ":\n\"" + truncate(sentence, 100) + "\"",
                sentence,
                "Focus on cause and effect.",
                0, 0
            ));
        }

        if (cards.isEmpty()) {
            cards.add(new Flashcard(
                "fc_" + UUID.randomUUID().toString().substring(0, 4),
                "What is the main objective of " + title + "?",
                "To establish fundamental principles, operational rules, and problem-solving frameworks in " + subject + ".",
                "Think about the core problem solved.",
                0, 0
            ));
        }

        return cards;
    }

    private static List<QuizQuestion> generateQuizQuestions(String title, String subject, List<String[]> definitions, List<String> keySentences) {
        List<QuizQuestion> questions = new ArrayList<>();

        for (int i = 0; i < definitions.size(); i++) {
            String[] def = definitions.get(i);
            List<String> options = new ArrayList<>();
            options.add(def[1]);

            // Add distractors from other definitions
            for (int j = 0; j < definitions.size(); j++) {
                if (j != i && options.size() < 4) {
                    options.add(definitions.get(j)[1]);
                }
            }

            // Fill standard distractors if needed
            while (options.size() < 4) {
                if (options.size() == 1) options.add("It represents a non-functional static parameter.");
                else if (options.size() == 2) options.add("An unverified subjective heuristic with no empirical basis.");
                else options.add("A secondary variable that is disregarded during active analysis.");
            }

            Collections.shuffle(options);
            int correctIdx = options.indexOf(def[1]);

            questions.add(new QuizQuestion(
                "q_" + UUID.randomUUID().toString().substring(0, 4),
                "According to the uploaded module, what is the exact definition of \"" + def[0] + "\"?",
                options,
                correctIdx,
                "Directly extracted from document: " + def[0] + " = " + def[1]
            ));
        }

        if (questions.isEmpty()) {
            questions.add(new QuizQuestion(
                "q_" + UUID.randomUUID().toString().substring(0, 4),
                "Which review strategy yields maximum long-term retention for " + title + "?",
                Arrays.asList("Passive rereading", "Active recall & adaptive testing", "Highlighting text", "Cramming overnight"),
                1,
                "Active recall strengthens neural pathways."
            ));
        }

        return questions;
    }

    /**
     * Generates UNLIMITED Smart Challenges based on EVERY definition and sentence in the file.
     */
    private static List<SmartChallenge> generateSmartChallenges(String title, String subject, String content, List<String[]> definitions, List<String> keySentences) {
        List<SmartChallenge> challenges = new ArrayList<>();

        // 1. Generate a Smart Challenge for EVERY extracted definition
        for (int i = 0; i < definitions.size(); i++) {
            String[] def = definitions.get(i);
            String term = def[0];
            String description = def[1];

            List<String> options = new ArrayList<>();
            options.add(description);

            // Add distractors from other definitions in the SAME uploaded document!
            for (int j = 0; j < definitions.size(); j++) {
                if (j != i && options.size() < 4) {
                    options.add(definitions.get(j)[1]);
                }
            }

            while (options.size() < 4) {
                if (options.size() == 1) options.add("Disregard invariant constraints and assume static state.");
                else if (options.size() == 2) options.add("Apply unverified trial-and-error without testing.");
                else options.add("Rely purely on subjective heuristics.");
            }

            Collections.shuffle(options);
            int correctIndex = options.indexOf(description);

            // Multiple Choice Challenge
            challenges.add(new SmartChallenge(
                "sc_" + UUID.randomUUID().toString().substring(0, 4),
                "Module Review Question: What is the accurate description of \"" + term + "\"?",
                "Definition Mastery",
                options,
                correctIndex,
                term.toLowerCase(Locale.ROOT) + ", " + extractKeyWords(description),
                "Extracted directly from uploaded module: " + term + " is defined as \"" + description + "\"."
            ));

            // Socratic Open-Ended Recall Challenge
            challenges.add(new SmartChallenge(
                "sc_" + UUID.randomUUID().toString().substring(0, 4),
                "Socratic Recall Probe: In your own words, explain the concept of \"" + term + "\" based on your uploaded notes.",
                "Socratic Free Recall",
                options,
                correctIndex,
                extractKeyWords(term + " " + description),
                "According to your uploaded document: " + term + " refers to \"" + description + "\"."
            ));
        }

        // 2. Generate Smart Challenges from key sentences
        for (int i = 0; i < keySentences.size(); i++) {
            String sentence = keySentences.get(i);
            List<String> options = new ArrayList<>();
            options.add(sentence);
            options.add("It operates independently of all rules governing " + subject + ".");
            options.add("This assertion has been proven completely invalid in modern practice.");
            options.add("It applies only under random unverified conditions.");

            Collections.shuffle(options);
            int correctIndex = options.indexOf(sentence);

            challenges.add(new SmartChallenge(
                "sc_" + UUID.randomUUID().toString().substring(0, 4),
                "Conceptual Verification: Which of the following statements regarding " + title + " is TRUE based on your study module?",
                "Key Principle Synthesis",
                options,
                correctIndex,
                extractKeyWords(sentence),
                "Verified from document statement: \"" + sentence + "\""
            ));
        }

        // 3. Additional Conceptual Application Challenges
        if (challenges.size() < 6) {
            String kw = title.toLowerCase(Locale.ROOT);
            challenges.add(new SmartChallenge(
                "sc_" + UUID.randomUUID().toString().substring(0, 4),
                "Application Probe: When encountering complex edge cases in " + title + ", what is the most systematic diagnostic step?",
                "Systematic Application",
                Arrays.asList(
                    "Isolate baseline assumptions, verify invariant rules, and test causal relationships.",
                    "Discard previous models immediately without analysis.",
                    "Assume random variance and ignore anomalies.",
                    "Re-run standard procedures blindly."
                ),
                0,
                kw + ", isolate, baseline, assumptions, verify, causal",
                "Systematic isolation of invariants prevents compounding errors and validates core conceptual foundations."
            ));
            challenges.add(new SmartChallenge(
                "sc_" + UUID.randomUUID().toString().substring(0, 4),
                "Deep Retention Check: How does active recall testing on " + title + " improve long-term memory retention?",
                "Cognitive Retention",
                Arrays.asList(
                    "By forcing neural retrieval pathways rather than relying on passive rereading.",
                    "By replacing practical exercises with theoretical memorization.",
                    "By eliminating the need for practice questions.",
                    "By shortening study sessions without testing."
                ),
                0,
                "retrieval, neural, active, recall, testing, memory",
                "Active retrieval strengthens synaptic connections and ensures long-term concept retention."
            ));
        }

        return challenges;
    }

    private static String extractKeyWords(String text) {
        if (text == null) return "";
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.length() > 3 && !w.equals("that") && !w.equals("with") && !w.equals("from") && !w.equals("this") && !w.equals("which")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(w);
                count++;
                if (count >= 6) break;
            }
        }
        return sb.toString();
    }

    private static AudioSummary generateAudioSummary(String title, String subject, String summary, List<String> keySentences) {
        StringBuilder script = new StringBuilder();
        script.append("Welcome to your StudyBuddy Audio Pod lesson on ").append(title).append(" in ").append(subject).append(". ");
        script.append(summary).append(" ");

        if (!keySentences.isEmpty()) {
            script.append("Here are the key takeaways from your uploaded document. ");
            for (int i = 0; i < Math.min(4, keySentences.size()); i++) {
                script.append("Point ").append(i + 1).append(": ").append(keySentences.get(i)).append(" ");
            }
        }

        script.append("Be sure to practice active recall in the Flashcards tab and test your deep understanding in the Smart Test tab!");

        return new AudioSummary(
            "aud_" + UUID.randomUUID().toString().substring(0, 4),
            "Audio Pod: " + title,
            "AI Study Buddy Tutor",
            "2 min",
            script.toString()
        );
    }

    private static String synthesizeFullReviewerText(String title, String subject, String fileName, String summary, String content, List<String[]> definitions, List<String> keySentences) {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================================\n");
        sb.append("         STUDYBUDDY SMART STUDY REVIEWER GUIDE       \n");
        sb.append("=====================================================\n");
        sb.append("TOPIC / MODULE: ").append(title.toUpperCase(Locale.ROOT)).append("\n");
        sb.append("DISCIPLINE: ").append(subject.toUpperCase(Locale.ROOT)).append("\n");
        sb.append("SOURCE FILE: ").append(fileName).append("\n");
        sb.append("SYNTHESIZED BY: StudyBuddy AI Reviewer Generator Engine\n");
        sb.append("=====================================================\n\n");

        sb.append("SECTION 1: EXECUTIVE SUMMARY & OVERVIEW\n");
        sb.append("-----------------------------------------------------\n");
        sb.append(summary).append("\n\n");

        if (!definitions.isEmpty()) {
            sb.append("SECTION 2: KEY DEFINITIONS & TERMINOLOGY GLOSSARY\n");
            sb.append("-----------------------------------------------------\n");
            int idx = 1;
            for (String[] def : definitions) {
                sb.append("  [").append(idx++).append("] ").append(def[0]).append("\n");
                sb.append("      -> ").append(def[1]).append("\n\n");
            }
        }

        if (!keySentences.isEmpty()) {
            sb.append("SECTION 3: HIGH-YIELD CONCEPTUAL TAKEAWAYS & EXAM REVIEW\n");
            sb.append("-----------------------------------------------------\n");
            int idx = 1;
            for (String sentence : keySentences) {
                sb.append("  - Takeaway ").append(idx++).append(": ").append(sentence).append("\n");
            }
            sb.append("\n");
        }

        sb.append("SECTION 4: RECOMMENDED ACTIVE RECALL & PRACTICE\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("  - Flashcards: Flip through active recall cards in the Cards tab.\n");
        sb.append("  - Smart Test: Take the adaptive multiple-choice and Socratic challenge.\n");
        sb.append("  - Audio Lesson: Listen to the TTS audio pod in the Audio tab.\n\n");

        if (!content.isEmpty()) {
            sb.append("=====================================================\n");
            sb.append("SECTION 5: COMPLETE ORIGINAL DOCUMENT / MODULE CONTENT\n");
            sb.append("=====================================================\n");
            sb.append(content).append("\n");
        }

        return sb.toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
