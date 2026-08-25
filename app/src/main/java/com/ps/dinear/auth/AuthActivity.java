package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.MainActivity;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.ServerConfigActivity;
import com.ps.dinear.SharedPrefManager;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already authenticated or guest
        if (SharedPrefManager.isLoggedIn(this) || SharedPrefManager.isGuest(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);

        findViewById(R.id.btnServerSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, ServerConfigActivity.class));
        });

        findViewById(R.id.btnSignup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        findViewById(R.id.btnGuest).setOnClickListener(v -> {
            SharedPrefManager.setIsGuest(this, true);
            SharedPrefManager.setOnboardingFinished(this, true);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
