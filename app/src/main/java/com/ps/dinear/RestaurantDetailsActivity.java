package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.menu.MenuActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private int restaurantId;
    private String name;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set status bar color and light status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);
        
        setContentView(R.layout.activity_restaurant_details);

        restaurantId = getIntent().getIntExtra("restaurantId", -1);
        name = getIntent().getStringExtra("restaurantName");
        String cuisine = getIntent().getStringExtra("cuisine");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Set initial data from intent
        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
        ((TextView) findViewById(R.id.tvRestCuisineDetails)).setText(cuisine);
        ((TextView) findViewById(R.id.tvRestAddressDetails)).setText("Bharatpur, Chitwan, Nepal");

        String fullImageUrl = RetrofitClient.getFullUrl(this, imageUrl);
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        // Fetch full details (like description) from API
        fetchRestaurantDetails();

        findViewById(R.id.btnBackRestDetails).setOnClickListener(v -> finish());

        ImageView btnFavorite = findViewById(R.id.btnFavoriteRest);
        btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                btnFavorite.setImageResource(R.drawable.icon_favorite);
                btnFavorite.setColorFilter(getResources().getColor(R.color.black));
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnViewMenu).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });
    }

    private void fetchRestaurantDetails() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getRestaurantDetails(restaurantId).enqueue(new Callback<Restaurant>() {
            @Override
            public void onResponse(Call<Restaurant> call, Response<Restaurant> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Restaurant details = response.body();
                    ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(details.getDescription());
                    
                    if (details.getDeliveryTime() != null) {
                        ((TextView) findViewById(R.id.tvRestTimeDetails)).setText(details.getDeliveryTime());
                    }
                    
                    if (details.getDistance() > 0) {
                        ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText(details.getDistance() + " km");
                    }
                }
            }

            @Override
            public void onFailure(Call<Restaurant> call, Throwable t) {
                // handle error
            }
        });
    }
}
