package com.example.wallpaperapp2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private AuthSessionManager sessionManager;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private MaterialCheckBox checkRememberMe;
    private MaterialButton btnAction;
    private TextView txtToggleMode;
    private TextView txtAuthSubtitle;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        sessionManager = new AuthSessionManager(this);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        checkRememberMe = findViewById(R.id.checkRememberMe);
        btnAction = findViewById(R.id.btnAuthAction);
        txtToggleMode = findViewById(R.id.txtToggleMode);
        txtAuthSubtitle = findViewById(R.id.txtAuthSubtitle);

        checkRememberMe.setChecked(sessionManager.isRememberMeEnabled());
        updateModeUi();

        btnAction.setOnClickListener(v -> authenticate());
        txtToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            clearInputErrors();
            updateModeUi();
        });
    }

    private void updateModeUi() {
        if (isLoginMode) {
            btnAction.setText(R.string.login);
            txtToggleMode.setText(R.string.no_account_register);
            txtAuthSubtitle.setText(R.string.auth_subtitle_login);
            checkRememberMe.setVisibility(android.view.View.VISIBLE);
        } else {
            btnAction.setText(R.string.register);
            txtToggleMode.setText(R.string.already_have_account_login);
            txtAuthSubtitle.setText(R.string.auth_subtitle_register);
            checkRememberMe.setVisibility(android.view.View.GONE);
        }
    }

    private void authenticate() {
        clearInputErrors();

        String email = editEmail.getText() == null ? "" : editEmail.getText().toString().trim();
        String password = editPassword.getText() == null ? "" : editPassword.getText().toString().trim();

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.invalid_email_format));
            return;
        }

        if (password.length() < 6) {
            layoutPassword.setError(getString(R.string.weak_password));
            return;
        }

        setLoading(true);
        if (isLoginMode) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        sessionManager.setRememberMe(checkRememberMe.isChecked());
                        setLoading(false);
                        openMain();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showMessage(mapAuthError(e));
                    });
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        sessionManager.setRememberMe(true);
                        setLoading(false);
                        openMain();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showMessage(mapAuthError(e));
                    });
        }
    }

    private void clearInputErrors() {
        if (layoutEmail != null) layoutEmail.setError(null);
        if (layoutPassword != null) layoutPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnAction.setEnabled(!loading);
        txtToggleMode.setEnabled(!loading);
        editEmail.setEnabled(!loading);
        editPassword.setEnabled(!loading);
        checkRememberMe.setEnabled(!loading);
        if (loading) {
            btnAction.setText(R.string.please_wait);
        } else {
            btnAction.setText(isLoginMode ? R.string.login : R.string.register);
        }
    }

    private String mapAuthError(Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return getString(R.string.network_error_emulator);
        }

        if (!(error instanceof FirebaseAuthException)) {
            String raw = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            return getString(R.string.auth_failed, raw);
        }

        String code = ((FirebaseAuthException) error).getErrorCode();
        switch (code) {
            case "ERROR_INVALID_EMAIL":
                return getString(R.string.invalid_email_format);
            case "ERROR_USER_NOT_FOUND":
                return getString(R.string.user_not_found);
            case "ERROR_WRONG_PASSWORD":
                return getString(R.string.wrong_password);
            case "ERROR_INVALID_CREDENTIAL":
                return getString(R.string.invalid_login_credentials);
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return getString(R.string.email_already_registered);
            case "ERROR_WEAK_PASSWORD":
                return getString(R.string.weak_password);
            case "ERROR_OPERATION_NOT_ALLOWED":
                return getString(R.string.enable_email_provider);
            case "ERROR_APP_NOT_AUTHORIZED":
            case "ERROR_INVALID_API_KEY":
            case "ERROR_CONFIG_NOT_FOUND":
                return getString(R.string.firebase_config_issue);
            case "ERROR_NETWORK_REQUEST_FAILED":
                return getString(R.string.network_error);
            default:
                return getString(R.string.auth_error_code, code);
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
