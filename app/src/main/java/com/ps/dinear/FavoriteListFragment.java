package com.ps.dinear;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.SearchResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteListFragment extends Fragment implements FavoritesManager.FavoritesListener {

    private static final String ARG_TYPE = "type";

    private int type;
    private RecyclerView rvFavItems;
    private View shimmerFav;
    private View llNoResults;

    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter foodAdapter;

    private List<Restaurant> favRestaurants = new ArrayList<>();
    private List<MenuItem> favFoods = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    
    private boolean isLoading = false;

    public static FavoriteListFragment newInstance(int type) {
        FavoriteListFragment fragment = new FavoriteListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFavItems = view.findViewById(R.id.rvFavItems);
        shimmerFav = view.findViewById(R.id.shimmerFav);
        llNoResults = view.findViewById(R.id.llFavNoResults);

        if (rvFavItems != null) {
            rvFavItems.setLayoutManager(new LinearLayoutManager(getContext()));
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rvFavItems, (v, insets) -> {
                androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                
                if (isAdded() && getContext() != null) {
                    float density = v.getResources().getDisplayMetrics().density;
                    // Keep a small standard list padding from tabs (8dp)
                    int topPadding = (int)(8 * density);
                    v.setPadding(v.getPaddingLeft(), topPadding, 
                               v.getPaddingRight(), (int) (40 * density) + navBars.bottom);
                }
                return insets;
            });
        }

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        } catch (Exception ignored) {}
        
        FavoritesManager.getInstance().addListener(this);
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isLoading && favRestaurants.isEmpty() && favFoods.isEmpty()) {
            loadFavorites();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FavoritesManager.getInstance().removeListener(this);
    }

    @Override
    public void onFavoritesUpdated() {
        if (isAdded()) {
            loadFavorites();
        }
    }

    public void refreshData() {
        if (!isAdded()) return;
        loadFavorites();
    }

    private void loadFavorites() {
        if (!isAdded() || getContext() == null || isLoading) return;

        if (!SharedPrefManager.isLoggedIn(getContext())) {
            if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
            return;
        }

        isLoading = true;
        if (shimmerFav != null) shimmerFav.setVisibility(View.VISIBLE);
        if (llNoResults != null) llNoResults.setVisibility(View.GONE);
        if (rvFavItems != null) rvFavItems.setVisibility(View.GONE);

        String token = "Bearer " + SharedPrefManager.getAccessToken(getContext());
        ApiService api = RetrofitClient.getClient(getContext()).create(ApiService.class);
        api.getFavoriteDetails(token).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                isLoading = false;
                if (!isAdded() || getView() == null) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    if (shimmerFav != null) shimmerFav.setVisibility(View.GONE);
                    notifyActivitySuccess();
                    favRestaurants = response.body().getRestaurants();
                    favFoods = response.body().getFoodItems();
                    fetchLocationAndUpdateDistances();
                    updateView();
                } else {
                    notifyActivityFailure();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                isLoading = false;
                if (!isAdded() || getView() == null) return;
                notifyActivityFailure();
            }
        });
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

    private void fetchLocationAndUpdateDistances() {
        if (!isAdded() || getContext() == null || fusedLocationClient == null) return;

        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && isAdded()) {
                    userLocation = location;
                    calculateDistances(favRestaurants);
                    if (restaurantAdapter != null) {
                        restaurantAdapter.notifyDataSetChanged();
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
                android.location.Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        r.getLatitude(), r.getLongitude(), results);
                r.setDistance(results[0] / 1000.0);
            }
        }
    }

    private void updateView() {
        if (!isAdded() || getView() == null || rvFavItems == null) return;
        
        boolean hasItems;

        if (type == 0) {
            if (restaurantAdapter == null) {
                restaurantAdapter = new RestaurantAdapter(getContext(), new ArrayList<>(favRestaurants), restaurant -> {
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
                rvFavItems.setAdapter(restaurantAdapter);
            } else {
                restaurantAdapter.updateList(favRestaurants);
            }
            hasItems = !favRestaurants.isEmpty();
        } else {
            if (foodAdapter == null) {
                foodAdapter = new MenuAdapter(getContext());
                rvFavItems.setAdapter(foodAdapter);
            }
            foodAdapter.setRestaurants(new ArrayList<>());
            foodAdapter.setData(favFoods);
            hasItems = !favFoods.isEmpty();
        }

        rvFavItems.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (llNoResults != null) llNoResults.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }
}
