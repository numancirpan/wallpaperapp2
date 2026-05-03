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

        if (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty()) {
            holder.txtFavoriteAiCategory.setText("AI Category: Not analyzed yet");
        } else {
            holder.txtFavoriteAiCategory.setText("AI Category: " + wallpaper.aiCategory);
        }

        if (wallpaper.aiLabels == null || wallpaper.aiLabels.isEmpty()) {
            holder.txtFavoriteAiLabels.setText("AI Labels: Not available");
        } else {
            holder.txtFavoriteAiLabels.setText("AI Labels: " + wallpaper.aiLabels);
        }

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
        ImageView imageFavorite;
        TextView txtFavoriteTitle;
        TextView txtFavoriteAiCategory;
        TextView txtFavoriteAiLabels;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFavorite = itemView.findViewById(R.id.imageFavorite);
            txtFavoriteTitle = itemView.findViewById(R.id.txtFavoriteTitle);
            txtFavoriteAiCategory = itemView.findViewById(R.id.txtFavoriteAiCategory);
            txtFavoriteAiLabels = itemView.findViewById(R.id.txtFavoriteAiLabels);
        }
    }
}