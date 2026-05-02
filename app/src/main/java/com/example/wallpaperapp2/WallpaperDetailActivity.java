package com.example.wallpaperapp2;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

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
    private MaterialButton btnSetHomeWallpaper;
    private MaterialButton btnSetLockWallpaper;

    private Wallpaper wallpaper;
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
                wallpaper.aiCategory = "";
                wallpaper.aiLabels = "";
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
                Bitmap bitmap;
                if (wallpaper.hasRemoteImage()) {
                    bitmap = Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(wallpaper.imageUrl)
                            .submit()
                            .get();
                } else {
                    bitmap = ((android.graphics.drawable.BitmapDrawable) imageWallpaper.getDrawable()).getBitmap();
                }

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

    private void updateFavoriteIcon() {
        FavoriteButtonStyler.apply(btnFavorite, wallpaper.isFavorite);
    }

    private void updateAiTexts() {
        if (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty()) {
            txtAiCategory.setText(getString(R.string.ai_category_prefix, getString(R.string.not_analyzed_yet)));
        } else {
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
        backgroundExecutor.shutdownNow();
    }
}
