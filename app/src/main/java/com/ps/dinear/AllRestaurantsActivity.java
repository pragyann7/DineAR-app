package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllRestaurantsActivity extends AppCompatActivity {

    private RecyclerView rvAllRestaurants;
    private RestaurantAdapter adapter;
    private View shimmer;
    private View llNoResults;
    private List<Restaurant> restaurantList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Handle Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_all_restaurants);

        findViewById(R.id.btnBackAllRestaurants).setOnClickListener(v -> finish());

        rvAllRestaurants = findViewById(R.id.rvAllRestaurants);
        shimmer = findViewById(R.id.shimmerAllRestaurants);
        llNoResults = findViewById(R.id.llNoResultsAllRestaurants);

        rvAllRestaurants.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new RestaurantAdapter(this, restaurantList, restaurant -> {
            Intent intent = new Intent(this, RestaurantDetailsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantSlug", restaurant.getSlug());
            intent.putExtra("restaurantName", restaurant.getName());
            intent.putExtra("cuisine", restaurant.getCuisine());
            intent.putExtra("rating", restaurant.getRating());
            intent.putExtra("description", restaurant.getDescription());
            intent.putExtra("deliveryTime", restaurant.getDeliveryTime());
            intent.putExtra("distance", restaurant.getDistance());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            intent.putExtra("bannerImage", restaurant.getBannerImage());
            intent.putExtra("address", restaurant.getAddress());
            intent.putExtra("isFeatured", restaurant.isFeatured());
            intent.putExtra("latitude", restaurant.getLatitude());
            intent.putExtra("longitude", restaurant.getLongitude());
            intent.putExtra("deliveryCharge", restaurant.getDeliveryCharge());
            if (restaurant.getLocation() != null) {
                intent.putExtra("city", restaurant.getLocation().getCity());
                intent.putExtra("district", restaurant.getLocation().getDistrict());
            }
            startActivity(intent);
        });
        rvAllRestaurants.setAdapter(adapter);

        // Apply Insets
        View topBar = (View) findViewById(R.id.btnBackAllRestaurants).getParent();
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), statusBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        loadRestaurants();
    }

    private void loadRestaurants() {
        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
        if (rvAllRestaurants != null) rvAllRestaurants.setVisibility(View.GONE);
        if (llNoResults != null) llNoResults.setVisibility(View.GONE);

        String selectedCity = SharedPrefManager.getCity(this);
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        
        apiService.getRestaurants(selectedCity, null).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(@NonNull Call<List<Restaurant>> call, @NonNull Response<List<Restaurant>> response) {
                if (shimmer != null) shimmer.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> restaurants = response.body();
                    if (restaurants.isEmpty()) {
                        if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
                        if (rvAllRestaurants != null) rvAllRestaurants.setVisibility(View.GONE);
                    } else {
                        restaurantList.clear();
                        restaurantList.addAll(restaurants);
                        adapter.notifyDataSetChanged();
                        if (rvAllRestaurants != null) rvAllRestaurants.setVisibility(View.VISIBLE);
                        if (llNoResults != null) llNoResults.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(AllRestaurantsActivity.this, "Failed to load restaurants", Toast.LENGTH_SHORT).show();
                    if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Restaurant>> call, @NonNull Throwable t) {
                if (shimmer != null) shimmer.setVisibility(View.GONE);
                Toast.makeText(AllRestaurantsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
            }
        });
    }
}
