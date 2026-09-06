package com.ps.dinear;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.SearchResponse;
import com.ps.dinear.location.LocationActivity;
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

public class HomeFragment extends Fragment implements FavoritesManager.FavoritesListener {

    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter menuAdapter;
    private FilterAdapter filterAdapter;
    private FeaturedFoodAdapter featuredFoodAdapter;
    private ProgressBar progressBar;
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

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final CartManager.CartListener cartListener = this::updateCartBadge;

    private final ActivityResultLauncher<Intent> categoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    String selected = result.getData().getStringExtra("selected_category");
                    if (selected != null) {
                        currentCategory = selected;
                        previousCategory = selected;
                        if (filterAdapter != null) {
                            filterAdapter.setSelectedCategory(selected);
                        }
                        if (isAdded()) setupRestaurants(getView());
                        Toast.makeText(getContext(), "Filtered by: " + selected, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (filterAdapter != null) {
                        filterAdapter.setSelectedCategory(previousCategory);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        updateWelcomeText();

        TextView tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        String locationText = SharedPrefManager.getCity(getContext()) + ", " + SharedPrefManager.getDistrict(getContext());
        tvCurrentLocation.setText(locationText);

        tvCartBadge = view.findViewById(R.id.tvCartBadge);
        View btnCart = view.findViewById(R.id.btnCart);
        if (btnCart != null) btnCart.setOnClickListener(v -> startActivity(new Intent(getContext(), CartActivity.class)));

        View btnChangeLocation = view.findViewById(R.id.btnChangeLocation);
        if (btnChangeLocation != null) btnChangeLocation.setOnClickListener(v -> startActivity(new Intent(getContext(), LocationActivity.class)));

        View btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) btnFilter.setOnClickListener(v -> {
            SearchFilterBottomSheet bottomSheet = new SearchFilterBottomSheet(
                (sort, rating) -> {
                    currentSort = sort;
                    currentMinRating = rating;
                    if (isAdded()) updateSearchResultsView(getView());
                },
                currentSort,
                currentMinRating
            );
            bottomSheet.show(getChildFragmentManager(), "filter");
        });

        setupSearchView(view);
        setupFilters(view);
        setupRestaurants(view);
        setupTabs(view);
        setupFeaturedFoods(view);
        
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            requestLocationPermission();
        } catch (Exception ignored) {}

        View cvPromo = view.findViewById(R.id.cvPromoBanner);
        if (cvPromo != null) cvPromo.setOnClickListener(v -> {
            if (featuredDish != null) {
                Intent intent = new Intent(getContext(), ARActivity.class);
                intent.putExtra("selectedItem", featuredDish);
                intent.putExtra("restaurantSlug", featuredRestaurantSlug);
                intent.putExtra("restaurantId", featuredRestaurantId);
                startActivity(intent);
            }
        });

        View btnRetry = view.findViewById(R.id.btnRetry);
        if (btnRetry != null) btnRetry.setOnClickListener(v -> {
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                performSearch(getView());
            } else {
                setupRestaurants(getView());
            }
        });

        RecyclerView rvFeatured = view.findViewById(R.id.rvFeaturedFoods);
        if (rvFeatured != null) {
            rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            featuredFoodAdapter = new FeaturedFoodAdapter(getContext());
            rvFeatured.setAdapter(featuredFoodAdapter);
        }

        CartManager.getInstance().addListener(cartListener);
        FavoritesManager.getInstance().addListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWelcomeText();
        updateCartBadge();
        
        if (getView() != null) {
            ImageView ivProfile = getView().findViewById(R.id.ivHomeProfile);
            if (ivProfile != null) {
                ivProfile.setImageResource(SharedPrefManager.getUserAvatar(getContext()));
            }
        }

