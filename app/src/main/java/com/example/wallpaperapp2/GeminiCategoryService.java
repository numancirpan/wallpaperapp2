package com.example.wallpaperapp2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiCategoryService {

    public interface Callback {
        void onResult(AnalysisResult result);
    }

    public static class AnalysisResult {
        public final String category;
        public final String labelsCsv;

        public AnalysisResult(String category, String labelsCsv) {
            this.category = category == null ? "Uncategorized" : category.trim();
            this.labelsCsv = labelsCsv == null ? "" : labelsCsv.trim();
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<String> MODEL_CANDIDATES = Arrays.asList(
            "gemini-2.0-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash",
            "gemini-pro-vision"
    );

    public static boolean isConfigured() {
        return BuildConfig.GEMINI_API_KEY != null && !BuildConfig.GEMINI_API_KEY.trim().isEmpty();
    }

    public static void analyzeWallpaper(
            @NonNull Context context,
            @NonNull Wallpaper wallpaper,
            List<String> existingCategories,
            @NonNull Callback callback
    ) {
        String apiKey = BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
        if (apiKey.isEmpty()) {
            callback.onResult(new AnalysisResult("Uncategorized", "Gemini API key missing - check local.properties and rebuild"));
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                Bitmap bitmap = loadBitmap(context, wallpaper);
                if (bitmap == null) {
                    callback.onResult(new AnalysisResult("Uncategorized", "Image could not be loaded"));
                    return;
                }

                String base64Image = bitmapToBase64(resizeBitmap(bitmap, 768));
                String requestBody = buildRequestBody(wallpaper, existingCategories, base64Image).toString();
                String lastError = "Unknown Gemini error";

                for (String modelName : MODEL_CANDIDATES) {
                    GeminiHttpResult httpResult = callGemini(apiKey, modelName, requestBody);

                    if (httpResult.success) {
                        String rawText = extractGeminiText(httpResult.response);
                        AnalysisResult result = parseAnalysis(rawText, existingCategories);
                        callback.onResult(result);
                        return;
                    }

                    lastError = buildHttpErrorMessage(httpResult.responseCode, httpResult.response);
                    boolean canTryNextModel = httpResult.responseCode == 404 || httpResult.responseCode == 400;
                    if (!canTryNextModel) {
                        callback.onResult(new AnalysisResult("Uncategorized", lastError));
                        return;
                    }
                }

                callback.onResult(new AnalysisResult("Uncategorized", lastError));
            } catch (Exception e) {
                callback.onResult(new AnalysisResult("Uncategorized", "Gemini exception: " + shortMessage(e.getMessage())));
            }
        });
    }

    public static void refineCategory(
            @NonNull Context context,
            @NonNull Wallpaper wallpaper,
            List<AiLabelData> labels,
            String onDeviceCategory,
            List<String> existingCategories,
            @NonNull Callback callback
    ) {
        analyzeWallpaper(context, wallpaper, existingCategories, callback);
    }

    private static GeminiHttpResult callGemini(String apiKey, String modelName, String requestBody) throws Exception {
        HttpURLConnection connection = null;
        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName
                    + ":generateContent?key="
                    + apiKey;

            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(requestBody.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readStream(stream);
            return new GeminiHttpResult(responseCode >= 200 && responseCode < 300, responseCode, response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONObject buildRequestBody(Wallpaper wallpaper, List<String> existingCategories, String base64Image) throws Exception {
        JSONObject root = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();

        parts.put(new JSONObject().put("text", buildPrompt(wallpaper, existingCategories)));
        parts.put(new JSONObject().put("inline_data", new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image)));

        content.put("parts", parts);
        contents.put(content);
        root.put("contents", contents);
        return root;
    }

    private static String buildPrompt(Wallpaper wallpaper, List<String> existingCategories) {
        String categoriesText = existingCategories == null || existingCategories.isEmpty()
                ? "None"
                : String.join(", ", existingCategories);

        return "Analyze this wallpaper image visually. Ignore photographer names, author names, and random titles unless they describe the image.\n"
                + "Existing app categories: " + categoriesText + "\n"
                + "Return valid JSON only, with no markdown and no explanation.\n"
                + "Format: {\"category\":\"Category Name\",\"labels\":[\"label one\",\"label two\",\"label three\"]}\n"
                + "Rules:\n"
                + "1. Labels must be based on what is visible in the image. Produce 4 to 7 short visual labels.\n"
                + "2. Category must be broad, reusable, Title Case, and 1 to 3 words.\n"
                + "3. If an existing category strongly fits, use that existing category exactly.\n"
                + "4. If no existing category fits, create a new broad category that future similar wallpapers can join.\n"
                + "5. Do not create weird over-specific categories such as 'Musical Instrument Cup' or categories based on tiny objects.\n"
                + "6. Prefer useful wallpaper categories such as Nature, Water Scenes, Technology, Urban, Architecture, Animals, Vehicles, Food, Space, Abstract, Minimal, Dark, People, Art, or similar broad names.\n"
                + "Known author/title text from API, usually not useful: " + safe(wallpaper.title);
    }

    private static Bitmap loadBitmap(Context context, Wallpaper wallpaper) throws Exception {
        if (wallpaper.hasRemoteImage()) {
            return Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(wallpaper.imageUrl)
                    .submit(768, 768)
                    .get();
        }
        return BitmapFactory.decodeResource(context.getResources(), wallpaper.imageRes);
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.max(1, Math.round(width * ratio));
        int newHeight = Math.max(1, Math.round(height * ratio));
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private static String extractGeminiText(String response) throws Exception {
        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return "";
        JSONObject first = candidates.getJSONObject(0);
        return first.getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .optString("text", "");
    }

    private static AnalysisResult parseAnalysis(String raw, List<String> existingCategories) {
        try {
            String cleaned = cleanJsonText(raw);
            JSONObject json = new JSONObject(cleaned);
            String category = normalizeCategory(json.optString("category", "Uncategorized"), existingCategories);
            JSONArray labelsArray = json.optJSONArray("labels");
            List<String> labels = new ArrayList<>();

            if (labelsArray != null) {
                for (int i = 0; i < labelsArray.length(); i++) {
                    String label = cleanLabel(labelsArray.optString(i, ""));
                    if (!label.isEmpty() && !containsIgnoreCase(labels, label)) {
                        labels.add(label);
                    }
                    if (labels.size() == 7) break;
                }
            }

            if (labels.isEmpty()) {
                labels.add("No labels returned");
            }

            return new AnalysisResult(category, String.join(", ", labels));
        } catch (Exception e) {
            return new AnalysisResult("Uncategorized", "Gemini parse failed: " + shortMessage(e.getMessage()));
        }
    }

    private static String buildHttpErrorMessage(int code, String response) {
        String message = "";
        try {
            JSONObject json = new JSONObject(response);
            JSONObject error = json.optJSONObject("error");
            if (error != null) {
                message = error.optString("message", "");
            }
        } catch (Exception ignored) {
            message = response == null ? "" : response;
        }

        return "Gemini HTTP " + code + ": " + shortMessage(message);
    }

    private static String shortMessage(String message) {
        if (message == null || message.trim().isEmpty()) return "Unknown error";
        String cleaned = message.replace("\n", " ").replace("\r", " ").trim();
        return cleaned.length() > 90 ? cleaned.substring(0, 90) + "..." : cleaned;
    }

    private static String cleanJsonText(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private static String normalizeCategory(String rawCategory, List<String> existingCategories) {
        String cleaned = rawCategory == null ? "" : rawCategory.replaceAll("[^a-zA-Z0-9 &-]", " ").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) return "Uncategorized";

        if (existingCategories != null) {
            for (String existing : existingCategories) {
                if (existing != null && existing.equalsIgnoreCase(cleaned)) {
                    return existing;
                }
            }
        }

        return toTitleCase(cleaned);
    }

    private static String cleanLabel(String rawLabel) {
        String cleaned = rawLabel == null ? "" : rawLabel.replaceAll("[^a-zA-Z0-9 &-]", " ").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > 28) cleaned = cleaned.substring(0, 28).trim();
        return toTitleCase(cleaned);
    }

    private static boolean containsIgnoreCase(List<String> source, String value) {
        for (String item : source) {
            if (item.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static String toTitleCase(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] parts = text.trim().toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(" ");
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) builder.append(part.substring(1));
        }
        return builder.toString();
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

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static class GeminiHttpResult {
        final boolean success;
        final int responseCode;
        final String response;

        GeminiHttpResult(boolean success, int responseCode, String response) {
            this.success = success;
            this.responseCode = responseCode;
            this.response = response;
        }
    }
}
