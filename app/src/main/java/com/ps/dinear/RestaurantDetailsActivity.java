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
    private double latitude, longitude;

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
        String address = getIntent().getStringExtra("address");
        String city = getIntent().getStringExtra("city");
        String district = getIntent().getStringExtra("district");
        boolean isFeatured = getIntent().getBooleanExtra("isFeatured", false);
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Set all data from intent immediately (INSTANT LOAD)
        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
        ((TextView) findViewById(R.id.tvRestTimeDetails)).setText(deliveryTime != null ? deliveryTime : "20-30 min");
        ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(description);
        
        findViewById(R.id.tvPromoBadgeDetails).setVisibility(isFeatured ? View.VISIBLE : View.GONE);

        // Setup Cuisines as Chips
        com.google.android.material.chip.ChipGroup cgCuisines = findViewById(R.id.cgCuisines);
        if (cuisine != null && !cuisine.isEmpty()) {
            String[] parts = cuisine.split(", ");
            for (String part : parts) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(part);
                chip.setChipBackgroundColorResource(R.color.gray_light);
                chip.setChipStrokeWidth(0f);
                chip.setTextColor(getResources().getColor(R.color.gray_text));
                chip.setTextSize(13f);
                cgCuisines.addView(chip);
            }
        }

        // Format Address
        StringBuilder fullAddress = new StringBuilder();
        if (address != null && !address.isEmpty()) fullAddress.append(address);
        
        StringBuilder areaInfo = new StringBuilder();
        if (city != null && !city.isEmpty()) areaInfo.append(city);
        if (district != null && !district.isEmpty()) {
            if (areaInfo.length() > 0) areaInfo.append(", ");
            areaInfo.append(district);
        }

        if (fullAddress.length() > 0 && areaInfo.length() > 0) {
            fullAddress.append(", ").append(areaInfo);
        } else if (areaInfo.length() > 0) {
            fullAddress.append(areaInfo);
        }

        TextView tvAddress = findViewById(R.id.tvRestAddressDetails);
        if (fullAddress.length() > 0) {
            tvAddress.setText(fullAddress.toString());
        } else {
            tvAddress.setText("Location details unavailable");
            ((ImageView) findViewById(R.id.ivMapIcon)).setImageResource(R.drawable.icon_nolocation);
        }
        
        if (distance > 0) {
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText(String.format(Locale.US, "%.1f km", distance));
            ((ImageView) findViewById(R.id.ivDistanceIcon)).setImageResource(R.drawable.icon_walk);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText("No GPS");
            ((ImageView) findViewById(R.id.ivDistanceIcon)).setImageResource(R.drawable.icon_nolocation);
        } else {
            findViewById(R.id.llDistanceDetails).setVisibility(View.GONE);
        }

        String displayImageUrl = (bannerImage != null && !bannerImage.isEmpty()) ? bannerImage : imageUrl;
        String fullImageUrl = RetrofitClient.getFullUrl(this, displayImageUrl);
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        // Load front logo next to name
        String fullLogoUrl = RetrofitClient.getFullUrl(this, imageUrl);
        Glide.with(this).load(fullLogoUrl).into((ImageView) findViewById(R.id.ivRestLogoDetails));

        findViewById(R.id.btnBackRestDetails).setOnClickListener(v -> finish());

        findViewById(R.id.cvAddressCard).setOnClickListener(v -> {
            if (latitude != 0 && longitude != 0) {
                Intent discoverIntent = new Intent(this, DiscoverActivity.class);
                discoverIntent.putExtra("focusRestaurantId", restaurantId);
                // Flag to ensure it doesn't just keep opening new activities if already there, 
                // though from details screen we usually want to jump back or start fresh.
                discoverIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(discoverIntent);
            } else {
                Toast.makeText(this, "Coordinates not available for this restaurant", Toast.LENGTH_SHORT).show();
            }
        });

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

        findViewById(R.id.cvARExplore).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantSlug", restaurantSlug);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });

        // Add "Read more" to description if it's long
        TextView tvDescription = findViewById(R.id.tvRestDescriptionDetails);
        if (description != null && description.length() > 150) {
            String truncated = description.substring(0, 150) + "... ";
            android.text.SpannableString ss = new android.text.SpannableString(truncated + "Read more");
            android.text.style.ForegroundColorSpan fcs = new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.orange_primary));
            android.text.style.StyleSpan bold = new android.text.style.StyleSpan(android.graphics.Typeface.BOLD);
            ss.setSpan(fcs, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(bold, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvDescription.setText(ss);
            tvDescription.setOnClickListener(v -> tvDescription.setText(description));
        } else {
            tvDescription.setText(description);
        }
    }
}
