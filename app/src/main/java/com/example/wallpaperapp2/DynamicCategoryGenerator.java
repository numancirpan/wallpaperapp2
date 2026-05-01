package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DynamicCategoryGenerator {

    private static final Set<String> GENERIC_LABELS = new HashSet<>(Arrays.asList(
            "font", "material", "line", "slope", "rectangle", "object", "organism",
            "terrestrial plant", "product", "gesture", "event", "fun", "room", "flooring"
    ));

    private static final Set<String> DETAIL_LABELS = new HashSet<>(Arrays.asList(
            "hand", "nail", "eyelash", "jewellery", "jewelry", "ring", "finger", "wrist",
            "skin", "arm", "metal", "watch", "human body", "close-up", "thumb"
    ));

    public static String generateCategory(List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Uncategorized";
        }

        List<String> normalized = normalizeLabels(labels);

        if (containsAny(normalized, "laptop", "computer", "keyboard", "desk", "notebook", "pen", "writing", "paper", "office", "mobile phone", "phone")) {
            return "Workspace";
        }

        if (containsAny(normalized, "beach", "sea", "ocean", "coast", "shore", "sand", "wave", "sunset", "sunrise")) {
            if (containsAny(normalized, "sunset", "sunrise")) {
                return "Beach Sunset";
            }
            return "Beach";
        }

        if (containsAny(normalized, "waterfall", "river", "lake", "water", "stream")) {
            return "Water Scenes";
        }

        if (containsAny(normalized, "forest", "tree", "mountain", "landscape", "plant", "grass", "flower", "sky")) {
            return "Nature";
        }

        if (containsAny(normalized, "city", "street", "building", "architecture", "road", "urban")) {
            return "Urban";
        }

        if (containsAny(normalized, "car", "vehicle", "race", "racing", "motorcycle")) {
            return "Vehicles";
        }

        if (containsAny(normalized, "dog", "cat", "bird", "animal", "wildlife", "fish", "horse")) {
            return "Animals";
        }

        if (containsAny(normalized, "space", "planet", "moon", "star", "galaxy")) {
            return "Space";
        }

        if (containsAny(normalized, "poster", "illustration", "graphics", "graphic", "design", "pattern", "abstract", "art")) {
            return "Art";
        }

        if (containsAny(normalized, "person", "people", "face", "portrait", "smile", "human")) {
            return "People";
        }

        List<String> meaningful = getMeaningfulLabels(labels);
        if (meaningful.isEmpty()) {
            return "Uncategorized";
        }

        if (meaningful.size() >= 2) {
            return capitalize(toSingular(meaningful.get(0))) + " " + capitalize(toSingular(meaningful.get(1)));
        }

        return capitalize(toSingular(meaningful.get(0)));
    }

    public static String labelsToDisplay(List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) return "Not available";

        List<String> displayLabels = new ArrayList<>();
        for (AiLabelData label : labels) {
            String cleaned = cleanLabel(label.text);
            if (cleaned.isEmpty()) continue;
            if (GENERIC_LABELS.contains(cleaned.toLowerCase(Locale.ROOT))) continue;
            if (DETAIL_LABELS.contains(cleaned.toLowerCase(Locale.ROOT)) && displayLabels.size() >= 2) continue;
            if (!containsIgnoreCase(displayLabels, cleaned)) {
                displayLabels.add(cleaned);
            }
            if (displayLabels.size() == 5) break;
        }

        if (displayLabels.isEmpty()) {
            return "Visual wallpaper content";
        }

        return String.join(", ", displayLabels);
    }

    private static List<String> getMeaningfulLabels(List<AiLabelData> labels) {
        List<String> result = new ArrayList<>();

        for (AiLabelData labelData : labels) {
            String cleaned = cleanLabel(labelData.text);
            String lower = cleaned.toLowerCase(Locale.ROOT);

            if (cleaned.isEmpty()) continue;
            if (GENERIC_LABELS.contains(lower)) continue;
            if (DETAIL_LABELS.contains(lower)) continue;

            result.add(cleaned);

            if (result.size() == 2) {
                break;
            }
        }

        return result;
    }

    private static List<String> normalizeLabels(List<AiLabelData> labels) {
        List<String> normalized = new ArrayList<>();
        for (AiLabelData labelData : labels) {
            String cleaned = cleanLabel(labelData.text).toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    private static boolean containsAny(List<String> labels, String... keywords) {
        for (String label : labels) {
            for (String keyword : keywords) {
                String k = keyword.toLowerCase(Locale.ROOT);
                if (label.equals(k) || label.contains(k) || k.contains(label)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> source, String value) {
        for (String item : source) {
            if (item.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static String cleanLabel(String text) {
        if (text == null) return "";
        return text.trim().replace("_", " ");
    }

    private static String toSingular(String word) {
        if (word == null || word.isEmpty()) return "";

        String w = word.toLowerCase(Locale.ROOT);

        if (w.equals("people")) return "person";
        if (w.equals("men")) return "man";
        if (w.equals("women")) return "woman";

        if (w.endsWith("ies") && w.length() > 3) {
            return w.substring(0, w.length() - 3) + "y";
        }

        if (w.endsWith("s") && !w.endsWith("ss") && w.length() > 3) {
            return w.substring(0, w.length() - 1);
        }

        return w;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "Uncategorized";

        String[] parts = text.split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;

            builder.append(p.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(p.substring(1).toLowerCase(Locale.ROOT));

            if (i < parts.length - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }
}
