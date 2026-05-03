package com.example.wallpaperapp2;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private static final long THEME_ANIMATION_DELAY_MS = 350L;

    private MaterialCardView cardLightMode;
    private MaterialCardView cardDarkMode;
    private LottieAnimationView themeToggleAnimation;
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

        cardLightMode = view.findViewById(R.id.cardLightMode);
        cardDarkMode = view.findViewById(R.id.cardDarkMode);
        themeToggleAnimation = view.findViewById(R.id.themeToggleAnimation);
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
        updateThemeCards(darkModeEnabled);
        updateThemeAnimationState(darkModeEnabled);

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
        cardLightMode.setOnClickListener(v -> applyThemeWithAnimation(false));
        cardDarkMode.setOnClickListener(v -> applyThemeWithAnimation(true));
        themeToggleAnimation.setOnClickListener(v -> applyThemeWithAnimation(!settingsManager.isDarkModeEnabled()));

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

    private void applyThemeWithAnimation(boolean darkModeEnabled) {
        if (isThemeChanging || darkModeEnabled == settingsManager.isDarkModeEnabled()) return;

        isThemeChanging = true;
        settingsManager.setDarkMode(darkModeEnabled);
        updateThemeCards(darkModeEnabled);
        playThemeToggleAnimation(darkModeEnabled);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            AppCompatDelegate.setDefaultNightMode(
                    darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        }, THEME_ANIMATION_DELAY_MS);
    }

    private void playThemeToggleAnimation(boolean darkModeEnabled) {
        if (themeToggleAnimation == null) return;
        themeToggleAnimation.cancelAnimation();
        themeToggleAnimation.setSpeed(darkModeEnabled ? 1f : -1f);
        themeToggleAnimation.playAnimation();
    }

    private void updateThemeAnimationState(boolean darkModeEnabled) {
        if (themeToggleAnimation == null) return;
        themeToggleAnimation.setProgress(darkModeEnabled ? 1f : 0f);
    }

    private void updateThemeCards(boolean darkModeEnabled) {
        setThemeCardSelected(cardLightMode, !darkModeEnabled);
        setThemeCardSelected(cardDarkMode, darkModeEnabled);
    }

    private void setThemeCardSelected(MaterialCardView card, boolean selected) {
        if (card == null) return;
        int primary = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_primary80);
        int transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        card.setStrokeWidth(selected ? 4 : 1);
        card.setStrokeColor(selected ? primary : ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_neutral_variant60));
        card.setCardBackgroundColor(ColorStateList.valueOf(selected ? primary : transparent));
    }
}
