package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.List;

public class AiClassifier {

    public static void analyzeWallpaper(Wallpaper wallpaper) {
        if (wallpaper == null) return;

        List<String> labels = generateLabels(wallpaper);
        String suggestedCategory = generateCategoryFromLabels(labels);
        String finalCategory = findBestExistingCategoryOrCreateNew(suggestedCategory);

        wallpaper.aiLabels = joinLabels(labels);
        wallpaper.aiCategory = finalCategory;
    }

    private static List<String> generateLabels(Wallpaper wallpaper) {
        List<String> labels = new ArrayList<>();

        switch (wallpaper.id) {
            case 1:
                labels.add("forest");
                labels.add("green");
                labels.add("landscape");
                break;
            case 2:
                labels.add("pattern");
                labels.add("abstract");
                labels.add("design");
                break;
            case 3:
                labels.add("car");
                labels.add("street");
                labels.add("urban");
                break;
            case 4:
                labels.add("water");
                labels.add("lake");
                labels.add("sky");
                break;
            default:
                labels.add("unknown");
                break;
        }

        return labels;
    }

    private static String generateCategoryFromLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Uncategorized";
        }

        if (containsAny(labels, "forest", "green", "landscape", "water", "lake", "sky", "mountain", "tree")) {
            return "Nature";
        }

        if (containsAny(labels, "car", "street", "urban", "building", "city", "road")) {
            return "City";
        }

        if (containsAny(labels, "pattern", "abstract", "design", "illustration", "graphic")) {
            return "Art";
        }

        return capitalize(labels.get(0));
    }

    private static String findBestExistingCategoryOrCreateNew(String suggestedCategory) {
        List<String> existingCategories = WallpaperRepository.getExistingAiCategories();

        for (String existing : existingCategories) {
            if (existing.equalsIgnoreCase(suggestedCategory)) {
                return existing;
            }
        }

        return suggestedCategory;
    }

    private static boolean containsAny(List<String> labels, String... keywords) {
        for (String keyword : keywords) {
            for (String label : labels) {
                if (label.equalsIgnoreCase(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String joinLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            builder.append(labels.get(i));
            if (i < labels.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "Uncategorized";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}