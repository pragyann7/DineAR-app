package com.ps.dinear;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.ps.dinear.auth.ProfileResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etEmail, etPhone;
    private ImageView ivProfile;
    private ProgressBar progressBar;
    private View btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFirstName = findViewById(R.id.etEditFirstName);
        etLastName = findViewById(R.id.etEditLastName);
        etEmail = findViewById(R.id.etEditEmail);
        etPhone = findViewById(R.id.etEditPhone);
        ivProfile = findViewById(R.id.ivEditProfileImage);
        progressBar = findViewById(R.id.pbEditProfile);
        btnSave = findViewById(R.id.btnSaveProfile);

        findViewById(R.id.btnBackEditProfile).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> updateProfile());
        findViewById(R.id.btnMoreEditProfile).setOnClickListener(this::showOverflowMenu);

        if (ivProfile != null) {
            ivProfile.setImageResource(SharedPrefManager.getUserAvatar(this));
        }

        fetchProfile();
    }

    private void fetchProfile() {
        setLoading(true);
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.getProfile(token).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();
                    etFirstName.setText(profile.getFirstName());
                    etLastName.setText(profile.getLastName());
                    etEmail.setText(profile.getEmail());
                    etPhone.setText(profile.getPhoneNumber());
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        if (etFirstName.getText() == null || etLastName.getText() == null) return;
        
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Name fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, Object> fields = new HashMap<>();
        fields.put("first_name", firstName);
        fields.put("last_name", lastName);
        fields.put("phone_number", phone);

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.updateProfile(token, fields).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();
                    SharedPrefManager.saveUserName(EditProfileActivity.this, profile.getFirstName() + " " + profile.getLastName());
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private void showOverflowMenu(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_profile_options, popup.getMenu());
        
        // Color the delete option red
        android.view.MenuItem deleteItem = popup.getMenu().findItem(R.id.menu_delete_account);
        if (deleteItem != null) {
            android.text.SpannableString s = new android.text.SpannableString(deleteItem.getTitle());
            s.setSpan(new android.text.style.ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            deleteItem.setTitle(s);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_delete_account) {
                showDeleteAccountDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteAccountDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action is permanent and cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteAccountFromServer())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void deleteAccountFromServer() {
        setLoading(true);
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.deleteAccount(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                    performLogout();
                } else {
                    String errorDetail = "Failed to delete account";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail += ": " + response.errorBody().string();
                        }
                    } catch (IOException ignored) {}
                    Toast.makeText(EditProfileActivity.this, errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        // Clear all managers
        CartManager.getInstance().clear();
        FavoritesManager.getInstance().clear();
        
        SharedPrefManager.clearAuth(this);
        
        android.content.Intent intent = new android.content.Intent(this, com.ps.dinear.auth.AuthActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
