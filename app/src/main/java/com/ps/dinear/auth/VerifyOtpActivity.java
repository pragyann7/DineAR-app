package com.ps.dinear.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.ApiService;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {
    private String email;
    private final EditText[] otpInputs = new EditText[6];
    private TextView btnResendOtp;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

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

        TextView tvInstructions = findViewById(R.id.tvVerifyOtpInstructions);
        tvInstructions.setText("We sent a 6-digit verification code to\n" + email);

        btnResendOtp = findViewById(R.id.btnResendOtp);

        setupOtpInputs();
        otpInputs[0].requestFocus();

        findViewById(R.id.btnSubmitVerifyOtp).setOnClickListener(v -> {
            String otp = getOtpText();
            if (otp.length() != 6) {
                Toast.makeText(this, "Please enter a 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtp(otp);
        });

        btnResendOtp.setOnClickListener(v -> {
            if (!isTimerRunning) {
                resendOtp();
            }
        });

        startResendTimer();
    }

    private void startResendTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        isTimerRunning = true;
        btnResendOtp.setEnabled(false);
        btnResendOtp.setAlpha(0.5f);

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                btnResendOtp.setText(String.format(Locale.getDefault(), "Resend Code in 00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnResendOtp.setEnabled(true);
                btnResendOtp.setAlpha(1.0f);
                btnResendOtp.setText("Didn't receive code? Resend");
            }
        }.start();
    }

    private void setupOtpInputs() {
        otpInputs[0] = findViewById(R.id.etOtp1);
        otpInputs[1] = findViewById(R.id.etOtp2);
        otpInputs[2] = findViewById(R.id.etOtp3);
        otpInputs[3] = findViewById(R.id.etOtp4);
        otpInputs[4] = findViewById(R.id.etOtp5);
        otpInputs[5] = findViewById(R.id.etOtp6);

        for (int i = 0; i < 6; i++) {
            final int index = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < 5) {
                        otpInputs[index + 1].requestFocus();
                    }
                }
            });

            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpInputs[index].getText().length() == 0 && index > 0) {
                        otpInputs[index - 1].requestFocus();
                        otpInputs[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getOtpText() {
        StringBuilder sb = new StringBuilder();
        for (EditText input : otpInputs) {
            sb.append(input.getText().toString());
        }
        return sb.toString();
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
                    startResendTimer();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
