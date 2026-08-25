package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.ARActivity;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

public class FoodDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_details);

        MenuItem item = (MenuItem) getIntent().getSerializableExtra("selectedItem");
        
        if (item == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvFoodNameDetails)).setText(item.getName());
        ((TextView) findViewById(R.id.tvFoodPriceDetails)).setText("₹" + item.getPrice());
        ((TextView) findViewById(R.id.tvDescriptionDetails)).setText(item.getDescription());
        ((TextView) findViewById(R.id.tvTagDetails1)).setText(item.getTag1());
        ((TextView) findViewById(R.id.tvTagDetails2)).setText(item.getTag2());

        String fullImageUrl = RetrofitClient.getFullUrl(this, item.getImageUrl());
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivFoodLarge));

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        findViewById(R.id.btnViewARDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            intent.putExtra("selectedItem", item);
            startActivity(intent);
        });
    }
}
