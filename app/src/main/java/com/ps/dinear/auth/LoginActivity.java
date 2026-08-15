package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.ps.dinear.MainActivity;
import com.ps.dinear.R;
import com.ps.dinear.SharedPrefManager;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.btnSubmitLogin).setOnClickListener(v -> {
            SharedPrefManager.setIsLoggedIn(this, true);
            SharedPrefManager.setOnboardingFinished(this, true);
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });
    }
}