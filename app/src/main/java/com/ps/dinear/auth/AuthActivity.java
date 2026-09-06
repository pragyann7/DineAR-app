package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.ps.dinear.MainActivity;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.ServerConfigActivity;
import com.ps.dinear.SharedPrefManager;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Check if user is already authenticated or guest
        if (SharedPrefManager.isLoggedIn(this) || SharedPrefManager.isGuest(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        super.setContentView(R.layout.activity_auth);

        // Handle navigation bar insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBars.bottom);
            return insets;
        });

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
