package com.example.wallpaperapp2;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

    List<Wallpaper> list;

    public WallpaperAdapter(List<Wallpaper> list) {
        this.list = list;
    }

    public void updateList(List<Wallpaper> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallpaper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wallpaper wallpaper = list.get(position);

        if (wallpaper.hasRemoteImage()) {
            Glide.with(holder.itemView.getContext())
                    .load(wallpaper.imageUrl)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(wallpaper.imageRes);
        }
        holder.txtWallpaperTitle.setText(wallpaper.title);

        if (wallpaper.isFavorite) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }

        holder.btnFavorite.setOnClickListener(v -> {
            wallpaper.isFavorite = !wallpaper.isFavorite;
            if (wallpaper.isFavorite) {
                FirebaseFavoritesStore.saveFavorite(wallpaper);
            } else {
                FirebaseFavoritesStore.removeFavorite(wallpaper);
            }
            AppSettingsManager settingsManager = new AppSettingsManager(v.getContext());
            boolean aiAutoEnabled = settingsManager.isAiAutoCategorizeEnabled();

            if (aiAutoEnabled && wallpaper.isFavorite && (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty())) {
                AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
                    @Override
                    public void onSuccess(java.util.List<AiLabelData> labels) {
                        String generatedCategory = DynamicCategoryGenerator.generateCategory(labels);
                        String finalCategory = CategoryMatcher.matchOrCreate(
                                generatedCategory,
                                WallpaperRepository.getExistingAiCategories()
                        );

                        wallpaper.aiLabels = DynamicCategoryGenerator.labelsToDisplay(labels);
                        GeminiCategoryService.generateCategory(wallpaper.title, wallpaper.aiLabels, geminiCategory -> {
                            String geminiMatched = CategoryMatcher.matchOrCreate(
                                    geminiCategory,
                                    WallpaperRepository.getExistingAiCategories()
                            );
                            wallpaper.aiCategory = geminiCategory == null || geminiCategory.trim().isEmpty()
                                    ? finalCategory
                                    : geminiMatched;
                            FirebaseFavoritesStore.saveFavorite(wallpaper);
                            holder.itemView.post(() -> notifyItemChanged(position));
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        wallpaper.aiLabels = "Analysis failed";
                        wallpaper.aiCategory = "Uncategorized";
                        FirebaseFavoritesStore.saveFavorite(wallpaper);
                        notifyItemChanged(position);
                    }
                };

                if (wallpaper.hasRemoteImage()) {
                    AiClassifier.analyzeImageUrl(v.getContext(), wallpaper.imageUrl, listener);
                } else {
                    AiClassifier.analyzeImage(v.getContext(), wallpaper.imageRes, listener);
                }
            } else {
                notifyItemChanged(position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WallpaperDetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnFavorite;
        TextView txtWallpaperTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            txtWallpaperTitle = itemView.findViewById(R.id.txtWallpaperTitle);
        }
    }
}