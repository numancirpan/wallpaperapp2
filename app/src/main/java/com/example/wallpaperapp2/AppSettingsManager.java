package com.example.wallpaperapp2;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingsManager {

    private static final String PREF_NAME = "wallpaper_app_settings";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_GRID_COLUMNS = "grid_columns";
    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String KEY_AI_AUTO_CATEGORIZE = "ai_auto_categorize";

    private final SharedPreferences sharedPreferences;

    public AppSettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setDarkMode(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setGridColumns(int count) {
        sharedPreferences.edit().putInt(KEY_GRID_COLUMNS, count).apply();
    }

    public int getGridColumns() {
        return sharedPreferences.getInt(KEY_GRID_COLUMNS, 2);
    }

    public void setLanguageCode(String languageCode) {
        sharedPreferences.edit().putString(KEY_LANGUAGE_CODE, languageCode).apply();
    }

    public String getLanguageCode() {
        return sharedPreferences.getString(KEY_LANGUAGE_CODE, "en");
    }

    public void setAiAutoCategorizeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AI_AUTO_CATEGORIZE, enabled).apply();
    }

    public boolean isAiAutoCategorizeEnabled() {
        return sharedPreferences.getBoolean(KEY_AI_AUTO_CATEGORIZE, true);
    }
}
