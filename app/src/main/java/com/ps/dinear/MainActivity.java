package com.ps.dinear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
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
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter menuAdapter;
    private FilterAdapter filterAdapter;
    private ProgressBar progressBar;
    private TextView tvCurrentLocation;
    private TextView tvWelcome;
    private TabLayout tabLayoutSearch;
    private TextView tvCartBadge;
    
    private String currentSearchQuery = "";
    private String currentCategory = "Home";
    private String currentSort = "";
    private float currentMinRating = 0f;
    private List<Restaurant> lastResResults = new ArrayList<>();
    private List<MenuItem> lastFoodResults = new ArrayList<>();

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private CartManager.CartListener cartListener = this::updateCartBadge;

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

        super.setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        updateWelcomeText();

        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        String locationText = SharedPrefManager.getCity(this) + ", " + SharedPrefManager.getDistrict(this);
        tvCurrentLocation.setText(locationText);

        tvCartBadge = findViewById(R.id.tvCartBadge);
        findViewById(R.id.btnCart).setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });

        findViewById(R.id.btnChangeLocation).setOnClickListener(v -> {
            startActivity(new Intent(this, LocationActivity.class));
        });

        findViewById(R.id.btnFilter).setOnClickListener(v -> {
            SearchFilterBottomSheet bottomSheet = new SearchFilterBottomSheet(
                (sort, rating) -> {
                    currentSort = sort;
                    currentMinRating = rating;
                    updateSearchResultsView();
                },
                currentSort,
                currentMinRating
            );
            bottomSheet.show(getSupportFragmentManager(), "filter");
        });

        setupSearchView();
        setupFilters();
        setupRestaurants();
        setupTabs();
        FavoritesManager.getInstance().loadFavorites(this);
        CartManager.getInstance().addListener(cartListener);

        findViewById(R.id.btnARScan).setOnClickListener(v -> {
            Toast.makeText(this, "AR Scan coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.cvHomeProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.navFavorites).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
        });

        findViewById(R.id.btnRegisterRestaurant).setOnClickListener(v -> {
            String url = RetrofitClient.getBaseUrl() + "dashboard/register/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                performSearch();
            } else {
                setupRestaurants();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        updateWelcomeText();
        
        ImageView ivProfile = findViewById(R.id.ivHomeProfile);
        if (ivProfile != null) {
            ivProfile.setImageResource(SharedPrefManager.getUserAvatar(this));
        }

        // Refresh adapters to show updated favorite status when returning from detail screens
        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
        if (menuAdapter != null) {
            menuAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeListener(cartListener);
    }

    private void updateWelcomeText() {
        if (tvWelcome == null) return;
        String name = SharedPrefManager.getUserName(this);
        String email = SharedPrefManager.getUserEmail(this);

        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Guest")) {
            // Fallback: If name is missing, use email prefix
            if (email != null && !email.trim().isEmpty() && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "User";
            }
        }
        
        // Ensure name is not null before uppercase
        if (name != null) {
            tvWelcome.setText("HELLO, " + name.toUpperCase() + "!");
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        int count = CartManager.getInstance().getItemCount();
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchViewHome);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                if (!query.isEmpty()) {
                    searchHandler.removeCallbacks(searchRunnable);
                    toggleHomeContent(false);
                    if (tabLayoutSearch != null) tabLayoutSearch.setVisibility(View.VISIBLE);
                    performSearch();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                searchHandler.removeCallbacks(searchRunnable);

                if (newText.isEmpty()) {
                    currentSort = "";
                    currentMinRating = 0f;
                    toggleHomeContent(true);
                    if (tabLayoutSearch != null) {
                        tabLayoutSearch.setVisibility(View.GONE);
                        if (tabLayoutSearch.getTabAt(0) != null) {
                            tabLayoutSearch.getTabAt(0).select();
                        }
                    }
                    setupRestaurants(); // Reset to normal list
                } else {
                    toggleHomeContent(false);
                    if (tabLayoutSearch != null) tabLayoutSearch.setVisibility(View.VISIBLE);
                    
                    searchRunnable = () -> performSearch();
                    searchHandler.postDelayed(searchRunnable, 400);
                }
                return true;
            }
        });
    }

    private void toggleHomeContent(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        View promoBanner = findViewById(R.id.cvPromoBanner);
        View filters = findViewById(R.id.rvFilters);
        View header = findViewById(R.id.rlExploreHeader);
        
        if (promoBanner != null) promoBanner.setVisibility(visibility);
        if (filters != null) filters.setVisibility(visibility);
        if (header != null) header.setVisibility(visibility);

        if (show) {
            findViewById(R.id.llNoResults).setVisibility(View.GONE);
            findViewById(R.id.llNetworkError).setVisibility(View.GONE);
        }
    }

    private void setupFilters() {
        RecyclerView rvFilters = findViewById(R.id.rvFilters);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getCategories().enqueue(new Callback<List<Restaurant.Category>>() {
            @Override
            public void onResponse(Call<List<Restaurant.Category>> call, Response<List<Restaurant.Category>> response) {
                List<String> filters = new ArrayList<>();
                filters.add("Home");
                if (response.isSuccessful() && response.body() != null) {
                    for (Restaurant.Category cat : response.body()) {
                        filters.add(cat.getName());
                    }
                }
                filters.add("More →");

                filterAdapter = new FilterAdapter(MainActivity.this, filters, category -> {
                    if (category.equals("More →")) {
                        categoryLauncher.launch(new Intent(MainActivity.this, CategoryActivity.class));
                    } else {
                        currentCategory = category;
                        setupRestaurants(); // Fetch with category filter
                    }
                });
                rvFilters.setAdapter(filterAdapter);
            }

            @Override
            public void onFailure(Call<List<Restaurant.Category>> call, Throwable t) {
                // Fallback to minimal hardcoded if API fails
                List<String> filters = new ArrayList<>();
                filters.add("Home");
                filters.add("More →");
                filterAdapter = new FilterAdapter(MainActivity.this, filters, category -> {
                    if (category.equals("More →")) {
                        categoryLauncher.launch(new Intent(MainActivity.this, CategoryActivity.class));
                    } else {
                        currentCategory = category;
                        setupRestaurants();
                    }
                });
                rvFilters.setAdapter(filterAdapter);
            }
        });
    }

    private void setupTabs() {
        tabLayoutSearch = findViewById(R.id.tabLayoutSearch);
        tabLayoutSearch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateSearchResultsView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateSearchResultsView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (tabLayoutSearch == null) return;
        
        int selectedTab = tabLayoutSearch.getSelectedTabPosition();
        boolean hasResults = false;

        if (selectedTab == 0) { // Restaurants
            List<Restaurant> filtered = new ArrayList<>(lastResResults);
            
            if (currentMinRating > 0) {
                List<Restaurant> temp = new ArrayList<>();
                for (Restaurant r : filtered) {
                    if (r.getRating() >= currentMinRating) temp.add(r);
                }
                filtered = temp;
            }

            if (currentSort.equals("rating")) {
                filtered.sort((r1, r2) -> Double.compare(r2.getRating(), r1.getRating()));
            } else if (currentSort.equals("distance")) {
                filtered.sort((r1, r2) -> Double.compare(r1.getDistance(), r2.getDistance()));
            }
            
            hasResults = !filtered.isEmpty();
            recyclerView.setAdapter(restaurantAdapter);
            if (restaurantAdapter != null) {
                restaurantAdapter.updateList(filtered);
            }
        } else { // Foods
            List<MenuItem> filtered = new ArrayList<>(lastFoodResults);
            
            if (currentSort.equals("price")) {
                filtered.sort((f1, f2) -> Double.compare(f1.getPrice(), f2.getPrice()));
            }
            
            hasResults = !filtered.isEmpty();
            if (menuAdapter == null) {
                menuAdapter = new MenuAdapter(this);
            }
            menuAdapter.setRestaurants(lastResResults);
            recyclerView.setAdapter(menuAdapter);
            menuAdapter.setData(filtered);
        }

        // Update Visibility
        boolean globallyEmpty = lastResResults.isEmpty() && lastFoodResults.isEmpty();
        if (globallyEmpty) {
            findViewById(R.id.llNoResults).setVisibility(View.VISIBLE);
            findViewById(R.id.nestedScrollView).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvNoResultsMessage)).setText("No results for \"" + currentSearchQuery + "\"");
        } else {
            findViewById(R.id.llNoResults).setVisibility(hasResults ? View.GONE : View.VISIBLE);
            findViewById(R.id.nestedScrollView).setVisibility(hasResults ? View.VISIBLE : View.GONE);
            if (!hasResults) {
                ((TextView) findViewById(R.id.tvNoResultsMessage)).setText("No results match your filters");
            }
        }
    }

    private void performSearch() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.llNoResults).setVisibility(View.GONE);
        findViewById(R.id.llNetworkError).setVisibility(View.GONE);
        findViewById(R.id.nestedScrollView).setVisibility(View.GONE);

        String city = SharedPrefManager.getCity(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.search(currentSearchQuery, city).enqueue(new Callback<com.ps.dinear.data.model.SearchResponse>() {
            @Override
            public void onResponse(Call<com.ps.dinear.data.model.SearchResponse> call, Response<com.ps.dinear.data.model.SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    lastResResults = response.body().getRestaurants();
                    lastFoodResults = response.body().getFoodItems();
                    
                    updateSearchResultsView();
                    
                    // Scroll to top of results
                    View scrollView = findViewById(R.id.nestedScrollView);
                    if (scrollView != null) {
                        scrollView.scrollTo(0, 0);
                    }
                }
            }

            @Override
            public void onFailure(Call<com.ps.dinear.data.model.SearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.llNetworkError).setVisibility(View.VISIBLE);
                findViewById(R.id.nestedScrollView).setVisibility(View.GONE);
            }
        });
    }

    private void setupRestaurants() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.llNoResults).setVisibility(View.GONE);
        findViewById(R.id.llNetworkError).setVisibility(View.GONE);
        findViewById(R.id.nestedScrollView).setVisibility(View.GONE);

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
                        restaurantAdapter.updateList(restaurants);
                    }
                    recyclerView.setAdapter(restaurantAdapter);
                    findViewById(R.id.nestedScrollView).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.llNetworkError).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.llNetworkError).setVisibility(View.VISIBLE);
                findViewById(R.id.nestedScrollView).setVisibility(View.GONE);
            }
        });
    }

}