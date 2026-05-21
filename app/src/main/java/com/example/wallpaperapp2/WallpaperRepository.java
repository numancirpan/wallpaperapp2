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
            "calisma", "is",
            "animal", "animals", "hayvan", "hayvanlar",
            "nature", "doga", "beach", "sahil", "water", "ocean", "sea",
            "city", "urban", "architecture", "sehir",
            "vehicle", "vehicles",
            "people", "person", "portrait",
            "art", "abstract", "soyut", "space", "food", "phone", "dark", "minimal",
            "dog", "kopek", "cat", "kedi"
    );

    private static final Map<String, List<String>> SEARCH_SYNONYMS = new LinkedHashMap<>();

    static {
        SEARCH_SYNONYMS.put("kopek", Arrays.asList("dog", "puppy", "pet", "canine", "animal"));
        SEARCH_SYNONYMS.put("dog", Arrays.asList("kopek", "puppy", "pet", "canine", "animal"));
        SEARCH_SYNONYMS.put("kedi", Arrays.asList("cat", "kitten", "pet", "feline", "animal"));
        SEARCH_SYNONYMS.put("cat", Arrays.asList("kedi", "kitten", "pet", "feline", "animal"));
        SEARCH_SYNONYMS.put("hayvan", Arrays.asList("animal", "animals", "wildlife", "pet", "dog", "cat", "bird", "horse"));
        SEARCH_SYNONYMS.put("hayvanlar", Arrays.asList("animal", "animals", "wildlife", "pet", "dog", "cat", "bird", "horse"));
        SEARCH_SYNONYMS.put("animal", Arrays.asList("hayvan", "hayvanlar", "wildlife", "pet", "dog", "cat", "bird", "horse"));
        SEARCH_SYNONYMS.put("doga", Arrays.asList("nature", "forest", "landscape", "tree", "mountain", "flower"));
        SEARCH_SYNONYMS.put("nature", Arrays.asList("doga", "forest", "landscape", "tree", "mountain", "flower"));
        SEARCH_SYNONYMS.put("sahil", Arrays.asList("beach", "sea", "ocean", "coast", "shore", "water"));
        SEARCH_SYNONYMS.put("beach", Arrays.asList("sahil", "sea", "ocean", "coast", "shore", "water"));
        SEARCH_SYNONYMS.put("sehir", Arrays.asList("city", "urban", "street", "building", "architecture"));
        SEARCH_SYNONYMS.put("city", Arrays.asList("sehir", "urban", "street", "building", "architecture"));
        SEARCH_SYNONYMS.put("urban", Arrays.asList("sehir", "city", "street", "building", "architecture"));
        SEARCH_SYNONYMS.put("calisma", Arrays.asList("work", "workspace", "office", "desk", "computer"));
        SEARCH_SYNONYMS.put("work", Arrays.asList("calisma", "workspace", "office", "desk", "computer"));
    }

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

        List<String> expandedQueries = expandSearchQuery(normalizedQuery);
        List<Wallpaper> filteredList = new ArrayList<>();

        for (Wallpaper wallpaper : wallpaperList) {
            String rawCategory = safeString(wallpaper.aiCategory);
            String rawLabels = safeString(wallpaper.aiLabels);
            String displayCategory = CategoryDisplayMapper.toDisplayName(context, rawCategory);
            String displayLabels = AiLabelDisplayMapper.toDisplayLabels(context, rawLabels);
            String searchable = normalizeSearch(
                    safeString(wallpaper.title) + " "
                            + safeString(wallpaper.tags) + " "
                            + safeString(wallpaper.photographer) + " "
                            + rawCategory + " "
                            + rawLabels + " "
                            + displayCategory + " "
                            + displayLabels
            );
            List<String> searchableTokens = tokenize(searchable);

            boolean matchesQuery = false;
            for (String expandedQuery : expandedQueries) {
                if (matchesSingleQuery(wallpaper, expandedQuery, searchable, searchableTokens, rawCategory, rawLabels, displayCategory, displayLabels)) {
                    matchesQuery = true;
                    break;
                }
            }

            if (matchesQuery) {
                filteredList.add(wallpaper);
            }
        }

        return filteredList;
    }

    private static boolean matchesSingleQuery(Wallpaper wallpaper, String query, String searchable, List<String> searchableTokens,
                                              String rawCategory, String rawLabels, String displayCategory, String displayLabels) {
        boolean matchesQuery;
        if (isCategoryStyleQuery(query)) {
            String categoryOnlySearchable = normalizeSearch(rawCategory + " " + displayCategory + " " + safeString(wallpaper.tags));
            matchesQuery = categoryOnlySearchable.contains(query)
                    || searchable.contains(query)
                    || CategoryResolver.matchesQuery(query, rawCategory, "");
            if (matchesQuery && isAnimalQuery(query)) {
                matchesQuery = hasAnimalEvidence(wallpaper, rawLabels, displayLabels);
            }
        } else if (isShortSpecificQuery(query)) {
            matchesQuery = matchesTokenPrefix(searchableTokens, query);
        } else {
            matchesQuery = searchable.contains(query)
                    || matchesTokenPrefix(searchableTokens, query);
        }
        return matchesQuery;
    }

    private static List<String> expandSearchQuery(String query) {
        List<String> expanded = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return expanded;

        addUnique(expanded, query);
        List<String> synonyms = SEARCH_SYNONYMS.get(query);
        if (synonyms != null) {
            for (String synonym : synonyms) {
                addUnique(expanded, normalizeSearch(synonym));
            }
        }
        return expanded;
    }

    private static void addUnique(List<String> values, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (!values.contains(value)) values.add(value);
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

            String category = sanitizeCategoryForLabels(wallpaper.aiCategory, wallpaper.aiLabels);
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
        clearFavoriteState();
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
            wallpaper.aiCategory = sanitizeCategoryForLabels(
                    normalizeCategoryKey(safeString(data.get("aiCategory"))),
                    safeString(data.get("aiLabels"))
            );
            wallpaper.aiLabels = safeString(data.get("aiLabels"));
            wallpaper.tags = safeString(data.get("tags"));
            wallpaper.photographer = safeString(data.get("photographer"));
            wallpaper.views = safeInt(data.get("views"));
            wallpaper.downloads = safeInt(data.get("downloads"));
            wallpaper.likes = safeInt(data.get("likes"));
            wallpaper.sourceUrl = safeString(data.get("sourceUrl"));
        }
    }

    public static void clearUserState() {
        clearFavoriteState();
    }

    private static void clearFavoriteState() {
        for (Wallpaper wallpaper : wallpaperList) {
            wallpaper.isFavorite = false;
        }
    }

    public static void replaceAll(List<Wallpaper> newWallpapers) {
        Map<Integer, Wallpaper> existingById = new LinkedHashMap<>();
        Map<String, Wallpaper> existingByUrl = new LinkedHashMap<>();
        for (Wallpaper existing : wallpaperList) {
            existingById.put(existing.id, existing);
            if (existing.imageUrl != null && !existing.imageUrl.trim().isEmpty()) {
                existingByUrl.put(existing.imageUrl.trim(), existing);
            }
        }

        wallpaperList.clear();
        if (newWallpapers != null) {
            for (Wallpaper wallpaper : newWallpapers) {
                Wallpaper previous = existingById.get(wallpaper.id);
                if (previous == null && wallpaper.imageUrl != null) {
                    previous = existingByUrl.get(wallpaper.imageUrl.trim());
                }
                if (previous != null) {
                    wallpaper.isFavorite = previous.isFavorite;
                    wallpaper.aiCategory = previous.aiCategory;
                    wallpaper.aiLabels = previous.aiLabels;
                    if (wallpaper.tags == null || wallpaper.tags.trim().isEmpty()) wallpaper.tags = previous.tags;
                    if (wallpaper.photographer == null || wallpaper.photographer.trim().isEmpty()) wallpaper.photographer = previous.photographer;
                    if (wallpaper.views == 0) wallpaper.views = previous.views;
                    if (wallpaper.downloads == 0) wallpaper.downloads = previous.downloads;
                    if (wallpaper.likes == 0) wallpaper.likes = previous.likes;
                    if (wallpaper.sourceUrl == null || wallpaper.sourceUrl.trim().isEmpty()) wallpaper.sourceUrl = previous.sourceUrl;
                }
                wallpaper.aiCategory = sanitizeCategoryForLabels(wallpaper.aiCategory, wallpaper.aiLabels);
                wallpaperList.add(wallpaper);
            }
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

    public static String sanitizeCategoryForLabels(String category, String labels) {
        String normalized = normalizeCategoryKey(category);
        if (!"Animals".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        if (hasAnimalEvidence(null, labels, labels)) {
            return normalized;
        }

        String labelText = normalizeSearch(labels);
        if (labelText.contains("roof") || labelText.contains("building")
                || labelText.contains("architecture") || labelText.contains("house")) {
            return "Architecture";
        }
        if (labelText.contains("boat") || labelText.contains("vacation")
                || labelText.contains("sea") || labelText.contains("coast")
                || labelText.contains("beach") || labelText.contains("water")) {
            return "Beach";
        }
        return "Uncategorized";
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int safeInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
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

    private static boolean isAnimalQuery(String query) {
        return "animal".equals(query) || "animals".equals(query)
                || "hayvan".equals(query) || "hayvanlar".equals(query);
    }

    private static boolean hasAnimalEvidence(Wallpaper wallpaper, String rawLabels, String displayLabels) {
        String evidence = normalizeSearch(
                (wallpaper == null ? "" : safeString(wallpaper.title) + " " + safeString(wallpaper.tags)) + " " + rawLabels + " " + displayLabels
        );
        List<String> tokens = tokenize(evidence);
        String[] animalTerms = new String[]{
                "animal", "animals", "cat", "dog", "bird", "horse", "lion", "tiger",
                "bear", "fish", "deer", "fox", "wolf", "elephant", "zebra", "giraffe",
                "pet", "wildlife", "mammal", "reptile", "insect", "puppy", "kitten", "calf", "cattle"
        };

        for (String term : animalTerms) {
            if (tokens.contains(term)) {
                return true;
            }
        }
        return false;
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
