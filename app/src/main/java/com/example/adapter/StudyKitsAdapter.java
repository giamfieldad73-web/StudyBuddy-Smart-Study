package com.example.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.StudyKit;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class StudyKitsAdapter extends RecyclerView.Adapter<StudyKitsAdapter.ViewHolder> {

    public interface OnKitClickListener {
        void onKitClick(StudyKit kit);
    }

    public interface OnKitDeleteListener {
        void onKitDelete(StudyKit kit);
    }

    private List<StudyKit> kitList;
    private String selectedKitId;
    private final OnKitClickListener clickListener;
    private final OnKitDeleteListener deleteListener;

    public StudyKitsAdapter(List<StudyKit> kitList, String selectedKitId, OnKitClickListener clickListener, OnKitDeleteListener deleteListener) {
        this.kitList = kitList;
        this.selectedKitId = selectedKitId;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public StudyKitsAdapter(List<StudyKit> kitList, String selectedKitId, OnKitClickListener clickListener) {
        this(kitList, selectedKitId, clickListener, null);
    }

    public void updateData(List<StudyKit> newList, String selectedKitId) {
        this.kitList = newList;
        this.selectedKitId = selectedKitId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_study_kit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudyKit kit = kitList.get(position);
        holder.tvSubject.setText(kit.getSubject().toUpperCase());
        holder.tvTitle.setText(kit.getTitle());
        holder.tvSummary.setText(kit.getSummary());
        holder.tvMastery.setText(kit.getMasteryPercentage() + "%");

        holder.tvBadgeCards.setText(kit.getFlashcards().size() + " Cards");
        holder.tvBadgeQuiz.setText(kit.getQuizQuestions().size() + " Quiz Qs");
        if (kit.getAudioSummary() != null) {
            holder.tvBadgeAudio.setText(kit.getAudioSummary().getDurationText());
        }

        try {
            int parsedColor = Color.parseColor(kit.getColorHex());
            holder.tvSubject.setTextColor(parsedColor);
            holder.viewIndicator.setBackgroundColor(parsedColor);
        } catch (Exception ignored) {}

        boolean isSelected = kit.getId().equals(selectedKitId);
        holder.cardView.setStrokeWidth(isSelected ? 4 : 1);
        holder.cardView.setStrokeColor(isSelected ? Color.parseColor("#4F46E5") : Color.parseColor("#CBD5E1"));

        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onKitClick(kit);
        });

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onKitDelete(kit);
            });
        }
    }

    @Override
    public int getItemCount() {
        return kitList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View viewIndicator;
        TextView tvSubject;
        TextView tvTitle;
        TextView tvSummary;
        TextView tvMastery;
        TextView tvBadgeCards;
        TextView tvBadgeQuiz;
        TextView tvBadgeAudio;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            viewIndicator = itemView.findViewById(R.id.view_kit_indicator);
            tvSubject = itemView.findViewById(R.id.tv_kit_subject);
            tvTitle = itemView.findViewById(R.id.tv_kit_title);
            tvSummary = itemView.findViewById(R.id.tv_kit_summary);
            tvMastery = itemView.findViewById(R.id.tv_kit_mastery);
            tvBadgeCards = itemView.findViewById(R.id.tv_badge_cards);
            tvBadgeQuiz = itemView.findViewById(R.id.tv_badge_quiz);
            tvBadgeAudio = itemView.findViewById(R.id.tv_badge_audio);
            btnDelete = itemView.findViewById(R.id.btn_delete_kit);
        }
    }
}
