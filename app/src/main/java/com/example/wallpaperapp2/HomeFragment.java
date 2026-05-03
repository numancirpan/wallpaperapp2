package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final List<String> QUICK_SEARCHES = Arrays.asList(
            "nature",
            "work",
            "animals",
            "beach",
            "city",
            "abstract"
    );

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> list;
    TextInputEditText editSearch;
    ChipGroup chipGroupSuggestions;

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
        setupQuickSearchChips();
        loadWallpapersFromApi();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWallpapers();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    private void setupQuickSearchChips() {
        if (chipGroupSuggestions == null) return;

        chipGroupSuggestions.removeAllViews();
        for (String quickSearch : QUICK_SEARCHES) {
            Chip chip = new Chip(requireContext());
            chip.setText(capitalizeLabel(quickSearch));
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setChipBackgroundColorResource(R.color.chip_background_color);
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.chip_stroke)));
            chip.setChipStrokeWidth(2f);
            chip.setChipCornerRadius(24f);
            chip.setOnClickListener(v -> {
                if (editSearch == null) return;
                editSearch.setText(quickSearch);
                editSearch.setSelection(quickSearch.length());
            });
            chipGroupSuggestions.addView(chip);
        }
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
                warmUpSearchMetadata();
            });
        });
    }

    private void filterWallpapers() {
        String query = "";

        if (editSearch.getText() != null) {
            query = editSearch.getText().toString().trim();
        }

        List<Wallpaper> filteredList = WallpaperRepository.searchWallpapers(requireContext(), query);
        adapter.updateList(filteredList);
        updateQuickSearchSelection(query);
    }

    private void warmUpSearchMetadata() {
        if (!isAdded()) return;

        List<String> existingCategories = WallpaperRepository.getExistingAiCategories();
        for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
            WallpaperAiMetadataIndexer.ensureSearchMetadata(
                    requireContext(),
                    wallpaper,
                    existingCategories,
                    updatedWallpaper -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (editSearch == null || editSearch.getText() == null) return;
                            String activeQuery = editSearch.getText().toString().trim();
                            if (!activeQuery.isEmpty()) {
                                filterWallpapers();
                            }
                        });
                    }
            );
        }
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
        }
    }

    private String capitalizeLabel(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private void updateQuickSearchSelection(String query) {
        if (chipGroupSuggestions == null) return;

        String normalizedQuery = query == null ? "" : query.trim();
        for (int i = 0; i < chipGroupSuggestions.getChildCount(); i++) {
            View child = chipGroupSuggestions.getChildAt(i);
            if (!(child instanceof Chip)) continue;

            Chip chip = (Chip) child;
            String chipText = chip.getText() == null ? "" : chip.getText().toString();
            chip.setChecked(chipText.equalsIgnoreCase(normalizedQuery));
        }
    }
}
