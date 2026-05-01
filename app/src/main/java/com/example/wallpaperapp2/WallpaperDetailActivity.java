package com.example.wallpaperapp2;

import android.os.Bundle;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperDetailActivity extends AppCompatActivity {

    private ImageView imageWallpaper;
    private TextView txtTitle;
    private TextView txtPhotographer;
    private TextView txtAiCategory;
    private TextView txtAiLabels;
    private ImageButton btnFavorite;
    private Button btnSetHomeWallpaper;
    private Button btnSetLockWallpaper;

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
        btnSetHomeWallpaper = findViewById(R.id.btnSetHomeWallpaper);
        btnSetLockWallpaper = findViewById(R.id.btnSetLockWallpaper);

        int wallpaperId = getIntent().getIntExtra("wallpaper_id", -1);

        WallpaperRepository.initializeData();
        wallpaper = WallpaperRepository.getWallpaperById(wallpaperId);

        if (wallpaper != null) {
            renderImage();
            txtTitle.setText("Wallpaper Preview");
            txtPhotographer.setText("Photo by " + wallpaper.title);
            updateAiTexts();
            updateFavoriteIcon();
            bindWallpaperActions();
            bindFavoriteAction();
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
            boolean aiAutoEnabled = new AppSettingsManager(this).isAiAutoCategorizeEnabled();

            if (aiAutoEnabled && needsAnalysis()) {
                analyzeFavoriteFromDetail();
            } else {
                updateAiTexts();
            }
        });
    }

    private void analyzeFavoriteFromDetail() {
        wallpaper.aiCategory = "Analyzing";
        wallpaper.aiLabels = "Checking AI cache";
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

            wallpaper.aiLabels = "Gemini analysis in progress";
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
        wallpaper.aiCategory = "Analyzing";
        wallpaper.aiLabels = "Using on-device AI fallback";
        FirebaseFavoritesStore.saveFavorite(wallpaper);
        runOnUiThread(this::updateUiSafe);

        AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
            @Override
            public void onSuccess(java.util.List<AiLabelData> labels) {
                wallpaper.aiCategory = DynamicCategoryGenerator.generateCategory(
                        labels,
                        WallpaperRepository.getExistingAiCategories()
                );
                wallpaper.aiLabels = DynamicCategoryGenerator.labelsToDisplay(labels) + " (on-device)";
                FirebaseFavoritesStore.saveFavorite(wallpaper);
                FirebaseFavoritesStore.saveAiCache(wallpaper);
                runOnUiThread(WallpaperDetailActivity.this::updateUiSafe);
            }

            @Override
            public void onError(Exception e) {
                wallpaper.aiCategory = "Uncategorized";
                wallpaper.aiLabels = "On-device analysis failed";
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
        Toast.makeText(this, "Setting wallpaper...", Toast.LENGTH_SHORT).show();

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

                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Wallpaper set successfully",
                        Toast.LENGTH_SHORT
                ).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Wallpaper set failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
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
        if (wallpaper.isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void updateAiTexts() {
        if (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty()) {
            txtAiCategory.setText("AI Category: Not analyzed yet");
        } else {
            txtAiCategory.setText("AI Category: " + wallpaper.aiCategory);
        }

        if (wallpaper.aiLabels == null || wallpaper.aiLabels.isEmpty()) {
            txtAiLabels.setText("AI Labels: Not available");
        } else {
            txtAiLabels.setText("AI Labels: " + wallpaper.aiLabels);
        }
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
