package com.example.wallpaperapp2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Locale;
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
                .addOnSuccessListener(snapshot -> callback.onLoaded(mapFavoritesSnapshot(snapshot)))
                .addOnFailureListener(e -> callback.onLoaded(new HashMap<>()));
    }

    public static ListenerRegistration listenFavorites(FavoritesCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onLoaded(new HashMap<>());
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        callback.onLoaded(new HashMap<>());
                        return;
                    }
                    callback.onLoaded(mapFavoritesSnapshot(snapshot));
                });
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
        String lowerC = c.toLowerCase(Locale.ROOT);
        String lowerL = l.toLowerCase(Locale.ROOT);

        if (c.isEmpty() || l.isEmpty()) return false;
        if (lowerC.equals("uncategorized") || lowerC.equals("kategorisiz")) return false;
        if (lowerC.equals("analyzing") || lowerC.equals("analiz ediliyor")) return false;
        if (lowerL.contains("visual wallpaper content")) return false;
        if (lowerL.contains("görsel duvar kağıdı içeriği")) return false;
        if (lowerL.contains("gemini http")) return false;
        if (lowerL.contains("gemini api key")) return false;
        if (lowerL.contains("gemini analysis failed")) return false;
        if (lowerL.contains("gemini analizi")) return false;
        if (lowerL.contains("image analysis in progress")) return false;
        if (lowerL.contains("image could not be loaded")) return false;
        if (lowerL.contains("on-device analysis failed")) return false;
        if (lowerL.contains("cihaz üstü analiz başarısız")) return false;
        if (lowerL.contains("ml kit analizi başarısız")) return false;

        if (lowerC.contains("hand") || lowerC.contains("nail") || lowerC.contains("musical instrument")) return false;
        if (lowerC.equals("beach rock") || lowerC.equals("field prairie") || lowerC.equals("mobile phone nail")) return false;
        return true;
    }

    private static Map<Integer, Map<String, Object>> mapFavoritesSnapshot(com.google.firebase.firestore.QuerySnapshot snapshot) {
        Map<Integer, Map<String, Object>> mapped = new HashMap<>();
        snapshot.getDocuments().forEach(doc -> {
            Object idValue = doc.get("id");
            int id = idValue instanceof Number ? ((Number) idValue).intValue() : -1;
            if (id > -1) mapped.put(id, doc.getData());
        });
        return mapped;
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
