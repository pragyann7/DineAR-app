package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.location.LocationActivity;
import com.ps.dinear.menu.MenuActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RestaurantAdapter restaurantAdapter;
    private FilterAdapter filterAdapter;
    private ProgressBar progressBar;
    private TextView tvCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SharedPrefManager.getDistrict(this) == null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        initializeRetrofit();

        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        String locationText = SharedPrefManager.getCity(this) + ", " + SharedPrefManager.getDistrict(this);
        tvCurrentLocation.setText(locationText);

        findViewById(R.id.btnChangeLocation).setOnClickListener(v -> {
            startActivity(new Intent(this, LocationActivity.class));
        });

        setupFilters();
        setupRestaurants();

        findViewById(R.id.btnARScan).setOnClickListener(v -> {
            Toast.makeText(this, "AR Scan coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupFilters() {
        RecyclerView rvFilters = findViewById(R.id.rvFilters);
        List<String> filters = new ArrayList<>();
        filters.add("Home");
        filters.add("Non Veg Restaurants");
        filters.add("Veg Restaurants");
        filters.add("Fast Food");

        filterAdapter = new FilterAdapter(this, filters);
        rvFilters.setAdapter(filterAdapter);
    }

    private void setupRestaurants() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Restaurant> mockRestaurants = new ArrayList<>();
        mockRestaurants.add(new Restaurant(1, "Qwerty mi' amor", "Monument of savory indulgence", "French", "$$$", 0.8, 4.8, "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4", "20-30 MIN"));
        mockRestaurants.add(new Restaurant(2, "The Burger Joint", "Best burgers in town", "American", "$$", 1.2, 4.5, "https://images.unsplash.com/photo-1552566626-52f8b828add9", "15-25 MIN"));

        restaurantAdapter = new RestaurantAdapter(this, mockRestaurants, restaurant -> {
            Intent intent = new Intent(this, RestaurantDetailsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantName", restaurant.getName());
            intent.putExtra("cuisine", restaurant.getCuisine());
            intent.putExtra("rating", restaurant.getRating());
            intent.putExtra("description", restaurant.getDescription());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            startActivity(intent);
        });

        recyclerView.setAdapter(restaurantAdapter);
    }

    private void initializeRetrofit() {
        String ip = SharedPrefManager.getIp(this);
        String port = SharedPrefManager.getPort(this);

        if (ip == null) {
            ip = "192.168.1.66"; // Updated to match user's backend IP
        }

        String baseUrl = "http://" + ip + ":" + port + "/";
        RetrofitClient.initialize(baseUrl);
    }
}