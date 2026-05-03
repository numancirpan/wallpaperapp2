package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DynamicCategoryGenerator {

    private static final Set<String> GENERIC_LABELS = new HashSet<>(Arrays.asList(
            "sky", "plant", "food", "font", "material", "line", "slope",
            "rectangle", "tree", "landscape", "object", "organism", "terrestrial plant"
    ));

    public static String generateCategory(List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Uncategorized";
        }

        List<String> meaningful = getMeaningfulLabels(labels);

        if (meaningful.isEmpty()) {
            return capitalize(cleanLabel(labels.get(0).text));
        }

        String first = meaningful.get(0);

        // çok temel insanlaştırma
        if (equalsAny(first, "person", "people", "face", "portrait", "smile", "human")) {
            return "People";
        }

        if (equalsAny(first, "dog", "cat", "bird", "frog", "animal", "wildlife")) {
            return capitalize(toSingular(first));
        }

        if (equalsAny(first, "car", "vehicle", "race", "racing")) {
            return "Motorsports";
        }

        if (equalsAny(first, "poster", "illustration", "graphics", "graphic", "design", "pattern", "abstract")) {
            return "Graphic Design";
        }

        if (equalsAny(first, "city", "street", "building", "architecture", "road", "urban")) {
            return "Urban";
        }

        if (meaningful.size() >= 2) {
            String second = meaningful.get(1);

            // çok yakın confidence varsa iki kelimeli kategori üret
            return capitalize(toSingular(first)) + " " + capitalize(toSingular(second));
        }

        return capitalize(toSingular(first));
    }

    public static String labelsToDisplay(List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) return "Not available";

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < labels.size(); i++) {
            builder.append(cleanLabel(labels.get(i).text));

            if (i < labels.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private static List<String> getMeaningfulLabels(List<AiLabelData> labels) {
        List<String> result = new ArrayList<>();

        for (AiLabelData labelData : labels) {
            String cleaned = cleanLabel(labelData.text);

            if (cleaned.isEmpty()) continue;
            if (GENERIC_LABELS.contains(cleaned.toLowerCase(Locale.ROOT))) continue;

            result.add(cleaned);

            if (result.size() == 2) {
                break;
            }
        }

        return result;
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

    private static boolean equalsAny(String value, String... options) {
        for (String option : options) {
            if (value.equalsIgnoreCase(option)) {
                return true;
            }
        }
        return false;
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