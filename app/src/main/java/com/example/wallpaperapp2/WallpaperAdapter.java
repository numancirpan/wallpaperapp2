package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

    public interface ImageLoadListener {
        void onImageLoadFinished();
    }

    List<Wallpaper> list;
    private ImageLoadListener imageLoadListener;

    public WallpaperAdapter(List<Wallpaper> list) {
        this.list = list;
    }

    public void setImageLoadListener(ImageLoadListener listener) {
        this.imageLoadListener = listener;
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
            Glide.with(holder.itemView.getContext())
                    .load(wallpaper.imageUrl)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            notifyImageLoadFinished();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            notifyImageLoadFinished();
                            return false;
                        }
                    })
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(wallpaper.imageRes);
            notifyImageLoadFinished();
        }

        holder.txtWallpaperTitle.setVisibility(View.GONE);
        FavoriteButtonStyler.apply(holder.btnFavorite, wallpaper.isFavorite);
        applyCollectionButtonStyle(holder.btnAddToCollection, UserProfileStore.isWallpaperInCachedCollection(wallpaper.id));

        holder.btnFavorite.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            wallpaper.isFavorite = !wallpaper.isFavorite;
            FavoriteButtonStyler.apply(holder.btnFavorite, wallpaper.isFavorite);

            if (!wallpaper.isFavorite) {
                FirebaseFavoritesStore.removeFavorite(wallpaper);
                notifyItemChanged(adapterPosition);
                return;
            }

            applyFastApiTagAnalysis(wallpaper);
            FirebaseFavoritesStore.saveFavorite(wallpaper);
            notifyItemChanged(adapterPosition);
        });

        holder.btnRepost.setOnClickListener(v -> RepostDialogHelper.show(v.getContext(), holder.itemView, wallpaper));
        holder.btnAddToCollection.setOnClickListener(v -> CollectionDialogHelper.show(v.getContext(), holder.itemView, wallpaper));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WallpaperDetailActivity.class);
            intent.putExtra("wallpaper_id", wallpaper.id);
            v.getContext().startActivity(intent);
        });
    }

    private void notifyImageLoadFinished() {
        if (imageLoadListener != null) {
            imageLoadListener.onImageLoadFinished();
        }
    }

    private void applyFastApiTagAnalysis(Wallpaper wallpaper) {
        if (!needsAnalysis(wallpaper)) return;

        String tags = wallpaper.tags == null ? "" : wallpaper.tags.trim();
        if (!tags.isEmpty()) {
            wallpaper.aiCategory = inferCategoryFromTags(tags);
            wallpaper.aiLabels = tags;
            return;
        }

        wallpaper.aiCategory = "Other";
        wallpaper.aiLabels = "API metadata not available";
    }

    private String inferCategoryFromTags(String tags) {
        String text = tags.toLowerCase();
        if (containsAny(text, "dog", "cat", "animal", "wildlife", "bird", "horse", "cow", "pet", "puppy", "kitten")) {
            return "Animals";
        }
        if (containsAny(text, "beach", "sea", "ocean", "water", "coast", "shore", "lake", "river")) {
            return "Beach";
        }
        if (containsAny(text, "city", "urban", "street", "building", "architecture", "skyline")) {
            return "Urban";
        }
        if (containsAny(text, "space", "star", "galaxy", "moon", "planet", "night sky")) {
            return "Space";
        }
        if (containsAny(text, "car", "vehicle", "motorcycle", "road", "transport")) {
            return "Vehicles";
        }
        if (containsAny(text, "people", "person", "portrait", "face", "human")) {
            return "People";
        }
        if (containsAny(text, "desk", "office", "computer", "laptop", "keyboard", "workspace", "work")) {
            return "Workspace";
        }
        if (containsAny(text, "abstract", "pattern", "texture", "design", "art")) {
            return "Art";
        }
        if (containsAny(text, "nature", "flower", "forest", "tree", "leaf", "mountain", "landscape", "plant")) {
            return "Nature";
        }
        return "Other";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
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
