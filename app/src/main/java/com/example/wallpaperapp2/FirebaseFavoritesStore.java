package com.example.wallpaperapp2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseFavoritesStore {

    public interface FavoritesCallback {
        void onLoaded(Map<Integer, Map<String, Object>> favoritesById);
    }

    public interface AiCacheCallback {
        void onLoaded(boolean found, String category, String labels);
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String AI_CACHE_COLLECTION = "wallpaper_ai_cache";

    public static void saveFavorite(Wallpaper wallpaper) {
        if (wallpaper == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", wallpaper.id);
        payload.put("title", wallpaper.title);
        payload.put("imageUrl", wallpaper.imageUrl);
        payload.put("aiCategory", wallpaper.aiCategory);
        payload.put("aiLabels", wallpaper.aiLabels);
        payload.put("isFavorite", wallpaper.isFavorite);
        payload.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(String.valueOf(wallpaper.id))
                .set(payload);
    }

    public static void removeFavorite(Wallpaper wallpaper) {
        if (wallpaper == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(String.valueOf(wallpaper.id))
                .delete();
    }

    public static void fetchFavorites(FavoritesCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onLoaded(new HashMap<>());
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<Integer, Map<String, Object>> mapped = new HashMap<>();
                    snapshot.getDocuments().forEach(doc -> {
                        Object idValue = doc.get("id");
                        int id = idValue instanceof Number ? ((Number) idValue).intValue() : -1;
                        if (id > -1) {
                            mapped.put(id, doc.getData());
                        }
                    });
                    callback.onLoaded(mapped);
                })
                .addOnFailureListener(e -> callback.onLoaded(new HashMap<>()));
    }

    public static void fetchAiCache(Wallpaper wallpaper, AiCacheCallback callback) {
        if (wallpaper == null) {
            callback.onLoaded(false, "", "");
            return;
        }

        db.collection(AI_CACHE_COLLECTION)
                .document(getCacheId(wallpaper))
                .get()
                .addOnSuccessListener(document -> {
                    if (document == null || !document.exists()) {
                        callback.onLoaded(false, "", "");
                        return;
                    }

                    String category = safeString(document.get("aiCategory"));
                    String labels = safeString(document.get("aiLabels"));
                    if (isUsableAiData(category, labels)) {
                        callback.onLoaded(true, category, labels);
                    } else {
                        callback.onLoaded(false, "", "");
                    }
                })
                .addOnFailureListener(e -> callback.onLoaded(false, "", ""));
    }

    public static void saveAiCache(Wallpaper wallpaper) {
        if (wallpaper == null) return;
        if (!isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", wallpaper.id);
        payload.put("title", wallpaper.title);
        payload.put("imageUrl", wallpaper.imageUrl);
        payload.put("aiCategory", wallpaper.aiCategory);
        payload.put("aiLabels", wallpaper.aiLabels);
        payload.put("analyzedAt", System.currentTimeMillis());

        db.collection(AI_CACHE_COLLECTION)
                .document(getCacheId(wallpaper))
                .set(payload);
    }

    public static boolean isUsableAiData(String category, String labels) {
        String c = category == null ? "" : category.trim();
        String l = labels == null ? "" : labels.trim();

        if (c.isEmpty() || l.isEmpty()) return false;
        if (c.equalsIgnoreCase("Uncategorized")) return false;
        if (c.equalsIgnoreCase("Analyzing")) return false;
        if (l.toLowerCase().contains("gemini http")) return false;
        if (l.toLowerCase().contains("gemini api key")) return false;
        if (l.toLowerCase().contains("gemini analysis failed")) return false;
        if (l.toLowerCase().contains("image analysis in progress")) return false;
        if (l.toLowerCase().contains("image could not be loaded")) return false;
        return true;
    }

    private static String getCacheId(Wallpaper wallpaper) {
        if (wallpaper.imageUrl != null && !wallpaper.imageUrl.trim().isEmpty()) {
            return String.valueOf(wallpaper.imageUrl.trim().hashCode());
        }
        return "local_" + wallpaper.id;
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
