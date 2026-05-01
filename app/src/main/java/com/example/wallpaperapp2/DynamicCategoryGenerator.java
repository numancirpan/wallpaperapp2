package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DynamicCategoryGenerator {

    private static final Set<String> GENERIC_LABELS = new HashSet<>(Arrays.asList(
            "font", "material", "line", "slope", "rectangle", "object", "organism",
            "terrestrial plant", "product", "gesture", "event", "fun", "room", "flooring",
            "sky", "plant", "food", "grass", "soil"
    ));

    private static final Set<String> DETAIL_LABELS = new HashSet<>(Arrays.asList(
            "hand", "nail", "eyelash", "jewellery", "jewelry", "ring", "finger", "wrist",
            "skin", "arm", "metal", "watch", "human body", "close-up", "thumb",
            "musical instrument", "string instrument", "guitar accessory"
    ));

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("Workspace", Arrays.asList(
                "laptop", "computer", "keyboard", "desk", "table", "notebook", "pen", "writing",
                "paper", "office", "workspace", "work", "study", "mobile phone", "phone", "screen", "cup"
        ));
        CATEGORY_KEYWORDS.put("Beach", Arrays.asList(
                "beach", "sea", "ocean", "coast", "shore", "sand", "wave", "water", "rock", "sunset", "sunrise"
        ));
        CATEGORY_KEYWORDS.put("Water Scenes", Arrays.asList(
                "waterfall", "river", "lake", "water", "stream", "sea", "ocean", "coast"
        ));
        CATEGORY_KEYWORDS.put("Nature", Arrays.asList(
                "forest", "tree", "mountain", "landscape", "plant", "grass", "flower", "prairie", "field", "meadow", "sky"
        ));
        CATEGORY_KEYWORDS.put("Urban", Arrays.asList(
                "city", "street", "building", "architecture", "road", "urban", "traffic"
        ));
        CATEGORY_KEYWORDS.put("Vehicles", Arrays.asList(
                "car", "vehicle", "race", "racing", "motorcycle", "automotive", "wheel"
        ));
        CATEGORY_KEYWORDS.put("Animals", Arrays.asList(
                "dog", "cat", "bird", "animal", "wildlife", "fish", "horse", "pet"
        ));
        CATEGORY_KEYWORDS.put("Space", Arrays.asList(
                "space", "planet", "moon", "star", "galaxy", "astronomy"
        ));
        CATEGORY_KEYWORDS.put("Art", Arrays.asList(
                "poster", "illustration", "graphics", "graphic", "design", "pattern", "abstract", "art", "drawing"
        ));
        CATEGORY_KEYWORDS.put("People", Arrays.asList(
                "person", "people", "face", "portrait", "smile", "human"
        ));
    }

    public static String generateCategory(List<AiLabelData> labels) {
        return generateCategory(labels, null);
    }

    public static String generateCategory(List<AiLabelData> labels, List<String> existingCategories) {
        if (labels == null || labels.isEmpty()) return "Uncategorized";

        List<String> normalized = normalizeLabels(labels);
        String baseCategory = chooseBaseCategory(normalized);
        if (baseCategory == null || baseCategory.trim().isEmpty()) {
            baseCategory = fallbackCategory(labels);
        }
        if (baseCategory == null || baseCategory.trim().isEmpty()) {
            baseCategory = "Uncategorized";
        }
        return matchExistingCategory(baseCategory, normalized, existingCategories);
    }

    public static String labelsToDisplay(List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) return "Not available";

        List<String> displayLabels = new ArrayList<>();
        for (AiLabelData label : labels) {
            String cleaned = cleanLabel(label.text);
            String lower = cleaned.toLowerCase(Locale.ROOT);
            if (cleaned.isEmpty()) continue;
            if (GENERIC_LABELS.contains(lower)) continue;
            if (DETAIL_LABELS.contains(lower)) continue;
            if (!containsIgnoreCase(displayLabels, cleaned)) displayLabels.add(capitalize(cleaned));
            if (displayLabels.size() == 5) break;
        }

        if (displayLabels.isEmpty()) {
            String category = generateCategory(labels);
            return category.equals("Uncategorized") ? "Visual wallpaper content" : category;
        }
        return String.join(", ", displayLabels);
    }

    private static String chooseBaseCategory(List<String> labels) {
        Map<String, Integer> scores = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String label : labels) {
                if (DETAIL_LABELS.contains(label) || GENERIC_LABELS.contains(label)) continue;
                for (String keyword : entry.getValue()) {
                    if (matches(label, keyword)) score += keywordWeight(keyword);
                }
            }
            scores.put(entry.getKey(), score);
        }

        if (containsAny(labels, "laptop", "computer", "keyboard", "desk", "notebook", "pen", "writing", "paper")) {
            scores.put("Workspace", scores.get("Workspace") + 8);
        }
        if (containsAny(labels, "mobile phone", "phone") && containsAny(labels, "laptop", "computer", "desk", "notebook")) {
            scores.put("Workspace", scores.get("Workspace") + 8);
        }
        if (containsAny(labels, "beach", "sand", "coast", "shore")) {
            scores.put("Beach", scores.get("Beach") + 7);
        }
        if (containsAny(labels, "field", "prairie", "meadow")) {
            scores.put("Nature", scores.get("Nature") + 7);
        }

        String bestCategory = null;
        int bestScore = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        return bestScore >= 4 ? bestCategory : null;
    }

    private static String matchExistingCategory(String baseCategory, List<String> labels, List<String> existingCategories) {
        if (existingCategories == null || existingCategories.isEmpty()) return baseCategory;

        String baseFamily = familyOf(baseCategory);
        String bestMatch = null;
        int bestScore = 0;
        for (String existing : existingCategories) {
            if (existing == null || existing.trim().isEmpty()) continue;
            String existingFamily = familyOf(existing);
            int score = 0;
            if (existing.equalsIgnoreCase(baseCategory)) score += 20;
            if (existingFamily.equals(baseFamily)) score += 12;
            for (String token : tokenize(existing)) {
                for (String label : labels) {
                    if (matches(label, token)) score += 3;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = existing;
            }
        }
        return bestScore >= 10 ? bestMatch : baseCategory;
    }

    private static String fallbackCategory(List<AiLabelData> labels) {
        List<String> meaningful = getMeaningfulLabels(labels);
        if (meaningful.isEmpty()) return "Uncategorized";
        return normalizeFallbackName(meaningful.get(0));
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
            if (result.size() == 2) break;
        }
        return result;
    }

    private static List<String> normalizeLabels(List<AiLabelData> labels) {
        List<String> normalized = new ArrayList<>();
        for (AiLabelData labelData : labels) {
            String cleaned = cleanLabel(labelData.text).toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) normalized.add(cleaned);
        }
        return normalized;
    }

    private static boolean containsAny(List<String> labels, String... keywords) {
        for (String label : labels) {
            for (String keyword : keywords) {
                if (matches(label, keyword)) return true;
            }
        }
        return false;
    }

    private static boolean matches(String label, String keyword) {
        String l = label.toLowerCase(Locale.ROOT).trim();
        String k = keyword.toLowerCase(Locale.ROOT).trim();
        return l.equals(k) || l.contains(k) || k.contains(l);
    }

    private static int keywordWeight(String keyword) {
        if (keyword.equals("laptop") || keyword.equals("computer") || keyword.equals("beach") || keyword.equals("forest") || keyword.equals("car")) return 5;
        if (keyword.equals("desk") || keyword.equals("keyboard") || keyword.equals("coast") || keyword.equals("field") || keyword.equals("prairie")) return 4;
        return 2;
    }

    private static String familyOf(String category) {
        String c = category.toLowerCase(Locale.ROOT);
        if (c.contains("work") || c.contains("office") || c.contains("tech") || c.contains("computer") || c.contains("laptop")) return "workspace";
        if (c.contains("beach") || c.contains("coast") || c.contains("sea") || c.contains("ocean") || c.contains("water")) return "beach";
        if (c.contains("nature") || c.contains("field") || c.contains("prairie") || c.contains("forest") || c.contains("mountain")) return "nature";
        if (c.contains("urban") || c.contains("city") || c.contains("street") || c.contains("architecture")) return "urban";
        if (c.contains("vehicle") || c.contains("car") || c.contains("motor")) return "vehicles";
        if (c.contains("animal")) return "animals";
        if (c.contains("space")) return "space";
        if (c.contains("art") || c.contains("abstract") || c.contains("design")) return "art";
        if (c.contains("people") || c.contains("person") || c.contains("portrait")) return "people";
        return c;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) return tokens;
        for (String part : text.toLowerCase(Locale.ROOT).split("\\s+")) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) tokens.add(cleaned);
        }
        return tokens;
    }

    private static String normalizeFallbackName(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("mobile phone") || lower.equals("phone")) return "Workspace";
        if (lower.contains("rock") || lower.contains("sand")) return "Beach";
        return capitalize(toSingular(label));
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
        if (w.endsWith("ies") && w.length() > 3) return w.substring(0, w.length() - 3) + "y";
        if (w.endsWith("s") && !w.endsWith("ss") && w.length() > 3) return w.substring(0, w.length() - 1);
        return w;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "Uncategorized";
        String[] parts = text.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            builder.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1).toLowerCase(Locale.ROOT));
            if (i < parts.length - 1) builder.append(" ");
        }
        return builder.toString();
    }
}
