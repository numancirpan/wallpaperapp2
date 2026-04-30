package com.example.wallpaperapp2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> list;
    TextInputEditText editSearch;
    private final Set<Integer> aiAnalysisInProgressIds = new HashSet<>();

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        editSearch = view.findViewById(R.id.editSearch);

        AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
        int columnCount = settingsManager.getGridColumns();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        WallpaperRepository.initializeData();
        list = WallpaperRepository.wallpaperList;

        adapter = new WallpaperAdapter(list);
        recyclerView.setAdapter(adapter);
        loadWallpapersFromApi();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWallpapers();
                triggerAiSearchIndexing();
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
                        Toast.makeText(requireContext(), "API fetch failed, showing cached data", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void restoreCloudFavoritesAndRender() {
        FirebaseFavoritesStore.fetchFavorites(favoritesById -> {
            for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
                java.util.Map<String, Object> data = favoritesById.get(wallpaper.id);
                if (data == null) continue;
                wallpaper.isFavorite = true;
                Object category = data.get("aiCategory");
                Object labels = data.get("aiLabels");
                wallpaper.aiCategory = category == null ? "" : String.valueOf(category);
                wallpaper.aiLabels = labels == null ? "" : String.valueOf(labels);
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(this::filterWallpapers);
        });
    }

    private void filterWallpapers() {
        String query = "";

        if (editSearch.getText() != null) {
            query = editSearch.getText().toString().trim();
        }

        List<Wallpaper> filteredList =
                WallpaperRepository.searchWallpapersByTitle(query);

        adapter.updateList(filteredList);
    }

    private void triggerAiSearchIndexing() {
        if (!isAdded() || editSearch.getText() == null) return;
        String query = editSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        int startedCount = 0;
        for (Wallpaper wallpaper : WallpaperRepository.wallpaperList) {
            if (startedCount >= 12) break;
            if (wallpaper.aiCategory != null && !wallpaper.aiCategory.trim().isEmpty()) continue;
            if (aiAnalysisInProgressIds.contains(wallpaper.id)) continue;

            aiAnalysisInProgressIds.add(wallpaper.id);
            startedCount++;
            analyzeWallpaperForSearch(wallpaper);
        }
    }

    private void analyzeWallpaperForSearch(Wallpaper wallpaper) {
        AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
            @Override
            public void onSuccess(List<AiLabelData> labels) {
                String generatedCategory = DynamicCategoryGenerator.generateCategory(labels);
                String matchedCategory = CategoryMatcher.matchOrCreate(
                        generatedCategory,
                        WallpaperRepository.getExistingAiCategories()
                );
                wallpaper.aiLabels = DynamicCategoryGenerator.labelsToDisplay(labels);

                GeminiCategoryService.generateCategory(wallpaper.title, wallpaper.aiLabels, geminiCategory -> {
                    String finalCategory = geminiCategory == null || geminiCategory.trim().isEmpty()
                            ? matchedCategory
                            : CategoryMatcher.matchOrCreate(geminiCategory, WallpaperRepository.getExistingAiCategories());

                    wallpaper.aiCategory = finalCategory;
                    aiAnalysisInProgressIds.remove(wallpaper.id);
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(this::refreshSearchResults);
                });
            }

            @Override
            public void onError(Exception e) {
                aiAnalysisInProgressIds.remove(wallpaper.id);
            }

            private void refreshSearchResults() {
                filterWallpapers();
            }
        };

        if (wallpaper.hasRemoteImage()) {
            AiClassifier.analyzeImageUrl(requireContext(), wallpaper.imageUrl, listener);
        } else {
            AiClassifier.analyzeImage(requireContext(), wallpaper.imageRes, listener);
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
}