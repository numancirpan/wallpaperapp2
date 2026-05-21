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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private LinearLayout favoritesContainer;
    private LinearLayout favoritesGroupsContainer;
    private MaterialCardView cardFavoritesEmptyState;
    private TextView txtFavoritesEmptyTitle;
    private TextView txtFavoritesEmptyDescription;
    private TextInputEditText editFavoritesSearch;
    private ListenerRegistration favoritesListener;
    private boolean hasRenderedLocalFavorites = false;

    private final UserProfileStore.CollectionsChangeListener collectionsChangeListener = () -> {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::renderFavoriteGroups);
    };

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        favoritesContainer = view.findViewById(R.id.favoritesContainer);
        favoritesGroupsContainer = view.findViewById(R.id.favoritesGroupsContainer);
        cardFavoritesEmptyState = view.findViewById(R.id.cardFavoritesEmptyState);
        txtFavoritesEmptyTitle = view.findViewById(R.id.txtFavoritesEmptyTitle);
        txtFavoritesEmptyDescription = view.findViewById(R.id.txtFavoritesEmptyDescription);
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

        UserProfileStore.addCollectionsChangeListener(collectionsChangeListener);
        renderFavoriteGroups();
        loadCollectionsForBadges();
        startFavoritesListener();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        renderFavoriteGroups();
        startFavoritesListener();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UserProfileStore.removeCollectionsChangeListener(collectionsChangeListener);
        stopFavoritesListener();
    }

    private void startFavoritesListener() {
        if (favoritesContainer == null || favoritesGroupsContainer == null) return;
        if (favoritesListener != null) return;

        favoritesListener = FirebaseFavoritesStore.listenFavorites(favoritesById -> {
            WallpaperRepository.applyFavoriteData(favoritesById);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(this::renderFavoriteGroups);
        });
    }

    private void loadCollectionsForBadges() {
        UserProfileStore.fetchCollections(collections -> {
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

    private void renderFavoriteGroups() {
        if (favoritesContainer == null || favoritesGroupsContainer == null) return;

        favoritesGroupsContainer.removeAllViews();
        hideEmptyState();
        hasRenderedLocalFavorites = true;

        Map<String, List<Wallpaper>> groupedFavorites =
                WallpaperRepository.getFavoriteWallpapersGroupedByCategory();
        String query = editFavoritesSearch != null && editFavoritesSearch.getText() != null
                ? normalizeSearch(editFavoritesSearch.getText().toString())
                : "";

        if (groupedFavorites.isEmpty()) {
            showEmptyState(R.string.no_favorites_yet, R.string.no_favorites_yet_description);
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
            showEmptyState(R.string.no_favorite_matches, R.string.no_favorite_matches_description);
        }
    }

    private void showEmptyState(int titleRes, int descriptionRes) {
        if (cardFavoritesEmptyState == null) return;
        txtFavoritesEmptyTitle.setText(titleRes);
        txtFavoritesEmptyDescription.setText(descriptionRes);
        cardFavoritesEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (cardFavoritesEmptyState != null) {
            cardFavoritesEmptyState.setVisibility(View.GONE);
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

            String searchable = normalizeSearch(
                    wallpaper.title + " "
                            + rawCategory + " "
                            + rawLabels + " "
                            + displayCategory
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
