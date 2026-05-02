package com.example.wallpaperapp2;

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
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> list;
    TextInputEditText editSearch;

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
            requireActivity().runOnUiThread(this::filterWallpapers);
        });
    }

    private void filterWallpapers() {
        String query = "";

        if (editSearch.getText() != null) {
            query = editSearch.getText().toString().trim();
        }

        List<Wallpaper> filteredList = WallpaperRepository.searchWallpapersByTitle(query);
        adapter.updateList(filteredList);
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
