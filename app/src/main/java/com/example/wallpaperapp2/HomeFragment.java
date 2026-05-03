package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> list;
    TextInputEditText editSearch;
    ChipGroup chipGroupSuggestions;
    private final Set<Integer> metadataRequestedWallpaperIds = new HashSet<>();

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        editSearch = view.findViewById(R.id.editSearch);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);

        AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
        int columnCount = settingsManager.getGridColumns();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        WallpaperRepository.initializeData();
        list = WallpaperRepository.wallpaperList;

        adapter = new WallpaperAdapter(list);
        recyclerView.setAdapter(adapter);
        renderSuggestionChips();
        loadCollectionsForIndicators();
        loadWallpapersFromApi();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWallpapers();
                updateActiveSuggestion(s == null ? "" : s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    private void loadWallpapersFromApi() {
        WallpaperApiService.fetchWallpapers(new WallpaperApiService.Callback() {
            @Override
            public void onSuccess(List<Wallpaper> wallpapers) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    WallpaperRepository.replaceAll(wallpapers);
                    restoreCloudFavoritesAndRender();
                });
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.api_fetch_failed_cached, Snackbar.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void restoreCloudFavoritesAndRender() {
        FirebaseFavoritesStore.fetchFavorites(favoritesById -> {
            WallpaperRepository.applyFavoriteData(favoritesById);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                filterWallpapers();
                indexSearchMetadata();
                renderSuggestionChips();
            });
        });
    }

    private void loadCollectionsForIndicators() {
        UserProfileStore.fetchCollections(collections -> {
            if (!isAdded() || adapter == null) return;
            requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }

    private void filterWallpapers() {
        String query = "";

        if (editSearch.getText() != null) {
            query = editSearch.getText().toString().trim();
        }

        List<Wallpaper> filteredList = WallpaperRepository.searchWallpapersByTitle(requireContext(), query);
        adapter.updateList(filteredList);
    }

    private void indexSearchMetadata() {
        if (!isAdded()) return;

        List<String> existingCategories = WallpaperRepository.getExistingAiCategories();
        for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
            if (wallpaper == null || metadataRequestedWallpaperIds.contains(wallpaper.id)) continue;
            if (FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) continue;

            metadataRequestedWallpaperIds.add(wallpaper.id);
            WallpaperAiMetadataIndexer.ensureSearchMetadata(
                    requireContext(),
                    wallpaper,
                    existingCategories,
                    updatedWallpaper -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            filterWallpapers();
                            renderSuggestionChips();
                        });
                    }
            );
        }
    }

    private void renderSuggestionChips() {
        if (chipGroupSuggestions == null || !isAdded()) return;
        chipGroupSuggestions.removeAllViews();

        List<String> suggestions = buildSearchSuggestions();
        String currentQuery = editSearch.getText() == null ? "" : editSearch.getText().toString().trim();
        for (String suggestion : suggestions) {
            Chip chip = new Chip(requireContext());
            chip.setText(suggestion);
            chip.setCheckable(true);
            chip.setChecked(suggestion.equalsIgnoreCase(currentQuery));
            chip.setChipBackgroundColor(chipColors());
            chip.setTextColor(textColors());
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#8A7AA6")));
            chip.setChipStrokeWidth(1f);
            chip.setOnClickListener(v -> {
                editSearch.setText(suggestion);
                editSearch.setSelection(suggestion.length());
                updateActiveSuggestion(suggestion);
            });
            chipGroupSuggestions.addView(chip);
        }
    }

    private List<String> buildSearchSuggestions() {
        Set<String> pool = new LinkedHashSet<>();
        for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
            if (FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
                addSuggestion(pool, CategoryDisplayMapper.toDisplayName(requireContext(), wallpaper.aiCategory));
            }
        }

        if (pool.size() < 5) {
            for (String fallback : preferredSimpleSuggestions()) {
                if (WallpaperRepository.searchWallpapersByTitle(requireContext(), fallback).isEmpty()) continue;
                addSuggestion(pool, fallback);
                if (pool.size() == 5) break;
            }
        }

        List<String> suggestions = new ArrayList<>(pool);
        Collections.shuffle(suggestions);
        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }

    private void addSuggestion(Set<String> pool, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.isEmpty()) return;
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.contains("unknown") || lower.contains("analyzing") || lower.contains("analiz")) return;
        if (lower.contains("not available") || lower.contains("mevcut")) return;
        if (lower.contains("kategorisiz") || lower.contains("uncategorized")) return;
        pool.add(clean);
    }

    private List<String> preferredSimpleSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add(getString(R.string.category_workspace));
        suggestions.add(getString(R.string.category_nature));
        suggestions.add(getString(R.string.category_beach));
        suggestions.add(getString(R.string.category_animals));
        suggestions.add(getString(R.string.category_urban));
        suggestions.add(getString(R.string.category_architecture));
        suggestions.add(getString(R.string.category_water_scenes));
        suggestions.add(getString(R.string.category_vehicles));
        return suggestions;
    }

    private void updateActiveSuggestion(String selected) {
        for (int i = 0; i < chipGroupSuggestions.getChildCount(); i++) {
            View child = chipGroupSuggestions.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(chip.getText() != null && chip.getText().toString().equalsIgnoreCase(selected));
            }
        }
    }

    private ColorStateList chipColors() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                Color.parseColor("#D8C9F3"),
                Color.parseColor("#00FFFFFF")
        };
        return new ColorStateList(states, colors);
    }

    private ColorStateList textColors() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                Color.parseColor("#3A2368"),
                Color.parseColor("#4B4256")
        };
        return new ColorStateList(states, colors);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (recyclerView != null) {
            AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
            int columnCount = settingsManager.getGridColumns();
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        }

        if (adapter != null) {
            filterWallpapers();
            loadCollectionsForIndicators();
        }
    }
}
