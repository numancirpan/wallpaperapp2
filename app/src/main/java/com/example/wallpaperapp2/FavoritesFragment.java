package com.example.wallpaperapp2;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class FavoritesFragment extends Fragment {

    RecyclerView recyclerView;
    WallpaperAdapter adapter;
    List<Wallpaper> favoriteList;

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recyclerFavorites);

        AppSettingsManager settingsManager = new AppSettingsManager(requireContext());
        int columnCount = settingsManager.getGridColumns();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        favoriteList = WallpaperRepository.getFavoriteWallpapers();
        adapter = new WallpaperAdapter(favoriteList);
        recyclerView.setAdapter(adapter);

        return view;
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
            favoriteList.clear();
            favoriteList.addAll(WallpaperRepository.getFavoriteWallpapers());
            adapter.notifyDataSetChanged();
        }
    }
}