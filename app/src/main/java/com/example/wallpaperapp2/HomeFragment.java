package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final long SEARCH_DELAY_MS = 700;
    private static final List<String> QUICK_SEARCHES = Arrays.asList("nature", "urban", "animals", "beach", "space");
    private static final Map<String, String> API_QUERY_MAP = new LinkedHashMap<>();

    static {
        API_QUERY_MAP.put("kopek", "dog");
        API_QUERY_MAP.put("kedi", "cat");
        API_QUERY_MAP.put("hayvan", "animals");
        API_QUERY_MAP.put("hayvanlar", "animals");
        API_QUERY_MAP.put("doga", "nature");
        API_QUERY_MAP.put("sahil", "beach");
        API_QUERY_MAP.put("deniz", "beach");
        API_QUERY_MAP.put("sehir", "city");
        API_QUERY_MAP.put("uzay", "space");
        API_QUERY_MAP.put("cicek", "flower");
        API_QUERY_MAP.put("araba", "car");
        API_QUERY_MAP.put("arac", "car");
        API_QUERY_MAP.put("urban", "city");
        API_QUERY_MAP.put("animals", "animals");
        API_QUERY_MAP.put("nature", "nature");
        API_QUERY_MAP.put("beach", "beach");
        API_QUERY_MAP.put("space", "space");
    }

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> list;
    TextInputEditText editSearch;
    ChipGroup chipGroupSuggestions;
    LinearProgressIndicator progressDynamicSearch;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;
    private int searchRequestCounter = 0;
    private String lastApiQuery = "";
    private boolean chipClickInProgress = false;
    private boolean apiSearchActive = false;

    private final UserProfileStore.CollectionsChangeListener collectionsChangeListener = () -> {
        if (!isAdded() || adapter == null) return;
        requireActivity().runOnUiThread(() -> {
            if (apiSearchActive) {
                renderApiResultsWithoutLocalFilter();
            } else {
                filterWallpapers();
            }
        });
    };

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        editSearch = view.findViewById(R.id.editSearch);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);
        progressDynamicSearch = view.findViewById(R.id.progressDynamicSearch);

        AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), settingsManager.getGridColumns()));

        WallpaperRepository.initializeData();
        list = WallpaperRepository.wallpaperList;
        adapter = new WallpaperAdapter(list);
        recyclerView.setAdapter(adapter);

        setupQuickSearchChips();
        UserProfileStore.addCollectionsChangeListener(collectionsChangeListener);
        loadCollectionsForBadges();
        loadWallpapersFromApi();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chipClickInProgress) return;
                handleSearchChanged(s == null ? "" : s.toString());
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        UserProfileStore.removeCollectionsChangeListener(collectionsChangeListener);
        cancelPendingSearch();
        super.onDestroyView();
    }

    private void setupQuickSearchChips() {
        if (chipGroupSuggestions == null) return;
        chipGroupSuggestions.removeAllViews();

        for (String quickSearch : QUICK_SEARCHES) {
            Chip chip = new Chip(requireContext());
            chip.setTag(quickSearch);
            chip.setText(quickSearchLabel(quickSearch));
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setChipBackgroundColorResource(R.color.chip_background_color);
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.chip_stroke)));
            chip.setChipStrokeWidth(2f);
            chip.setChipCornerRadius(24f);
            chip.setOnClickListener(v -> selectQuickSearch(quickSearch));
            chipGroupSuggestions.addView(chip);
        }
    }

    private void selectQuickSearch(String quickSearch) {
        if (editSearch == null) return;
        String label = quickSearchLabel(quickSearch);
        chipClickInProgress = true;
        editSearch.setText(label);
        editSearch.setSelection(label.length());
        chipClickInProgress = false;
        updateQuickSearchSelection(label);
        scheduleDynamicApiSearch(quickSearch, label);
    }

    private void handleSearchChanged(String rawQuery) {
        String normalized = normalizeQuickSearch(rawQuery);
        if (normalized.isEmpty()) {
            apiSearchActive = false;
            cancelPendingSearch();
            lastApiQuery = "";
            hideLoading();
            updateQuickSearchSelection("");
            adapter.updateList(new ArrayList<>());
            loadWallpapersFromApi();
            return;
        }
        updateQuickSearchSelection(rawQuery);
        scheduleDynamicApiSearch(rawQuery, rawQuery);
    }

    private void loadWallpapersFromApi() {
        WallpaperApiService.fetchWallpapers(new WallpaperApiService.Callback() {
            @Override
            public void onSuccess(List<Wallpaper> wallpapers) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    WallpaperRepository.replaceAll(wallpapers);
                    restoreCloudFavoritesAndRender(false);
                });
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), R.string.api_fetch_failed_cached, Snackbar.LENGTH_SHORT).show());
            }
        });
    }

    private void scheduleDynamicApiSearch(String rawQuery, String visibleQuery) {
        cancelPendingSearch();
        String apiQuery = toApiQuery(rawQuery);
        if (apiQuery.isEmpty()) {
            hideLoading();
            adapter.updateList(new ArrayList<>());
            return;
        }
        apiSearchActive = true;
        adapter.updateList(new ArrayList<>());
        showLoading();
        pendingSearchRunnable = () -> fetchDynamicApiResults(apiQuery, visibleQuery);
        searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DELAY_MS);
    }

    private void fetchDynamicApiResults(String apiQuery, String visibleQuery) {
        lastApiQuery = apiQuery;
        int requestId = ++searchRequestCounter;

        WallpaperApiService.fetchWallpapersForQuery(apiQuery, new WallpaperApiService.Callback() {
            @Override
            public void onSuccess(List<Wallpaper> wallpapers) {
                if (!isAdded() || requestId != searchRequestCounter) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    if (!isSearchStillActive(visibleQuery)) return;
                    if (wallpapers == null || wallpapers.isEmpty()) {
                        adapter.updateList(new ArrayList<>());
                        return;
                    }
                    WallpaperRepository.replaceAll(wallpapers);
                    restoreCloudFavoritesAndRender(true);
                });
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded() || requestId != searchRequestCounter) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    adapter.updateList(new ArrayList<>());
                });
            }
        });
    }

    private boolean isSearchStillActive(String visibleQuery) {
        if (editSearch == null || editSearch.getText() == null) return false;
        String current = normalizeQuickSearch(editSearch.getText().toString());
        String visible = normalizeQuickSearch(visibleQuery);
        return current.equals(visible) || toApiQuery(current).equals(toApiQuery(visible));
    }

    private String toApiQuery(String rawQuery) {
        String normalized = normalizeQuickSearch(rawQuery);
        if (normalized.length() < 2) return "";
        for (Map.Entry<String, String> entry : API_QUERY_MAP.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(normalized) || normalized.startsWith(key)) {
                return entry.getValue();
            }
        }
        return normalized;
    }

    private void cancelPendingSearch() {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
            pendingSearchRunnable = null;
        }
    }

    private void showLoading() {
        if (progressDynamicSearch != null) progressDynamicSearch.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        if (progressDynamicSearch != null) progressDynamicSearch.setVisibility(View.GONE);
    }

    private void loadCollectionsForBadges() {
        UserProfileStore.fetchCollections(collections -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (apiSearchActive) renderApiResultsWithoutLocalFilter(); else filterWallpapers();
            });
        });
    }

    private void restoreCloudFavoritesAndRender(boolean showApiResultsOnly) {
        FirebaseFavoritesStore.fetchFavorites(favoritesById -> {
            WallpaperRepository.applyFavoriteData(favoritesById);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (showApiResultsOnly) {
                    renderApiResultsWithoutLocalFilter();
                } else {
                    filterWallpapers();
                }
                warmUpSearchMetadata();
            });
        });
    }

    private void renderApiResultsWithoutLocalFilter() {
        adapter.updateList(new ArrayList<>(WallpaperRepository.wallpaperList));
        String query = editSearch != null && editSearch.getText() != null ? editSearch.getText().toString() : "";
        updateQuickSearchSelection(query);
    }

    private void filterWallpapers() {
        String query = editSearch.getText() == null ? "" : editSearch.getText().toString().trim();
        List<Wallpaper> filteredList = WallpaperRepository.searchWallpapers(requireContext(), query);
        adapter.updateList(filteredList);
        updateQuickSearchSelection(query);
    }

    private void warmUpSearchMetadata() {
        if (!isAdded()) return;
        List<String> existingCategories = WallpaperRepository.getExistingAiCategories();
        for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
            WallpaperAiMetadataIndexer.ensureSearchMetadata(requireContext(), wallpaper, existingCategories, updatedWallpaper -> {
                if (!isAdded() || apiSearchActive) return;
                requireActivity().runOnUiThread(() -> {
                    if (editSearch == null || editSearch.getText() == null) return;
                    if (!editSearch.getText().toString().trim().isEmpty()) filterWallpapers();
                });
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recyclerView != null) {
            AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), settingsManager.getGridColumns()));
        }
        if (adapter != null) {
            if (apiSearchActive) renderApiResultsWithoutLocalFilter(); else filterWallpapers();
        }
    }

    private String quickSearchLabel(String value) {
        return CategoryDisplayMapper.toDisplayName(requireContext(), value);
    }

    private void updateQuickSearchSelection(String query) {
        if (chipGroupSuggestions == null) return;
        String normalizedQuery = normalizeQuickSearch(query);
        for (int i = 0; i < chipGroupSuggestions.getChildCount(); i++) {
            View child = chipGroupSuggestions.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            String quickSearch = chip.getTag() == null ? "" : chip.getTag().toString();
            String chipText = chip.getText() == null ? "" : chip.getText().toString();
            chip.setChecked(normalizedQuery.equals(normalizeQuickSearch(quickSearch)) || normalizedQuery.equals(normalizeQuickSearch(chipText)));
        }
    }

    private String normalizeQuickSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
