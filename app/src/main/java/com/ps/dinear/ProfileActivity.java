package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ps.dinear.auth.AuthActivity;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private int tempSelectedAvatar = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_profile);

        // Header Info
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);
        ImageView ivProfile = findViewById(R.id.ivProfileImage);
        
        ivProfile.setImageResource(SharedPrefManager.getUserAvatar(this));

        String name = SharedPrefManager.getUserName(this);
        String email = SharedPrefManager.getUserEmail(this);
        
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Guest")) {
            // Fallback: If name is missing, use email prefix
            if (email != null && !email.isEmpty() && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "User";
            }
        }
        
        tvName.setText(name);
        tvEmail.setText(email);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());
        
        // Avatar Change Trigger
        View.OnClickListener avatarTrigger = v -> showChangeAvatarSheet(ivProfile);
        ivProfile.setOnClickListener(avatarTrigger);
        findViewById(R.id.llProfileHeader).setOnClickListener(avatarTrigger);

        // Menu Actions
        findViewById(R.id.btnPersonalInfo).setOnClickListener(v -> {
            Toast.makeText(this, "Personal Information coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnProfileOrders).setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersActivity.class));
        });

        findViewById(R.id.btnAddresses).setOnClickListener(v -> {
            Toast.makeText(this, "Addresses coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPaymentMethods).setOnClickListener(v -> {
            Toast.makeText(this, "Payment Methods coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, ServerConfigActivity.class));
        });

        findViewById(R.id.btnHelpSupport).setOnClickListener(v -> {
            Toast.makeText(this, "Help & Support coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_alert, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v1 -> dialog.dismiss());
            dialogView.findViewById(R.id.btnDialogConfirm).setOnClickListener(v1 -> {
                dialog.dismiss();
                performLogout();
            });

            dialog.show();
        });
    }

    private void performLogout() {
        SharedPrefManager.clearAuth(this);
        SharedPrefManager.setIsLoggedIn(this, false);
        SharedPrefManager.setIsGuest(this, false);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showChangeAvatarSheet(ImageView ivProfile) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_change_avatar, null);
        dialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rvAvatarList);
        List<Integer> avatarList = new ArrayList<>();
        // Add WebP avatars from drawable
        avatarList.add(R.drawable.avatar_0);
        avatarList.add(R.drawable.avatar_1);
        avatarList.add(R.drawable.avatar_2);
        avatarList.add(R.drawable.avatar_3);
        avatarList.add(R.drawable.avatar_4);
        avatarList.add(R.drawable.avatar_5);

        int currentAvatar = SharedPrefManager.getUserAvatar(this);
        tempSelectedAvatar = currentAvatar;

        AvatarAdapter adapter = new AvatarAdapter(this, avatarList, currentAvatar, avatarResId -> {
            tempSelectedAvatar = avatarResId;
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnCloseAvatarSheet).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSaveAvatar).setOnClickListener(v -> {
            SharedPrefManager.saveUserAvatar(this, tempSelectedAvatar);
            ivProfile.setImageResource(tempSelectedAvatar);
            Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
