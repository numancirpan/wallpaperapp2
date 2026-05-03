package com.example.wallpaperapp2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiCategoryService {

    public interface Callback {
        void onResult(String category);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "Nature",
            "City",
            "Cars",
            "Anime",
            "Abstract",
            "Animals",
            "Space",
            "Technology",
            "People",
            "Art",
            "Dark",
            "Minimal"
    );

    public static void generateCategory(String title, String labelsCsv, Callback callback) {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            callback.onResult("");
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                        + BuildConfig.GEMINI_API_KEY;
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String prompt = "You are a strict wallpaper categorizer.\n"
                        + "Allowed categories: " + String.join(", ", ALLOWED_CATEGORIES) + ".\n"
                        + "Return exactly one category from allowed list only.\n"
                        + "Do not explain, do not add punctuation, do not output extra words.\n"
                        + "If uncertain, return Nature.\n"
                        + "Title: " + safe(title) + "\n"
                        + "Labels: " + safe(labelsCsv);

                JSONObject root = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", prompt));
                content.put("parts", parts);
                contents.put(content);
                root.put("contents", contents);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(root.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                String response = readStream(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    callback.onResult("");
                    return;
                }

                JSONObject json = new JSONObject(response);
                JSONArray candidates = json.optJSONArray("candidates");
                if (candidates == null || candidates.length() == 0) {
                    callback.onResult("");
                    return;
                }

                JSONObject first = candidates.getJSONObject(0);
                String raw = first.getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .optString("text", "");
                callback.onResult(normalizeToAllowedCategory(raw));
            } catch (Exception e) {
                callback.onResult("");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private static String normalizeToAllowedCategory(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replace("\n", " ")
                .replace(".", " ")
                .replace(":", " ")
                .replace("-", " ")
                .trim();
        if (cleaned.isEmpty()) return "";

        for (String allowed : ALLOWED_CATEGORIES) {
            if (allowed.equalsIgnoreCase(cleaned)) {
                return allowed;
            }
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String allowed : ALLOWED_CATEGORIES) {
            if (lower.contains(allowed.toLowerCase(Locale.ROOT))) {
                return allowed;
            }
        }

        if (lower.contains("car") || lower.contains("vehicle") || lower.contains("race")) return "Cars";
        if (lower.contains("city") || lower.contains("urban") || lower.contains("street")) return "City";
        if (lower.contains("animal") || lower.contains("cat") || lower.contains("dog")) return "Animals";
        if (lower.contains("space") || lower.contains("galaxy") || lower.contains("planet")) return "Space";
        if (lower.contains("anime") || lower.contains("manga") || lower.contains("cartoon")) return "Anime";
        if (lower.contains("person") || lower.contains("portrait") || lower.contains("face")) return "People";
        if (lower.contains("tech") || lower.contains("device") || lower.contains("computer")) return "Technology";
        if (lower.contains("abstract") || lower.contains("pattern")) return "Abstract";
        if (lower.contains("art") || lower.contains("illustration")) return "Art";
        if (lower.contains("dark") || lower.contains("night")) return "Dark";
        if (lower.contains("minimal") || lower.contains("simple")) return "Minimal";
        if (lower.contains("nature") || lower.contains("forest") || lower.contains("mountain")) return "Nature";

        return "";
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
