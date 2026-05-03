package com.example.wallpaperapp2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperApiService {

    public interface Callback {
        void onSuccess(List<Wallpaper> wallpapers);
        void onError(Exception exception);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void fetchWallpapers(Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                List<Wallpaper> result = fetchWallpapersFromUrl(BuildConfig.WALLPAPER_API_URL);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private static List<Wallpaper> fetchWallpapersFromUrl(String urlText) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String raw = readStream(stream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("Wallpaper API error: " + responseCode + " " + raw);
            }

            JSONArray array = new JSONArray(raw);
            List<Wallpaper> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String idText = item.optString("id", String.valueOf(i + 1));
                int id = safeParseInt(idText, i + 1);
                String author = item.optString("author", "Wallpaper " + id);
                String imageUrl = item.optString("download_url", "");
                result.add(new Wallpaper(id, imageUrl, author));
            }

            return result;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private static int safeParseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
