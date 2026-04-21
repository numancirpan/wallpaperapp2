package com.example.wallpaperapp2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private LinearLayout favoritesContainer;

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        favoritesContainer = view.findViewById(R.id.favoritesContainer);

        renderFavoriteGroups();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        renderFavoriteGroups();
    }

    private void renderFavoriteGroups() {
        if (favoritesContainer == null) return;

        favoritesContainer.removeAllViews();

        TextView title = new TextView(requireContext());
        title.setText("AI Categorized Favorites");
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        titleParams.bottomMargin = 16;
        title.setLayoutParams(titleParams);
        favoritesContainer.addView(title);

        Map<String, List<Wallpaper>> groupedFavorites =
                WallpaperRepository.getFavoriteWallpapersGroupedByCategory();

        if (groupedFavorites.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("No favorites yet.");
            emptyText.setTextSize(16);
            favoritesContainer.addView(emptyText);
            return;
        }

        for (Map.Entry<String, List<Wallpaper>> entry : groupedFavorites.entrySet()) {
            String categoryName = entry.getKey();
            List<Wallpaper> wallpapers = entry.getValue();

            TextView categoryTitle = new TextView(requireContext());
            categoryTitle.setText(categoryName);
            categoryTitle.setTextSize(20);
            categoryTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams categoryParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            categoryParams.topMargin = 16;
            categoryParams.bottomMargin = 8;
            categoryTitle.setLayoutParams(categoryParams);

            favoritesContainer.addView(categoryTitle);

            RecyclerView recyclerView = new RecyclerView(requireContext());
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            );
            recyclerView.setAdapter(new FavoriteAiAdapter(wallpapers));

            LinearLayout.LayoutParams recyclerParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            recyclerView.setLayoutParams(recyclerParams);

            favoritesContainer.addView(recyclerView);
        }
    }
}