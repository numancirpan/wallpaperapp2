package com.example.wallpaperapp2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void applyFavoriteData_emptySnapshotClearsPreviousFavorites() {
        WallpaperRepository.wallpaperList.clear();
        Wallpaper first = new Wallpaper(1, "https://example.com/1.jpg", "First");
        Wallpaper second = new Wallpaper(2, "https://example.com/2.jpg", "Second");
        first.isFavorite = true;
        second.isFavorite = true;
        WallpaperRepository.wallpaperList.add(first);
        WallpaperRepository.wallpaperList.add(second);

        WallpaperRepository.applyFavoriteData(new HashMap<>());

        assertFalse(first.isFavorite);
        assertFalse(second.isFavorite);
    }

    @Test
    public void applyFavoriteData_replacesPreviousFavoriteState() {
        WallpaperRepository.wallpaperList.clear();
        Wallpaper first = new Wallpaper(1, "https://example.com/1.jpg", "First");
        Wallpaper second = new Wallpaper(2, "https://example.com/2.jpg", "Second");
        first.isFavorite = true;
        WallpaperRepository.wallpaperList.add(first);
        WallpaperRepository.wallpaperList.add(second);

        Map<String, Object> favoritePayload = new HashMap<>();
        favoritePayload.put("id", 2);
        favoritePayload.put("title", "Second");
        favoritePayload.put("imageUrl", "https://example.com/2.jpg");
        favoritePayload.put("aiCategory", "Nature");
        favoritePayload.put("aiLabels", "tree");

        Map<Integer, Map<String, Object>> favoritesById = new HashMap<>();
        favoritesById.put(2, favoritePayload);

        WallpaperRepository.applyFavoriteData(favoritesById);

        assertFalse(first.isFavorite);
        assertTrue(second.isFavorite);
    }
}
