package com.example.wallpaperapp2;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WallpaperRepository {

    private static final List<String> CATEGORY_SEARCH_TERMS = Arrays.asList(
            "work", "working", "workspace",
            "animal", "animals",
            "nature", "beach", "water", "ocean", "sea",
            "city", "urban", "architecture",
            "vehicle", "vehicles",
            "people", "person", "portrait",
            "art", "abstract", "space", "food", "phone", "dark", "minimal"
    );

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

    public static List<Wallpaper> searchWallpapers(Context context, String query) {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>(wallpaperList);
        }

        List<Wallpaper> filteredList = new ArrayList<>();

        for (Wallpaper wallpaper : wallpaperList) {
            String rawCategory = safeString(wallpaper.aiCategory);
            String rawLabels = safeString(wallpaper.aiLabels);
            String displayCategory = CategoryDisplayMapper.toDisplayName(context, rawCategory);
            String displayLabels = AiLabelDisplayMapper.toDisplayLabels(context, rawLabels);
            String searchable = normalizeSearch(
                    safeString(wallpaper.title) + " "
                            + rawCategory + " "
                            + rawLabels + " "
                            + displayCategory + " "
                            + displayLabels
            );
            List<String> searchableTokens = tokenize(searchable);

            boolean matchesQuery;
            if (isCategoryStyleQuery(normalizedQuery)) {
                String categoryOnlySearchable = normalizeSearch(rawCategory + " " + displayCategory);
                matchesQuery = categoryOnlySearchable.contains(normalizedQuery)
                        || CategoryResolver.matchesQuery(normalizedQuery, rawCategory, "");
            } else if (isShortSpecificQuery(normalizedQuery)) {
                matchesQuery = matchesTokenPrefix(searchableTokens, normalizedQuery);
            } else {
                matchesQuery = searchable.contains(normalizedQuery)
                        || matchesTokenPrefix(searchableTokens, normalizedQuery);
            }

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

            String category = normalizeCategoryKey(wallpaper.aiCategory);
            wallpaper.aiCategory = category;

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
            String normalized = normalizeCategoryKey(wallpaper.aiCategory);
            if (normalized.trim().isEmpty()) continue;

            boolean exists = false;
            for (String category : categories) {
                if (category.equalsIgnoreCase(normalized)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                categories.add(normalized);
            }
        }

        return categories;
    }

    public static void applyFavoriteData(Map<Integer, Map<String, Object>> favoritesById) {
        if (favoritesById == null || favoritesById.isEmpty()) return;

        for (Map.Entry<Integer, Map<String, Object>> entry : favoritesById.entrySet()) {
            int id = entry.getKey();
            Map<String, Object> data = entry.getValue();
            if (data == null) continue;

            Wallpaper wallpaper = getWallpaperById(id);
            if (wallpaper == null) {
                String imageUrl = safeString(data.get("imageUrl"));
                String title = safeString(data.get("title"));
                if (title.isEmpty()) title = "Wallpaper";
                wallpaper = new Wallpaper(id, imageUrl, title);
                wallpaperList.add(wallpaper);
            }

            wallpaper.isFavorite = true;
            wallpaper.aiCategory = normalizeCategoryKey(safeString(data.get("aiCategory")));
            wallpaper.aiLabels = safeString(data.get("aiLabels"));
        }
    }

    public static void replaceAll(List<Wallpaper> newWallpapers) {
        wallpaperList.clear();
        if (newWallpapers != null) {
            wallpaperList.addAll(newWallpapers);
        }
    }

    public static String normalizeCategoryKey(String rawCategory) {
        if (rawCategory == null || rawCategory.trim().isEmpty()) return "Uncategorized";

        String c = rawCategory.trim().toLowerCase(Locale.ROOT);

        if (c.equals("çalışma alanı") || c.equals("workspace")) return "Workspace";
        if (c.equals("kafe") || c.equals("cafe")) return "Interior";
        if (c.equals("iç mekan") || c.equals("iç mekân") || c.equals("interior")) return "Interior";
        if (c.equals("sahil") || c.equals("beach")) return "Beach";
        if (c.equals("su manzaraları") || c.equals("water scenes")) return "Water Scenes";
        if (c.equals("doğa") || c.equals("nature") || c.equals("branch") || c.equals("flesh")) return "Nature";
        if (c.equals("şehir") || c.equals("urban")) return "Urban";
        if (c.equals("mimari") || c.equals("architecture")) return "Architecture";
        if (c.equals("araçlar") || c.equals("vehicles")) return "Vehicles";
        if (c.equals("hayvanlar") || c.equals("animals")) return "Animals";
        if (c.equals("doku") || c.equals("texture") || c.equals("asphalt")) return "Texture";
        if (c.equals("ışıklar") || c.equals("lights")) return "Lights";
        if (c.equals("soyut") || c.equals("abstract")) return "Abstract";
        if (c.equals("sanat") || c.equals("art")) return "Art";
        if (c.equals("insanlar") || c.equals("people")) return "People";
        if (c.equals("uzay") || c.equals("space")) return "Space";
        if (c.equals("moda") || c.equals("fashion") || c.equals("shoe") || c.equals("foot")) return "Fashion";
        if (c.equals("tek renk") || c.equals("monochrome")) return "Monochrome";
        if (c.equals("uncategorized") || c.equals("kategorisiz")) return "Uncategorized";
        if (c.equals("analyzing") || c.equals("analiz ediliyor")) return "Analyzing";

        return rawCategory.trim();
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replace("Ä±", "i")
                .replace("ÄŸ", "g")
                .replace("Ã¼", "u")
                .replace("ÅŸ", "s")
                .replace("Ã¶", "o")
                .replace("Ã§", "c")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isCategoryStyleQuery(String query) {
        return CATEGORY_SEARCH_TERMS.contains(query);
    }

    private static boolean isShortSpecificQuery(String query) {
        return query.length() <= 4 && !isCategoryStyleQuery(query);
    }

    private static List<String> tokenize(String value) {
        List<String> tokens = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return tokens;

        for (String token : value.split("\\s+")) {
            String cleaned = token.trim();
            if (!cleaned.isEmpty()) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    private static boolean matchesTokenPrefix(List<String> tokens, String query) {
        for (String token : tokens) {
            if (token.equals(query) || token.startsWith(query)) {
                return true;
            }
        }
        return false;
    }
}
