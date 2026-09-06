package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.ApiService;
import com.ps.dinear.MainActivity;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etLoginEmail);
        EditText etPassword = findViewById(R.id.etLoginPassword);

        findViewById(R.id.btnSubmitLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(email, password);
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

            api.login(request).enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        TokenResponse tokens = response.body();
                        SharedPrefManager.saveTokens(LoginActivity.this, tokens.getAccess(), tokens.getRefresh());
                        SharedPrefManager.saveUserInfo(LoginActivity.this, tokens.getFullName(), email);
                        SharedPrefManager.setIsLoggedIn(LoginActivity.this, true);
                        SharedPrefManager.setOnboardingFinished(LoginActivity.this, true);
                        
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnGoToSignup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });

        findViewById(R.id.btnForgotPassword).setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }
}