package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.List;

public class WallpaperRepository {

    public static List<Wallpaper> wallpaperList = new ArrayList<>();

    public static void initializeData() {
        if (!wallpaperList.isEmpty()) return;

        wallpaperList.add(new Wallpaper(R.drawable.wall1, "Green Dream", "Nature"));
        wallpaperList.add(new Wallpaper(R.drawable.wall2, "Pattern World", "Art"));
        wallpaperList.add(new Wallpaper(R.drawable.wall3, "City Racing", "City"));
        wallpaperList.add(new Wallpaper(R.drawable.wall4, "Mystic Water", "Nature"));
    }

    public static List<Wallpaper> getFavoriteWallpapers() {
        List<Wallpaper> favoriteList = new ArrayList<>();

        for (Wallpaper wallpaper : wallpaperList) {
            if (wallpaper.isFavorite) {
                favoriteList.add(wallpaper);
            }
        }

        return favoriteList;
    }

    public static List<Wallpaper> searchWallpapers(String query, String category) {
        List<Wallpaper> filteredList = new ArrayList<>();

        for (Wallpaper wallpaper : wallpaperList) {
            boolean matchesQuery = wallpaper.title.toLowerCase().contains(query.toLowerCase());
            boolean matchesCategory = category.equals("All") || wallpaper.category.equalsIgnoreCase(category);

            if (matchesQuery && matchesCategory) {
                filteredList.add(wallpaper);
            }
        }

        return filteredList;
    }
}