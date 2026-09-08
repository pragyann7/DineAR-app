package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.util.ARStorageManager;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvCacheSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvCacheSize = findViewById(R.id.tvArCacheSize);

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnClearArCache).setOnClickListener(v -> showClearCacheDialog());
        
        findViewById(R.id.btnServerConfig).setOnClickListener(v -> {
            startActivity(new Intent(this, ServerConfigActivity.class));
        });

        updateCacheSize();
    }

    private void updateCacheSize() {
        long size = ARStorageManager.INSTANCE.getCacheSize(this);
        tvCacheSize.setText("Usage: " + ARStorageManager.INSTANCE.formatSize(size));
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear 3D Models")
                .setMessage("This will delete all downloaded 3D dish models from your device. You will need to download them again when you view them in AR.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    ARStorageManager.INSTANCE.clearCache(this);
                    updateCacheSize();
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCacheSize();
    }
}
