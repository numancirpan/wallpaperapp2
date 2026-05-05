package com.example.wallpaperapp2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

    public interface OnCollectionClickListener {
        void onCollectionClick(WallpaperCollection collection);
    }

    public interface OnCollectionActionListener {
        void onRename(WallpaperCollection collection);
        void onDelete(WallpaperCollection collection);
    }

    private List<WallpaperCollection> collections;
    private final OnCollectionClickListener clickListener;
    private final OnCollectionActionListener actionListener;

    public CollectionAdapter(List<WallpaperCollection> collections) {
        this(collections, null);
    }

    public CollectionAdapter(List<WallpaperCollection> collections, OnCollectionClickListener clickListener) {
        this(collections, clickListener, null);
    }

    public CollectionAdapter(
            List<WallpaperCollection> collections,
            OnCollectionClickListener clickListener,
            OnCollectionActionListener actionListener
    ) {
        this.collections = collections;
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    public void updateList(List<WallpaperCollection> newCollections) {
        this.collections = newCollections;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WallpaperCollection collection = collections.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCollectionClick(collection);
        });
        holder.btnRenameCollection.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onRename(collection);
        });
        holder.btnDeleteCollection.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(collection);
        });
        holder.txtCollectionName.setText(collection.name);
        int count = collection.wallpapers == null ? 0 : collection.wallpapers.size();
        holder.txtCollectionMeta.setText(
                holder.itemView.getContext().getResources().getQuantityString(
                        R.plurals.collection_photo_count,
                        count,
                        count
                )
        );

        holder.previewRow.removeAllViews();
        if (collection.wallpapers == null || collection.wallpapers.isEmpty()) {
            holder.emptyPreview.setVisibility(View.VISIBLE);
            return;
        }

        holder.emptyPreview.setVisibility(View.GONE);
        int previewCount = Math.min(collection.wallpapers.size(), 4);
        for (int i = 0; i < previewCount; i++) {
            Wallpaper wallpaper = collection.wallpapers.get(i);
            ImageView image = new ImageView(holder.itemView.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(holder, 88), 1f);
            params.setMargins(i == 0 ? 0 : dp(holder, 6), 0, 0, 0);
            image.setLayoutParams(params);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(String.format(Locale.getDefault(), "%s %d", collection.name, i + 1));
            if (wallpaper.hasRemoteImage()) {
                Glide.with(holder.itemView.getContext()).load(wallpaper.imageUrl).centerCrop().into(image);
            } else {
                image.setImageResource(wallpaper.imageRes);
            }
            holder.previewRow.addView(image);
        }
    }

    @Override
    public int getItemCount() {
        return collections == null ? 0 : collections.size();
    }

    private int dp(ViewHolder holder, int value) {
        return (int) (value * holder.itemView.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCollectionName;
        TextView txtCollectionMeta;
        TextView emptyPreview;
        MaterialButton btnRenameCollection;
        MaterialButton btnDeleteCollection;
        LinearLayout previewRow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCollectionName = itemView.findViewById(R.id.txtCollectionName);
            txtCollectionMeta = itemView.findViewById(R.id.txtCollectionMeta);
            emptyPreview = itemView.findViewById(R.id.txtCollectionEmptyPreview);
            btnRenameCollection = itemView.findViewById(R.id.btnRenameCollection);
            btnDeleteCollection = itemView.findViewById(R.id.btnDeleteCollection);
            previewRow = itemView.findViewById(R.id.collectionPreviewRow);
        }
    }
}
