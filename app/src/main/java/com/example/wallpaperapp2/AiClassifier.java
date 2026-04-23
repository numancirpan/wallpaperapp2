package com.example.wallpaperapp2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;

public class AiClassifier {

    public interface OnClassificationCompleteListener {
        void onComplete(Wallpaper wallpaper);
        void onError(Exception e);
    }

    public static void analyzeWallpaper(
            @NonNull Context context,
            @NonNull Wallpaper wallpaper,
            @NonNull OnClassificationCompleteListener listener
    ) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, wallpaper.imageRes);
            if (!(drawable instanceof BitmapDrawable)) {
                listener.onError(new Exception("Image resource could not be converted to bitmap."));
                return;
            }

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.5f)
                    .build();

            ImageLabeling.getClient(options)
                    .process(image)
                    .addOnSuccessListener(labels -> {
                        List<String> topLabels = extractTopLabels(labels);
                        List<String> existingCategories = WallpaperRepository.getExistingAiCategories();

                        String resolvedCategory = CategoryResolver.resolveCategory(topLabels, existingCategories);

                        wallpaper.aiLabels = joinLabels(topLabels);
                        wallpaper.aiCategory = resolvedCategory;

                        listener.onComplete(wallpaper);
                    })
                    .addOnFailureListener(listener::onError);

        } catch (Exception e) {
            listener.onError(e);
        }
    }

    private static List<String> extractTopLabels(List<ImageLabel> labels) {
        List<String> results = new ArrayList<>();

        int limit = Math.min(labels.size(), 5);
        for (int i = 0; i < limit; i++) {
            results.add(labels.get(i).getText());
        }

        if (results.isEmpty()) {
            results.add("unknown");
        }

        return results;
    }

    private static String joinLabels(List<String> labels) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < labels.size(); i++) {
            builder.append(labels.get(i));
            if (i < labels.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }
}