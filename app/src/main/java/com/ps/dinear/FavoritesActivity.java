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

import com.google.android.material.tabs.TabLayout;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.SearchResponse;
import com.ps.dinear.menu.FoodDetailsActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavItems;
    private ProgressBar pbFav;
    private LinearLayout llNoResults;
    private TabLayout tabLayoutFav;

    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter foodAdapter;

    private List<Restaurant> favRestaurants = new ArrayList<>();
    private List<MenuItem> favFoods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.app_bg));
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        super.setContentView(R.layout.activity_favorites);

        rvFavItems = findViewById(R.id.rvFavItems);
        pbFav = findViewById(R.id.pbFav);
        llNoResults = findViewById(R.id.llFavNoResults);
        tabLayoutFav = findViewById(R.id.tabLayoutFav);

        rvFavItems.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBackFav).setOnClickListener(v -> finish());

        tabLayoutFav.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // loadFavorites(); // Removed to avoid double call on startup (called in onResume)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from detail screens in case something was unfavorited
        loadFavorites();
    }

    private void loadFavorites() {
        if (!SharedPrefManager.isLoggedIn(this)) {
            llNoResults.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvFavNoResultsMessage)).setText("Please login to see favorites");
            return;
        }

        pbFav.setVisibility(View.VISIBLE);
        llNoResults.setVisibility(View.GONE);
        rvFavItems.setVisibility(View.GONE);

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getFavoriteDetails(token).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                pbFav.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    favRestaurants = response.body().getRestaurants();
                    favFoods = response.body().getFoodItems();
                    updateView();
                } else {
                    Toast.makeText(FavoritesActivity.this, "Failed to load favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                pbFav.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateView() {
        int selectedTab = tabLayoutFav.getSelectedTabPosition();
        boolean hasItems = false;

        if (selectedTab == 0) { // Restaurants
            if (restaurantAdapter == null) {
                restaurantAdapter = new RestaurantAdapter(this, new ArrayList<>(favRestaurants), restaurant -> {
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
                    startActivity(intent);
                });
            } else {
                restaurantAdapter.updateList(favRestaurants);
            }
            rvFavItems.setAdapter(restaurantAdapter);
            hasItems = !favRestaurants.isEmpty();
        } else { // Foods
            if (foodAdapter == null) {
                foodAdapter = new MenuAdapter(this);
            }
            foodAdapter.setRestaurants(new ArrayList<>()); // Placeholder
            rvFavItems.setAdapter(foodAdapter);
            foodAdapter.setData(favFoods);
            hasItems = !favFoods.isEmpty();
        }

        rvFavItems.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        llNoResults.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }
}
