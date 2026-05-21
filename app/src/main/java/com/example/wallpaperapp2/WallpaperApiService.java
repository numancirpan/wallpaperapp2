package com.example.wallpaperapp2;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                callback.onSuccess(fetchFromUrl(BuildConfig.WALLPAPER_API_URL, 200));
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public static void fetchWallpapersForQuery(String query, Callback callback) {
        fetchWallpapersForQuery(query, 200, callback);
    }

    public static void fetchWallpapersForQuery(String query, int perPage, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String q = query == null ? "" : query.trim();
                String url = BuildConfig.WALLPAPER_API_URL;
                boolean pixabaySearch = isPixabayUrl(url) && !q.isEmpty();
                if (pixabaySearch) {
                    url = withQueryParameter(url, "q", URLEncoder.encode(q, "UTF-8"));
                }
                List<Wallpaper> result = fetchFromUrl(url, perPage);
                callback.onSuccess(pixabaySearch ? onlyMatching(result, q) : result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private static List<Wallpaper> onlyMatching(List<Wallpaper> source, String query) {
        List<Wallpaper> out = new ArrayList<>();
        String wanted = clean(query);
        if (wanted.isEmpty() || source == null) return out;

        for (Wallpaper wallpaper : source) {
            String tags = clean(text(wallpaper.tags));
            String title = clean(text(wallpaper.title));

            if (matchesWantedKeyword(tags, title, wanted)) {
                out.add(wallpaper);
            }
        }
        return out;
    }

    private static boolean matchesWantedKeyword(String tags, String title, String wanted) {
        String searchable = tags + " " + title;

        if (wanted.equals("dog")) {
            return containsAnyToken(searchable, "dog", "dogs", "puppy", "puppies", "canine", "hound", "retriever", "husky", "terrier", "bulldog", "beagle");
        }
        if (wanted.equals("cat")) {
            return containsAnyToken(searchable, "cat", "cats", "kitten", "kittens", "feline");
        }
        if (wanted.equals("city")) {
            return containsAnyToken(searchable, "city", "urban", "street", "building", "skyline", "architecture");
        }
        if (wanted.equals("nature")) {
            return containsAnyToken(searchable, "nature", "forest", "tree", "flower", "leaf", "plant", "mountain", "landscape");
        }
        if (wanted.equals("beach")) {
            return containsAnyToken(searchable, "beach", "sea", "ocean", "coast", "shore", "sand");
        }
        if (wanted.equals("space")) {
            return containsAnyToken(searchable, "space", "stars", "star", "galaxy", "moon", "planet", "night");
        }

        for (String token : searchable.split(" ")) {
            if (token.equals(wanted) || token.startsWith(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyToken(String text, String... acceptedTokens) {
        if (text == null || text.isEmpty()) return false;
        for (String token : text.split(" ")) {
            for (String accepted : acceptedTokens) {
                if (token.equals(accepted) || token.startsWith(accepted)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("ı", "i").replace("ğ", "g").replace("ü", "u")
                .replace("ş", "s").replace("ö", "o").replace("ç", "c")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static List<Wallpaper> fetchFromUrl(String urlText, int perPage) throws Exception {
        if (isPixabayUrl(urlText)) {
            int safePerPage = Math.max(3, Math.min(perPage, 200));
            urlText = withQueryParameter(withQueryParameter(urlText, "page", "1"), "per_page", String.valueOf(safePerPage));
        }
        return fetchSingleUrl(urlText);
    }

    private static List<Wallpaper> fetchSingleUrl(String urlText) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream();
            String raw = readStream(stream);
            if (responseCode < 200 || responseCode >= 300) throw new Exception("Wallpaper API error: " + responseCode + " " + raw);
            return parseResponse(raw);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isPixabayUrl(String urlText) {
        return urlText != null && urlText.toLowerCase(Locale.ROOT).contains("pixabay.com/api");
    }

    private static String withQueryParameter(String urlText, String key, String value) {
        String pattern = "([?&])" + key + "=[^&]*";
        if (urlText.matches(".*" + pattern + ".*")) return urlText.replaceAll(pattern, "$1" + key + "=" + value);
        return urlText + (urlText.contains("?") ? "&" : "?") + key + "=" + value;
    }

    private static List<Wallpaper> parseResponse(String raw) throws Exception {
        Object json = new JSONTokener(raw).nextValue();
        if (json instanceof JSONObject) return parsePixabay((JSONObject) json);
        if (json instanceof JSONArray) return parsePicsum((JSONArray) json);
        throw new Exception("Unsupported wallpaper API response");
    }

    private static List<Wallpaper> parsePixabay(JSONObject root) throws Exception {
        JSONArray array = root.optJSONArray("hits");
        if (array == null) throw new Exception("Pixabay response does not contain hits array");
        List<Wallpaper> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            int id = item.optInt("id", i + 1);
            String tags = item.optString("tags", "");
            String photographer = item.optString("user", "");
            String imageUrl = item.optString("largeImageURL", item.optString("webformatURL", ""));
            if (!imageUrl.trim().isEmpty()) {
                Wallpaper wallpaper = new Wallpaper(id, imageUrl, photographer.trim().isEmpty() ? "Wallpaper " + id : photographer);
                wallpaper.tags = tags;
                wallpaper.photographer = photographer;
                wallpaper.views = item.optInt("views", 0);
                wallpaper.downloads = item.optInt("downloads", 0);
                wallpaper.likes = item.optInt("likes", 0);
                wallpaper.sourceUrl = item.optString("pageURL", "");
                result.add(wallpaper);
            }
        }
        return result;
    }

    private static List<Wallpaper> parsePicsum(JSONArray array) throws Exception {
        List<Wallpaper> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            int id = safeParseInt(item.optString("id", String.valueOf(i + 1)), i + 1);
            String author = item.optString("author", "Wallpaper " + id);
            String imageUrl = item.optString("download_url", "");
            if (!imageUrl.trim().isEmpty()) {
                Wallpaper wallpaper = new Wallpaper(id, imageUrl, author);
                wallpaper.photographer = author;
                wallpaper.sourceUrl = item.optString("url", "");
                result.add(wallpaper);
            }
        }
        return result;
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
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
