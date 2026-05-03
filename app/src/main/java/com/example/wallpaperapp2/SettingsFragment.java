package com.example.wallpaperapp2;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private static final long SWITCH_ANIMATION_DURATION_MS = 240L;

    private View themeSwitchContainer;
    private FrameLayout themeSwitchTrack;
    private TextView themeSwitchThumb;
    private TextView txtLightModeLabel;
    private TextView txtDarkModeLabel;
    private RadioGroup radioGroupColumns;
    private RadioButton radioTwoColumns;
    private RadioButton radioThreeColumns;
    private Spinner spinnerLanguage;
    private MaterialButton btnLogout;

    private AppSettingsManager settingsManager;
    private boolean isInitializingLanguage = true;
    private boolean isThemeChanging = false;

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        themeSwitchContainer = view.findViewById(R.id.themeSwitchContainer);
        themeSwitchTrack = view.findViewById(R.id.themeSwitchTrack);
        themeSwitchThumb = view.findViewById(R.id.themeSwitchThumb);
        txtLightModeLabel = view.findViewById(R.id.txtLightModeLabel);
        txtDarkModeLabel = view.findViewById(R.id.txtDarkModeLabel);
        radioGroupColumns = view.findViewById(R.id.radioGroupColumns);
        radioTwoColumns = view.findViewById(R.id.radioTwoColumns);
        radioThreeColumns = view.findViewById(R.id.radioThreeColumns);
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        btnLogout = view.findViewById(R.id.btnLogout);

        settingsManager = new AppSettingsManager(requireContext());

        setupLanguageSpinner();
        loadSavedSettings();
        registerListeners();

        return view;
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.language_names,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }

    private void loadSavedSettings() {
        boolean darkModeEnabled = settingsManager.isDarkModeEnabled();
        themeSwitchTrack.post(() -> updateThemeSwitch(darkModeEnabled, false));

        int columnCount = settingsManager.getGridColumns();
        if (columnCount == 3) {
            radioThreeColumns.setChecked(true);
        } else {
            radioTwoColumns.setChecked(true);
        }

        String savedLanguageCode = settingsManager.getLanguageCode();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(savedLanguageCode)) {
                selectedIndex = i;
                break;
            }
        }
        spinnerLanguage.setSelection(selectedIndex);
        isInitializingLanguage = false;
    }

    private void registerListeners() {
        themeSwitchContainer.setOnClickListener(v -> applyTheme(!settingsManager.isDarkModeEnabled()));

        radioGroupColumns.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioThreeColumns) {
                settingsManager.setGridColumns(3);
            } else if (checkedId == R.id.radioTwoColumns) {
                settingsManager.setGridColumns(2);
            }
        });

        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitializingLanguage) return;

                String[] languageCodes = getResources().getStringArray(R.array.language_codes);
                if (position < 0 || position >= languageCodes.length) return;

                String selectedLanguageCode = languageCodes[position];
                if (selectedLanguageCode.equals(settingsManager.getLanguageCode())) return;

                settingsManager.setLanguageCode(selectedLanguageCode);
                AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(selectedLanguageCode)
                );
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new android.content.Intent(requireContext(), AuthActivity.class));
            requireActivity().finish();
        });
    }

    private void applyTheme(boolean darkModeEnabled) {
        if (isThemeChanging || darkModeEnabled == settingsManager.isDarkModeEnabled()) return;

        isThemeChanging = true;
        themeSwitchContainer.setEnabled(false);
        settingsManager.setDarkMode(darkModeEnabled);
        updateThemeSwitch(darkModeEnabled, true);

        themeSwitchTrack.postDelayed(() -> {
            if (!isAdded()) return;
            AppCompatDelegate.setDefaultNightMode(
                    darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        }, SWITCH_ANIMATION_DURATION_MS);
    }

    private void updateThemeSwitch(boolean darkModeEnabled, boolean animate) {
        if (themeSwitchTrack == null || themeSwitchThumb == null) return;

        int trackWidth = themeSwitchTrack.getWidth();
        int thumbWidth = themeSwitchThumb.getWidth();
        int maxTranslation = Math.max(0, trackWidth - thumbWidth - themeSwitchTrack.getPaddingStart() - themeSwitchTrack.getPaddingEnd());
        float targetTranslation = darkModeEnabled ? maxTranslation : 0f;

        themeSwitchThumb.setText(darkModeEnabled ? "🌙" : "☀️");
        txtLightModeLabel.setAlpha(darkModeEnabled ? 0.55f : 1f);
        txtDarkModeLabel.setAlpha(darkModeEnabled ? 1f : 0.55f);
        txtLightModeLabel.setTextColor(getLabelColor(!darkModeEnabled));
        txtDarkModeLabel.setTextColor(getLabelColor(darkModeEnabled));

        int startColor = getTrackColor(!darkModeEnabled);
        int endColor = getTrackColor(darkModeEnabled);

        if (animate) {
            themeSwitchThumb.animate()
                    .translationX(targetTranslation)
                    .setDuration(SWITCH_ANIMATION_DURATION_MS)
                    .start();

            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
            colorAnimator.setDuration(SWITCH_ANIMATION_DURATION_MS);
            colorAnimator.addUpdateListener(animation -> setTrackBackgroundColor((int) animation.getAnimatedValue()));
            colorAnimator.start();
        } else {
            themeSwitchThumb.setTranslationX(targetTranslation);
            setTrackBackgroundColor(endColor);
        }
    }

    private int getTrackColor(boolean darkModeEnabled) {
        return darkModeEnabled
                ? Color.rgb(30, 33, 48)
                : Color.rgb(232, 232, 232);
    }

    private int getLabelColor(boolean selected) {
        return selected
                ? ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_neutral10)
                : ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_neutral50);
    }

    private void setTrackBackgroundColor(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(26));
        drawable.setColor(color);
        themeSwitchTrack.setBackground(drawable);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
