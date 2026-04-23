package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.List;

public class CategoryResolver {

    public static String resolveCategory(List<String> labels, List<String> existingCategories) {
        String candidate = generateDynamicCategory(labels);

        if (existingCategories == null || existingCategories.isEmpty()) {
            return candidate;
        }

        String matchedExisting = findBestExistingMatch(candidate, existingCategories);
        if (matchedExisting != null) {
            return matchedExisting;
        }

        return candidate;
    }

    private static String generateDynamicCategory(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Uncategorized";
        }

        List<String> normalized = normalize(labels);

        if (containsAny(normalized, "person", "face", "smile", "fashion", "portrait")) {
            return "People";
        }

        if (containsAny(normalized, "anime", "cartoon", "manga", "illustration")) {
            return "Anime";
        }

        if (containsAny(normalized, "poster", "graphics", "graphic", "pattern", "abstract", "design", "art")) {
            return "Graphic Design";
        }

        if (containsAny(normalized, "car", "vehicle", "race", "road", "transport")) {
            return "Motorsports";
        }

        if (containsAny(normalized, "building", "city", "street", "architecture", "infrastructure")) {
            if (containsAny(normalized, "night", "neon", "light")) {
                return "Urban Night";
            }
            return "Urban";
        }

        if (containsAny(normalized, "tree", "forest", "plant", "grass", "flower", "mountain", "landscape")) {
            if (containsAny(normalized, "water", "lake", "river", "ocean")) {
                return "Nature & Water";
            }
            return "Nature";
        }

        if (containsAny(normalized, "water", "lake", "river", "ocean", "sea")) {
            return "Water Scenes";
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

        if (containsAny(normalized, "computer", "technology", "electronics", "screen", "device")) {
            return "Technology";
        }

        return buildFallbackCategory(normalized);
    }

    private static String findBestExistingMatch(String candidate, List<String> existingCategories) {
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
        if (c.contains("anime")) return "anime";
        if (c.contains("graphic") || c.contains("design") || c.contains("art") || c.contains("abstract")) return "art";
        if (c.contains("motor") || c.contains("car") || c.contains("race") || c.contains("vehicle")) return "motorsports";
        if (c.contains("urban") || c.contains("city") || c.contains("architecture")) return "urban";
        if (c.contains("nature") || c.contains("water") || c.contains("landscape")) return "nature";
        if (c.contains("animal")) return "animals";
        if (c.contains("space")) return "space";
        if (c.contains("food")) return "food";
        if (c.contains("technology")) return "technology";

        return c;
    }

    private static String buildFallbackCategory(List<String> labels) {
        List<String> filtered = new ArrayList<>();

        for (String label : labels) {
            String lower = label.toLowerCase();

            if (lower.equals("sky") || lower.equals("cloud") || lower.equals("plant") || lower.equals("object")) {
                continue;
            }

            filtered.add(capitalize(lower));

            if (filtered.size() == 2) {
                break;
            }
        }

        if (filtered.isEmpty()) {
            return "Uncategorized";
        }

        if (filtered.size() == 1) {
            return filtered.get(0);
        }

        return filtered.get(0) + " " + filtered.get(1);
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

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}