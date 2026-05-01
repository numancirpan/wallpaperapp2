package com.example.wallpaperapp2;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class FavoriteButtonStyler {

    public static void apply(MaterialButton button, boolean isFavorite) {
        if (button == null) return;

        Context context = button.getContext();
        int backgroundColor = ContextCompat.getColor(
                context,
                isFavorite ? R.color.favorite_active_bg : R.color.favorite_inactive_bg
        );
        int iconColor = ContextCompat.getColor(
                context,
                isFavorite ? R.color.favorite_active_icon : R.color.favorite_inactive_icon
        );

        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setIconTint(ColorStateList.valueOf(iconColor));
        button.setIconResource(isFavorite
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
    }
}
