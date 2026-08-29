package com.ps.dinear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ps.dinear.auth.AuthActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_profile);

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);

        tvName.setText(SharedPrefManager.getUserName(this));
        tvEmail.setText(SharedPrefManager.getUserEmail(this));

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        findViewById(R.id.btnProfileOrders).setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersActivity.class));
        });

        findViewById(R.id.btnProfileRegisterRestaurant).setOnClickListener(v -> {
            String url = RetrofitClient.getBaseUrl() + "dashboard/register/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        findViewById(R.id.btnProfileServerSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, ServerConfigActivity.class));
        });

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            SharedPrefManager.clearAuth(this);
            SharedPrefManager.setIsLoggedIn(this, false);
            SharedPrefManager.setIsGuest(this, false);
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}