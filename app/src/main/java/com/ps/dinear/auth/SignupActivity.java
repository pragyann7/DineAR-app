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

public class SignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_signup);

        EditText etFirstName = findViewById(R.id.etSignupFirstName);
        EditText etLastName = findViewById(R.id.etSignupLastName);
        EditText etEmail = findViewById(R.id.etSignupEmail);
        EditText etPassword = findViewById(R.id.etSignupPassword);

        findViewById(R.id.btnSubmitSignup).setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (firstName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            RegistrationRequest request = new RegistrationRequest(email, firstName, lastName, password);
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            
            api.register(request).enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(SignupActivity.this, "Signup successful! Please verify your email.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, VerifyOtpActivity.class);
                        intent.putExtra("email", response.body().getEmail());
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                    Toast.makeText(SignupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}