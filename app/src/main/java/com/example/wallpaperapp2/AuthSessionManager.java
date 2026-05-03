package com.example.wallpaperapp2;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthSessionManager {

    private static final String PREF_NAME = "auth_session_preferences";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences preferences;

    public AuthSessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setRememberMe(boolean rememberMe) {
        preferences.edit().putBoolean(KEY_REMEMBER_ME, rememberMe).apply();
    }

    public boolean isRememberMeEnabled() {
        return preferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    public void clearRememberMe() {
        preferences.edit().putBoolean(KEY_REMEMBER_ME, false).apply();
    }
}
