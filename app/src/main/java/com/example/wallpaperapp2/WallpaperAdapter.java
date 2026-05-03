package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

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
        FavoriteButtonStyler.apply(holder.btnFavorite, wallpaper.isFavorite);
        applyCollectionButtonStyle(holder.btnAddToCollection, UserProfileStore.isWallpaperInCachedCollection(wallpaper.id));

        holder.btnFavorite.setOnClickListener(v -> {
            wallpaper.isFavorite = !wallpaper.isFavorite;
            FavoriteButtonStyler.apply(holder.btnFavorite, wallpaper.isFavorite);

            if (!wallpaper.isFavorite) {
                wallpaper.aiCategory = "";
                wallpaper.aiLabels = "";
                FirebaseFavoritesStore.removeFavorite(wallpaper);
                notifyItemChanged(position);
                return;
            }

            FirebaseFavoritesStore.saveFavorite(wallpaper);

            if (needsAnalysis(wallpaper)) {
                wallpaper.aiCategory = "Analyzing";
                wallpaper.aiLabels = v.getContext().getString(R.string.checking_ai_cache);
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                notifyItemChanged(position);

                FirebaseFavoritesStore.fetchAiCache(wallpaper, (found, category, labels) -> {
                    if (found) {
                        wallpaper.aiCategory = category;
                        wallpaper.aiLabels = labels;
                        FirebaseFavoritesStore.saveFavorite(wallpaper);
                        holder.itemView.post(() -> notifyItemChanged(position));
                        return;
                    }

                    wallpaper.aiLabels = v.getContext().getString(R.string.gemini_analysis_in_progress);
                    FirebaseFavoritesStore.saveFavorite(wallpaper);
                    holder.itemView.post(() -> notifyItemChanged(position));

                    GeminiCategoryService.analyzeWallpaper(
                            v.getContext(),
                            wallpaper,
                            WallpaperRepository.getExistingAiCategories(),
                            result -> {
                                wallpaper.aiCategory = result.category;
                                wallpaper.aiLabels = result.labelsCsv;

                                if (FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
                                    FirebaseFavoritesStore.saveFavorite(wallpaper);
                                    FirebaseFavoritesStore.saveAiCache(wallpaper);
                                    holder.itemView.post(() -> notifyItemChanged(position));
                                } else {
                                    runOnDeviceFallback(holder, wallpaper, position);
                                }
                            }
                    );
                });
            } else {
                notifyItemChanged(position);
            }
        });

        holder.btnRepost.setOnClickListener(v -> RepostDialogHelper.show(v.getContext(), holder.itemView, wallpaper));
        holder.btnAddToCollection.setOnClickListener(v -> CollectionDialogHelper.show(v.getContext(), holder.itemView, wallpaper));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WallpaperDetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.id);
            v.getContext().startActivity(intent);
        });
    }

    private void runOnDeviceFallback(ViewHolder holder, Wallpaper wallpaper, int position) {
        wallpaper.aiCategory = "Analyzing";
        wallpaper.aiLabels = holder.itemView.getContext().getString(R.string.using_on_device_fallback);
        FirebaseFavoritesStore.saveFavorite(wallpaper);
        holder.itemView.post(() -> notifyItemChanged(position));

        AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
            @Override
            public void onSuccess(java.util.List<AiLabelData> labels) {
                android.content.Context context = holder.itemView.getContext();
                String finalCategory = DynamicCategoryGenerator.generateCategory(
                        context,
                        labels,
                        WallpaperRepository.getExistingAiCategories()
                );
                wallpaper.aiCategory = finalCategory;
                wallpaper.aiLabels = context.getString(
                        R.string.on_device_suffix,
                        DynamicCategoryGenerator.labelsToDisplay(context, labels)
                );

                if (!FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
                    wallpaper.aiCategory = holder.itemView.getContext().getString(R.string.uncategorized);
                    wallpaper.aiLabels = holder.itemView.getContext().getString(R.string.not_available);
                    FirebaseFavoritesStore.saveFavorite(wallpaper);
                    holder.itemView.post(() -> notifyItemChanged(position));
                    return;
                }

                FirebaseFavoritesStore.saveFavorite(wallpaper);
                FirebaseFavoritesStore.saveAiCache(wallpaper);
                holder.itemView.post(() -> notifyItemChanged(position));
            }

            @Override
            public void onError(Exception e) {
                wallpaper.aiCategory = "Uncategorized";
                wallpaper.aiLabels = holder.itemView.getContext().getString(R.string.on_device_analysis_failed);
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                holder.itemView.post(() -> notifyItemChanged(position));
            }
        };

        if (wallpaper.hasRemoteImage()) {
            AiClassifier.analyzeImageUrl(holder.itemView.getContext(), wallpaper.imageUrl, listener);
        } else {
            AiClassifier.analyzeImage(holder.itemView.getContext(), wallpaper.imageRes, listener);
        }
    }

    private boolean needsAnalysis(Wallpaper wallpaper) {
        return !FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels);
    }

    private void applyCollectionButtonStyle(MaterialButton button, boolean inCollection) {
        int background = inCollection ? Color.parseColor("#6E4BA8") : Color.parseColor("#33FFFFFF");
        int icon = inCollection ? Color.WHITE : Color.parseColor("#E8DEF8");
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setIconTint(ColorStateList.valueOf(icon));
        button.setAlpha(inCollection ? 1f : 0.82f);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        MaterialButton btnFavorite;
        MaterialButton btnRepost;
        MaterialButton btnAddToCollection;
        TextView txtWallpaperTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnRepost = itemView.findViewById(R.id.btnRepost);
            btnAddToCollection = itemView.findViewById(R.id.btnAddToCollection);
            txtWallpaperTitle = itemView.findViewById(R.id.txtWallpaperTitle);
        }
    }
}
