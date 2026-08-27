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

        EditText etUsername = findViewById(R.id.etLoginUsername);
        EditText etPassword = findViewById(R.id.etLoginPassword);

        findViewById(R.id.btnSubmitLogin).setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(username, password);
            ApiService api = RetrofitClient.getClient().create(ApiService.class);

            api.login(request).enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        TokenResponse tokens = response.body();
                        SharedPrefManager.saveTokens(LoginActivity.this, tokens.getAccess(), tokens.getRefresh());
                        SharedPrefManager.saveUserInfo(LoginActivity.this, username, username + "@example.com"); // Email as placeholder or if returned
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
    }
}