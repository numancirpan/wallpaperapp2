package com.example.wallpaperapp2;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class FavoriteAiAdapter extends RecyclerView.Adapter<FavoriteAiAdapter.ViewHolder> {

    private List<Wallpaper> list;

    public FavoriteAiAdapter(List<Wallpaper> list) {
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
                .inflate(R.layout.item_favorite_ai, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wallpaper wallpaper = list.get(position);

        if (wallpaper.hasRemoteImage()) {
            Glide.with(holder.itemView.getContext())
                    .load(wallpaper.imageUrl)
                    .centerCrop()
                    .into(holder.imageFavorite);
        } else {
            holder.imageFavorite.setImageResource(wallpaper.imageRes);
        }
        holder.txtFavoriteTitle.setText(wallpaper.title);
        FavoriteButtonStyler.apply(holder.btnFavoriteRemove, true);
        holder.progressFavoriteAnalysis.setVisibility(isAnalyzing(wallpaper) ? View.VISIBLE : View.GONE);

        if (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty()) {
            holder.txtFavoriteAiCategory.setText(
                    holder.itemView.getContext().getString(
                            R.string.ai_category_prefix,
                            holder.itemView.getContext().getString(R.string.not_analyzed_yet)
                    )
            );
        } else {
            holder.txtFavoriteAiCategory.setText(
                    holder.itemView.getContext().getString(
                            R.string.ai_category_prefix,
                            CategoryDisplayMapper.toDisplayName(holder.itemView.getContext(), wallpaper.aiCategory)
                    )
            );
        }

        if (wallpaper.aiLabels == null || wallpaper.aiLabels.isEmpty()) {
            holder.txtFavoriteAiLabels.setText(
                    holder.itemView.getContext().getString(
                            R.string.ai_labels_prefix,
                            holder.itemView.getContext().getString(R.string.not_available)
                    )
            );
        } else {
            holder.txtFavoriteAiLabels.setText(
                    holder.itemView.getContext().getString(
                            R.string.ai_labels_prefix,
                            AiLabelDisplayMapper.toDisplayLabels(holder.itemView.getContext(), wallpaper.aiLabels)
                    )
            );
        }

        holder.btnFavoriteRepost.setOnClickListener(v -> RepostDialogHelper.show(v.getContext(), holder.itemView, wallpaper));

        holder.btnFavoriteRemove.setOnClickListener(v -> {
            wallpaper.isFavorite = false;
            wallpaper.aiCategory = "";
            wallpaper.aiLabels = "";
            FirebaseFavoritesStore.removeFavorite(wallpaper);
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                list.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
            } else {
                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WallpaperDetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.id);
            v.getContext().startActivity(intent);
        });
    }

    private boolean isAnalyzing(Wallpaper wallpaper) {
        if (wallpaper == null) return false;
        String category = wallpaper.aiCategory == null ? "" : wallpaper.aiCategory.toLowerCase();
        String labels = wallpaper.aiLabels == null ? "" : wallpaper.aiLabels.toLowerCase();
        return category.contains("analyzing")
                || category.contains("analiz")
                || labels.contains("progress")
                || labels.contains("devam")
                || labels.contains("cache")
                || labels.contains("önbellek");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageFavorite;
        MaterialButton btnFavoriteRemove;
        MaterialButton btnFavoriteRepost;
        CircularProgressIndicator progressFavoriteAnalysis;
        TextView txtFavoriteTitle;
        TextView txtFavoriteAiCategory;
        TextView txtFavoriteAiLabels;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFavorite = itemView.findViewById(R.id.imageFavorite);
            btnFavoriteRemove = itemView.findViewById(R.id.btnFavoriteRemove);
            btnFavoriteRepost = itemView.findViewById(R.id.btnFavoriteRepost);
            progressFavoriteAnalysis = itemView.findViewById(R.id.progressFavoriteAnalysis);
            txtFavoriteTitle = itemView.findViewById(R.id.txtFavoriteTitle);
            txtFavoriteAiCategory = itemView.findViewById(R.id.txtFavoriteAiCategory);
            txtFavoriteAiLabels = itemView.findViewById(R.id.txtFavoriteAiLabels);
        }
    }
}
