package com.ps.dinear;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WriteReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        findViewById(R.id.btnBackWriteReview).setOnClickListener(v -> finish());

        findViewById(R.id.btnSubmitReview).setOnClickListener(v -> {
            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}