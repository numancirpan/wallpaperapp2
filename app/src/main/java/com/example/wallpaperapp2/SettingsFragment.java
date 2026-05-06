package com.example.wallpaperapp2;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

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
    private MaterialButton btnChangePassword;
    private MaterialButton btnDeleteAccount;
    private MaterialButton btnLogout;
    private ValueAnimator trackColorAnimator;

    private AppSettingsManager settingsManager;
    private boolean isInitializingLanguage = true;
    private boolean isThemeChanging = false;

    private final Runnable applyPendingThemeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || getActivity() == null || settingsManager == null) return;

            boolean darkModeEnabled = settingsManager.isDarkModeEnabled();
            int targetMode = darkModeEnabled
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;

            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
            }
        }
    };

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
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnLogout = view.findViewById(R.id.btnLogout);

        settingsManager = new AppSettingsManager(requireContext());

        setupLanguageSpinner();
        loadSavedSettings();
        registerListeners();

        return view;
    }

    @Override
    public void onDestroyView() {
        if (themeSwitchTrack != null) {
            themeSwitchTrack.removeCallbacks(applyPendingThemeRunnable);
        }
        if (themeSwitchThumb != null) {
            themeSwitchThumb.animate().cancel();
        }
        if (trackColorAnimator != null) {
            trackColorAnimator.cancel();
            trackColorAnimator = null;
        }
        super.onDestroyView();
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
        if (themeSwitchTrack != null) {
            themeSwitchTrack.post(() -> {
                if (!isAdded()) return;
                updateThemeSwitch(darkModeEnabled, false);
            });
        }

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

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null, false);
        TextInputEditText oldPassword = dialogView.findViewById(R.id.editOldPassword);
        TextInputEditText newPassword = dialogView.findViewById(R.id.editNewPassword);
        TextInputEditText confirmPassword = dialogView.findViewById(R.id.editConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_password, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass = getText(oldPassword);
            String newPass = getText(newPassword);
            String confirmPass = getText(confirmPassword);

            if (oldPass.length() < 6) {
                oldPassword.setError(getString(R.string.weak_password));
                return;
            }
            if (newPass.length() < 6) {
                newPassword.setError(getString(R.string.weak_password));
                return;
            }
            if (oldPass.equals(newPass)) {
                newPassword.setError(getString(R.string.new_password_same_as_old));
                return;
            }
            if (!newPass.equals(confirmPass)) {
                confirmPassword.setError(getString(R.string.passwords_do_not_match));
                return;
            }

            changePassword(oldPass, newPass, dialog);
        }));

        dialog.show();
    }

    private void changePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null ? "" : user.getEmail();
        if (user == null || email == null || email.trim().isEmpty()) return;

        final boolean[] completed = {false};
        user.reauthenticate(EmailAuthProvider.getCredential(email, oldPassword))
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(update -> {
                            completed[0] = true;
                            showMessage(getString(R.string.password_changed));
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            if (!completed[0]) {
                                showMessage(mapPasswordChangeError(e));
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (!completed[0]) {
                        showMessage(mapPasswordChangeError(e));
                    }
                });
    }

    private String mapPasswordChangeError(Exception error) {
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_INVALID_CREDENTIAL".equals(code) || "ERROR_WRONG_PASSWORD".equals(code)) {
                return getString(R.string.current_password_incorrect);
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return getString(R.string.weak_password);
            }
            if ("ERROR_REQUIRES_RECENT_LOGIN".equals(code)) {
                return getString(R.string.recent_login_required);
            }
        }
        return getString(R.string.password_change_failed_generic);
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(R.string.delete_account_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        btnDeleteAccount.setEnabled(false);
        user.delete()
                .addOnSuccessListener(unused -> {
                    showMessage(getString(R.string.account_deleted));
                    clearUserScopedCaches();
                    startActivity(new Intent(requireContext(), AuthActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    btnDeleteAccount.setEnabled(true);
                    showMessage(getString(R.string.delete_account_failed, e.getMessage()));
                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        clearUserScopedCaches();
        startActivity(new Intent(requireContext(), AuthActivity.class));
        requireActivity().finish();
    }

    private void clearUserScopedCaches() {
        WallpaperRepository.clearUserState();
        UserProfileStore.clearUserState();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void applyTheme(boolean darkModeEnabled) {
        if (isThemeChanging || darkModeEnabled == settingsManager.isDarkModeEnabled()) return;

        isThemeChanging = true;
        themeSwitchContainer.setEnabled(false);
        settingsManager.setDarkMode(darkModeEnabled);
        updateThemeSwitch(darkModeEnabled, true);

        themeSwitchTrack.removeCallbacks(applyPendingThemeRunnable);
        themeSwitchTrack.postDelayed(applyPendingThemeRunnable, SWITCH_ANIMATION_DURATION_MS + 80L);
    }

    private void updateThemeSwitch(boolean darkModeEnabled, boolean animate) {
        if (!isAdded() || themeSwitchTrack == null || themeSwitchThumb == null) return;

        int trackWidth = themeSwitchTrack.getWidth();
        int thumbWidth = themeSwitchThumb.getWidth();
        int maxTranslation = Math.max(0, trackWidth - thumbWidth - themeSwitchTrack.getPaddingStart() - themeSwitchTrack.getPaddingEnd());
        float targetTranslation = darkModeEnabled ? maxTranslation : 0f;

        themeSwitchThumb.setText(darkModeEnabled ? "🌙" : "☀️");
        txtLightModeLabel.setAlpha(darkModeEnabled ? 0.55f : 1f);
        txtDarkModeLabel.setAlpha(darkModeEnabled ? 1f : 0.55f);
        txtLightModeLabel.setTextColor(getLabelColor(!darkModeEnabled));
        txtDarkModeLabel.setTextColor(getLabelColor(darkModeEnabled));

        int currentColor = getTrackColor(!darkModeEnabled);
        int targetColor = getTrackColor(darkModeEnabled);

        if (trackColorAnimator != null) {
            trackColorAnimator.cancel();
            trackColorAnimator = null;
        }

        if (animate) {
            themeSwitchThumb.animate()
                    .translationX(targetTranslation)
                    .setDuration(SWITCH_ANIMATION_DURATION_MS)
                    .start();

            trackColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
            trackColorAnimator.setDuration(SWITCH_ANIMATION_DURATION_MS);
            trackColorAnimator.addUpdateListener(animation -> setTrackBackgroundColor((int) animation.getAnimatedValue()));
            trackColorAnimator.start();
        } else {
            themeSwitchThumb.setTranslationX(targetTranslation);
            setTrackBackgroundColor(targetColor);
        }
    }

    private int getTrackColor(boolean darkModeEnabled) {
        return darkModeEnabled
                ? Color.rgb(30, 33, 48)
                : Color.rgb(232, 232, 232);
    }

    private int getLabelColor(boolean selected) {
        if (!isAdded()) return Color.GRAY;
        return selected
                ? ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_neutral10)
                : ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_neutral50);
    }

    private void setTrackBackgroundColor(int color) {
        if (themeSwitchTrack == null) return;
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
