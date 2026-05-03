package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class WallpaperRepository {

    public static List<Wallpaper> wallpaperList = new ArrayList<>();

    public static void initializeData() {
        if (!wallpaperList.isEmpty()) {
            return;
        }
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

    public static List<Wallpaper> searchWallpapersByTitle(String query) {
        List<Wallpaper> filteredList = new ArrayList<>();
        String normalized = query == null ? "" : query.toLowerCase();

        for (Wallpaper wallpaper : wallpaperList) {
            boolean matchesQuery = wallpaper.title.toLowerCase().contains(normalized)
                    || (wallpaper.aiCategory != null && wallpaper.aiCategory.toLowerCase().contains(normalized))
                    || (wallpaper.aiLabels != null && wallpaper.aiLabels.toLowerCase().contains(normalized));

            if (matchesQuery) {
                filteredList.add(wallpaper);
            }
        }

        return filteredList;
    }

    public static Wallpaper getWallpaperById(int id) {
        for (Wallpaper wallpaper : wallpaperList) {
            if (wallpaper.id == id) {
                return wallpaper;
            }
        }
        return null;
    }
    public static Map<String, List<Wallpaper>> getFavoriteWallpapersGroupedByCategory() {
        Map<String, List<Wallpaper>> groupedMap = new LinkedHashMap<>();

        for (Wallpaper wallpaper : wallpaperList) {
            if (!wallpaper.isFavorite) continue;

            String category = wallpaper.aiCategory;
            if (category == null || category.trim().isEmpty()) {
                category = "Not Analyzed Yet";
            }

            if (!groupedMap.containsKey(category)) {
                groupedMap.put(category, new ArrayList<>());
            }

            groupedMap.get(category).add(wallpaper);
        }

        return groupedMap;
    }

    public static List<String> getExistingAiCategories() {
        List<String> categories = new ArrayList<>();

        for (Wallpaper wallpaper : wallpaperList) {
            if (!wallpaper.isFavorite) continue;
            if (wallpaper.aiCategory == null || wallpaper.aiCategory.trim().isEmpty()) continue;

            boolean exists = false;
            for (String category : categories) {
                if (category.equalsIgnoreCase(wallpaper.aiCategory)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                categories.add(wallpaper.aiCategory);
            }
        }

        return categories;
    }

    public static void replaceAll(List<Wallpaper> newWallpapers) {
        wallpaperList.clear();
        if (newWallpapers != null) {
            wallpaperList.addAll(newWallpapers);
        }
    }
}