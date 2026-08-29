package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.ApiService;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: Email not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText etOtp = findViewById(R.id.etVerifyOtp);
        TextView tvInstructions = findViewById(R.id.tvVerifyOtpInstructions);
        tvInstructions.setText("An OTP has been sent to " + email + ". Please enter it below to verify your account.");

        findViewById(R.id.btnSubmitVerifyOtp).setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() != 6) {
                Toast.makeText(this, "Please enter a 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtp(otp);
        });

        findViewById(R.id.btnResendOtp).setOnClickListener(v -> resendOtp());
    }

    private void verifyOtp(String otp) {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.verifyEmailOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOtpActivity.this, "Email verified successfully! Please login.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(VerifyOtpActivity.this, LoginActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Verification failed: Invalid or expired OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(VerifyOtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendOtp() {
        ResendOtpRequest request = new ResendOtpRequest(email);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.resendEmailOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOtpActivity.this, "OTP resent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Failed to resend OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(VerifyOtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
