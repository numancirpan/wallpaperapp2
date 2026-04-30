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
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AiClassifier {

    public interface OnLabelsReadyListener {
        void onSuccess(List<AiLabelData> labels);
        void onError(Exception e);
    }

    public static void analyzeImage(
            @NonNull Context context,
            int imageRes,
            @NonNull OnLabelsReadyListener listener
    ) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, imageRes);
            if (!(drawable instanceof BitmapDrawable)) {
                listener.onError(new Exception("Image resource could not be converted to bitmap."));
                return;
            }

            analyzeBitmap(((BitmapDrawable) drawable).getBitmap(), listener);

        } catch (Exception e) {
            listener.onError(e);
        }
    }

    public static void analyzeImageUrl(
            @NonNull Context context,
            @NonNull String imageUrl,
            @NonNull OnLabelsReadyListener listener
    ) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Bitmap bitmap = Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get();
                analyzeBitmap(bitmap, listener);
            } catch (Exception e) {
                listener.onError(e);
            }
        });
    }

    private static void analyzeBitmap(@NonNull Bitmap bitmap, @NonNull OnLabelsReadyListener listener) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.40f)
                .build();

        ImageLabeling.getClient(options)
                .process(image)
                .addOnSuccessListener(labels -> {
                    List<AiLabelData> result = new ArrayList<>();

                    int limit = Math.min(labels.size(), 6);
                    for (int i = 0; i < limit; i++) {
                        ImageLabel label = labels.get(i);
                        result.add(new AiLabelData(label.getText(), label.getConfidence()));
                    }

                    if (result.isEmpty()) {
                        result.add(new AiLabelData("Unknown", 0f));
                    }
                    listener.onSuccess(result);
                })
                .addOnFailureListener(listener::onError);
    }
}