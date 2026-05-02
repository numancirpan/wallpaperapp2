package com.example.wallpaperapp2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private LinearLayout favoritesContainer;
    private LinearLayout favoritesGroupsContainer;
    private TextInputEditText editFavoritesSearch;
    private boolean isLoadingFavorites = false;
    private ListenerRegistration favoritesListener;

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        favoritesContainer = view.findViewById(R.id.favoritesContainer);
        favoritesGroupsContainer = view.findViewById(R.id.favoritesGroupsContainer);
        editFavoritesSearch = view.findViewById(R.id.editFavoritesSearch);

        editFavoritesSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderFavoriteGroups();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        startFavoritesListener();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startFavoritesListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopFavoritesListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopFavoritesListener();
    }

    private void startFavoritesListener() {
        if (favoritesContainer == null || favoritesGroupsContainer == null) return;
        if (favoritesListener != null) return;

        isLoadingFavorites = true;
        showLoadingStateIfEmpty();

        favoritesListener = FirebaseFavoritesStore.listenFavorites(favoritesById -> {
            WallpaperRepository.applyFavoriteData(favoritesById);
            isLoadingFavorites = false;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(this::renderFavoriteGroups);
        });
    }

    private void stopFavoritesListener() {
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }

    private void showLoadingStateIfEmpty() {
        if (!WallpaperRepository.getFavoriteWallpapersGroupedByCategory().isEmpty()) {
            renderFavoriteGroups();
            return;
        }
        favoritesGroupsContainer.removeAllViews();
        TextView loadingText = new TextView(requireContext());
        loadingText.setText(R.string.loading_favorites);
        loadingText.setTextSize(16);
        favoritesGroupsContainer.addView(loadingText);
    }

    private void renderFavoriteGroups() {
        if (favoritesContainer == null || favoritesGroupsContainer == null) return;

        favoritesGroupsContainer.removeAllViews();

        Map<String, List<Wallpaper>> groupedFavorites =
                WallpaperRepository.getFavoriteWallpapersGroupedByCategory();
        String query = editFavoritesSearch != null && editFavoritesSearch.getText() != null
                ? normalizeSearch(editFavoritesSearch.getText().toString())
                : "";

        if (groupedFavorites.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(isLoadingFavorites ? R.string.loading_favorites : R.string.no_favorites_yet);
            emptyText.setTextSize(16);
            favoritesGroupsContainer.addView(emptyText);
            return;
        }

        boolean anyResult = false;
        for (Map.Entry<String, List<Wallpaper>> entry : groupedFavorites.entrySet()) {
            String categoryName = entry.getKey();
            List<Wallpaper> wallpapers = filterByQuery(entry.getValue(), query);
            if (wallpapers.isEmpty()) continue;
            anyResult = true;

            TextView categoryTitle = new TextView(requireContext());
            categoryTitle.setText(CategoryDisplayMapper.toDisplayName(requireContext(), categoryName));
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

            favoritesGroupsContainer.addView(categoryTitle);

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

            favoritesGroupsContainer.addView(recyclerView);
        }

        if (!anyResult) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(R.string.no_favorite_matches);
            emptyText.setTextSize(16);
            favoritesGroupsContainer.addView(emptyText);
        }
    }

    private List<Wallpaper> filterByQuery(List<Wallpaper> source, String query) {
        if (query == null || query.isEmpty()) {
            return source;
        }

        List<Wallpaper> filtered = new ArrayList<>();
        for (Wallpaper wallpaper : source) {
            String rawCategory = wallpaper.aiCategory == null ? "" : wallpaper.aiCategory;
            String rawLabels = wallpaper.aiLabels == null ? "" : wallpaper.aiLabels;
            String displayCategory = CategoryDisplayMapper.toDisplayName(requireContext(), rawCategory);
            String displayLabels = AiLabelDisplayMapper.toDisplayLabels(requireContext(), rawLabels);

            String searchable = normalizeSearch(
                    wallpaper.title + " "
                            + rawCategory + " "
                            + rawLabels + " "
                            + displayCategory + " "
                            + displayLabels
            );

            if (searchable.contains(query)) {
                filtered.add(wallpaper);
            }
        }
        return filtered;
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .trim();
    }
}
