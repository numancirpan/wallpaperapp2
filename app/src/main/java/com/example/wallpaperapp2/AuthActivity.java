package com.example.wallpaperapp2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText editEmail;
    private EditText editPassword;
    private Button btnAction;
    private TextView txtToggleMode;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnAction = findViewById(R.id.btnAuthAction);
        txtToggleMode = findViewById(R.id.txtToggleMode);

        updateModeUi();

        btnAction.setOnClickListener(v -> authenticate());
        txtToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateModeUi();
        });
    }

    private void updateModeUi() {
        if (isLoginMode) {
            btnAction.setText(R.string.login);
            txtToggleMode.setText(R.string.no_account_register);
        } else {
            btnAction.setText(R.string.register);
            txtToggleMode.setText(R.string.already_have_account_login);
        }
    }

    private void authenticate() {
        String email = editEmail.getText() == null ? "" : editEmail.getText().toString().trim();
        String password = editPassword.getText() == null ? "" : editPassword.getText().toString().trim();

        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, R.string.invalid_email_password, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoginMode) {
            setLoading(true);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        setLoading(false);
                        openMain();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, mapAuthError(e), Toast.LENGTH_LONG).show();
                    });
        } else {
            setLoading(true);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        setLoading(false);
                        openMain();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, mapAuthError(e), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void setLoading(boolean loading) {
        btnAction.setEnabled(!loading);
        txtToggleMode.setEnabled(!loading);
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

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
