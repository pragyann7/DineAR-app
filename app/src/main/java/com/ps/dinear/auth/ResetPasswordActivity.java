package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.ApiService;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");
        if (email == null) {
            Toast.makeText(this, "Error: Email missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText etOtp = findViewById(R.id.etResetOtp);
        EditText etNewPassword = findViewById(R.id.etNewPassword);

        findViewById(R.id.btnSubmitReset).setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            String password = etNewPassword.getText().toString().trim();

            if (otp.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            resetPassword(otp, password);
        });

        findViewById(R.id.btnBackToLoginReset).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void resetPassword(String otp, String password) {
        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, password);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Password reset successfully! Please login.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "Error: Invalid OTP or session expired", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ResetPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
