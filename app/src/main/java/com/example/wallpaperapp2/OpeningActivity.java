package com.example.wallpaperapp2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class OpeningActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView txtOpeningMessage;
    private MaterialButton btnDiscover;
    private String fullMessage;
    private int currentIndex = 0;

    private final Runnable typeWriterRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentIndex <= fullMessage.length()) {
                txtOpeningMessage.setText(fullMessage.substring(0, currentIndex));
                currentIndex++;
                handler.postDelayed(this, 45);
            } else {
                showDiscoverButton();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening);

        txtOpeningMessage = findViewById(R.id.txtOpeningMessage);
        btnDiscover = findViewById(R.id.btnDiscover);
        fullMessage = getString(R.string.opening_message);

        btnDiscover.setVisibility(View.INVISIBLE);
        btnDiscover.setOnClickListener(v -> openNextScreen());

        startIntroAnimation();
    }

    private void startIntroAnimation() {
        findViewById(R.id.imgOpeningBackground)
                .animate()
                .alpha(1f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        handler.postDelayed(typeWriterRunnable, 350);
    }

    private void showDiscoverButton() {
        btnDiscover.setVisibility(View.VISIBLE);
        btnDiscover.setTranslationY(24f);
        btnDiscover.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void openNextScreen() {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        startActivity(new Intent(this, loggedIn ? MainActivity.class : AuthActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
