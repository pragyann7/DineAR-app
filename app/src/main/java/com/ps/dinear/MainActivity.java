package com.ps.dinear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.location.LocationActivity;
import com.ps.dinear.menu.MenuActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RestaurantAdapter restaurantAdapter;
    private FilterAdapter filterAdapter;
    private ProgressBar progressBar;
    private TextView tvCurrentLocation;
    private TextView tvWelcome;
    
    private String currentSearchQuery = "";
    private String currentCategory = "Home";

    private final ActivityResultLauncher<Intent> categoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selected = result.getData().getStringExtra("selected_category");
                    if (selected != null) {
                        currentCategory = selected;
                        setupRestaurants();
                        Toast.makeText(this, "Filtered by: " + selected, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.app_bg));
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        if (SharedPrefManager.getDistrict(this) == null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        String userName = SharedPrefManager.getUserName(this).toUpperCase();
        tvWelcome.setText("HELLO, " + userName + "!");

        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        String locationText = SharedPrefManager.getCity(this) + ", " + SharedPrefManager.getDistrict(this);
        tvCurrentLocation.setText(locationText);

        findViewById(R.id.btnChangeLocation).setOnClickListener(v -> {
            startActivity(new Intent(this, LocationActivity.class));
        });

        findViewById(R.id.btnFilter).setOnClickListener(v -> {
            Toast.makeText(this, "Filter coming soon!", Toast.LENGTH_SHORT).show();
        });

        setupSearchView();
        setupFilters();
        setupRestaurants();

        findViewById(R.id.btnARScan).setOnClickListener(v -> {
            Toast.makeText(this, "AR Scan coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.btnRegisterRestaurant).setOnClickListener(v -> {
            String url = RetrofitClient.getBaseUrl() + "dashboard/register/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchViewHome);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                performSearch();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                if (newText.isEmpty()) {
                    setupRestaurants(); // Reset to normal list
                } else {
                    performSearch(); // Search as you type
                }
                return true;
            }
        });
    }

    private void setupFilters() {
        RecyclerView rvFilters = findViewById(R.id.rvFilters);
        List<String> filters = new ArrayList<>();
        filters.add("Home");
        filters.add("Veg Restaurants");
        filters.add("Non Veg Restaurants");
        filters.add("Fast Food");
        filters.add("Café");
        filters.add("More →");

        filterAdapter = new FilterAdapter(this, filters, category -> {
            if (category.equals("More →")) {
                categoryLauncher.launch(new Intent(this, CategoryActivity.class));
            } else {
                currentCategory = category;
                setupRestaurants(); // Fetch with category filter
            }
        });
        rvFilters.setAdapter(filterAdapter);
    }

    private void performSearch() {
        progressBar.setVisibility(View.VISIBLE);
        String city = SharedPrefManager.getCity(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.search(currentSearchQuery, city).enqueue(new Callback<com.ps.dinear.data.model.SearchResponse>() {
            @Override
            public void onResponse(Call<com.ps.dinear.data.model.SearchResponse> call, Response<com.ps.dinear.data.model.SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> resResults = response.body().getRestaurants();
                    List<MenuItem> foodResults = response.body().getFoodItems();
                    
                    restaurantAdapter.updateList(resResults);
                    
                    if (resResults.isEmpty() && foodResults.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No results found for \"" + currentSearchQuery + "\"", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<com.ps.dinear.data.model.SearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Search error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRestaurants() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);
        String selectedCity = SharedPrefManager.getCity(this);
        String apiCategory = currentCategory.equals("Home") ? null : currentCategory;
        
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getRestaurants(selectedCity, apiCategory).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(Call<List<Restaurant>> call, Response<List<Restaurant>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> restaurants = response.body();
                    if (restaurantAdapter == null) {
                        restaurantAdapter = new RestaurantAdapter(MainActivity.this, new ArrayList<>(restaurants), restaurant -> {
                            Intent intent = new Intent(MainActivity.this, RestaurantDetailsActivity.class);
                            intent.putExtra("restaurantId", restaurant.getId());
                            intent.putExtra("restaurantName", restaurant.getName());
                            intent.putExtra("cuisine", restaurant.getCuisine());
                            intent.putExtra("rating", restaurant.getRating());
                            intent.putExtra("imageUrl", restaurant.getImageUrl());
                            startActivity(intent);
                        });
                        recyclerView.setAdapter(restaurantAdapter);
                    } else {
                        restaurantAdapter.updateList(restaurants);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load restaurants", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

}