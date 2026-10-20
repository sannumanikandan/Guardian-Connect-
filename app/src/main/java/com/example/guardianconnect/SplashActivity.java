package com.example.guardianconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity
 * -----------------------------------------------------------------------------
 * Shows the Guardian Connect logo for 2 seconds, then launches LoginActivity.
 * This is the LAUNCHER activity (see AndroidManifest.xml).
 */
public class SplashActivity extends AppCompatActivity {

    // How long to show the splash screen (in milliseconds).
    private static final long SPLASH_DURATION_MS = 2000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // After SPLASH_DURATION_MS, move to the Login screen.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            // finish() so the user cannot press Back and return to the splash.
            finish();
        }, SPLASH_DURATION_MS);
    }
}
