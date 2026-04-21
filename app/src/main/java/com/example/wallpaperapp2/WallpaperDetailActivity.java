package com.example.wallpaperapp2;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WallpaperDetailActivity extends AppCompatActivity {

    private ImageView imageWallpaper;
    private TextView txtTitle;
    private TextView txtAiCategory;
    private TextView txtAiLabels;
    private ImageButton btnFavorite;

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

        int wallpaperId = getIntent().getIntExtra("wallpaper_id", -1);

        WallpaperRepository.initializeData();
        wallpaper = WallpaperRepository.getWallpaperById(wallpaperId);

        if (wallpaper != null) {
            imageWallpaper.setImageResource(wallpaper.imageRes);
            txtTitle.setText(wallpaper.title);
            updateAiTexts();
            updateFavoriteIcon();

            btnFavorite.setOnClickListener(v -> {
                wallpaper.isFavorite = !wallpaper.isFavorite;

                if (wallpaper.isFavorite && wallpaper.aiCategory.isEmpty()) {
                    AiClassifier.analyzeWallpaper(wallpaper);
                }

                updateAiTexts();
                updateFavoriteIcon();
            });
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


}