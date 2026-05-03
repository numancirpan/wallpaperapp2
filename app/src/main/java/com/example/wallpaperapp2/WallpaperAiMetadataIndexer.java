package com.example.wallpaperapp2;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

public class WallpaperAiMetadataIndexer {

    public interface Callback {
        void onUpdated(Wallpaper wallpaper);
    }

    public static void ensureSearchMetadata(
            @NonNull Context context,
            @NonNull Wallpaper wallpaper,
            @NonNull List<String> existingCategories,
            @NonNull Callback callback
    ) {
        if (FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
            callback.onUpdated(wallpaper);
            return;
        }

        FirebaseFavoritesStore.fetchAiCache(wallpaper, (found, category, labels) -> {
            if (found) {
                wallpaper.aiCategory = category;
                wallpaper.aiLabels = labels;
                callback.onUpdated(wallpaper);
                return;
            }

            AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
                @Override
                public void onSuccess(List<AiLabelData> labels) {
                    wallpaper.aiCategory = DynamicCategoryGenerator.generateCategory(
                            context,
                            labels,
                            existingCategories
                    );
                    wallpaper.aiLabels = context.getString(
                            R.string.on_device_suffix,
                            DynamicCategoryGenerator.labelsToDisplay(context, labels)
                    );

                    if (!FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
                        wallpaper.aiCategory = "";
                        wallpaper.aiLabels = "";
                        callback.onUpdated(wallpaper);
                        return;
                    }

                    FirebaseFavoritesStore.saveAiCache(wallpaper);
                    if (wallpaper.isFavorite) {
                        FirebaseFavoritesStore.saveFavorite(wallpaper);
                    }
                    callback.onUpdated(wallpaper);
                }

                @Override
                public void onError(Exception e) {
                    callback.onUpdated(wallpaper);
                }
            };

            if (wallpaper.hasRemoteImage()) {
                AiClassifier.analyzeImageUrl(context, wallpaper.imageUrl, listener);
            } else {
                AiClassifier.analyzeImage(context, wallpaper.imageRes, listener);
            }
        });
    }
}
