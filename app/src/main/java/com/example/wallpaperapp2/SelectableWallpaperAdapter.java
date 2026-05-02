package com.example.wallpaperapp2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SelectableWallpaperAdapter extends RecyclerView.Adapter<SelectableWallpaperAdapter.ViewHolder> {

    public interface OnWallpaperSelectedListener {
        void onSelected(Wallpaper wallpaper);
    }

    private final List<Wallpaper> wallpapers;
    private final OnWallpaperSelectedListener listener;

    public SelectableWallpaperAdapter(List<Wallpaper> wallpapers, OnWallpaperSelectedListener listener) {
        this.wallpapers = wallpapers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_wallpaper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wallpaper wallpaper = wallpapers.get(position);
        if (wallpaper.hasRemoteImage()) {
            Glide.with(holder.itemView.getContext()).load(wallpaper.imageUrl).centerCrop().into(holder.imageSelectableWallpaper);
        } else {
            holder.imageSelectableWallpaper.setImageResource(wallpaper.imageRes);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(wallpaper);
        });
    }

    @Override
    public int getItemCount() {
        return wallpapers == null ? 0 : wallpapers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageSelectableWallpaper;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSelectableWallpaper = itemView.findViewById(R.id.imageSelectableWallpaper);
        }
    }
}