        refreshAdapters();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        CartManager.getInstance().removeListener(cartListener);
        FavoritesManager.getInstance().removeListener(this);
    }

    @Override
    public void onFavoritesUpdated() {
        refreshAdapters();
    }

    private void refreshAdapters() {
        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
        if (menuAdapter != null) {
            menuAdapter.notifyDataSetChanged();
        }
    }

    private void updateWelcomeText() {
        if (tvWelcome == null || !isAdded()) return;
        String name = SharedPrefManager.getUserName(getContext());
        String email = SharedPrefManager.getUserEmail(getContext());

        if (SharedPrefManager.isGuest(getContext())) {
            name = "Guest";
        } else if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Guest")) {
            if (email != null && !email.trim().isEmpty() && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "User";
            }
        }
        
        if (name != null) {
            tvWelcome.setText(getString(R.string.hello_placeholder, name.toUpperCase()));
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || !isAdded()) return;
        int count = CartManager.getInstance().getItemCount();
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void setupSearchView(View root) {
        if (root == null) return;
        SearchView searchView = root.findViewById(R.id.searchViewHome);
        if (searchView == null) return;
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                if (!query.isEmpty()) {
                    searchHandler.removeCallbacks(searchRunnable);
                    toggleHomeContent(getView(), false);
                    if (tabLayoutSearch != null) tabLayoutSearch.setVisibility(View.VISIBLE);
                    performSearch(getView());
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
                    toggleHomeContent(getView(), true);
                    if (tabLayoutSearch != null) {
                        tabLayoutSearch.setVisibility(View.GONE);
                        if (tabLayoutSearch.getTabAt(0) != null) {
                            tabLayoutSearch.getTabAt(0).select();
                        }
                    }
                    setupRestaurants(getView());
                } else {
                    toggleHomeContent(getView(), false);
                    if (tabLayoutSearch != null) tabLayoutSearch.setVisibility(View.VISIBLE);
                    
                    searchRunnable = () -> {
                        if (isAdded()) performSearch(getView());
                    };
                    searchHandler.postDelayed(searchRunnable, 400);
                }
                return true;
            }
        });
    }

    private void toggleHomeContent(View root, boolean show) {
        if (root == null || !isAdded()) return;
        int visibility = show ? View.VISIBLE : View.GONE;
        View promoBanner = root.findViewById(R.id.cvPromoBanner);
        View catHeader = root.findViewById(R.id.rlCategoriesHeader);
        View filters = root.findViewById(R.id.rvFilters);
        View exploreHeader = root.findViewById(R.id.rlExploreHeader);
        View featuredHeader = root.findViewById(R.id.rlFeaturedFoodsHeader);
        View featuredList = root.findViewById(R.id.rvFeaturedFoods);
        View registerCard = root.findViewById(R.id.cardRegisterInfo);
        
        if (promoBanner != null) promoBanner.setVisibility(visibility);
        if (catHeader != null) catHeader.setVisibility(visibility);
        if (filters != null) filters.setVisibility(visibility);
        if (exploreHeader != null) exploreHeader.setVisibility(visibility);
        if (featuredHeader != null) featuredHeader.setVisibility(visibility);
        if (featuredList != null) featuredList.setVisibility(visibility);
        if (registerCard != null) registerCard.setVisibility(visibility);

        if (show) {
            View noRes = root.findViewById(R.id.llNoResults);
            if (noRes != null) noRes.setVisibility(View.GONE);
            View netErr = root.findViewById(R.id.llNetworkError);
            if (netErr != null) netErr.setVisibility(View.GONE);
            View scroll = root.findViewById(R.id.nestedScrollView);
            if (scroll != null) scroll.setVisibility(View.VISIBLE);
        }
    }

    private void setupFilters(View root) {
        if (root == null || !isAdded()) return;
        RecyclerView rvFilters = root.findViewById(R.id.rvFilters);
        if (rvFilters == null) return;
        
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.getCategories().enqueue(new Callback<List<Restaurant.Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Restaurant.Category>> call, @NonNull Response<List<Restaurant.Category>> response) {
                if (!isAdded()) return;
                List<String> filters = new ArrayList<>();
                filters.add("Home");
                if (response.isSuccessful() && response.body() != null) {
                    for (Restaurant.Category cat : response.body()) {
                        filters.add(cat.getName());
                    }
                }
                filters.add("More →");

                filterAdapter = new FilterAdapter(getContext(), filters, category -> {
                    if (category.equals("More →")) {
                        categoryLauncher.launch(new Intent(getContext(), CategoryActivity.class));
                    } else {
                        currentCategory = category;
                        previousCategory = category;
                        if (isAdded()) setupRestaurants(getView());
                    }
                });
                rvFilters.setAdapter(filterAdapter);
            }

            @Override
            public void onFailure(@NonNull Call<List<Restaurant.Category>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                List<String> filters = new ArrayList<>();
                filters.add("Home");
                filters.add("More →");
                filterAdapter = new FilterAdapter(getContext(), filters, category -> {
                    if (category.equals("More →")) {
                        categoryLauncher.launch(new Intent(getContext(), CategoryActivity.class));
                    } else {
                        currentCategory = category;
                        previousCategory = category;
                        if (isAdded()) setupRestaurants(getView());
                    }
                });
                rvFilters.setAdapter(filterAdapter);
            }
        });
    }

    private void setupFeaturedFoods(View root) {
        if (root == null || !isAdded()) return;
        String city = SharedPrefManager.getCity(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        
        api.search("", city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<MenuItem> foods = response.body().getFoodItems();
                    if (foods != null && !foods.isEmpty()) {
                        java.util.Collections.shuffle(foods);
                        if (featuredFoodAdapter != null) featuredFoodAdapter.setData(foods);
                        featuredDish = foods.get(0);
                        
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
                        if (isAdded()) updatePromoBanner(getView());
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {}
        });
    }

    private void updatePromoBanner(View root) {
        if (root == null || featuredDish == null || !isAdded()) return;
        
        ImageView ivBanner = root.findViewById(R.id.ivPromoBannerImage);
        TextView tvBannerTitle = root.findViewById(R.id.tvPromoBannerTitle);
        
        if (tvBannerTitle != null) {
            tvBannerTitle.setText(getString(R.string.special_offer_placeholder, featuredDish.getName()));
        }
        
        if (ivBanner != null) {
            String fullImageUrl = RetrofitClient.getFullUrl(getContext(), featuredDish.getImageUrl());
            Glide.with(HomeFragment.this).load(fullImageUrl).placeholder(R.drawable.burger).into(ivBanner);
        }
    }

    private void setupTabs(View root) {
        if (root == null) return;
        tabLayoutSearch = root.findViewById(R.id.tabLayoutSearch);
        if (tabLayoutSearch == null) return;
        
        tabLayoutSearch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isAdded()) updateSearchResultsView(getView());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateSearchResultsView(View root) {
        if (root == null || !isAdded()) return;
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        if (tabLayoutSearch == null || recyclerView == null) return;
        
        int selectedTab = tabLayoutSearch.getSelectedTabPosition();
        boolean hasResults;

        if (selectedTab == 0) {
            List<Restaurant> filtered = new ArrayList<>(lastResResults);
            if (userLocation != null) calculateDistances(filtered);

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
            if (restaurantAdapter != null) restaurantAdapter.updateList(filtered);
        } else {
            List<MenuItem> filtered = new ArrayList<>(lastFoodResults);
            if (currentSort.equals("price")) {
                filtered.sort((f1, f2) -> Double.compare(f1.getPrice(), f2.getPrice()));
            }
            
            hasResults = !filtered.isEmpty();
            if (menuAdapter == null) menuAdapter = new MenuAdapter(getContext());
            menuAdapter.setRestaurants(lastResResults);
            recyclerView.setAdapter(menuAdapter);
            menuAdapter.setData(filtered);
        }

        boolean globallyEmpty = lastResResults.isEmpty() && lastFoodResults.isEmpty();
        if (globallyEmpty) {
            View noRes = root.findViewById(R.id.llNoResults);
            if (noRes != null) {
                noRes.setVisibility(View.VISIBLE);
                TextView tvMsg = noRes.findViewById(R.id.tvNoResultsMessage);
                if (tvMsg != null) tvMsg.setText("No treats found here!");
            }
            recyclerView.setVisibility(View.GONE);
        } else {
            View noRes = root.findViewById(R.id.llNoResults);
            if (noRes != null) noRes.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        }
    }

    private void performSearch(View root) {
        if (root == null || !isAdded()) return;
        progressBar = root.findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        View noRes = root.findViewById(R.id.llNoResults);
        if (noRes != null) noRes.setVisibility(View.GONE);
        View netErr = root.findViewById(R.id.llNetworkError);
        if (netErr != null) netErr.setVisibility(View.GONE);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);

        String city = SharedPrefManager.getCity(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.search(currentSearchQuery, city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    lastResResults = new ArrayList<>(response.body().getRestaurants());
                    lastFoodResults = response.body().getFoodItems();

                    if (lastFoodResults != null && !lastFoodResults.isEmpty()) {
                        Set<Integer> resIdsFromFood = new HashSet<>();
                        for (MenuItem item : lastFoodResults) {
                            if (item.getRestaurantIds() != null) resIdsFromFood.addAll(item.getRestaurantIds());
                        }
                        for (Integer resId : resIdsFromFood) {
                            boolean exists = false;
                            for (Restaurant r : lastResResults) {
                                if (r.getId() == resId) { exists = true; break; }
                            }
                            if (!exists) {
                                for (Restaurant r : allRestaurantsInCity) {
                                    if (r.getId() == resId) { lastResResults.add(r); break; }
                                }
                            }
                        }
                    }
                    if (isAdded()) updateSearchResultsView(getView());
                }
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (isAdded() && getView() != null) {
                    View err = getView().findViewById(R.id.llNetworkError);
                    if (err != null) err.setVisibility(View.VISIBLE);
                    View scroll = getView().findViewById(R.id.nestedScrollView);
                    if (scroll != null) scroll.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupRestaurants(View root) {
        if (root == null || !isAdded()) return;
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        if (recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        progressBar = root.findViewById(R.id.progressBar);

        View noRes = root.findViewById(R.id.llNoResults);
        if (noRes != null) noRes.setVisibility(View.GONE);
        View netErr = root.findViewById(R.id.llNetworkError);
        if (netErr != null) netErr.setVisibility(View.GONE);

        boolean isHome = currentCategory.equals("Home");
        View cvPromo = root.findViewById(R.id.cvPromoBanner);
        if (cvPromo != null) cvPromo.setVisibility(isHome ? View.VISIBLE : View.GONE);

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String selectedCity = SharedPrefManager.getCity(getContext());
        String apiCategory = currentCategory.equals("Home") ? null : currentCategory;
        
        ApiService apiService = RetrofitClient.getClient(getContext()).create(ApiService.class);
        apiService.getRestaurants(selectedCity, apiCategory).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(@NonNull Call<List<Restaurant>> call, @NonNull Response<List<Restaurant>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> restaurants = response.body();
                    if (apiCategory == null) allRestaurantsInCity = new ArrayList<>(restaurants);

                    if (restaurants.isEmpty()) {
                        if (getView() != null) {
                            View nr = getView().findViewById(R.id.llNoResults);
                            if (nr != null) nr.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    } else {
                        if (userLocation != null) calculateDistances(restaurants);
                        if (getView() != null) {
                            View nr = getView().findViewById(R.id.llNoResults);
                            if (nr != null) nr.setVisibility(View.GONE);
                        }
                        recyclerView.setVisibility(View.VISIBLE);
                        
                        if (restaurantAdapter == null) {
                            restaurantAdapter = new RestaurantAdapter(getContext(), new ArrayList<>(restaurants), restaurant -> {
                                Intent intent = new Intent(getContext(), RestaurantDetailsActivity.class);
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
                    if (getView() != null) {
                        View err = getView().findViewById(R.id.llNetworkError);
                        if (err != null) err.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Restaurant>> call, @NonNull Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (isAdded() && getView() != null) {
                    View err = getView().findViewById(R.id.llNetworkError);
                    if (err != null) err.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void requestLocationPermission() {
        if (!isAdded()) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (!isAdded()) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null && isAdded()) {
                    userLocation = location;
                    if (!lastResResults.isEmpty()) {
                        calculateDistances(lastResResults);
                        if (isAdded()) updateSearchResultsView(getView());
                    } else {
                        if (isAdded()) setupRestaurants(getView());
                    }
                }
            });
        }
    }

    private void calculateDistances(List<Restaurant> restaurants) {
        if (userLocation == null || restaurants == null) return;
        for (Restaurant r : restaurants) {
            if (r != null && r.getLatitude() != 0 && r.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        r.getLatitude(), r.getLongitude(), results);
                r.setDistance(results[0] / 1000.0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    public void setFeaturedDish(MenuItem item, String slug, int id) {
        this.featuredDish = item;
        this.featuredRestaurantSlug = slug;
        this.featuredRestaurantId = id;
    }
}