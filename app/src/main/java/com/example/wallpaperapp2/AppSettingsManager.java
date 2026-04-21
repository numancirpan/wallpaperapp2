package com.example.wallpaperapp2;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingsManager {

    private static final String PREF_NAME = "wallpaper_app_settings";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_AI_AUTO = "ai_auto";
    private static final String KEY_GRID_COLUMNS = "grid_columns";
    private static final String KEY_LANGUAGE = "language";

    private SharedPreferences sharedPreferences;

    public AppSettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setDarkMode(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setAiAutoCategorize(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AI_AUTO, enabled).apply();
    }

    public boolean isAiAutoCategorizeEnabled() {
        return sharedPreferences.getBoolean(KEY_AI_AUTO, true);
    }

    public void setGridColumns(int count) {
        sharedPreferences.edit().putInt(KEY_GRID_COLUMNS, count).apply();
    }

    public int getGridColumns() {
        return sharedPreferences.getInt(KEY_GRID_COLUMNS, 2);
    }

    public void setLanguage(String language) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public String getLanguage() {
        return sharedPreferences.getString(KEY_LANGUAGE, "English");
    }
}