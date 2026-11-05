package com.example.model;

import java.io.Serializable;

public class AudioSummary implements Serializable {
    private String id;
    private String title;
    private String speakerRole;
    private String durationText;
    private String audioScript;

    public AudioSummary(String id, String title, String speakerRole, String durationText, String audioScript) {
        this.id = id;
        this.title = title;
        this.speakerRole = speakerRole;
        this.durationText = durationText;
        this.audioScript = audioScript;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSpeakerRole() { return speakerRole; }
    public String getDurationText() { return durationText; }
    public String getAudioScript() { return audioScript; }
}
