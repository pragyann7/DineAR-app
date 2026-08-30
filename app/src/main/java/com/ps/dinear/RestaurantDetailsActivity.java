package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.ps.dinear.menu.MenuActivity;

import java.util.Locale;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private int restaurantId;
    private String restaurantSlug;
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
        restaurantSlug = getIntent().getStringExtra("restaurantSlug");
        name = getIntent().getStringExtra("restaurantName");
        String cuisine = getIntent().getStringExtra("cuisine");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String description = getIntent().getStringExtra("description");
        String deliveryTime = getIntent().getStringExtra("deliveryTime");
        double distance = getIntent().getDoubleExtra("distance", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String bannerImage = getIntent().getStringExtra("bannerImage");

        // Set all data from intent immediately (INSTANT LOAD)
        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
        ((TextView) findViewById(R.id.tvRestCuisineDetails)).setText(cuisine);
        ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(description);
        ((TextView) findViewById(R.id.tvRestAddressDetails)).setText("Bharatpur, Chitwan, Nepal");
        
        if (deliveryTime != null) {
            ((TextView) findViewById(R.id.tvRestTimeDetails)).setText(deliveryTime);
        }
        
        View distanceTag = (View) findViewById(R.id.tvRestDistanceDetails).getParent();
        if (distance > 0) {
            distanceTag.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText(String.format(Locale.US, "%.1f km", distance));
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            distanceTag.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText("No GPS");
        } else {
            distanceTag.setVisibility(View.GONE);
        }

        String displayImageUrl = (bannerImage != null && !bannerImage.isEmpty()) ? bannerImage : imageUrl;
        String fullImageUrl = RetrofitClient.getFullUrl(this, displayImageUrl);
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        // Load front icon (logo) next to name
        String fullLogoUrl = RetrofitClient.getFullUrl(this, imageUrl);
        Glide.with(this).load(fullLogoUrl).into((ImageView) findViewById(R.id.ivRestLogoDetails));

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
            intent.putExtra("restaurantSlug", restaurantSlug);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });
    }
}
