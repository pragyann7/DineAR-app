package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.ARActivity;
import com.ps.dinear.R;

public class FoodDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_details);

        String name = getIntent().getStringExtra("foodName");
        int price = getIntent().getIntExtra("foodPrice", 0);
        String desc = getIntent().getStringExtra("foodDesc");
        String imageUrl = getIntent().getStringExtra("foodImage");
        String modelUrl = getIntent().getStringExtra("modelUrl");
        String modelName = getIntent().getStringExtra("modelName");
        String modelVersion = getIntent().getStringExtra("modelVersion");
        java.util.ArrayList<com.ps.dinear.MenuItem> menuList = (java.util.ArrayList<com.ps.dinear.MenuItem>) getIntent().getSerializableExtra("menuList");
        
        String tag1 = getIntent().getStringExtra("tag1");
        String tag2 = getIntent().getStringExtra("tag2");

        ((TextView) findViewById(R.id.tvFoodNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvFoodPriceDetails)).setText("$" + price + ".00");
        ((TextView) findViewById(R.id.tvDescriptionDetails)).setText(desc);
        ((TextView) findViewById(R.id.tvTagDetails1)).setText(tag1);
        ((TextView) findViewById(R.id.tvTagDetails2)).setText(tag2);

        Glide.with(this).load(imageUrl).into((ImageView) findViewById(R.id.ivFoodLarge));

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        findViewById(R.id.btnViewARDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            intent.putExtra("modelUrl", modelUrl);
            intent.putExtra("modelName", modelName);
            intent.putExtra("modelVersion", modelVersion);
            intent.putExtra("menuList", menuList);
            startActivity(intent);
        });
    }
}