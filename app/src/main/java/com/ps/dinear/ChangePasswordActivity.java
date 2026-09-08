package com.ps.dinear;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etCurrent, etNew, etConfirm;
    private ProgressBar progressBar;
    private View btnUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etCurrent = findViewById(R.id.etCurrentPassword);
        etNew = findViewById(R.id.etNewPassword);
        etConfirm = findViewById(R.id.etConfirmNewPassword);
        progressBar = findViewById(R.id.pbChangePassword);
        btnUpdate = findViewById(R.id.btnUpdatePassword);

        findViewById(R.id.btnBackChangePassword).setOnClickListener(v -> finish());
        btnUpdate.setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String current = etCurrent.getText().toString().trim();
        String newPass = etNew.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (current.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, String> data = new HashMap<>();
        data.put("current_password", current);
        data.put("new_password", newPass);

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.changePassword(token, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorDetail = "Failed to update password";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail += ": " + response.errorBody().string();
                        }
                    } catch (IOException ignored) {}
                    Toast.makeText(ChangePasswordActivity.this, errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnUpdate.setEnabled(!loading);
    }
}
