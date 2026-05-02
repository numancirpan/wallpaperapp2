package com.example.wallpaperapp2;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiLabelDisplayMapper {

    public static String toDisplayLabels(Context context, String labelsCsv) {
        if (labelsCsv == null || labelsCsv.trim().isEmpty()) {
            return context.getString(R.string.not_available);
        }

        String cleaned = labelsCsv.trim();
        boolean onDevice = cleaned.toLowerCase(Locale.ROOT).contains("(on-device)")
                || cleaned.toLowerCase(Locale.ROOT).contains("(cihaz üstü)");
        cleaned = cleaned.replace("(on-device)", "")
                .replace("(cihaz üstü)", "")
                .trim();

        if (looksLikeSystemMessage(cleaned)) {
            return cleaned;
        }

        String[] parts = cleaned.split(",");
        List<String> translated = new ArrayList<>();
        for (String part : parts) {
            String label = part.trim();
            if (label.isEmpty()) continue;
            translated.add(translateLabel(context, label));
        }

        String result = translated.isEmpty()
                ? context.getString(R.string.visual_wallpaper_content)
                : String.join(", ", translated);

        if (onDevice) {
            result = context.getString(R.string.on_device_suffix, result);
        }
        return result;
    }

    private static boolean looksLikeSystemMessage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("gemini http")
                || lower.contains("api key")
                || lower.contains("failed")
                || lower.contains("progress")
                || lower.contains("cache")
                || lower.contains("prepayment")
                || lower.contains("quota");
    }

    private static String translateLabel(Context context, String label) {
        String language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        if (!"tr".equals(language)) {
            return label;
        }

        Map<String, String> translations = readTurkishTranslations(context);
        String translated = translations.get(label.toLowerCase(Locale.ROOT));
        return translated == null ? label : translated;
    }

    private static Map<String, String> readTurkishTranslations(Context context) {
        Map<String, String> translations = new HashMap<>();
        String[] keys = context.getResources().getStringArray(R.array.ai_label_translation_keys);
        String[] values = context.getResources().getStringArray(R.array.ai_label_translation_values_tr);

        int count = Math.min(keys.length, values.length);
        for (int i = 0; i < count; i++) {
            translations.put(keys[i].toLowerCase(Locale.ROOT).trim(), values[i].trim());
        }
        return translations;
    }
}
