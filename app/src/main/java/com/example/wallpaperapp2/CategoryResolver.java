package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CategoryResolver {

    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "Beach",
            "Nature",
            "Working",
            "Phone",
            "City",
            "Architecture",
            "Animals",
            "Vehicles",
            "Food",
            "Space",
            "Abstract",
            "Minimal",
            "Dark",
            "People",
            "Art"
    );

    public static String resolveCategory(List<String> labels, List<String> existingCategories) {
        String candidate = generateDynamicCategory(labels);
        return resolveToAllowedCategory(candidate, existingCategories);
    }

    public static boolean isAllowedCategory(String category) {
        if (category == null) return false;
        for (String allowed : ALLOWED_CATEGORIES) {
            if (allowed.equalsIgnoreCase(category.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesQuery(String query, String category, String labels) {
        String normalizedQuery = normalizeCategoryText(query).toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String inferredQueryCategory = inferAllowedCategory(normalizedQuery);
        String resolvedItemCategory = resolveToAllowedCategory(category, null);

        if (inferredQueryCategory != null) {
            if (inferredQueryCategory.equalsIgnoreCase(resolvedItemCategory)) {
                return true;
            }
            return sharesFamily(inferredQueryCategory, resolvedItemCategory);
        }

        String searchable = (safe(category) + " " + safe(labels)).toLowerCase(Locale.ROOT);
        if (searchable.contains(normalizedQuery)) {
            return true;
        }

        return sharesFamily(normalizedQuery, resolvedItemCategory);
    }

    public static String resolveToAllowedCategory(String candidate, List<String> existingCategories) {
        String cleanedCandidate = normalizeCategoryText(candidate);
        if (cleanedCandidate.isEmpty()) {
            return "Art";
        }

        List<String> sanitizedExisting = sanitizeExistingCategories(existingCategories);
        String exactAllowed = findExactAllowed(cleanedCandidate);
        if (exactAllowed != null) {
            String existingMatch = findBestExistingMatch(exactAllowed, sanitizedExisting);
            return existingMatch != null ? existingMatch : exactAllowed;
        }

        String inferredCategory = inferAllowedCategory(cleanedCandidate);
        if (inferredCategory != null) {
            String existingMatch = findBestExistingMatch(inferredCategory, sanitizedExisting);
            return existingMatch != null ? existingMatch : inferredCategory;
        }

        String closestAllowed = findClosestAllowed(cleanedCandidate);
        if (closestAllowed != null) {
            String existingMatch = findBestExistingMatch(closestAllowed, sanitizedExisting);
            return existingMatch != null ? existingMatch : closestAllowed;
        }

        return "Art";
    }

    public static List<String> sanitizeExistingCategories(List<String> existingCategories) {
        List<String> sanitized = new ArrayList<>();
        if (existingCategories == null) {
            return sanitized;
        }

        for (String existing : existingCategories) {
            if (isTemporaryCategory(existing)) continue;
            String resolved = resolveToAllowedCategory(existing, null);
            if (!containsIgnoreCase(sanitized, resolved)) {
                sanitized.add(resolved);
            }
        }

        return sanitized;
    }

    public static boolean isTemporaryCategory(String category) {
        if (category == null) return true;
        String lower = category.trim().toLowerCase(Locale.ROOT);
        return lower.isEmpty()
                || lower.equals("analyzing")
                || lower.equals("uncategorized")
                || lower.equals("not analyzed yet");
    }

    private static String generateDynamicCategory(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Art";
        }

        List<String> normalized = normalize(labels);

        if (containsAny(normalized, "person", "face", "smile", "fashion", "portrait")) {
            return "People";
        }

        if (containsAny(normalized, "poster", "graphics", "graphic", "pattern", "abstract", "design", "art")) {
            return "Art";
        }

        if (containsAny(normalized, "car", "vehicle", "race", "road", "transport")) {
            return "Vehicles";
        }

        if (containsAny(normalized, "building", "city", "street", "architecture", "infrastructure")) {
            if (containsAny(normalized, "interior", "bridge", "tower", "temple", "house")) {
                return "Architecture";
            }
            return "City";
        }

        if (containsAny(normalized, "tree", "forest", "plant", "grass", "flower", "mountain", "landscape")) {
            return "Nature";
        }

        if (containsAny(normalized, "water", "lake", "river", "ocean", "sea")) {
            return "Beach";
        }

        if (containsAny(normalized, "animal", "bird", "cat", "dog", "wildlife")) {
            return "Animals";
        }

        if (containsAny(normalized, "space", "planet", "moon", "star", "galaxy")) {
            return "Space";
        }

        if (containsAny(normalized, "food", "fruit", "vegetable", "meal", "drink")) {
            return "Food";
        }

        if (containsAny(normalized, "phone", "mobile", "smartphone", "cellphone", "handset")) {
            return "Phone";
        }

        if (containsAny(normalized, "computer", "technology", "electronics", "screen", "device")) {
            if (containsAny(normalized, "desk", "keyboard", "laptop", "notebook", "pen", "office", "computer")) {
                return "Working";
            }
            return "Phone";
        }

        if (containsAny(normalized, "dark", "black", "night", "shadow")) {
            return "Dark";
        }

        if (containsAny(normalized, "minimal", "simple", "clean")) {
            return "Minimal";
        }

        return inferAllowedCategory(String.join(" ", normalized));
    }

    private static String findBestExistingMatch(String candidate, List<String> existingCategories) {
        if (existingCategories == null || existingCategories.isEmpty()) {
            return null;
        }

        String candidateFamily = inferFamily(candidate);

        for (String existing : existingCategories) {
            if (existing.equalsIgnoreCase(candidate)) {
                return existing;
            }
        }

        for (String existing : existingCategories) {
            String existingFamily = inferFamily(existing);
            if (candidateFamily.equals(existingFamily)) {
                return existing;
            }
        }

        for (String existing : existingCategories) {
            if (existing.toLowerCase().contains(candidate.toLowerCase()) ||
                    candidate.toLowerCase().contains(existing.toLowerCase())) {
                return existing;
            }
        }

        return null;
    }

    private static String inferFamily(String category) {
        String c = category.toLowerCase();

        if (c.contains("people") || c.contains("portrait") || c.contains("person")) return "people";
        if (c.contains("beach") || c.contains("sea") || c.contains("ocean") || c.contains("coast")) return "beach";
        if (c.contains("graphic") || c.contains("design") || c.contains("art") || c.contains("abstract")) return "art";
        if (c.contains("motor") || c.contains("car") || c.contains("race") || c.contains("vehicle")) return "vehicles";
        if (c.contains("city") || c.contains("urban")) return "city";
        if (c.contains("architecture")) return "architecture";
        if (c.contains("nature") || c.contains("water") || c.contains("landscape")) return "nature";
        if (c.contains("animal")) return "animals";
        if (c.contains("space")) return "space";
        if (c.contains("food")) return "food";
        if (c.contains("phone") || c.contains("mobile")) return "phone";
        if (c.contains("technology") || c.contains("workspace") || c.contains("working")) return "working";
        if (c.contains("dark") || c.contains("night")) return "dark";
        if (c.contains("minimal")) return "minimal";

        return c;
    }

    private static boolean containsAny(List<String> labels, String... keywords) {
        for (String label : labels) {
            for (String keyword : keywords) {
                if (label.equalsIgnoreCase(keyword) || label.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> normalize(List<String> labels) {
        List<String> normalized = new ArrayList<>();
        for (String label : labels) {
            normalized.add(label.toLowerCase().trim());
        }
        return normalized;
    }

    private static String inferAllowedCategory(String text) {
        String normalized = normalizeCategoryText(text).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;

        if (normalized.contains("workspace")) {
            return "Working";
        }
        if (normalized.contains("urban")) {
            return "City";
        }
        if (normalized.contains("water scenes")) {
            return "Beach";
        }
        if (normalized.contains("nature") || normalized.contains("forest") || normalized.contains("mountain")
                || normalized.contains("flower") || normalized.contains("landscape") || normalized.contains("tree")) {
            return "Nature";
        }
        if (normalized.contains("water") || normalized.contains("ocean") || normalized.contains("river")
                || normalized.contains("sea") || normalized.contains("lake") || normalized.contains("beach")) {
            return "Beach";
        }
        if (normalized.contains("phone") || normalized.contains("mobile") || normalized.contains("smartphone")
                || normalized.contains("cellphone") || normalized.contains("handset")) {
            return "Phone";
        }
        if (normalized.contains("workspace") || normalized.contains("desk") || normalized.contains("keyboard")
                || normalized.contains("office") || normalized.contains("laptop") || normalized.contains("computer")
                || normalized.contains("notebook")) {
            return "Working";
        }
        if (normalized.contains("technology") || normalized.contains("screen") || normalized.contains("device")
                || normalized.contains("computer") || normalized.contains("digital")) {
            return "Working";
        }
        if (normalized.contains("city") || normalized.contains("street") || normalized.contains("urban")) {
            return "City";
        }
        if (normalized.contains("architecture") || normalized.contains("building") || normalized.contains("bridge")
                || normalized.contains("tower") || normalized.contains("interior")) {
            return "Architecture";
        }
        if (normalized.contains("animal") || normalized.contains("dog") || normalized.contains("cat")
                || normalized.contains("bird") || normalized.contains("wildlife")) {
            return "Animals";
        }
        if (normalized.contains("vehicle") || normalized.contains("car") || normalized.contains("motorcycle")
                || normalized.contains("race")) {
            return "Vehicles";
        }
        if (normalized.contains("food") || normalized.contains("fruit") || normalized.contains("meal")
                || normalized.contains("drink")) {
            return "Food";
        }
        if (normalized.contains("space") || normalized.contains("planet") || normalized.contains("moon")
                || normalized.contains("galaxy") || normalized.contains("star")) {
            return "Space";
        }
        if (normalized.contains("abstract") || normalized.contains("pattern")) {
            return "Abstract";
        }
        if (normalized.contains("minimal") || normalized.contains("simple") || normalized.contains("clean")) {
            return "Minimal";
        }
        if (normalized.contains("dark") || normalized.contains("night") || normalized.contains("shadow")
                || normalized.contains("black")) {
            return "Dark";
        }
        if (normalized.contains("people") || normalized.contains("person") || normalized.contains("portrait")
                || normalized.contains("face") || normalized.contains("human")) {
            return "People";
        }
        if (normalized.contains("art") || normalized.contains("illustration") || normalized.contains("graphic")
                || normalized.contains("design") || normalized.contains("poster")) {
            return "Art";
        }

        return null;
    }

    private static String findExactAllowed(String candidate) {
        for (String allowed : ALLOWED_CATEGORIES) {
            if (allowed.equalsIgnoreCase(candidate)) {
                return allowed;
            }
        }
        return null;
    }

    private static String findClosestAllowed(String candidate) {
        String best = null;
        int bestScore = 0;
        for (String allowed : ALLOWED_CATEGORIES) {
            int score = similarityScore(candidate, allowed);
            if (score > bestScore) {
                best = allowed;
                bestScore = score;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private static int similarityScore(String a, String b) {
        String[] left = normalizeCategoryText(a).toLowerCase(Locale.ROOT).split("\\s+");
        String[] right = normalizeCategoryText(b).toLowerCase(Locale.ROOT).split("\\s+");
        int score = 0;
        for (String l : left) {
            for (String r : right) {
                if (l.equals(r) || l.contains(r) || r.contains(l)) {
                    score++;
                }
            }
        }
        return score;
    }

    private static String normalizeCategoryText(String text) {
        if (text == null) return "";
        return text.replaceAll("[^a-zA-Z0-9 &-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean sharesFamily(String query, String itemCategory) {
        String queryFamily = inferFamily(resolveToAllowedCategory(query, null));
        String itemFamily = inferFamily(itemCategory == null ? "" : itemCategory);
        return !queryFamily.isEmpty() && queryFamily.equals(itemFamily);
    }

    private static boolean containsIgnoreCase(List<String> source, String value) {
        for (String item : source) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
