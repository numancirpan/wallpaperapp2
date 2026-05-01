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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wallpaper wallpaper = list.get(position);

        if (wallpaper.hasRemoteImage()) {
            Glide.with(holder.itemView.getContext()).load(wallpaper.imageUrl).centerCrop().into(holder.imageView);
        } else {
            holder.imageView.setImageResource(wallpaper.imageRes);
        }

        holder.txtWallpaperTitle.setVisibility(View.GONE);
        holder.btnFavorite.setImageResource(wallpaper.isFavorite
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);

        holder.btnFavorite.setOnClickListener(v -> {
            wallpaper.isFavorite = !wallpaper.isFavorite;

            if (!wallpaper.isFavorite) {
                wallpaper.aiCategory = "";
                wallpaper.aiLabels = "";
                FirebaseFavoritesStore.removeFavorite(wallpaper);
                notifyItemChanged(position);
                return;
            }

            FirebaseFavoritesStore.saveFavorite(wallpaper);
            boolean aiAutoEnabled = new AppSettingsManager(v.getContext()).isAiAutoCategorizeEnabled();

            if (aiAutoEnabled && needsAnalysis(wallpaper)) {
                wallpaper.aiCategory = "Analyzing";
                wallpaper.aiLabels = "Image analysis in progress";
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                notifyItemChanged(position);

                GeminiCategoryService.analyzeWallpaper(
                        v.getContext(),
                        wallpaper,
                        WallpaperRepository.getExistingAiCategories(),
                        result -> {
                            wallpaper.aiCategory = result.category;
                            wallpaper.aiLabels = result.labelsCsv;
                            FirebaseFavoritesStore.saveFavorite(wallpaper);
                            holder.itemView.post(() -> notifyItemChanged(position));
                        }
                );
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

    private boolean needsAnalysis(Wallpaper wallpaper) {
        String category = wallpaper.aiCategory == null ? "" : wallpaper.aiCategory.trim();
        String labels = wallpaper.aiLabels == null ? "" : wallpaper.aiLabels.trim();

        return category.isEmpty()
                || category.equalsIgnoreCase("Not Analyzed Yet")
                || category.equalsIgnoreCase("Analyzing")
                || category.equalsIgnoreCase("Uncategorized")
                || labels.equalsIgnoreCase("Gemini API key missing")
                || labels.equalsIgnoreCase("Gemini analysis failed")
                || labels.equalsIgnoreCase("Image could not be loaded");
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
