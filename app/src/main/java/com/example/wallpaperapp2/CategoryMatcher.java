package com.example.wallpaperapp2;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CategoryMatcher {

    public static String matchOrCreate(String candidate, List<String> existingCategories) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return "Uncategorized";
        }

        if (existingCategories == null || existingCategories.isEmpty()) {
            return candidate;
        }

        String normalizedCandidate = normalize(candidate);

        for (String existing : existingCategories) {
            if (normalize(existing).equals(normalizedCandidate)) {
                return existing;
            }
        }

        String bestMatch = null;
        int bestScore = 0;

        for (String existing : existingCategories) {
            int score = similarityScore(candidate, existing);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = existing;
            }
        }

        if (bestScore >= 1) {
            return bestMatch;
        }

        return candidate;
    }

    private static int similarityScore(String a, String b) {
        Set<String> tokensA = tokenize(normalize(a));
        Set<String> tokensB = tokenize(normalize(b));

        int score = 0;
        for (String token : tokensA) {
            if (tokensB.contains(token)) {
                score++;
            }
        }

        return score;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        String[] parts = text.split("\\s+");

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                tokens.add(part.trim());
            }
        }

        return tokens;
    }

    private static String normalize(String text) {
        if (text == null) return "";

        String lower = text.toLowerCase(Locale.ROOT).trim();

        if (lower.equals("people")) return "person";
        if (lower.equals("graphic")) return "graphic design";
        if (lower.equals("graphics")) return "graphic design";

        return lower;
    }
}