package com.ps.dinear;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.SearchResponse;
import com.ps.dinear.location.LocationActivity;
import com.ps.dinear.menu.MenuActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter menuAdapter;
    private FilterAdapter filterAdapter;
    private FeaturedFoodAdapter featuredFoodAdapter;
    private ProgressBar progressBar;
    private TextView tvCurrentLocation;
    private TextView tvWelcome;
    private TabLayout tabLayoutSearch;
    private TextView tvCartBadge;
    
    private MenuItem featuredDish;
    private String featuredRestaurantSlug;
    private int featuredRestaurantId = -1;
    
    private String currentSearchQuery = "";
    private String currentCategory = "Home";
    private String currentSort = "";
    private float currentMinRating = 0f;
    private String previousCategory = "Home";
    private List<Restaurant> lastResResults = new ArrayList<>();
    private List<MenuItem> lastFoodResults = new ArrayList<>();
    private List<Restaurant> allRestaurantsInCity = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;

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
                        previousCategory = selected;
                        if (filterAdapter != null) {
                            filterAdapter.setSelectedCategory(selected);
                        }
                        setupRestaurants();
                        Toast.makeText(this, "Filtered by: " + selected, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Reset selection if cancelled
                    if (filterAdapter != null) {
                        filterAdapter.setSelectedCategory(previousCategory);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        if (SharedPrefManager.getDistrict(this) == null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        super.setContentView(R.layout.activity_main);

        // Dynamically adjust status bar spacer height
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
            androidx.core.graphics.Insets statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.getLayoutParams().height = statusBars.top;
            v.requestLayout();
            
            // Also handle bottom navigation insets
            View bottomNav = findViewById(R.id.bottomNav);
            View arScan = findViewById(R.id.btnARScan);
            androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            
            if (bottomNav != null) {
                float density = getResources().getDisplayMetrics().density;
                bottomNav.getLayoutParams().height = (int) (70 * density) + navBars.bottom;
                bottomNav.setPadding(bottomNav.getPaddingLeft(), (int) (4 * density), 
                                  bottomNav.getPaddingRight(), (int) (8 * density) + navBars.bottom);
                bottomNav.requestLayout();
            }
            
            if (arScan != null) {
                android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) arScan.getLayoutParams();
                float density = getResources().getDisplayMetrics().density;
                mlp.bottomMargin = (int) (35 * density) + navBars.bottom;
                arScan.setLayoutParams(mlp);
            }
            
            return insets;
        });

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
        setupFeaturedFoods();
        FavoritesManager.getInstance().loadFavorites(this);
        CartManager.getInstance().addListener(cartListener);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        findViewById(R.id.btnARScan).setOnClickListener(v -> {
            if (featuredDish != null) {
                Intent intent = new Intent(this, ARActivity.class);
                intent.putExtra("selectedItem", featuredDish);
                // Not passing slug here to enable "Global" mode for the floating button
                startActivity(intent);
            } else {
                Toast.makeText(this, "Finding a featured dish for you...", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.cvPromoBanner).setOnClickListener(v -> {
            if (featuredDish != null) {
                Intent intent = new Intent(this, ARActivity.class);
                intent.putExtra("selectedItem", featuredDish);
                intent.putExtra("restaurantSlug", featuredRestaurantSlug);
                intent.putExtra("restaurantId", featuredRestaurantId);
                startActivity(intent);
            }
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.navNearby).setOnClickListener(v -> {
            startActivity(new Intent(this, DiscoverActivity.class));
        });

        findViewById(R.id.cvHomeProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.navFavorites).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
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

        if (SharedPrefManager.isGuest(this)) {
            name = "Guest";
        } else if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Guest")) {
            // Fallback for logged in users with missing name
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
        View catHeader = findViewById(R.id.rlCategoriesHeader);
        View filters = findViewById(R.id.rvFilters);
        View exploreHeader = findViewById(R.id.rlExploreHeader);
        View featuredHeader = findViewById(R.id.rlFeaturedFoodsHeader);
        View featuredList = findViewById(R.id.rvFeaturedFoods);
        View registerCard = findViewById(R.id.cardRegisterInfo);
        
        if (promoBanner != null) promoBanner.setVisibility(visibility);
        if (catHeader != null) catHeader.setVisibility(visibility);
        if (filters != null) filters.setVisibility(visibility);
        if (exploreHeader != null) exploreHeader.setVisibility(visibility);
        if (featuredHeader != null) featuredHeader.setVisibility(visibility);
        if (featuredList != null) featuredList.setVisibility(visibility);
        if (registerCard != null) registerCard.setVisibility(visibility);

        if (show) {
            findViewById(R.id.llNoResults).setVisibility(View.GONE);
            findViewById(R.id.llNetworkError).setVisibility(View.GONE);
            findViewById(R.id.nestedScrollView).setVisibility(View.VISIBLE);
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
                        previousCategory = category;
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
                        previousCategory = category;
                        setupRestaurants();
                    }
                });
                rvFilters.setAdapter(filterAdapter);
            }
        });
    }

    private void setupFeaturedFoods() {
        RecyclerView rvFeatured = findViewById(R.id.rvFeaturedFoods);
        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredFoodAdapter = new FeaturedFoodAdapter(this);
        rvFeatured.setAdapter(featuredFoodAdapter);

        String city = SharedPrefManager.getCity(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        // Fetching all foods by using an empty query now allowed by backend
        android.util.Log.d("MainActivity", "Fetching all foods for trending dishes in city: " + city);
        api.search("", city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MenuItem> foods = response.body().getFoodItems();
                    if (foods != null && !foods.isEmpty()) {
                        // Jumble the sequence
                        java.util.Collections.shuffle(foods);
                        
                        featuredFoodAdapter.setData(foods);
                        featuredDish = foods.get(0);
                        
                        // Handle slug/id for the first item for banner
                        List<Restaurant> restaurants = response.body().getRestaurants();
                        if (featuredDish.getRestaurantIds() != null && !featuredDish.getRestaurantIds().isEmpty() && restaurants != null) {
                            for (Restaurant r : restaurants) {
                                if (featuredDish.getRestaurantIds().contains(r.getId())) {
                                    featuredRestaurantSlug = r.getSlug();
                                    featuredRestaurantId = r.getId();
                                    break;
                                }
                            }
                        }
                        
                        updatePromoBanner();
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                android.util.Log.e("MainActivity", "Failed to load trending dishes: " + t.getMessage());
            }
        });
    }

    private void updatePromoBanner() {
        if (featuredDish == null) return;
        
        ImageView ivBanner = findViewById(R.id.ivPromoBannerImage);
        TextView tvBannerTitle = findViewById(R.id.tvPromoBannerTitle);
        
        if (tvBannerTitle != null) {
            tvBannerTitle.setText("Special Offer:\n" + featuredDish.getName());
        }
        
        if (ivBanner != null) {
            String fullImageUrl = RetrofitClient.getFullUrl(this, featuredDish.getImageUrl());
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.burger)
                    .into(ivBanner);
        }
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
            
            if (userLocation != null) {
                calculateDistances(filtered);
            }

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
            recyclerView.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvNoResultsMessage)).setText("No treats found here!");
            ((TextView) findViewById(R.id.tvNoResultsDescription)).setText("We couldn't find any results for \"" + currentSearchQuery + "\". Try a different keyword or explore our categories.");
        } else {
            findViewById(R.id.llNoResults).setVisibility(hasResults ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
            if (!hasResults) {
                ((TextView) findViewById(R.id.tvNoResultsMessage)).setText("No matching treats!");
                ((TextView) findViewById(R.id.tvNoResultsDescription)).setText("None of our items match your current filters. Try relaxing them to see more options!");
            }
        }
    }

    private void performSearch() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.llNoResults).setVisibility(View.GONE);
        findViewById(R.id.llNetworkError).setVisibility(View.GONE);
        // Do not hide nestedScrollView, just hide content within it if needed
        findViewById(R.id.recyclerView).setVisibility(View.GONE);

        String city = SharedPrefManager.getCity(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.search(currentSearchQuery, city).enqueue(new Callback<com.ps.dinear.data.model.SearchResponse>() {
            @Override
            public void onResponse(Call<com.ps.dinear.data.model.SearchResponse> call, Response<com.ps.dinear.data.model.SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    lastResResults = new ArrayList<>(response.body().getRestaurants());
                    lastFoodResults = response.body().getFoodItems();

                    // Enrich restaurants list with those selling matching food items
                    if (lastFoodResults != null && !lastFoodResults.isEmpty()) {
                        Set<Integer> resIdsFromFood = new HashSet<>();
                        for (MenuItem item : lastFoodResults) {
                            if (item.getRestaurantIds() != null) {
                                resIdsFromFood.addAll(item.getRestaurantIds());
                            }
                        }

                        for (Integer resId : resIdsFromFood) {
                            boolean exists = false;
                            for (Restaurant r : lastResResults) {
                                if (r.getId() == resId) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                for (Restaurant r : allRestaurantsInCity) {
                                    if (r.getId() == resId) {
                                        lastResResults.add(r);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
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

        // Prevent flickering: hide extra sections immediately if not in Home category
        boolean isHome = currentCategory.equals("Home");
        View promoBanner = findViewById(R.id.cvPromoBanner);
        View featuredHeader = findViewById(R.id.rlFeaturedFoodsHeader);
        View featuredList = findViewById(R.id.rvFeaturedFoods);
        View registerCard = findViewById(R.id.cardRegisterInfo);

        if (promoBanner != null) promoBanner.setVisibility(isHome ? View.VISIBLE : View.GONE);
        if (featuredHeader != null) featuredHeader.setVisibility(isHome ? View.VISIBLE : View.GONE);
        if (featuredList != null) featuredList.setVisibility(isHome ? View.VISIBLE : View.GONE);
        if (registerCard != null) registerCard.setVisibility(isHome ? View.VISIBLE : View.GONE);

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

                    // Cache full list for search enrichment
                    if (apiCategory == null) {
                        allRestaurantsInCity = new ArrayList<>(restaurants);
                    }

                    if (restaurants.isEmpty()) {
                        findViewById(R.id.llNoResults).setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        
                        String catName = currentCategory.equals("Home") ? "this area" : "the " + currentCategory + " category";
                        ((TextView) findViewById(R.id.tvNoResultsMessage)).setText("Kitchen is quiet!");
                        ((TextView) findViewById(R.id.tvNoResultsDescription)).setText("We don't have any restaurants in " + catName + " yet. We're working on bringing them to you!");
                    } else {
                        if (userLocation != null) {
                            calculateDistances(restaurants);
                        }
                        findViewById(R.id.llNoResults).setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        findViewById(R.id.nestedScrollView).setVisibility(View.VISIBLE);
                        
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
                                intent.putExtra("address", restaurant.getAddress());
                                intent.putExtra("isFeatured", restaurant.isFeatured());
                                intent.putExtra("latitude", restaurant.getLatitude());
                                intent.putExtra("longitude", restaurant.getLongitude());
                                if (restaurant.getLocation() != null) {
                                    intent.putExtra("city", restaurant.getLocation().getCity());
                                    intent.putExtra("district", restaurant.getLocation().getDistrict());
                                }
                                startActivity(intent);
                            });
                        } else {
                            restaurantAdapter.updateList(restaurants);
                        }
                        recyclerView.setAdapter(restaurantAdapter);
                    }
                } else {
                    findViewById(R.id.llNetworkError).setVisibility(View.VISIBLE);
                    findViewById(R.id.nestedScrollView).setVisibility(View.GONE);
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

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            if (restaurantAdapter != null) {
                restaurantAdapter.setLocationPermissionDenied(false);
            }
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    if (!lastResResults.isEmpty()) {
                        calculateDistances(lastResResults);
                        updateSearchResultsView();
                    } else {
                        setupRestaurants();
                    }
                }
            });
        }
    }

    private void calculateDistances(List<Restaurant> restaurants) {
        if (userLocation == null) return;
        for (Restaurant r : restaurants) {
            if (r.getLatitude() != 0 && r.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        r.getLatitude(), r.getLongitude(), results);
                r.setDistance(results[0] / 1000.0); // Convert to km
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (restaurantAdapter != null) {
                    restaurantAdapter.setLocationPermissionDenied(false);
                }
                getCurrentLocation();
            } else {
                if (restaurantAdapter != null) {
                    restaurantAdapter.setLocationPermissionDenied(true);
                }
            }
        }
    }
}
