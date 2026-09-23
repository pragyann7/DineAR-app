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
import android.widget.TextView;

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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.ReviewListResponse;
import com.ps.dinear.data.model.SearchResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements FavoritesManager.FavoritesListener {

    private RestaurantAdapter restaurantAdapter;
    private RestaurantAdapter exploreMoreAdapter;
    private MenuAdapter menuAdapter;
    private FilterAdapter filterAdapter;
    private FeaturedFoodAdapter featuredFoodAdapter;
    
    // Professional New Adapters
    private QuickPickAdapter quickPickAdapter;
    private FoodHorizontalAdapter priceDropAdapter;
    private FoodHorizontalAdapter hotPickAdapter;
    private RestaurantCompactAdapter featuredRestAdapter;
    private RestaurantCompactAdapter topRatedAdapter;
    private ReviewHomeAdapter reviewsAdapter;
    
    private View layoutShimmer;
    private TabLayout tabLayoutSearch;
    private ViewPager2 vpPromoCarousel;
    private TabLayout tlPromoIndicator;

    private View llFeaturedRestaurantsContainer;
    private View llHotPicksContainer;
    private View llPriceDropsContainer;
    private View llTopPicksContainer;
    private View llCommunityReviewsContainer;
    private View llAllRestaurantsContainer;
    private View llExploreMoreRestaurantsContainer;

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
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private boolean isInitialDataLoaded = false;
    private boolean isRestaurantsLoading = false;
    private boolean isFoodsLoading = false;
    private boolean isReviewsLoading = false;
    private boolean isCategoriesLoading = false;
    private long lastLoadTime = 0;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;

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

        layoutShimmer = view.findViewById(R.id.layoutShimmer);
        llFeaturedRestaurantsContainer = view.findViewById(R.id.llFeaturedRestaurantsContainer);
        llHotPicksContainer = view.findViewById(R.id.llHotPicksContainer);
        llPriceDropsContainer = view.findViewById(R.id.llPriceDropsContainer);
        llTopPicksContainer = view.findViewById(R.id.llTopPicksContainer);
        llCommunityReviewsContainer = view.findViewById(R.id.llCommunityReviewsContainer);
        llAllRestaurantsContainer = view.findViewById(R.id.llAllRestaurantsContainer);
        llExploreMoreRestaurantsContainer = view.findViewById(R.id.llExploreMoreRestaurantsContainer);
        
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> {
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
        setupQuickPicks(view);
        setupFilters(view);
        fetchFeaturedRestaurants(view);
        setupTabs(view);
        setupFeaturedFoods(view);
        setupPromoCarousel(view);
        setupProfessionalSections(view);
        setupCommunityReviews(view);
        
        lastLoadTime = System.currentTimeMillis();
        isInitialDataLoaded = true;

        // Professional Standard: Dynamically adjust to Activity Header height
        view.post(() -> {
            if (getActivity() instanceof MainActivity) {
                int totalHeaderHeight = ((MainActivity) getActivity()).getHeaderTotalHeight();
                View search = view.findViewById(R.id.llSearchContainer);
                if (search != null && totalHeaderHeight > 0) {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = 
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) search.getLayoutParams();
                    lp.topMargin = totalHeaderHeight;
                    search.setLayoutParams(lp);
                }
            }
        });
        
        try {
            fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity());
            requestLocationPermission();
        } catch (Exception ignored) {}

        View cvPromo = view.findViewById(R.id.rlPromoCarouselContainer);
        if (cvPromo != null) cvPromo.setOnClickListener(v -> {
            // Replaced by specific promo item click handling in setupPromoCarousel
        });

        FavoritesManager.getInstance().addListener(this);

        View btnViewAllTrending = view.findViewById(R.id.tvViewAllFeatured);
        if (btnViewAllTrending != null) {
            btnViewAllTrending.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AllFoodsActivity.class);
                intent.putExtra("title", "Trending Dishes");
                startActivity(intent);
            });
        }

        View btnSeeAllAllRestaurants = view.findViewById(R.id.tvSeeAllAllRestaurants);
        if (btnSeeAllAllRestaurants != null) {
            btnSeeAllAllRestaurants.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AllRestaurantsActivity.class);
                startActivity(intent);
            });
        }

        View btnViewAllHotPicks = view.findViewById(R.id.tvViewAllHotPicks);
        if (btnViewAllHotPicks != null) {
            btnViewAllHotPicks.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AllFoodsActivity.class);
                intent.putExtra("title", "Hot Picks");
                intent.putExtra("filter", "hot");
                startActivity(intent);
            });
        }

        View btnViewAllPriceDrops = view.findViewById(R.id.tvViewAllPriceDrops);
        if (btnViewAllPriceDrops != null) {
            btnViewAllPriceDrops.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AllFoodsActivity.class);
                intent.putExtra("title", "Price Drops");
                intent.putExtra("filter", "price_drop");
                startActivity(intent);
            });
        }
    }

    private void setupQuickPicks(View root) {
        RecyclerView rv = root.findViewById(R.id.rvQuickPicks);
        if (rv == null) return;
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        quickPickAdapter = new QuickPickAdapter(getContext(), query -> {
            SearchView searchView = root.findViewById(R.id.searchViewHome);
            if (searchView != null) {
                searchView.setQuery(query, true);
            }
        });
        rv.setAdapter(quickPickAdapter);
    }

    private void setupProfessionalSections(View root) {
        RecyclerView rvPriceDrops = root.findViewById(R.id.rvPriceDrops);
        if (rvPriceDrops != null) {
            rvPriceDrops.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            priceDropAdapter = new FoodHorizontalAdapter(getContext());
            rvPriceDrops.setAdapter(priceDropAdapter);
        }

        RecyclerView rvHotPicks = root.findViewById(R.id.rvHotPicks);
        if (rvHotPicks != null) {
            rvHotPicks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            hotPickAdapter = new FoodHorizontalAdapter(getContext());
            rvHotPicks.setAdapter(hotPickAdapter);
        }

        RecyclerView rvFeaturedRest = root.findViewById(R.id.rvFeaturedRestaurants);
        if (rvFeaturedRest != null) {
            rvFeaturedRest.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            featuredRestAdapter = new RestaurantCompactAdapter(getContext());
            rvFeaturedRest.setAdapter(featuredRestAdapter);
        }

        RecyclerView rvTopPicks = root.findViewById(R.id.rvTopPicks);
        if (rvTopPicks != null) {
            rvTopPicks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            topRatedAdapter = new RestaurantCompactAdapter(getContext());
            rvTopPicks.setAdapter(topRatedAdapter);
        }

        RecyclerView rvExploreMore = root.findViewById(R.id.rvExploreMoreRestaurants);
        if (rvExploreMore != null) {
            rvExploreMore.setLayoutManager(new LinearLayoutManager(getContext()));
            exploreMoreAdapter = createRestaurantAdapter(new ArrayList<>());
            rvExploreMore.setAdapter(exploreMoreAdapter);
        }
    }

    private RestaurantAdapter createRestaurantAdapter(List<Restaurant> list) {
        return new RestaurantAdapter(getContext(), list, restaurant -> {
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
            intent.putExtra("deliveryCharge", restaurant.getDeliveryCharge());
            if (restaurant.getLocation() != null) {
                intent.putExtra("city", restaurant.getLocation().getCity());
                intent.putExtra("district", restaurant.getLocation().getDistrict());
            }
            startActivity(intent);
        });
    }

    private void setupCommunityReviews(View root) {
        if (root == null || !isAdded() || isReviewsLoading) return;
        RecyclerView rvReviews = root.findViewById(R.id.rvCommunityReviews);
        if (rvReviews == null) return;
        
        isReviewsLoading = true;
        rvReviews.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        reviewsAdapter = new ReviewHomeAdapter(getContext());
        rvReviews.setAdapter(reviewsAdapter);
        
        String token = SharedPrefManager.getAccessToken(getContext());
        if (token != null && !token.isEmpty()) {
            token = "Bearer " + token;
        }
        
        String city = SharedPrefManager.getCity(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.getReviews(token, null, null, false, 1, "-created_at", null, city).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewListResponse> call, @NonNull Response<ReviewListResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    List<com.ps.dinear.data.model.Review> reviews = response.body().getResults();
                    if (reviews != null && !reviews.isEmpty()) {
                        reviewsAdapter.setData(reviews);
                        if (llCommunityReviewsContainer != null) {
                            llCommunityReviewsContainer.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (llCommunityReviewsContainer != null) {
                            llCommunityReviewsContainer.setVisibility(View.GONE);
                        }
                    }
                }
                isReviewsLoading = false;
            }
            @Override
            public void onFailure(@NonNull Call<ReviewListResponse> call, @NonNull Throwable t) {
                isReviewsLoading = false;
                if (isAdded() && llCommunityReviewsContainer != null) {
                    llCommunityReviewsContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapters();
        
        // Removed redundant API calls that are already triggered in onViewCreated.
        // We only re-fetch if lastResResults is empty AND it's not the initial view creation.
        // Or better, just rely on refreshData() if the user explicitly wants to refresh.
        
        if (carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
            carouselHandler.postDelayed(carouselRunnable, 4000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        carouselHandler.removeCallbacks(carouselRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FavoritesManager.getInstance().removeListener(this);
    }

    @Override
    public void onFavoritesUpdated() {
        refreshAdapters();
    }

    public void refreshData() {
        if (!isAdded() || getView() == null) return;
        
        // Prevent redundant loading on startup or rapid successive calls.
        // We only allow a full refresh if it's been at least 3 seconds since the last one.
        long currentTime = System.currentTimeMillis();
        if (isInitialDataLoaded && (currentTime - lastLoadTime > 3000)) {
            lastLoadTime = currentTime;
            setupFilters(getView());
            setupRestaurants(getView());
            setupFeaturedFoods(getView());
            setupCommunityReviews(getView());
        }
    }

    private void refreshAdapters() {
        if (restaurantAdapter != null) restaurantAdapter.notifyDataSetChanged();
        if (menuAdapter != null) menuAdapter.notifyDataSetChanged();
        if (featuredFoodAdapter != null) featuredFoodAdapter.notifyDataSetChanged();
        if (priceDropAdapter != null) priceDropAdapter.notifyDataSetChanged();
        if (hotPickAdapter != null) hotPickAdapter.notifyDataSetChanged();
        if (featuredRestAdapter != null) featuredRestAdapter.notifyDataSetChanged();
        if (topRatedAdapter != null) topRatedAdapter.notifyDataSetChanged();
        if (reviewsAdapter != null) reviewsAdapter.notifyDataSetChanged();
        if (exploreMoreAdapter != null) exploreMoreAdapter.notifyDataSetChanged();
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
                    setupFeaturedFoods(getView());
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
        View quickPicks = root.findViewById(R.id.llQuickPicksContainer);
        View promoBanner = root.findViewById(R.id.rlPromoCarouselContainer);
        View catHeader = root.findViewById(R.id.rlCategoriesHeader);
        View filters = root.findViewById(R.id.rvFilters);
        View exploreHeader = root.findViewById(R.id.rlExploreHeader);
        View trendingContainer = root.findViewById(R.id.llTrendingContainer);

        // Headers and new sections
        if (quickPicks != null) quickPicks.setVisibility(visibility);
        if (promoBanner != null) promoBanner.setVisibility(visibility);
        if (catHeader != null) catHeader.setVisibility(visibility);
        if (filters != null) filters.setVisibility(visibility);
        if (exploreHeader != null) exploreHeader.setVisibility(visibility);
        if (trendingContainer != null) trendingContainer.setVisibility(visibility);

        // Professional Containers
        if (llPriceDropsContainer != null) llPriceDropsContainer.setVisibility(visibility);
        if (llHotPicksContainer != null) llHotPicksContainer.setVisibility(visibility);
        if (llFeaturedRestaurantsContainer != null) llFeaturedRestaurantsContainer.setVisibility(visibility);
        if (llTopPicksContainer != null) llTopPicksContainer.setVisibility(visibility);
        if (llCommunityReviewsContainer != null) llCommunityReviewsContainer.setVisibility(visibility);
        if (llExploreMoreRestaurantsContainer != null) llExploreMoreRestaurantsContainer.setVisibility(visibility);

        if (show) {
            View noRes = root.findViewById(R.id.llNoResults);
            if (noRes != null) noRes.setVisibility(View.GONE);
            View scroll = root.findViewById(R.id.nestedScrollView);
            if (scroll != null) scroll.setVisibility(View.VISIBLE);
        }
    }

    private void setupFilters(View root) {
        if (root == null || !isAdded() || isCategoriesLoading) return;
        RecyclerView rvFilters = root.findViewById(R.id.rvFilters);
        if (rvFilters == null || (rvFilters.getAdapter() != null && !isInitialDataLoaded)) return;
        
        isCategoriesLoading = true;
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.getCategories().enqueue(new Callback<List<Restaurant.Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Restaurant.Category>> call, @NonNull Response<List<Restaurant.Category>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) notifyActivitySuccess();
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
                isCategoriesLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<List<Restaurant.Category>> call, @NonNull Throwable t) {
                isCategoriesLoading = false;
                if (!isAdded()) return;
                notifyActivityFailure();
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
        if (root == null || !isAdded() || isFoodsLoading) return;
        RecyclerView rvFeatured = root.findViewById(R.id.rvFeaturedFoods);
        if (rvFeatured == null) return;
        
        isFoodsLoading = true;
        if (featuredFoodAdapter == null) {
            rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            featuredFoodAdapter = new FeaturedFoodAdapter(getContext());
            rvFeatured.setAdapter(featuredFoodAdapter);
        }

        String city = SharedPrefManager.getCity(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        
        api.search("", city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    notifyActivitySuccess();
                    List<MenuItem> foods = response.body().getFoodItems();
                    if (foods != null && !foods.isEmpty()) {
                        java.util.Collections.shuffle(foods);
                        featuredFoodAdapter.setData(foods);
                        
                        // Populate Price Drops
                        List<MenuItem> priceDrops = new ArrayList<>();
                        for (MenuItem item : foods) {
                            if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0 && item.getDiscountPrice() < item.getPrice()) {
                                priceDrops.add(item);
                            }
                        }
                        if (priceDropAdapter != null) {
                            priceDropAdapter.setData(priceDrops);
                            if (llPriceDropsContainer != null) {
                                llPriceDropsContainer.setVisibility(priceDrops.isEmpty() ? View.GONE : View.VISIBLE);
                            }
                        }
                        
                        // Populate Hot Picks (Elite/Trending)
                        List<MenuItem> hotPicks = new ArrayList<>(foods);
                        java.util.Collections.shuffle(hotPicks);
                        if (hotPickAdapter != null) {
                            hotPickAdapter.setData(hotPicks);
                            if (llHotPicksContainer != null) {
                                llHotPicksContainer.setVisibility(hotPicks.isEmpty() ? View.GONE : View.VISIBLE);
                            }
                        }

                        featuredDish = foods.get(0);
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).setFeaturedDish(featuredDish);
                        }
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
                        // updatePromoBanner(getView()); // Replaced by static carousel
                    } else {
                        if (llPriceDropsContainer != null) llPriceDropsContainer.setVisibility(View.GONE);
                        if (llHotPicksContainer != null) llHotPicksContainer.setVisibility(View.GONE);
                    }
                } else {
                    notifyActivityFailure();
                }
                isFoodsLoading = false;
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                isFoodsLoading = false;
                if (!isAdded()) return;
                notifyActivityFailure();
            }
        });
    }

    private void setupPromoCarousel(View root) {
        if (root == null || !isAdded()) return;

        vpPromoCarousel = root.findViewById(R.id.vpPromoCarousel);
        tlPromoIndicator = root.findViewById(R.id.tlPromoIndicator);

        if (vpPromoCarousel == null) return;

        List<PromoCarouselAdapter.PromoItem> promos = new ArrayList<>();
        promos.add(new PromoCarouselAdapter.PromoItem("1", "FLASH SALE", "Epic Burger Feast\n50% Off Today", R.drawable.burger, "Burger"));
        promos.add(new PromoCarouselAdapter.PromoItem("2", "FESTIVE DEAL", "Celebrate the Season\nwith 30% Off", R.drawable.pizza, "Pizza"));
        promos.add(new PromoCarouselAdapter.PromoItem("3", "NEW TECH", "The Future is Here:\nExplore in AR", R.drawable.momo, "Momo"));

        PromoCarouselAdapter adapter = new PromoCarouselAdapter(promos, item -> {
            SearchView searchView = root.findViewById(R.id.searchViewHome);
            if (searchView != null) {
                searchView.setQuery(item.actionQuery, true);
            }
        });

        vpPromoCarousel.setAdapter(adapter);
        
        // Advanced Professional Transformer
        androidx.viewpager2.widget.CompositePageTransformer compositePageTransformer = new androidx.viewpager2.widget.CompositePageTransformer();
        compositePageTransformer.addTransformer(new androidx.viewpager2.widget.MarginPageTransformer((int) (getResources().getDisplayMetrics().density * 16)));
        compositePageTransformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.9f + r * 0.1f);
            page.setAlpha(0.8f + r * 0.2f);
        });
        vpPromoCarousel.setPageTransformer(compositePageTransformer);
        
        // Pseudo-infinite scroll setup
        int startPos = (Integer.MAX_VALUE / 2);
        startPos = startPos - (startPos % promos.size());
        vpPromoCarousel.setCurrentItem(startPos, false);

        // Setup indicators for pseudo-infinite scroll
        if (tlPromoIndicator != null) {
            tlPromoIndicator.removeAllTabs();
            for (int i = 0; i < promos.size(); i++) {
                tlPromoIndicator.addTab(tlPromoIndicator.newTab());
            }
            vpPromoCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    TabLayout.Tab tab = tlPromoIndicator.getTabAt(position % promos.size());
                    if (tab != null) tab.select();
                }
            });
        }

        // Auto-sliding logic (Professional Interval: 6 seconds)
        carouselHandler.removeCallbacks(carouselRunnable);
        carouselRunnable = () -> {
            if (vpPromoCarousel != null && isAdded()) {
                vpPromoCarousel.setCurrentItem(vpPromoCarousel.getCurrentItem() + 1, true);
                carouselHandler.postDelayed(carouselRunnable, 6000);
            }
        };
        carouselHandler.postDelayed(carouselRunnable, 6000);
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
        
        if (layoutShimmer != null) layoutShimmer.setVisibility(View.GONE);
        View scroll = root.findViewById(R.id.nestedScrollView);
        if (scroll != null) scroll.setVisibility(View.VISIBLE);
    }

    private void performSearch(View root) {
        if (root == null || !isAdded()) return;
        if (layoutShimmer != null) layoutShimmer.setVisibility(View.VISIBLE);
        View noRes = root.findViewById(R.id.llNoResults);
        if (noRes != null) noRes.setVisibility(View.GONE);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);

        String city = SharedPrefManager.getCity(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.search(currentSearchQuery, city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    if (layoutShimmer != null) layoutShimmer.setVisibility(View.GONE);
                    notifyActivitySuccess();
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
                } else {
                    notifyActivityFailure();
                }
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                notifyActivityFailure();
            }
        });
    }

    private void setupRestaurants(View root) {
        if (root == null || !isAdded() || isRestaurantsLoading) return;
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        if (recyclerView == null) return;
        
        isRestaurantsLoading = true;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        View noRes = root.findViewById(R.id.llNoResults);
        if (noRes != null) noRes.setVisibility(View.GONE);

        boolean isHome = currentCategory.equals("Home");
        View cvPromo = root.findViewById(R.id.rlPromoCarouselContainer);
        if (cvPromo != null) cvPromo.setVisibility(View.VISIBLE); // Carousel stays persistent across categories
        
        if (llAllRestaurantsContainer != null) {
            llAllRestaurantsContainer.setVisibility(View.VISIBLE);
        }

        if (layoutShimmer != null) {
            layoutShimmer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        String selectedCity = SharedPrefManager.getCity(getContext());
        String apiCategory = currentCategory.equals("Home") ? null : currentCategory;
        
        ApiService apiService = RetrofitClient.getClient(getContext()).create(ApiService.class);
        apiService.getRestaurants(selectedCity, apiCategory).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(@NonNull Call<List<Restaurant>> call, @NonNull Response<List<Restaurant>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    if (layoutShimmer != null) layoutShimmer.setVisibility(View.GONE);
                    notifyActivitySuccess();
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
                        
                        // Populate Featured and Top Picks (only on Home)
                        if (isHome) {
                            List<Restaurant> featured = new ArrayList<>();
                            List<Restaurant> topPicks = new ArrayList<>();
                            for (Restaurant r : restaurants) {
                                if (r.isFeatured()) featured.add(r);
                                if (r.getRating() >= 4.0) topPicks.add(r);
                            }
                            if (featuredRestAdapter != null) {
                                featuredRestAdapter.setData(featured);
                                if (llFeaturedRestaurantsContainer != null) {
                                    llFeaturedRestaurantsContainer.setVisibility(featured.isEmpty() ? View.GONE : View.VISIBLE);
                                }
                            }
                            if (topRatedAdapter != null) {
                                topRatedAdapter.setData(topPicks);
                                if (llTopPicksContainer != null) {
                                    llTopPicksContainer.setVisibility(topPicks.isEmpty() ? View.GONE : View.VISIBLE);
                                }
                            }
                            
                            // Split logic: Only 4 in the middle section, all in the bottom section
                            List<Restaurant> limited = restaurants.size() > 4 ? new ArrayList<>(restaurants.subList(0, 4)) : new ArrayList<>(restaurants);
                            if (restaurantAdapter == null) {
                                restaurantAdapter = createRestaurantAdapter(limited);
                                recyclerView.setAdapter(restaurantAdapter);
                            } else {
                                restaurantAdapter.updateList(limited);
                            }
                            
                            if (exploreMoreAdapter != null) {
                                exploreMoreAdapter.updateList(restaurants);
                                if (llExploreMoreRestaurantsContainer != null) {
                                    llExploreMoreRestaurantsContainer.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            if (llFeaturedRestaurantsContainer != null) llFeaturedRestaurantsContainer.setVisibility(View.GONE);
                            if (llTopPicksContainer != null) llTopPicksContainer.setVisibility(View.GONE);
                            if (llExploreMoreRestaurantsContainer != null) llExploreMoreRestaurantsContainer.setVisibility(View.GONE);

                            if (restaurantAdapter == null) {
                                restaurantAdapter = createRestaurantAdapter(new ArrayList<>(restaurants));
                                recyclerView.setAdapter(restaurantAdapter);
                            } else {
                                restaurantAdapter.updateList(restaurants);
                            }
                        }
                    }
                    View scroll = root.findViewById(R.id.nestedScrollView);
                    if (scroll != null) scroll.setVisibility(View.VISIBLE);
                } else {
                    notifyActivityFailure();
                }
                isRestaurantsLoading = false;
            }
            @Override
            public void onFailure(@NonNull Call<List<Restaurant>> call, @NonNull Throwable t) {
                isRestaurantsLoading = false;
                if (!isAdded()) return;
                notifyActivityFailure();
            }
        });
    }

    private void fetchFeaturedRestaurants(View root) {
        // In this implementation, Featured Restaurants are handled within setupRestaurants
        // as they come from the same main restaurant list. 
        // We ensure it works by making sure setupRestaurants is called.
        setupRestaurants(root);
    }

    private void notifyActivityFailure() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showErrorOverlay();
        }
    }

    private void notifyActivitySuccess() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideErrorOverlay();
        }
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