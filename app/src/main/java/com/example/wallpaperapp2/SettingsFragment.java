package com.example.wallpaperapp2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchDarkMode;
    private RadioGroup radioGroupColumns;
    private RadioButton radioTwoColumns;
    private RadioButton radioThreeColumns;
    private Spinner spinnerLanguage;
    private MaterialButton btnLogout;

    private AppSettingsManager settingsManager;
    private boolean isInitializingLanguage = true;

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchDarkMode = view.findViewById(R.id.switchDarkMode);
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
        switchDarkMode.setChecked(settingsManager.isDarkModeEnabled());

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
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setDarkMode(isChecked);

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

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
}
