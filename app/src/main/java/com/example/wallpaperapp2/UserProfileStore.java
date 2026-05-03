package com.example.wallpaperapp2;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileStore {

    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_BIO_LENGTH = 120;
    public static final int MAX_COMMENT_LENGTH = 250;

    public interface ProfileCallback {
        void onLoaded(UserProfile profile);
    }

    public interface PostsCallback {
        void onLoaded(List<BlogPost> posts);
    }

    public interface CollectionsCallback {
        void onLoaded(List<WallpaperCollection> collections);
    }

    public interface ActionCallback {
        void onComplete(boolean success, String errorMessage);
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static List<WallpaperCollection> cachedCollections = new ArrayList<>();

    public static ListenerRegistration listenProfile(ProfileCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onLoaded(new UserProfile());
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("profile")
                .document("main")
                .addSnapshotListener((snapshot, error) -> callback.onLoaded(mapProfile(snapshot)));
    }

    public static void saveProfile(UserProfile profile, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", limit(profile.firstName, MAX_NAME_LENGTH));
        payload.put("lastName", limit(profile.lastName, MAX_NAME_LENGTH));
        payload.put("bio", limit(profile.bio, MAX_BIO_LENGTH));
        payload.put("profilePhotoUrl", safe(profile.profilePhotoUrl));
        payload.put("coverImageUrl", safe(profile.coverImageUrl));
        payload.put("coverWallpaperId", profile.coverWallpaperId);
        payload.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("profile")
                .document("main")
                .set(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public static void updateProfilePhotoFromWallpaper(Wallpaper wallpaper, ActionCallback callback) {
        if (wallpaper == null) {
            callback.onComplete(false, "Wallpaper not found");
            return;
        }
        updateProfileFields(mapOf("profilePhotoUrl", wallpaper.imageUrl), callback);
    }

    public static void uploadProfilePhoto(Context context, Uri imageUri, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (context == null || imageUri == null) {
            callback.onComplete(false, "Photo not found");
            return;
        }

        try {
            InputStream stream = context.getContentResolver().openInputStream(imageUri);
            if (stream == null) {
                callback.onComplete(false, "Selected file cannot be opened");
                return;
            }

            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference()
                    .child("profile_photos")
                    .child(uid)
                    .child("profile_" + System.currentTimeMillis() + ".jpg");

            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            ref.putStream(stream, metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> updateProfileFields(mapOf("profilePhotoUrl", uri.toString()), callback))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        } catch (Exception e) {
            callback.onComplete(false, e.getMessage());
        }
    }

    public static void updateCoverFromWallpaper(Wallpaper wallpaper, ActionCallback callback) {
        if (wallpaper == null) {
            callback.onComplete(false, "Wallpaper not found");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("coverImageUrl", wallpaper.imageUrl);
        payload.put("coverWallpaperId", wallpaper.id);
        updateProfileFields(payload, callback);
    }

    public static void addBlogPost(Wallpaper wallpaper, String comment, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (wallpaper == null) {
            callback.onComplete(false, "Wallpaper not found");
            return;
        }

        String cleanComment = limit(comment, MAX_COMMENT_LENGTH);
        if (cleanComment.isEmpty()) {
            callback.onComplete(false, "Comment is required");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("wallpaperId", wallpaper.id);
        payload.put("imageUrl", wallpaper.imageUrl);
        payload.put("photographer", wallpaper.title);
        payload.put("comment", cleanComment);
        payload.put("aiCategory", safe(wallpaper.aiCategory));
        payload.put("createdAt", System.currentTimeMillis());
        payload.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("posts")
                .add(payload)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "");
                    } else {
                        Exception e = task.getException();
                        callback.onComplete(false, e == null ? "Unknown error" : e.getMessage());
                    }
                });
    }

    public static ListenerRegistration listenBlogPosts(PostsCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onLoaded(new ArrayList<>());
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    List<BlogPost> posts = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            BlogPost post = mapPost(doc);
                            if (post != null) posts.add(post);
                        }
                    }
                    callback.onLoaded(posts);
                });
    }

    public static ListenerRegistration listenCollections(CollectionsCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onLoaded(new ArrayList<>());
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("collections")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    List<WallpaperCollection> collections = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            collections.add(mapCollection(doc));
                        }
                    }
                    cachedCollections = collections;
                    callback.onLoaded(collections);
                });
    }

    public static List<WallpaperCollection> getCachedCollections() {
        return new ArrayList<>(cachedCollections);
    }

    public static void fetchCollections(CollectionsCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("collections")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WallpaperCollection> collections = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        collections.add(mapCollection(doc));
                    }
                    cachedCollections = collections;
                    callback.onLoaded(collections);
                })
                .addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    public static void addWallpaperToCollection(String collectionId, Wallpaper wallpaper, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (collectionId == null || collectionId.trim().isEmpty()) {
            callback.onComplete(false, "Collection not found");
            return;
        }
        if (wallpaper == null) {
            callback.onComplete(false, "Wallpaper not found");
            return;
        }

        appendWallpaperToCollection(uid, collectionId, wallpaper, callback);
    }

    public static void addWallpaperToCollectionByName(String collectionName, Wallpaper wallpaper, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (wallpaper == null) {
            callback.onComplete(false, "Wallpaper not found");
            return;
        }

        String cleanName = limit(collectionName, 40);
        if (cleanName.isEmpty()) {
            callback.onComplete(false, "Collection name is required");
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("collections")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String targetId = "";
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String existingName = safe(doc.getString("name"));
                        if (existingName.equalsIgnoreCase(cleanName)) {
                            targetId = doc.getId();
                            break;
                        }
                    }

                    if (targetId.isEmpty()) {
                        createCollectionWithWallpaper(uid, cleanName, wallpaper, callback);
                    } else {
                        appendWallpaperToCollection(uid, targetId, wallpaper, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    private static void createCollectionWithWallpaper(String uid, String collectionName, Wallpaper wallpaper, ActionCallback callback) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(wallpaperPayload(wallpaper));

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", collectionName);
        payload.put("items", items);
        payload.put("createdAt", System.currentTimeMillis());
        payload.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("collections")
                .add(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    private static void appendWallpaperToCollection(String uid, String collectionId, Wallpaper wallpaper, ActionCallback callback) {
        db.collection("users")
                .document(uid)
                .collection("collections")
                .document(collectionId)
                .get()
                .addOnSuccessListener(document -> {
                    List<Map<String, Object>> items = new ArrayList<>();
                    Object rawItems = document.get("items");
                    if (rawItems instanceof List<?>) {
                        for (Object rawItem : (List<?>) rawItems) {
                            if (!(rawItem instanceof Map<?, ?>)) continue;
                            Object rawId = ((Map<?, ?>) rawItem).get("id");
                            int id = rawId instanceof Number ? ((Number) rawId).intValue() : -1;
                            if (id == wallpaper.id) continue;
                            Map<String, Object> item = new HashMap<>();
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawItem).entrySet()) {
                                if (entry.getKey() != null) item.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            items.add(item);
                        }
                    }
                    items.add(wallpaperPayload(wallpaper));

                    Map<String, Object> update = new HashMap<>();
                    update.put("items", items);
                    update.put("updatedAt", System.currentTimeMillis());
                    document.getReference()
                            .update(update)
                            .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                            .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public static void updateBlogPost(String postId, String comment, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            callback.onComplete(false, "Post not found");
            return;
        }

        String cleanComment = limit(comment, MAX_COMMENT_LENGTH);
        if (cleanComment.isEmpty()) {
            callback.onComplete(false, "Comment is required");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("comment", cleanComment);
        payload.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("posts")
                .document(postId)
                .update(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public static void deleteBlogPost(String postId, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            callback.onComplete(false, "Post not found");
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("posts")
                .document(postId)
                .delete()
                .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    private static void updateProfileFields(Map<String, Object> fields, ActionCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onComplete(false, "User session not found");
            return;
        }
        fields.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("profile")
                .document("main")
                .set(fields, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, ""))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static UserProfile mapProfile(DocumentSnapshot snapshot) {
        UserProfile profile = new UserProfile();
        if (snapshot == null || !snapshot.exists()) return profile;
        profile.firstName = safe(snapshot.getString("firstName"));
        profile.lastName = safe(snapshot.getString("lastName"));
        profile.bio = safe(snapshot.getString("bio"));
        profile.profilePhotoUrl = safe(snapshot.getString("profilePhotoUrl"));
        profile.coverImageUrl = safe(snapshot.getString("coverImageUrl"));
        Object coverId = snapshot.get("coverWallpaperId");
        profile.coverWallpaperId = coverId instanceof Number ? ((Number) coverId).intValue() : -1;
        Object updatedAt = snapshot.get("updatedAt");
        profile.updatedAt = updatedAt instanceof Number ? ((Number) updatedAt).longValue() : 0L;
        return profile;
    }

    private static BlogPost mapPost(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        BlogPost post = new BlogPost();
        post.id = doc.getId();
        Object wallpaperId = doc.get("wallpaperId");
        post.wallpaperId = wallpaperId instanceof Number ? ((Number) wallpaperId).intValue() : -1;
        post.imageUrl = safe(doc.getString("imageUrl"));
        post.photographer = safe(doc.getString("photographer"));
        post.comment = safe(doc.getString("comment"));
        post.aiCategory = safe(doc.getString("aiCategory"));
        Object createdAt = doc.get("createdAt");
        post.createdAt = createdAt instanceof Number ? ((Number) createdAt).longValue() : 0L;
        return post;
    }

    private static WallpaperCollection mapCollection(DocumentSnapshot doc) {
        WallpaperCollection collection = new WallpaperCollection();
        if (doc == null || !doc.exists()) return collection;

        collection.id = doc.getId();
        collection.name = safe(doc.getString("name"));
        Object updatedAt = doc.get("updatedAt");
        collection.updatedAt = updatedAt instanceof Number ? ((Number) updatedAt).longValue() : 0L;

        Object items = doc.get("items");
        if (items instanceof List<?>) {
            for (Object item : (List<?>) items) {
                if (item instanceof Map<?, ?>) {
                    Wallpaper wallpaper = mapWallpaperItem((Map<?, ?>) item);
                    if (wallpaper != null) collection.wallpapers.add(wallpaper);
                }
            }
        }
        return collection;
    }

    private static Wallpaper mapWallpaperItem(Map<?, ?> item) {
        Object idValue = item.get("id");
        int id = idValue instanceof Number ? ((Number) idValue).intValue() : -1;
        if (id < 0) return null;

        Wallpaper wallpaper = new Wallpaper(id, safeObject(item.get("imageUrl")), safeObject(item.get("title")));
        wallpaper.aiCategory = safeObject(item.get("aiCategory"));
        wallpaper.aiLabels = safeObject(item.get("aiLabels"));
        return wallpaper;
    }

    private static Map<String, Object> wallpaperPayload(Wallpaper wallpaper) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", wallpaper.id);
        payload.put("title", safe(wallpaper.title));
        payload.put("imageUrl", safe(wallpaper.imageUrl));
        payload.put("aiCategory", safe(wallpaper.aiCategory));
        payload.put("aiLabels", safe(wallpaper.aiLabels));
        payload.put("addedAt", System.currentTimeMillis());
        return payload;
    }

    public static String currentEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "" : safe(user.getEmail());
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private static String limit(String value, int maxLength) {
        String clean = safe(value);
        if (clean.length() <= maxLength) return clean;
        return clean.substring(0, maxLength);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeObject(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
