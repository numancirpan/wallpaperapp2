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

    public interface ProfileCallback {
        void onLoaded(UserProfile profile);
    }

    public interface PostsCallback {
        void onLoaded(List<BlogPost> posts);
    }

    public interface ActionCallback {
        void onComplete(boolean success, String errorMessage);
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        payload.put("firstName", safe(profile.firstName));
        payload.put("lastName", safe(profile.lastName));
        payload.put("bio", safe(profile.bio));
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("wallpaperId", wallpaper.id);
        payload.put("imageUrl", wallpaper.imageUrl);
        payload.put("photographer", wallpaper.title);
        payload.put("comment", safe(comment));
        payload.put("aiCategory", safe(wallpaper.aiCategory));
        payload.put("createdAt", System.currentTimeMillis());

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

    public static String currentEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "" : safe(user.getEmail());
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
