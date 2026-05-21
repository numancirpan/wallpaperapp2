package com.example.wallpaperapp2;

import android.content.Intent;
import android.content.res.ColorStateList;
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

        holder.txtFavoriteTitle.setVisibility(View.GONE);
        holder.txtFavoriteAiCategory.setVisibility(View.GONE);
        holder.txtFavoriteAiLabels.setVisibility(View.GONE);
        holder.progressFavoriteAnalysis.setVisibility(View.GONE);

        FavoriteButtonStyler.apply(holder.btnFavoriteRemove, true);
        applyCollectionButtonStyle(holder.btnFavoriteCollection, UserProfileStore.isWallpaperInCachedCollection(wallpaper.id));

        holder.btnFavoriteRepost.setOnClickListener(v -> RepostDialogHelper.show(v.getContext(), holder.itemView, wallpaper));
        holder.btnFavoriteCollection.setOnClickListener(v -> CollectionDialogHelper.show(v.getContext(), holder.itemView, wallpaper));

        holder.btnFavoriteRemove.setOnClickListener(v -> {
            wallpaper.isFavorite = false;
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
        ImageView imageFavorite;
        MaterialButton btnFavoriteRemove;
        MaterialButton btnFavoriteRepost;
        MaterialButton btnFavoriteCollection;
        CircularProgressIndicator progressFavoriteAnalysis;
        TextView txtFavoriteTitle;
        TextView txtFavoriteAiCategory;
        TextView txtFavoriteAiLabels;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFavorite = itemView.findViewById(R.id.imageFavorite);
            btnFavoriteRemove = itemView.findViewById(R.id.btnFavoriteRemove);
            btnFavoriteRepost = itemView.findViewById(R.id.btnFavoriteRepost);
            btnFavoriteCollection = itemView.findViewById(R.id.btnFavoriteCollection);
            progressFavoriteAnalysis = itemView.findViewById(R.id.progressFavoriteAnalysis);
            txtFavoriteTitle = itemView.findViewById(R.id.txtFavoriteTitle);
            txtFavoriteAiCategory = itemView.findViewById(R.id.txtFavoriteAiCategory);
            txtFavoriteAiLabels = itemView.findViewById(R.id.txtFavoriteAiLabels);
        }
    }
}
