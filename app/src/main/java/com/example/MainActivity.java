package com.example;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.StudyKitsAdapter;
import com.example.data.StudyRepository;
import com.example.model.AudioSummary;
import com.example.model.Flashcard;
import com.example.model.QuizQuestion;
import com.example.model.SmartChallenge;
import com.example.model.StudyKit;
import com.example.util.AiReviewerGenerator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private StudyRepository repository;
    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNav;
    private TextToSpeech textToSpeech;
    private boolean isPlayingAudio = false;

    // Flashcard study state
    private int currentCardIndex = 0;
    private boolean isCardFlipped = false;
    private boolean isHintVisible = false;

    // Quiz practice state
    private int currentQuizIndex = 0;
    private int selectedOptionIndex = -1;
    private boolean isQuizAnswerSubmitted = false;
    private int quizScore = 0;

    // Smart Study Challenge state
    private int currentSmartIndex = 0;
    private boolean isSmartSocraticMode = false;
    private boolean isSmartAnswerSubmitted = false;
    private int smartChallengeScore = 0;

    // File picker launcher
    private ActivityResultLauncher<String> filePickerLauncher;
    private EditText currentDialogNotesEt;
    private TextView currentDialogFileNameTv;
    private TextView currentDialogFileSubTv;
    private EditText currentDialogTitleEt;
    private String currentUploadedFileName = "";
    private String currentUploadedFullContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = StudyRepository.getInstance();
        repository.init(this);
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNav = findViewById(R.id.bottom_navigation);

        initToolbar();
        initFilePicker();
        initTTS();
        setupNavigation();

        // Default screen: Study Kits
        showStudyKitsView();
    }

    private void initToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.inflateMenu(R.menu.top_app_bar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_theme_toggle) {
                showThemeSelectionDialog();
                return true;
            } else if (item.getItemId() == R.id.action_upload_file) {
                showCreateKitDialog();
                return true;
            }
            return false;
        });
    }

    private void showThemeSelectionDialog() {
        String[] options = new String[] {
            "System Default (Follows Device)",
            "Light Mode",
            "Dark Mode"
        };
        int currentChoice;
        int mode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
            currentChoice = 1;
        } else if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            currentChoice = 2;
        } else {
            currentChoice = 0;
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Appearance Theme")
            .setSingleChoiceItems(options, currentChoice, (dialog, which) -> {
                int newMode;
                if (which == 1) {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                } else if (which == 2) {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                } else {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
                getSharedPreferences("studybuddy_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("night_mode", newMode)
                    .apply();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void initFilePicker() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processUploadedFile(uri);
                }
            }
        );
    }

    private void processUploadedFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 5000) {
                    stringBuilder.append(line).append("\n");
                    linesRead++;
                }
                reader.close();
                inputStream.close();

                String fileContent = stringBuilder.toString().trim();
                String fileName = uri.getLastPathSegment();
                if (fileName == null || fileName.isEmpty()) fileName = "Uploaded Document";
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }

                currentUploadedFileName = fileName;
                currentUploadedFullContent = fileContent;

                boolean dialogActive = (currentDialogNotesEt != null);

                if (dialogActive) {
                    currentDialogNotesEt.setText(fileContent);
                    if (currentDialogFileNameTv != null) {
                        currentDialogFileNameTv.setText("Attached: " + fileName);
                        currentDialogFileNameTv.setTextColor(Color.parseColor("#10B981"));
                    }
                    if (currentDialogFileSubTv != null) {
                        currentDialogFileSubTv.setText("Loaded " + linesRead + " lines from document");
                    }
                    if (currentDialogTitleEt != null && currentDialogTitleEt.getText().toString().trim().isEmpty()) {
                        String cleanName = fileName.replaceAll("(?i)\\.(txt|pdf|docx|md|csv)", "").replace("_", " ").replace("-", " ");
                        currentDialogTitleEt.setText(cleanName);
                    }
                }

                Toast.makeText(this, "Document loaded (" + linesRead + " lines)! AI Reviewer Engine Ready.", Toast.LENGTH_SHORT).show();

                if (!dialogActive) {
                    showCreateKitDialog();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_kits) {
                showStudyKitsView();
                return true;
            } else if (itemId == R.id.nav_cards) {
                showFlashcardsView();
                return true;
            } else if (itemId == R.id.nav_smart_study) {
                showSmartStudyView();
                return true;
            } else if (itemId == R.id.nav_podcast) {
                showPodcastView();
                return true;
            } else if (itemId == R.id.nav_stats) {
                showStatsView();
                return true;
            }
            return false;
        });
    }

    // ==========================================
    // TAB 1: STUDY KITS OVERVIEW
    // ==========================================
    private void showStudyKitsView() {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_study_kits, fragmentContainer, false);
        fragmentContainer.addView(view);

        EditText etSearch = view.findViewById(R.id.et_search);
        TextView tvSpotlightTitle = view.findViewById(R.id.tv_spotlight_title);
        TextView tvSpotlightSubject = view.findViewById(R.id.tv_spotlight_subject);
        TextView tvSpotlightSummary = view.findViewById(R.id.tv_spotlight_summary);
        MaterialButton btnQuickFlashcards = view.findViewById(R.id.btn_quick_flashcards);
        MaterialButton btnQuickSmartTest = view.findViewById(R.id.btn_quick_smart_test);
        MaterialButton btnQuickQuiz = view.findViewById(R.id.btn_quick_quiz);
        MaterialButton btnQuickAudio = view.findViewById(R.id.btn_quick_audio);
        ExtendedFloatingActionButton fabNewKit = view.findViewById(R.id.fab_new_kit);

        View cardSpotlight = view.findViewById(R.id.card_spotlight);
        View cardEmptyState = view.findViewById(R.id.card_empty_state);
        View tvAllKitsHeader = view.findViewById(R.id.tv_all_kits_header);
        MaterialButton btnEmptyCreate = view.findViewById(R.id.btn_empty_create_kit);

        List<StudyKit> kitList = new ArrayList<>(repository.getStudyKits());
        StudyKit selectedKit = repository.getSelectedKit();

        if (kitList.isEmpty()) {
            if (cardSpotlight != null) cardSpotlight.setVisibility(View.GONE);
            if (cardEmptyState != null) cardEmptyState.setVisibility(View.VISIBLE);
            if (tvAllKitsHeader != null) tvAllKitsHeader.setVisibility(View.GONE);
            if (btnEmptyCreate != null) btnEmptyCreate.setOnClickListener(v -> showCreateKitDialog());
        } else {
            if (cardSpotlight != null) cardSpotlight.setVisibility(View.VISIBLE);
            if (cardEmptyState != null) cardEmptyState.setVisibility(View.GONE);
            if (tvAllKitsHeader != null) tvAllKitsHeader.setVisibility(View.VISIBLE);

            if (selectedKit != null) {
                tvSpotlightTitle.setText(selectedKit.getTitle());
                tvSpotlightSubject.setText(selectedKit.getSubject());
                tvSpotlightSummary.setText(selectedKit.getSummary());
            }
        }

        RecyclerView rvKits = view.findViewById(R.id.rv_study_kits);
        rvKits.setLayoutManager(new LinearLayoutManager(this));

        StudyKitsAdapter adapter = new StudyKitsAdapter(
            kitList,
            selectedKit != null ? selectedKit.getId() : "",
            kit -> {
                repository.setSelectedKitId(kit.getId());
                currentCardIndex = 0;
                isCardFlipped = false;
                currentQuizIndex = 0;
                quizScore = 0;
                isQuizAnswerSubmitted = false;
                currentSmartIndex = 0;
                smartChallengeScore = 0;
                isSmartAnswerSubmitted = false;
                showStudyKitsView();
            },
            kit -> {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Study Kit?")
                    .setMessage("Are you sure you want to delete \"" + kit.getTitle() + "\"? This will remove it permanently from your SQLite database.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        repository.deleteKit(kit.getId());
                        Toast.makeText(this, "Deleted \"" + kit.getTitle() + "\" from SQL Database.", Toast.LENGTH_SHORT).show();
                        showStudyKitsView();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        );
        rvKits.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                List<StudyKit> filtered = new ArrayList<>();
                for (StudyKit k : repository.getStudyKits()) {
                    if (k.getTitle().toLowerCase().contains(query) ||
                        k.getSubject().toLowerCase().contains(query) ||
                        k.getSummary().toLowerCase().contains(query)) {
                        filtered.add(k);
                    }
                }
                adapter.updateData(filtered, repository.getSelectedKit() != null ? repository.getSelectedKit().getId() : "");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        MaterialButton btnReadFullReviewer = view.findViewById(R.id.btn_read_full_reviewer);
        View cardQuickUpload = view.findViewById(R.id.card_quick_upload);
        MaterialButton btnUploadModule = view.findViewById(R.id.btn_upload_module_reviewer);

        View.OnClickListener uploadListener = v -> {
            try {
                filePickerLauncher.launch("*/*");
            } catch (Exception e) {
                showCreateKitDialog();
            }
        };
        if (cardQuickUpload != null) cardQuickUpload.setOnClickListener(uploadListener);
        if (btnUploadModule != null) btnUploadModule.setOnClickListener(uploadListener);

        btnQuickFlashcards.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_cards));
        if (btnQuickSmartTest != null) {
            btnQuickSmartTest.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_smart_study));
        }
        btnQuickQuiz.setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.nav_smart_study);
        });
        btnQuickAudio.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_podcast));
        if (btnReadFullReviewer != null) {
            btnReadFullReviewer.setOnClickListener(v -> {
                StudyKit k = repository.getSelectedKit();
                if (k != null) {
                    showDocumentReaderView(k);
                } else {
                    Toast.makeText(this, "Select or create a kit first", Toast.LENGTH_SHORT).show();
                }
            });
        }
        fabNewKit.setOnClickListener(v -> showCreateKitDialog());
    }

    private void showCreateKitDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_kit, null);
        EditText etTitle = dialogView.findViewById(R.id.dialog_et_title);
        EditText etSubject = dialogView.findViewById(R.id.dialog_et_subject);
        EditText etNotes = dialogView.findViewById(R.id.dialog_et_notes);
        MaterialCardView cardFilePicker = dialogView.findViewById(R.id.card_file_picker);
        MaterialButton btnBrowse = dialogView.findViewById(R.id.btn_browse_file);
        TextView tvFileName = dialogView.findViewById(R.id.tv_uploaded_file_name);
        TextView tvFileSub = dialogView.findViewById(R.id.tv_uploaded_file_sub);

        currentDialogTitleEt = etTitle;
        currentDialogNotesEt = etNotes;
        currentDialogFileNameTv = tvFileName;
        currentDialogFileSubTv = tvFileSub;

        View.OnClickListener pickFileListener = v -> {
            try {
                filePickerLauncher.launch("*/*");
            } catch (Exception e) {
                Toast.makeText(this, "File browser error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (cardFilePicker != null) cardFilePicker.setOnClickListener(pickFileListener);
        if (btnBrowse != null) btnBrowse.setOnClickListener(pickFileListener);

        new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Generate AI Reviewer", (dialog, which) -> {
                String title = etTitle.getText().toString().trim();
                String subject = etSubject.getText().toString().trim();
                String notes = etNotes.getText().toString().trim();

                if (title.isEmpty() && notes.isEmpty() && currentUploadedFullContent.isEmpty()) {
                    Toast.makeText(this, "Please enter a topic title, paste notes, or upload a document file", Toast.LENGTH_SHORT).show();
                    return;
                }

                String contentToUse = !notes.isEmpty() ? notes : currentUploadedFullContent;

                StudyKit newKit = AiReviewerGenerator.generateFromContent(
                    title,
                    subject,
                    currentUploadedFileName,
                    contentToUse
                );

                currentUploadedFileName = "";
                currentUploadedFullContent = "";

                repository.addKit(newKit);
                Toast.makeText(this, "🤖 AI Synthesized Reviewer for " + newKit.getTitle() + "!", Toast.LENGTH_LONG).show();
                showStudyKitsView();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                currentUploadedFileName = "";
                currentUploadedFullContent = "";
            })
            .show();
    }

    // ==========================================
    // TAB 2: FLASHCARDS (ACTIVE RECALL)
    // ==========================================
    private void showFlashcardsView() {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_flashcards, fragmentContainer, false);
        fragmentContainer.addView(view);

        StudyKit kit = repository.getSelectedKit();
        if (kit == null || kit.getFlashcards().isEmpty()) {
            Toast.makeText(this, "Select a kit with flashcards first", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView tvCardCounter = view.findViewById(R.id.tv_card_counter);
        TextView tvCardMastery = view.findViewById(R.id.tv_card_mastery_badge);
        MaterialCardView cardSurface = view.findViewById(R.id.card_flip_surface);
        LinearLayout layoutFront = view.findViewById(R.id.layout_card_front);
        LinearLayout layoutBack = view.findViewById(R.id.layout_card_back);
        TextView tvQuestion = view.findViewById(R.id.tv_card_question);
        TextView tvAnswer = view.findViewById(R.id.tv_card_answer);
        TextView tvHint = view.findViewById(R.id.tv_card_hint);
        View btnHintToggle = view.findViewById(R.id.btn_card_hint_toggle);

        MaterialButton btnRateAgain = view.findViewById(R.id.btn_rate_again);
        MaterialButton btnRateMastered = view.findViewById(R.id.btn_rate_mastered);
        MaterialButton btnPrev = view.findViewById(R.id.btn_card_prev);
        MaterialButton btnNext = view.findViewById(R.id.btn_card_next);

        bindFlashcard(kit, tvCardCounter, tvCardMastery, layoutFront, layoutBack, tvQuestion, tvAnswer, tvHint);

        cardSurface.setOnClickListener(v -> {
            isCardFlipped = !isCardFlipped;
            layoutFront.setVisibility(isCardFlipped ? View.GONE : View.VISIBLE);
            layoutBack.setVisibility(isCardFlipped ? View.VISIBLE : View.GONE);
        });

        btnHintToggle.setOnClickListener(v -> {
            isHintVisible = !isHintVisible;
            tvHint.setVisibility(isHintVisible ? View.VISIBLE : View.GONE);
        });

        btnRateAgain.setOnClickListener(v -> {
            repository.updateFlashcardMastery(kit.getId(), currentCardIndex, false);
            advanceCard(kit, tvCardCounter, tvCardMastery, layoutFront, layoutBack, tvQuestion, tvAnswer, tvHint);
        });

        btnRateMastered.setOnClickListener(v -> {
            repository.updateFlashcardMastery(kit.getId(), currentCardIndex, true);
            advanceCard(kit, tvCardCounter, tvCardMastery, layoutFront, layoutBack, tvQuestion, tvAnswer, tvHint);
        });

        btnPrev.setOnClickListener(v -> {
            if (currentCardIndex > 0) currentCardIndex--;
            else currentCardIndex = kit.getFlashcards().size() - 1;
            isCardFlipped = false;
            isHintVisible = false;
            bindFlashcard(kit, tvCardCounter, tvCardMastery, layoutFront, layoutBack, tvQuestion, tvAnswer, tvHint);
        });

        btnNext.setOnClickListener(v -> advanceCard(kit, tvCardCounter, tvCardMastery, layoutFront, layoutBack, tvQuestion, tvAnswer, tvHint));
    }

    private void advanceCard(StudyKit kit, TextView tvCounter, TextView tvMastery, LinearLayout front, LinearLayout back,
                             TextView tvQ, TextView tvA, TextView tvHint) {
        if (currentCardIndex + 1 < kit.getFlashcards().size()) {
            currentCardIndex++;
        } else {
            currentCardIndex = 0;
            Toast.makeText(this, "Set completed! Looping for spaced repetition.", Toast.LENGTH_SHORT).show();
        }
        isCardFlipped = false;
        isHintVisible = false;
        bindFlashcard(kit, tvCounter, tvMastery, front, back, tvQ, tvA, tvHint);
    }

    private void bindFlashcard(StudyKit kit, TextView tvCounter, TextView tvMastery, LinearLayout front, LinearLayout back,
                              TextView tvQ, TextView tvA, TextView tvHint) {
        Flashcard card = kit.getFlashcards().get(currentCardIndex);
        tvCounter.setText("Card " + (currentCardIndex + 1) + " of " + kit.getFlashcards().size());

        if (card.getMasteryLevel() == 2) {
            tvMastery.setText("Mastered");
            tvMastery.setTextColor(Color.parseColor("#10B981"));
        } else if (card.getMasteryLevel() == 1) {
            tvMastery.setText("Learning");
            tvMastery.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            tvMastery.setText("New");
            tvMastery.setTextColor(Color.parseColor("#64748B"));
        }

        tvQ.setText(card.getQuestion());
        tvA.setText(card.getAnswer());
        tvHint.setText("💡 Hint: " + card.getHint());
        tvHint.setVisibility(isHintVisible ? View.VISIBLE : View.GONE);

        front.setVisibility(isCardFlipped ? View.GONE : View.VISIBLE);
        back.setVisibility(isCardFlipped ? View.VISIBLE : View.GONE);
    }

    // ==========================================
    // TAB 3: SMART STUDY KNOWLEDGE CHALLENGE
    // ==========================================
    private void showSmartStudyView() {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_smart_study, fragmentContainer, false);
        fragmentContainer.addView(view);

        StudyKit kit = repository.getSelectedKit();
        if (kit == null || kit.getSmartChallenges().isEmpty()) {
            Toast.makeText(this, "Select a kit with smart challenges first", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layoutActive = view.findViewById(R.id.layout_smart_active);
        LinearLayout layoutCompleted = view.findViewById(R.id.layout_smart_completed);
        TextView tvScore = view.findViewById(R.id.tv_smart_score);
        TextView tvCounter = view.findViewById(R.id.tv_smart_q_counter);
        LinearProgressIndicator progressBar = view.findViewById(R.id.smart_progress_bar);
        TextView tvDiffTag = view.findViewById(R.id.tv_smart_difficulty_tag);
        TextView tvQuestion = view.findViewById(R.id.tv_smart_question);

        MaterialButton btnModeMC = view.findViewById(R.id.btn_mode_multichoice);
        MaterialButton btnModeSocratic = view.findViewById(R.id.btn_mode_socratic);

        RadioGroup rgOptions = view.findViewById(R.id.rg_smart_options);
        RadioButton[] rbOptions = new RadioButton[] {
            view.findViewById(R.id.rb_smart_0),
            view.findViewById(R.id.rb_smart_1),
            view.findViewById(R.id.rb_smart_2),
            view.findViewById(R.id.rb_smart_3)
        };

        LinearLayout layoutSocraticInput = view.findViewById(R.id.layout_socratic_input);
        EditText etSocraticAnswer = view.findViewById(R.id.et_socratic_answer);

        MaterialCardView cardFeedback = view.findViewById(R.id.card_smart_feedback);
        TextView tvFeedbackHeader = view.findViewById(R.id.tv_smart_feedback_header);
        TextView tvFeedbackBody = view.findViewById(R.id.tv_smart_feedback_body);
        MaterialButton btnAction = view.findViewById(R.id.btn_smart_action);

        TextView tvFinalResult = view.findViewById(R.id.tv_smart_final_result);
        MaterialButton btnRestart = view.findViewById(R.id.btn_smart_restart);

        // Mode toggling
        btnModeMC.setOnClickListener(v -> {
            isSmartSocraticMode = false;
            btnModeMC.setBackgroundColor(Color.parseColor("#4F46E5"));
            btnModeMC.setTextColor(Color.WHITE);
            btnModeSocratic.setBackgroundColor(Color.parseColor("#F1F5F9"));
            btnModeSocratic.setTextColor(Color.parseColor("#0F172A"));
            rgOptions.setVisibility(View.VISIBLE);
            layoutSocraticInput.setVisibility(View.GONE);
        });

        btnModeSocratic.setOnClickListener(v -> {
            isSmartSocraticMode = true;
            btnModeSocratic.setBackgroundColor(Color.parseColor("#4F46E5"));
            btnModeSocratic.setTextColor(Color.WHITE);
            btnModeMC.setBackgroundColor(Color.parseColor("#F1F5F9"));
            btnModeMC.setTextColor(Color.parseColor("#0F172A"));
            rgOptions.setVisibility(View.GONE);
            layoutSocraticInput.setVisibility(View.VISIBLE);
        });

        bindSmartChallenge(kit, tvScore, tvCounter, progressBar, tvDiffTag, tvQuestion, rgOptions, rbOptions, cardFeedback, tvFeedbackHeader, tvFeedbackBody, btnAction, etSocraticAnswer);

        btnAction.setOnClickListener(v -> {
            SmartChallenge sc = kit.getSmartChallenges().get(currentSmartIndex);
            if (!isSmartAnswerSubmitted) {
                boolean isCorrect = false;
                int chosen = -1;
                if (!isSmartSocraticMode) {
                    int checkedId = rgOptions.getCheckedRadioButtonId();
                    if (checkedId == -1) {
                        Toast.makeText(this, "Select an option to test your knowledge", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (int i = 0; i < rbOptions.length; i++) {
                        if (rbOptions[i].getId() == checkedId) chosen = i;
                    }
                    isCorrect = (chosen == sc.getCorrectIndex());
                } else {
                    String userText = etSocraticAnswer.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (userText.isEmpty()) {
                        Toast.makeText(this, "Please write an explanation to challenge your knowledge", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] keywords = sc.getIdealAnswerKeywords().toLowerCase(Locale.ROOT).split(",");
                    int matched = 0;
                    for (String kw : keywords) {
                        if (!kw.trim().isEmpty() && userText.contains(kw.trim())) matched++;
                    }
                    isCorrect = matched >= 1 || userText.length() > 30;
                }

                isSmartAnswerSubmitted = true;

                String correctOptionText = (sc.getCorrectIndex() >= 0 && sc.getCorrectIndex() < sc.getOptions().size())
                    ? sc.getOptions().get(sc.getCorrectIndex()) : "";

                if (isCorrect) {
                    smartChallengeScore++;
                    tvScore.setText("Score: " + smartChallengeScore);
                    tvFeedbackHeader.setText("🌟 Knowledge Verified!");
                    tvFeedbackHeader.setTextColor(Color.parseColor("#10B981"));
                    tvFeedbackBody.setText("✓ Excellent job! Your response matches the document principles.\n\n💡 Explanation:\n" + sc.getDetailedAnalysis());
                } else {
                    tvFeedbackHeader.setText("❌ Needs Reinforcement");
                    tvFeedbackHeader.setTextColor(Color.parseColor("#F43F5E"));

                    if (!isSmartSocraticMode) {
                        tvFeedbackBody.setText("❌ Incorrect answer.\n\n"
                            + "✓ CORRECT RIGHT ANSWER:\n\"" + correctOptionText + "\"\n\n"
                            + "💡 Detailed Analysis:\n" + sc.getDetailedAnalysis());
                    } else {
                        tvFeedbackBody.setText("❌ Key concept missing from your answer.\n\n"
                            + "✓ EXPECTED KEY TERMS & CONCEPTS:\n" + sc.getIdealAnswerKeywords() + "\n\n"
                            + "💡 Detailed Analysis:\n" + sc.getDetailedAnalysis());
                    }
                }

                if (!isSmartSocraticMode) {
                    for (int i = 0; i < rbOptions.length; i++) {
                        rbOptions[i].setEnabled(false);
                        if (i == sc.getCorrectIndex()) {
                            rbOptions[i].setTextColor(Color.parseColor("#10B981"));
                            rbOptions[i].setTypeface(null, Typeface.BOLD);
                        } else if (i == chosen && !isCorrect) {
                            rbOptions[i].setTextColor(Color.parseColor("#F43F5E"));
                        }
                    }
                }

                cardFeedback.setVisibility(View.VISIBLE);
                btnAction.setText(currentSmartIndex + 1 < kit.getSmartChallenges().size() ? "Next Challenge" : "View Final Knowledge Score");
            } else {
                if (currentSmartIndex + 1 < kit.getSmartChallenges().size()) {
                    currentSmartIndex++;
                    isSmartAnswerSubmitted = false;
                    cardFeedback.setVisibility(View.GONE);
                    etSocraticAnswer.setText("");
                    btnAction.setText("Submit Knowledge Check");
                    bindSmartChallenge(kit, tvScore, tvCounter, progressBar, tvDiffTag, tvQuestion, rgOptions, rbOptions, cardFeedback, tvFeedbackHeader, tvFeedbackBody, btnAction, etSocraticAnswer);
                } else {
                    layoutActive.setVisibility(View.GONE);
                    layoutCompleted.setVisibility(View.VISIBLE);
                    int total = kit.getSmartChallenges().size();
                    int pct = (smartChallengeScore * 100) / total;
                    tvFinalResult.setText("You completed the Smart Study challenge!\nScore: " + smartChallengeScore + " / " + total + " (" + pct + "% Retention Score)\n\nTap 'Challenge Again' below to continuously familiarize yourself with all possible questions!");
                }
            }
        });

        btnRestart.setOnClickListener(v -> {
            currentSmartIndex = 0;
            smartChallengeScore = 0;
            isSmartAnswerSubmitted = false;
            Collections.shuffle(kit.getSmartChallenges());
            layoutCompleted.setVisibility(View.GONE);
            layoutActive.setVisibility(View.VISIBLE);
            cardFeedback.setVisibility(View.GONE);
            etSocraticAnswer.setText("");
            btnAction.setText("Submit Knowledge Check");
            bindSmartChallenge(kit, tvScore, tvCounter, progressBar, tvDiffTag, tvQuestion, rgOptions, rbOptions, cardFeedback, tvFeedbackHeader, tvFeedbackBody, btnAction, etSocraticAnswer);
        });
    }

    private void bindSmartChallenge(StudyKit kit, TextView tvScore, TextView tvCounter, LinearProgressIndicator progressBar,
                                   TextView tvDiff, TextView tvQ, RadioGroup rg, RadioButton[] rbs,
                                   View cardFeedback, TextView tvHeader, TextView tvBody, MaterialButton btnAction, EditText etAnswer) {
        SmartChallenge sc = kit.getSmartChallenges().get(currentSmartIndex);
        tvScore.setText("Score: " + smartChallengeScore);
        tvCounter.setText("Question " + (currentSmartIndex + 1) + " of " + kit.getSmartChallenges().size() + " (" + sc.getDifficulty() + ")");
        progressBar.setProgress(((currentSmartIndex + 1) * 100) / kit.getSmartChallenges().size());

        tvDiff.setText("CHALLENGE: " + sc.getDifficulty().toUpperCase());
        tvQ.setText(sc.getQuestion());
        rg.clearCheck();

        for (int i = 0; i < rbs.length; i++) {
            if (i < sc.getOptions().size()) {
                rbs[i].setVisibility(View.VISIBLE);
                rbs[i].setText(sc.getOptions().get(i));
                rbs[i].setEnabled(!isSmartAnswerSubmitted);
                rbs[i].setTextColor(Color.parseColor("#0F172A"));
                rbs[i].setTypeface(null, Typeface.NORMAL);
            } else {
                rbs[i].setVisibility(View.GONE);
            }
        }
    }

    // ==========================================
    // TAB 4: AUDIO PODCAST PLAYER
    // ==========================================
    private void showPodcastView() {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_podcast, fragmentContainer, false);
        fragmentContainer.addView(view);

        StudyKit kit = repository.getSelectedKit();
        if (kit == null) return;

        TextView tvTitle = view.findViewById(R.id.tv_audio_title);
        TextView tvSpeaker = view.findViewById(R.id.tv_audio_speaker);
        TextView tvScript = view.findViewById(R.id.tv_audio_script);
        MaterialButton btnPlayToggle = view.findViewById(R.id.btn_audio_play_toggle);

        AudioSummary audio = kit.getAudioSummary();
        tvTitle.setText(audio.getTitle());
        tvSpeaker.setText(audio.getSpeakerRole() + " • " + audio.getDurationText());
        tvScript.setText(audio.getAudioScript());

        updateAudioPlayButton(btnPlayToggle);

        btnPlayToggle.setOnClickListener(v -> {
            if (isPlayingAudio) {
                if (textToSpeech != null) textToSpeech.stop();
                isPlayingAudio = false;
            } else {
                if (textToSpeech != null) {
                    textToSpeech.speak(audio.getAudioScript(), TextToSpeech.QUEUE_FLUSH, null, "StudyBuddyAudio");
                    isPlayingAudio = true;
                }
            }
            updateAudioPlayButton(btnPlayToggle);
        });
    }

    private void updateAudioPlayButton(MaterialButton btn) {
        if (isPlayingAudio) {
            btn.setText("Pause Audio Pod");
            btn.setIconResource(android.R.drawable.ic_media_pause);
            btn.setBackgroundColor(Color.parseColor("#10B981"));
        } else {
            btn.setText("Play Audio Pod");
            btn.setIconResource(android.R.drawable.ic_media_play);
            btn.setBackgroundColor(Color.parseColor("#4F46E5"));
        }
    }

    // ==========================================
    // TAB 5: STATS & ANALYTICS
    // ==========================================
    private void showStatsView() {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_stats, fragmentContainer, false);
        fragmentContainer.addView(view);

        List<StudyKit> kits = repository.getStudyKits();
        TextView tvAvgMastery = view.findViewById(R.id.tv_stat_avg_mastery);
        TextView tvMasteredCards = view.findViewById(R.id.tv_stat_mastered_cards);
        TextView tvTotalKits = view.findViewById(R.id.tv_stat_total_kits);
        TextView tvQuizQuestions = view.findViewById(R.id.tv_stat_quiz_questions);
        LinearLayout layoutBreakdown = view.findViewById(R.id.layout_stats_breakdown);

        TextView tvThemeLabel = view.findViewById(R.id.tv_current_theme_label);
        MaterialButton btnToggleTheme = view.findViewById(R.id.btn_toggle_dark_mode);

        int currentMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            tvThemeLabel.setText("Dark Mode (Active)");
        } else if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
            tvThemeLabel.setText("Light Mode (Active)");
        } else {
            tvThemeLabel.setText("System Default (Follows Device)");
        }

        if (btnToggleTheme != null) {
            btnToggleTheme.setOnClickListener(v -> showThemeSelectionDialog());
        }

        int totalCards = 0;
        int masteredCards = 0;
        int totalSumMastery = 0;
        int totalQuestions = 0;

        for (StudyKit k : kits) {
            totalCards += k.getFlashcards().size();
            for (Flashcard c : k.getFlashcards()) {
                if (c.getMasteryLevel() == 2) masteredCards++;
            }
            totalSumMastery += k.getMasteryPercentage();
            totalQuestions += k.getQuizQuestions().size() + k.getSmartChallenges().size();
        }

        int avgMastery = kits.isEmpty() ? 0 : totalSumMastery / kits.size();

        tvAvgMastery.setText(avgMastery + "%");
        tvMasteredCards.setText(masteredCards + " / " + totalCards);
        tvTotalKits.setText(String.valueOf(kits.size()));
        tvQuizQuestions.setText(String.valueOf(totalQuestions));

        layoutBreakdown.removeAllViews();
        for (StudyKit kit : kits) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_study_kit, layoutBreakdown, false);
            TextView tvSub = itemView.findViewById(R.id.tv_kit_subject);
            TextView tvTitle = itemView.findViewById(R.id.tv_kit_title);
            TextView tvSummary = itemView.findViewById(R.id.tv_kit_summary);
            TextView tvMastery = itemView.findViewById(R.id.tv_kit_mastery);
            View indicator = itemView.findViewById(R.id.view_kit_indicator);

            tvSub.setText(kit.getSubject().toUpperCase());
            tvTitle.setText(kit.getTitle());
            tvSummary.setText("Retention score: " + kit.getMasteryPercentage() + "% (" + kit.getFlashcards().size() + " cards, " + kit.getSmartChallenges().size() + " smart challenges)");
            tvMastery.setText(kit.getMasteryPercentage() + "%");

            try {
                int c = Color.parseColor(kit.getColorHex());
                tvSub.setTextColor(c);
                indicator.setBackgroundColor(c);
            } catch (Exception ignored) {}

            layoutBreakdown.addView(itemView);
        }
    }

    // ==========================================
    // DOCUMENT READER & DOWNLOAD
    // ==========================================
    private void showDocumentReaderView(StudyKit kit) {
        fragmentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(R.layout.fragment_document_reader, fragmentContainer, false);
        fragmentContainer.addView(view);

        TextView tvTitle = view.findViewById(R.id.tv_reader_title);
        TextView tvSub = view.findViewById(R.id.tv_reader_subject_badge);
        TextView tvSource = view.findViewById(R.id.tv_reader_source_file);
        TextView tvBody = view.findViewById(R.id.tv_reader_body_content);
        View btnBack = view.findViewById(R.id.btn_reader_back);
        MaterialButton btnDownload = view.findViewById(R.id.btn_reader_download);
        MaterialButton btnAiEnhance = view.findViewById(R.id.btn_reader_ai_regenerate);

        tvTitle.setText(kit.getTitle());
        tvSub.setText("DISCIPLINE: " + kit.getSubject().toUpperCase());
        tvSource.setText("Source: " + (kit.getSourceFileName().isEmpty() ? "Synthesized Reviewer Guide" : kit.getSourceFileName()));
        tvBody.setText(kit.getFullDocumentText().isEmpty() ? kit.getSummary() : kit.getFullDocumentText());

        btnBack.setOnClickListener(v -> showStudyKitsView());

        if (btnAiEnhance != null) {
            btnAiEnhance.setOnClickListener(v -> {
                Toast.makeText(this, "🤖 AI re-analyzing document & expanding reviewer...", Toast.LENGTH_SHORT).show();
                StudyKit enhancedKit = AiReviewerGenerator.generateFromContent(
                    kit.getTitle(),
                    kit.getSubject(),
                    kit.getSourceFileName(),
                    kit.getFullDocumentText()
                );
                kit.setFullDocumentText(enhancedKit.getFullDocumentText());
                kit.setSmartChallenges(enhancedKit.getSmartChallenges());
                tvBody.setText(kit.getFullDocumentText());
                Toast.makeText(this, "Reviewer AI Enhanced! Active recall probes updated.", Toast.LENGTH_SHORT).show();
            });
        }

        btnDownload.setOnClickListener(v -> downloadReviewerDocument(kit));
    }

    private void downloadReviewerDocument(StudyKit kit) {
        try {
            String safeFileName = kit.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + "_Reviewer.txt";
            java.io.File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            java.io.File file = new java.io.File(downloadDir, safeFileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(kit.getFullDocumentText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.flush();
            fos.close();

            Toast.makeText(this, "Reviewer downloaded to Downloads/" + safeFileName + " 💾", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Fallback to internal storage and toast
            try {
                java.io.File internalFile = new java.io.File(getFilesDir(), kit.getTitle() + "_Reviewer.txt");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(internalFile);
                fos.write(kit.getFullDocumentText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.flush();
                fos.close();
                Toast.makeText(this, "Saved reviewer locally: " + internalFile.getName(), Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(this, "Download failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
