package com.example.wallpaperapp2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchDarkMode;
    private RadioGroup radioGroupColumns;
    private RadioButton radioTwoColumns;
    private RadioButton radioThreeColumns;
    private Spinner spinnerLanguage;
    private Button btnLogout;

    private AppSettingsManager settingsManager;

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
        String[] languages = {"English", "Turkish"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages
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

        String savedLanguage = settingsManager.getLanguage();
        if (savedLanguage.equals("Turkish")) {
            spinnerLanguage.setSelection(1);
        } else {
            spinnerLanguage.setSelection(0);
        }
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
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                settingsManager.setLanguage(selectedLanguage);
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
