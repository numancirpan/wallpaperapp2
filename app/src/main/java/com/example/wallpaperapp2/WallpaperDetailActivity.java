package com.example.wallpaperapp2;

import android.app.WallpaperManager;
import android.app.Dialog;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperDetailActivity extends AppCompatActivity {

    private ImageView imageWallpaper;
    private TextView txtTitle;
    private TextView txtPhotographer;
    private TextView txtAiCategory;
    private TextView txtAiLabels;
    private MaterialButton btnFavorite;
    private MaterialButton btnRepostWallpaper;
    private MaterialButton btnDetailCollection;
    private MaterialButton btnDownloadWallpaper;
    private MaterialButton btnSetHomeWallpaper;
    private MaterialButton btnSetLockWallpaper;

    private Wallpaper wallpaper;
    private ListenerRegistration collectionsListener;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_detail);

        imageWallpaper = findViewById(R.id.imageWallpaper);
        txtTitle = findViewById(R.id.txtDetailTitle);
        txtPhotographer = findViewById(R.id.txtPhotographer);
        txtAiCategory = findViewById(R.id.txtDetailAiCategory);
        txtAiLabels = findViewById(R.id.txtDetailAiLabels);
        btnFavorite = findViewById(R.id.btnDetailFavorite);
        btnRepostWallpaper = findViewById(R.id.btnRepostWallpaper);
        btnDetailCollection = findViewById(R.id.btnDetailCollection);
        btnDownloadWallpaper = findViewById(R.id.btnDownloadWallpaper);
        btnSetHomeWallpaper = findViewById(R.id.btnSetHomeWallpaper);
        btnSetLockWallpaper = findViewById(R.id.btnSetLockWallpaper);

        int wallpaperId = getIntent().getIntExtra("wallpaper_id", -1);

        WallpaperRepository.initializeData();
        wallpaper = WallpaperRepository.getWallpaperById(wallpaperId);

        if (wallpaper != null) {
            renderImage();
            txtTitle.setText(R.string.wallpaper_preview);
            txtPhotographer.setText(getString(R.string.photo_by, wallpaper.title));
            updateAiTexts();
            updateFavoriteIcon();
            bindWallpaperActions();
            bindFavoriteAction();
            bindRepostAction();
            bindCollectionAction();
            bindDownloadAction();
            watchCollections();
            imageWallpaper.setOnClickListener(v -> showFullscreenPreview());
        }
    }

    private void renderImage() {
        if (wallpaper.hasRemoteImage()) {
            Glide.with(this)
                    .load(wallpaper.imageUrl)
                    .centerCrop()
                    .into(imageWallpaper);
        } else {
            imageWallpaper.setImageResource(wallpaper.imageRes);
        }
    }

    private void bindFavoriteAction() {
        btnFavorite.setOnClickListener(v -> {
            wallpaper.isFavorite = !wallpaper.isFavorite;
            updateFavoriteIcon();

            if (!wallpaper.isFavorite) {
                FirebaseFavoritesStore.removeFavorite(wallpaper);
                updateAiTexts();
                return;
            }

            FirebaseFavoritesStore.saveFavorite(wallpaper);
            if (needsAnalysis()) {
                analyzeFavoriteFromDetail();
            } else {
                updateAiTexts();
            }
        });
    }

    private void bindRepostAction() {
        btnRepostWallpaper.setOnClickListener(v -> RepostDialogHelper.show(this, findViewById(android.R.id.content), wallpaper));
    }

    private void bindDownloadAction() {
        btnDownloadWallpaper.setOnClickListener(v -> downloadWallpaper());
    }

    private void bindCollectionAction() {
        btnDetailCollection.setOnClickListener(v ->
                CollectionDialogHelper.show(this, findViewById(android.R.id.content), wallpaper)
        );
    }

    private void watchCollections() {
        updateCollectionButton(UserProfileStore.getCachedCollections());
        collectionsListener = UserProfileStore.listenCollections(collections ->
                runOnUiThread(() -> updateCollectionButton(collections))
        );
    }

    private void updateCollectionButton(java.util.List<WallpaperCollection> collections) {
        if (wallpaper == null || btnDetailCollection == null) return;

        String collectionName = findContainingCollectionName(collections);
        if (collectionName.isEmpty()) {
            btnDetailCollection.setText(R.string.add_to_collection);
            btnDetailCollection.setIconResource(android.R.drawable.ic_menu_add);
            btnDetailCollection.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btnDetailCollection.setTextColor(android.graphics.Color.rgb(103, 80, 164));
            btnDetailCollection.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(103, 80, 164)));
            btnDetailCollection.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(103, 80, 164)));
            return;
        }

        btnDetailCollection.setText(getString(R.string.in_collection_button, collectionName));
        btnDetailCollection.setIconResource(android.R.drawable.checkbox_on_background);
        btnDetailCollection.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(103, 80, 164)));
        btnDetailCollection.setTextColor(android.graphics.Color.WHITE);
        btnDetailCollection.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        btnDetailCollection.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(103, 80, 164)));
    }

    private String findContainingCollectionName(java.util.List<WallpaperCollection> collections) {
        if (collections == null) return "";
        for (WallpaperCollection collection : collections) {
            if (collection == null || collection.wallpapers == null) continue;
            for (Wallpaper item : collection.wallpapers) {
                if (item != null && item.id == wallpaper.id) {
                    return collection.name == null ? "" : collection.name.trim();
                }
            }
        }
        return "";
    }

    private void downloadWallpaper() {
        btnDownloadWallpaper.setEnabled(false);
        showMessage(getString(R.string.downloading_wallpaper));

        backgroundExecutor.execute(() -> {
            try {
                Bitmap bitmap = loadCurrentBitmap();
                String displayName = "wallpaper_" + wallpaper.id + "_" + System.currentTimeMillis() + ".jpg";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WallpaperApp");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);
                }

                android.net.Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Gallery location could not be opened");

                OutputStream stream = getContentResolver().openOutputStream(uri);
                if (stream == null) throw new Exception("Image file could not be created");
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                stream.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }

                runOnUiThread(() -> showMessage(getString(R.string.wallpaper_downloaded)));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage(getString(R.string.wallpaper_download_failed, e.getMessage())));
            } finally {
                runOnUiThread(() -> btnDownloadWallpaper.setEnabled(true));
            }
        });
    }

    private void showFullscreenPreview() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView preview = new ImageView(this);
        preview.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        preview.setBackgroundColor(android.graphics.Color.BLACK);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setOnClickListener(v -> dialog.dismiss());

        if (wallpaper.hasRemoteImage()) {
            Glide.with(this).load(wallpaper.imageUrl).fitCenter().into(preview);
        } else {
            preview.setImageResource(wallpaper.imageRes);
        }

        dialog.setContentView(preview);
        dialog.show();
    }

    private void analyzeFavoriteFromDetail() {
        wallpaper.aiCategory = getString(R.string.analyzing);
        wallpaper.aiLabels = getString(R.string.checking_ai_cache);
        FirebaseFavoritesStore.saveFavorite(wallpaper);
        updateAiTexts();

        FirebaseFavoritesStore.fetchAiCache(wallpaper, (found, category, labels) -> {
            if (found) {
                wallpaper.aiCategory = category;
                wallpaper.aiLabels = labels;
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                runOnUiThread(this::updateUiSafe);
                return;
            }

            wallpaper.aiLabels = getString(R.string.gemini_analysis_in_progress);
            FirebaseFavoritesStore.saveFavorite(wallpaper);
            runOnUiThread(this::updateUiSafe);

            GeminiCategoryService.analyzeWallpaper(
                    this,
                    wallpaper,
                    WallpaperRepository.getExistingAiCategories(),
                    result -> {
                        wallpaper.aiCategory = result.category;
                        wallpaper.aiLabels = result.labelsCsv;

                        if (FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels)) {
                            FirebaseFavoritesStore.saveFavorite(wallpaper);
                            FirebaseFavoritesStore.saveAiCache(wallpaper);
                            runOnUiThread(this::updateUiSafe);
                        } else {
                            runOnDeviceFallback();
                        }
                    }
            );
        });
    }

    private void runOnDeviceFallback() {
        wallpaper.aiCategory = getString(R.string.analyzing);
        wallpaper.aiLabels = getString(R.string.using_on_device_fallback);
        FirebaseFavoritesStore.saveFavorite(wallpaper);
        runOnUiThread(this::updateUiSafe);

        AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
            @Override
            public void onSuccess(java.util.List<AiLabelData> labels) {
                wallpaper.aiCategory = DynamicCategoryGenerator.generateCategory(
                        WallpaperDetailActivity.this,
                        labels,
                        WallpaperRepository.getExistingAiCategories()
                );
                wallpaper.aiLabels = getString(
                        R.string.on_device_suffix,
                        DynamicCategoryGenerator.labelsToDisplay(WallpaperDetailActivity.this, labels)
                );
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                FirebaseFavoritesStore.saveAiCache(wallpaper);
                runOnUiThread(WallpaperDetailActivity.this::updateUiSafe);
            }

            @Override
            public void onError(Exception e) {
                wallpaper.aiCategory = getString(R.string.uncategorized);
                wallpaper.aiLabels = getString(R.string.on_device_analysis_failed);
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                runOnUiThread(WallpaperDetailActivity.this::updateUiSafe);
            }
        };

        if (wallpaper.hasRemoteImage()) {
            AiClassifier.analyzeImageUrl(this, wallpaper.imageUrl, listener);
        } else {
            AiClassifier.analyzeImage(this, wallpaper.imageRes, listener);
        }
    }

    private void bindWallpaperActions() {
        btnSetHomeWallpaper.setOnClickListener(v -> setWallpaper(false));
        btnSetLockWallpaper.setOnClickListener(v -> setWallpaper(true));
    }

    private void setWallpaper(boolean lockScreen) {
        btnSetHomeWallpaper.setEnabled(false);
        btnSetLockWallpaper.setEnabled(false);
        showMessage(getString(R.string.setting_wallpaper));

        backgroundExecutor.execute(() -> {
            try {
                Bitmap bitmap = loadCurrentBitmap();

                WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    manager.setBitmap(bitmap, null, true,
                            lockScreen ? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM);
                } else {
                    manager.setBitmap(bitmap);
                }

                runOnUiThread(() -> showMessage(getString(R.string.wallpaper_set_successfully)));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage(getString(R.string.wallpaper_set_failed, e.getMessage())));
            } finally {
                runOnUiThread(() -> {
                    btnSetHomeWallpaper.setEnabled(true);
                    btnSetLockWallpaper.setEnabled(true);
                });
            }
        });
    }

    private boolean needsAnalysis() {
        return !FirebaseFavoritesStore.isUsableAiData(wallpaper.aiCategory, wallpaper.aiLabels);
    }

    private Bitmap loadCurrentBitmap() throws Exception {
        if (wallpaper.hasRemoteImage()) {
            return Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(wallpaper.imageUrl)
                    .submit()
                    .get();
        }
        return ((BitmapDrawable) imageWallpaper.getDrawable()).getBitmap();
    }

    private void updateFavoriteIcon() {
        FavoriteButtonStyler.apply(btnFavorite, wallpaper.isFavorite);
    }

    private void updateAiTexts() {
        if (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty()) {
            txtAiCategory.setText(getString(R.string.ai_category_prefix, getString(R.string.not_analyzed_yet)));
        } else {
            wallpaper.aiCategory = WallpaperRepository.sanitizeCategoryForLabels(wallpaper.aiCategory, wallpaper.aiLabels);
            txtAiCategory.setText(getString(
                    R.string.ai_category_prefix,
                    CategoryDisplayMapper.toDisplayName(this, wallpaper.aiCategory)
            ));
        }

        if (wallpaper.aiLabels == null || wallpaper.aiLabels.isEmpty()) {
            txtAiLabels.setText(getString(R.string.ai_labels_prefix, getString(R.string.not_available)));
        } else {
            txtAiLabels.setText(getString(
                    R.string.ai_labels_prefix,
                    AiLabelDisplayMapper.toDisplayLabels(this, wallpaper.aiLabels)
            ));
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private void updateUiSafe() {
        updateAiTexts();
        updateFavoriteIcon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (collectionsListener != null) {
            collectionsListener.remove();
            collectionsListener = null;
        }
        backgroundExecutor.shutdownNow();
    }
}
