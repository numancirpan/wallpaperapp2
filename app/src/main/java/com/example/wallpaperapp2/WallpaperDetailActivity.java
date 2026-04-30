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

public class WallpaperDetailActivity extends AppCompatActivity {

    private ImageView imageWallpaper;
    private TextView txtTitle;
    private TextView txtAiCategory;
    private TextView txtAiLabels;
    private ImageButton btnFavorite;
    private Button btnSetHomeWallpaper;
    private Button btnSetLockWallpaper;

    private Wallpaper wallpaper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_detail);

        imageWallpaper = findViewById(R.id.imageWallpaper);
        txtTitle = findViewById(R.id.txtDetailTitle);
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
            txtTitle.setText(wallpaper.title);
            updateAiTexts();
            updateFavoriteIcon();
            bindWallpaperActions();

            btnFavorite.setOnClickListener(v -> {
                wallpaper.isFavorite = !wallpaper.isFavorite;
                updateFavoriteIcon();
                if (wallpaper.isFavorite) {
                    FirebaseFavoritesStore.saveFavorite(wallpaper);
                } else {
                    FirebaseFavoritesStore.removeFavorite(wallpaper);
                }
                boolean aiAutoEnabled = new AppSettingsManager(this).isAiAutoCategorizeEnabled();

                if (aiAutoEnabled && wallpaper.isFavorite && (wallpaper.aiCategory == null || wallpaper.aiCategory.isEmpty())) {
                    txtAiCategory.setText("AI Category: Analyzing...");
                    txtAiLabels.setText("AI Labels: Processing...");

                    AiClassifier.OnLabelsReadyListener listener = new AiClassifier.OnLabelsReadyListener() {
                        @Override
                        public void onSuccess(java.util.List<AiLabelData> labels) {
                            String generatedCategory = DynamicCategoryGenerator.generateCategory(labels);
                            String finalCategory = CategoryMatcher.matchOrCreate(
                                    generatedCategory,
                                    WallpaperRepository.getExistingAiCategories()
                            );

                            wallpaper.aiLabels = DynamicCategoryGenerator.labelsToDisplay(labels);
                            GeminiCategoryService.generateCategory(wallpaper.title, wallpaper.aiLabels, geminiCategory -> {
                                String geminiMatched = CategoryMatcher.matchOrCreate(
                                        geminiCategory,
                                        WallpaperRepository.getExistingAiCategories()
                                );
                                wallpaper.aiCategory = geminiCategory == null || geminiCategory.trim().isEmpty()
                                        ? finalCategory
                                        : geminiMatched;
                                FirebaseFavoritesStore.saveFavorite(wallpaper);
                                runOnUiThread(WallpaperDetailActivity.this::updateUiSafe);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            wallpaper.aiCategory = "Uncategorized";
                            wallpaper.aiLabels = "Analysis failed";
                            FirebaseFavoritesStore.saveFavorite(wallpaper);
                            runOnUiThread(WallpaperDetailActivity.this::updateUiSafe);
                        }
                    };

                    if (wallpaper.hasRemoteImage()) {
                        AiClassifier.analyzeImageUrl(this, wallpaper.imageUrl, listener);
                    } else {
                        AiClassifier.analyzeImage(this, wallpaper.imageRes, listener);
                    }
                } else {
                    updateAiTexts();
                }
            });
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

    private void bindWallpaperActions() {
        btnSetHomeWallpaper.setOnClickListener(v -> setWallpaper(false));
        btnSetLockWallpaper.setOnClickListener(v -> setWallpaper(true));
    }

    private void setWallpaper(boolean lockScreen) {
        try {
            Bitmap bitmap;
            if (wallpaper.hasRemoteImage()) {
                bitmap = Glide.with(this).asBitmap().load(wallpaper.imageUrl).submit().get();
            } else {
                imageWallpaper.setDrawingCacheEnabled(true);
                imageWallpaper.buildDrawingCache(true);
                bitmap = ((android.graphics.drawable.BitmapDrawable) imageWallpaper.getDrawable()).getBitmap();
            }

            WallpaperManager manager = WallpaperManager.getInstance(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.setBitmap(bitmap, null, true,
                        lockScreen ? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM);
            } else {
                manager.setBitmap(bitmap);
            }
            Toast.makeText(this, "Wallpaper set successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Wallpaper set failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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


}