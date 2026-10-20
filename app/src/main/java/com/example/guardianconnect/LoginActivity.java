package com.example.guardianconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity
 * -----------------------------------------------------------------------------
 * Simple login screen with no backend. For a college demo, we just validate
 * that the fields are not empty and then move on to the Dashboard.
 *
 * If you want to hard-code credentials, change the check inside btnLogin click.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Find views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // 2. Wire up the Login button
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }

        // OPTIONAL: Replace the accept-anything logic below with a hard-coded
        // credential check if your demo script requires it. Example:
        //
        //   if (!email.equals("admin@demo.com") || !password.equals("1234")) {
        //       Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        //       return;
        //   }

        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();

        // Go to the Dashboard
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish(); // Remove login from the back stack
    }
}
