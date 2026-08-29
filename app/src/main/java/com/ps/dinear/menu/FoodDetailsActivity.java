package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.ARActivity;
import com.ps.dinear.CartManager;
import com.ps.dinear.FavoritesManager;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

public class FoodDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        super.setContentView(R.layout.activity_food_details);

        MenuItem item = (MenuItem) getIntent().getSerializableExtra("selectedItem");
        
        if (item == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvFoodNameDetails)).setText(item.getName());
        ((TextView) findViewById(R.id.tvFoodPriceDetails)).setText("Rs. " + (int)item.getPrice());
        
        TextView tvResName = findViewById(R.id.tvRestaurantNameDetails);
        if (item.getRestaurantName() != null && !item.getRestaurantName().isEmpty()) {
            tvResName.setText(item.getRestaurantName());
            tvResName.setVisibility(View.VISIBLE);
        } else {
            tvResName.setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.tvDescriptionDetails)).setText(item.getDescription());
        ((TextView) findViewById(R.id.tvTagDetails1)).setText(item.getTag1());
        ((TextView) findViewById(R.id.tvTagDetails2)).setText(item.getTag2());

        String fullImageUrl = RetrofitClient.getFullUrl(this, item.getImageUrl());
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivFoodLarge));

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        ImageView btnFavorite = findViewById(R.id.btnFavoriteFoodDetails);
        if (FavoritesManager.getInstance().isFoodFavorite(item.getId())) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
        }

        btnFavorite.setOnClickListener(v -> {
            FavoritesManager.getInstance().toggleFoodFavorite(this, item.getId(), new FavoritesManager.ToggleCallback() {
                @Override
                public void onStateChanged(boolean isFavorite) {
                    if (isFavorite) {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                        btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
                    } else {
                        btnFavorite.setImageResource(R.drawable.icon_favorite);
                        btnFavorite.setColorFilter(getResources().getColor(R.color.black));
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(FoodDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnShareFood).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this food: " + item.getName());
            intent.putExtra(Intent.EXTRA_TEXT, "Check out this food on DineAR: " + item.getName() + "\n" + item.getDescription());
            startActivity(Intent.createChooser(intent, "Share via"));
        });

        findViewById(R.id.btnViewARDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            intent.putExtra("selectedItem", item);
            intent.putExtra("restaurantSlug", getIntent().getStringExtra("restaurantSlug"));
            startActivity(intent);
        });

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            int restaurantId = getIntent().getIntExtra("restaurantId", -1);
            if (restaurantId != -1) {
                CartManager.getInstance().addItem(item, restaurantId);
                Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Restaurant ID missing", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
