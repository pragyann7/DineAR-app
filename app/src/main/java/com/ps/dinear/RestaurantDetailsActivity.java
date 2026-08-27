package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.menu.MenuActivity;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private int restaurantId;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set status bar color and light status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);
        
        super.setContentView(R.layout.activity_restaurant_details);

        restaurantId = getIntent().getIntExtra("restaurantId", -1);
        name = getIntent().getStringExtra("restaurantName");
        String cuisine = getIntent().getStringExtra("cuisine");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String description = getIntent().getStringExtra("description");
        String deliveryTime = getIntent().getStringExtra("deliveryTime");
        double distance = getIntent().getDoubleExtra("distance", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Set all data from intent immediately (INSTANT LOAD)
        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
        ((TextView) findViewById(R.id.tvRestCuisineDetails)).setText(cuisine);
        ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(description);
        ((TextView) findViewById(R.id.tvRestAddressDetails)).setText("Bharatpur, Chitwan, Nepal");
        
        if (deliveryTime != null) {
            ((TextView) findViewById(R.id.tvRestTimeDetails)).setText(deliveryTime);
        }
        
        if (distance > 0) {
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText(distance + " km");
        }

        String fullImageUrl = RetrofitClient.getFullUrl(this, imageUrl);
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        findViewById(R.id.btnBackRestDetails).setOnClickListener(v -> finish());

        ImageView btnFavorite = findViewById(R.id.btnFavoriteRest);
        
        // Initial state
        if (FavoritesManager.getInstance().isRestaurantFavorite(restaurantId)) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
        }

        btnFavorite.setOnClickListener(v -> {
            FavoritesManager.getInstance().toggleRestaurantFavorite(this, restaurantId, new FavoritesManager.ToggleCallback() {
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
                    Toast.makeText(RestaurantDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnViewMenu).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });
    }
}
