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
            btnAction.setText("Login");
            txtToggleMode.setText("No account? Register");
        } else {
            btnAction.setText("Register");
            txtToggleMode.setText("Already have an account? Login");
        }
    }

    private void authenticate() {
        String email = editEmail.getText() == null ? "" : editEmail.getText().toString().trim();
        String password = editPassword.getText() == null ? "" : editPassword.getText().toString().trim();

        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Enter valid email and password (min 6)", Toast.LENGTH_SHORT).show();
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
        btnAction.setText(loading ? "Please wait..." : (isLoginMode ? "Login" : "Register"));
    }

    private String mapAuthError(Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return "Network error on emulator. Check emulator internet and Google Play image.";
        }

        if (!(error instanceof FirebaseAuthException)) {
            String raw = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            return "Authentication failed: " + raw;
        }

        String code = ((FirebaseAuthException) error).getErrorCode();
        switch (code) {
            case "ERROR_INVALID_EMAIL":
                return "Invalid email format.";
            case "ERROR_USER_NOT_FOUND":
                return "User not found. Register first.";
            case "ERROR_WRONG_PASSWORD":
                return "Wrong password.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "This email is already registered.";
            case "ERROR_WEAK_PASSWORD":
                return "Password is too weak (min 6).";
            case "ERROR_OPERATION_NOT_ALLOWED":
                return "Enable Email/Password provider in Firebase Authentication.";
            case "ERROR_APP_NOT_AUTHORIZED":
            case "ERROR_INVALID_API_KEY":
            case "ERROR_CONFIG_NOT_FOUND":
                return "Firebase configuration issue. Check google-services.json and package name.";
            case "ERROR_NETWORK_REQUEST_FAILED":
                return "Network error. Check internet connection.";
            default:
                return "Authentication error: " + code;
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
