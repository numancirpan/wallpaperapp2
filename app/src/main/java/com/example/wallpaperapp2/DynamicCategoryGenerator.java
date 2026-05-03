package com.example.wallpaperapp2;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DynamicCategoryGenerator {

    public static String generateCategory(Context context, List<AiLabelData> labels) {
        return generateCategory(context, labels, null);
    }

    public static String generateCategory(Context context, List<AiLabelData> labels, List<String> existingCategories) {
        if (labels == null || labels.isEmpty()) return context.getString(R.string.uncategorized);

        List<String> normalized = normalizeLabels(labels);
        String baseCategory = chooseBaseCategory(context, normalized);
        if (baseCategory == null || baseCategory.trim().isEmpty()) {
            baseCategory = fallbackCategory(context, labels);
        }
        if (baseCategory == null || baseCategory.trim().isEmpty()) {
            baseCategory = context.getString(R.string.uncategorized);
        }
        return matchExistingCategory(baseCategory, normalized, existingCategories);
    }

    public static String labelsToDisplay(Context context, List<AiLabelData> labels) {
        if (labels == null || labels.isEmpty()) return context.getString(R.string.not_available);

        Set<String> genericLabels = readNormalizedSet(context, R.array.ai_generic_labels);
        Set<String> detailLabels = readNormalizedSet(context, R.array.ai_detail_labels);

        List<String> displayLabels = new ArrayList<>();
        for (AiLabelData label : labels) {
            String cleaned = cleanLabel(label.text);
            String lower = cleaned.toLowerCase(Locale.ROOT);
            if (cleaned.isEmpty()) continue;
            if (genericLabels.contains(lower)) continue;
            if (detailLabels.contains(lower)) continue;
            if (isContradictoryAnimalLabel(displayLabels, lower)) continue;
            if (!containsIgnoreCase(displayLabels, cleaned)) displayLabels.add(capitalize(cleaned));
            if (displayLabels.size() == 5) break;
        }

        if (displayLabels.isEmpty()) {
            String category = generateCategory(context, labels);
            return category.equalsIgnoreCase(context.getString(R.string.uncategorized))
                    ? context.getString(R.string.visual_wallpaper_content)
                    : category;
        }
        return String.join(", ", displayLabels);
    }

    private static String chooseBaseCategory(Context context, List<String> labels) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        Set<String> genericLabels = readNormalizedSet(context, R.array.ai_generic_labels);
        Set<String> detailLabels = readNormalizedSet(context, R.array.ai_detail_labels);
        Set<String> highWeightKeywords = readNormalizedSet(context, R.array.ai_high_weight_keywords);
        Set<String> mediumWeightKeywords = readNormalizedSet(context, R.array.ai_medium_weight_keywords);

        for (String category : readRawList(context, R.array.ai_category_keys)) {
            int score = 0;
            for (String label : labels) {
                if (detailLabels.contains(label) || genericLabels.contains(label)) continue;
                for (String keyword : readNormalizedList(context, keywordArrayIdForCategory(context, category))) {
                    if (matches(label, keyword)) {
                        score += keywordWeight(keyword, highWeightKeywords, mediumWeightKeywords);
                    }
                }
            }
            scores.put(category, score);
        }

        applyBoost(scores, "Workspace", 10, containsAny(labels, "laptop", "computer", "keyboard", "desk", "notebook", "pen", "writing", "paper", "book"));
        applyBoost(scores, "Workspace", 10, containsAny(labels, "mobile phone", "phone") && containsAny(labels, "laptop", "computer", "desk", "notebook"));
        applyBoost(scores, "Interior", 10, containsAny(labels, "coffee", "cup", "mug", "tableware") && containsAny(labels, "table", "chair", "restaurant", "cafe", "saucer"));
        applyBoost(scores, "Fashion", 12, containsAny(labels, "shoe", "shoes", "footwear", "sneakers", "heel", "foot") && containsAny(labels, "curtain", "fabric", "flesh", "wall"));
        applyBoost(scores, "Interior", 8, containsAny(labels, "chair") && containsAny(labels, "table", "tableware", "building", "window"));
        applyBoost(scores, "Beach", 8, containsAny(labels, "beach", "sand", "coast", "shore"));
        applyBoost(scores, "Nature", 9, containsAny(labels, "field", "prairie", "meadow", "moss", "leaf", "branch", "twig", "flower", "insect"));
        applyBoost(scores, "Texture", 8, containsAny(labels, "droplet", "water drop", "macro", "surface", "asphalt", "wall"));
        applyBoost(scores, "Monochrome", 10, containsAny(labels, "monochrome") || containsAny(labels, "fork", "cutlery") && containsAny(labels, "dark", "shadow", "wing"));
        applyBoost(scores, "Lights", 8, containsAny(labels, "bokeh", "blur", "neon", "glow") || containsAny(labels, "light", "lights") && containsAny(labels, "color", "night", "blur"));
        applyBoost(scores, "Animals", 8, containsAny(labels, "cat", "dog", "fur", "snout", "nose"));

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

    private static void applyBoost(Map<String, Integer> scores, String category, int boost, boolean condition) {
        if (!condition || !scores.containsKey(category)) return;
        scores.put(category, scores.get(category) + boost);
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

    private static String fallbackCategory(Context context, List<AiLabelData> labels) {
        List<String> meaningful = getMeaningfulLabels(context, labels);
        if (meaningful.isEmpty()) return context.getString(R.string.uncategorized);
        return normalizeFallbackName(meaningful.get(0));
    }

    private static List<String> getMeaningfulLabels(Context context, List<AiLabelData> labels) {
        Set<String> genericLabels = readNormalizedSet(context, R.array.ai_generic_labels);
        Set<String> detailLabels = readNormalizedSet(context, R.array.ai_detail_labels);
        List<String> result = new ArrayList<>();
        for (AiLabelData labelData : labels) {
            String cleaned = cleanLabel(labelData.text);
            String lower = cleaned.toLowerCase(Locale.ROOT);
            if (cleaned.isEmpty()) continue;
            if (genericLabels.contains(lower)) continue;
            if (detailLabels.contains(lower)) continue;
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

    private static int keywordWeight(String keyword, Set<String> highWeightKeywords, Set<String> mediumWeightKeywords) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        if (highWeightKeywords.contains(normalized)) return 5;
        if (mediumWeightKeywords.contains(normalized)) return 4;
        return 2;
    }

    private static String familyOf(String category) {
        String c = category.toLowerCase(Locale.ROOT);
        if (c.contains("work") || c.contains("office") || c.contains("tech") || c.contains("computer") || c.contains("laptop")) return "workspace";
        if (c.contains("cafe") || c.contains("coffee") || c.contains("restaurant") || c.contains("interior") || c.contains("furniture") || c.contains("chair")) return "interior";
        if (c.contains("fashion") || c.contains("shoe") || c.contains("footwear")) return "fashion";
        if (c.contains("beach") || c.contains("coast") || c.contains("sea") || c.contains("ocean") || c.contains("water")) return "beach";
        if (c.contains("nature") || c.contains("field") || c.contains("prairie") || c.contains("forest") || c.contains("mountain") || c.contains("branch")) return "nature";
        if (c.contains("urban") || c.contains("city") || c.contains("street")) return "urban";
        if (c.contains("architecture") || c.contains("building")) return "architecture";
        if (c.contains("vehicle") || c.contains("car") || c.contains("motor")) return "vehicles";
        if (c.contains("animal") || c.contains("cat") || c.contains("dog")) return "animals";
        if (c.contains("texture") || c.contains("pattern") || c.contains("asphalt")) return "texture";
        if (c.contains("monochrome")) return "monochrome";
        if (c.contains("light") || c.contains("bokeh")) return "lights";
        if (c.contains("abstract")) return "abstract";
        if (c.contains("space")) return "space";
        if (c.contains("art") || c.contains("design")) return "art";
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
        if (lower.contains("chair")) return "Interior";
        if (lower.contains("shoe") || lower.contains("foot") || lower.contains("sneaker")) return "Fashion";
        if (lower.contains("branch") || lower.contains("twig") || lower.contains("flower") || lower.contains("insect")) return "Nature";
        if (lower.contains("asphalt") || lower.contains("wall")) return "Texture";
        if (lower.contains("monochrome")) return "Monochrome";
        if (lower.contains("pattern")) return "Texture";
        return capitalize(toSingular(label));
    }

    private static boolean isContradictoryAnimalLabel(List<String> selected, String candidate) {
        boolean hasCat = containsIgnoreCase(selected, "Cat");
        boolean hasDog = containsIgnoreCase(selected, "Dog");
        if (hasCat && candidate.equals("dog")) return true;
        if (hasDog && candidate.equals("cat")) return true;
        return false;
    }

    private static int keywordArrayIdForCategory(Context context, String category) {
        String resourceName = "ai_" + category.toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_") + "_keywords";
        int resourceId = context.getResources().getIdentifier(resourceName, "array", context.getPackageName());
        return resourceId == 0 ? R.array.ai_abstract_keywords : resourceId;
    }

    private static List<String> readRawList(Context context, int arrayId) {
        List<String> result = new ArrayList<>();
        if (arrayId == 0) return result;
        try {
            for (String item : context.getResources().getStringArray(arrayId)) {
                result.add(item.trim());
            }
        } catch (Resources.NotFoundException ignored) {
        }
        return result;
    }

    private static List<String> readNormalizedList(Context context, int arrayId) {
        List<String> result = new ArrayList<>();
        for (String item : readRawList(context, arrayId)) {
            result.add(item.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private static Set<String> readNormalizedSet(Context context, int arrayId) {
        return new HashSet<>(readNormalizedList(context, arrayId));
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
        if (text == null || text.isEmpty()) return "";
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
