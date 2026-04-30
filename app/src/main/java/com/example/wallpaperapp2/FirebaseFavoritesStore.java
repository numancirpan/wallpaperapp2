package com.example.wallpaperapp2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseFavoritesStore {

    public interface FavoritesCallback {
        void onLoaded(Map<Integer, Map<String, Object>> favoritesById);
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
}
