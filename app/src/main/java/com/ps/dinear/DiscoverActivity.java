package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Restaurant;
import java.util.List;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private MapLibreMap map;
    private View shimmerDiscover;
    private RecyclerView rvRestaurants;
    private RestaurantAdapter adapter;
    private View fabToggleList;
    private View llDiscoverError;
    private View btnDiscoverRetry;
    private ProgressBar pbDiscoverRetry;
    
    private boolean isListVisible = true;
    private List<Restaurant> restaurantList = new java.util.ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private int focusRestaurantId = -1;
    private static final String MAPTILER_API_KEY = "CqA49jSeY7aVGUdjsSmL";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_discover);

        mapView = findViewById(R.id.mapView);
        shimmerDiscover = findViewById(R.id.shimmerDiscover);
        rvRestaurants = findViewById(R.id.rvDiscoverRestaurants);
        fabToggleList = findViewById(R.id.fabToggleList);
        llDiscoverError = findViewById(R.id.llDiscoverError);
        btnDiscoverRetry = findViewById(R.id.btnDiscoverRetry);
        pbDiscoverRetry = findViewById(R.id.pbDiscoverRetry);

        if (getIntent() != null) {
            focusRestaurantId = getIntent().getIntExtra("focusRestaurantId", -1);
        }
        
        setupRecyclerView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        fabToggleList.setOnClickListener(v -> toggleRestaurantList());
        
        btnDiscoverRetry.setOnClickListener(v -> {
            setOverlayLoading(true);
            fetchRestaurants();
        });

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rvRestaurants, (v, insets) -> {
            androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            mlp.bottomMargin = (int) (16 * density) + navBars.bottom;
            v.setLayoutParams(mlp);
            
            android.view.ViewGroup.MarginLayoutParams fabMlp = (android.view.ViewGroup.MarginLayoutParams) fabToggleList.getLayoutParams();
            fabMlp.bottomMargin = (int) (20 * density) + navBars.bottom;
            fabToggleList.setLayoutParams(fabMlp);
            
            return insets;
        });

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.btnBackDiscover).setOnClickListener(v -> finish());
    }

    private void setOverlayLoading(boolean loading) {
        if (btnDiscoverRetry != null) btnDiscoverRetry.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (pbDiscoverRetry != null) pbDiscoverRetry.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showErrorOverlay() {
        handler.removeCallbacksAndMessages(null);

        Runnable showTask = () -> {
            setOverlayLoading(false);
            if (llDiscoverError != null && llDiscoverError.getVisibility() != View.VISIBLE) {
                llDiscoverError.setAlpha(0f);
                llDiscoverError.setVisibility(View.VISIBLE);
                llDiscoverError.animate().alpha(1f).setDuration(300).start();
            }
        };

        showTask.run();
    }

    private void hideErrorOverlay() {
        handler.removeCallbacksAndMessages(null);
        setOverlayLoading(false);
        if (llDiscoverError == null || llDiscoverError.getVisibility() == View.GONE) return;
        
        llDiscoverError.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> {
                    llDiscoverError.setVisibility(View.GONE);
                    llDiscoverError.setAlpha(1f);
                });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        focusRestaurantId = intent.getIntExtra("focusRestaurantId", -1);
        if (focusRestaurantId != -1 && !restaurantList.isEmpty()) {
            handleFocusRestaurant();
        }
    }

    private void handleFocusRestaurant() {
        if (focusRestaurantId == -1 || restaurantList.isEmpty()) return;
        
        for (int i = 0; i < restaurantList.size(); i++) {
            Restaurant r = restaurantList.get(i);
            if (r.getId() == focusRestaurantId) {
                rvRestaurants.smoothScrollToPosition(i);
                if (map != null && r.getLatitude() != 0) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(r.getLatitude(), r.getLongitude()), 16));
                }
                break;
            }
        }
    }

    private void toggleRestaurantList() {
        FloatingActionButton fab = (FloatingActionButton) fabToggleList;
        if (isListVisible) {
            rvRestaurants.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() -> {
                rvRestaurants.setVisibility(View.GONE);
                fab.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            });
            isListVisible = false;
        } else {
            rvRestaurants.setVisibility(View.VISIBLE);
            rvRestaurants.setAlpha(0f);
            rvRestaurants.setTranslationY(100f);
            rvRestaurants.animate().alpha(1f).translationY(0f).setDuration(200);
            fab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isListVisible = true;
        }
    }

    private void setupRecyclerView() {
        rvRestaurants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new RestaurantAdapter(this, restaurantList, R.layout.item_restaurant_discover, new RestaurantAdapter.OnRestaurantClickListener() {
            @Override
            public void onRestaurantClick(Restaurant restaurant) {
                if (restaurant.getLatitude() != 0) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(restaurant.getLatitude(), restaurant.getLongitude()), 16));
                }
            }

            @Override
            public void onDetailClick(Restaurant restaurant) {
                Intent intent = new Intent(DiscoverActivity.this, RestaurantDetailsActivity.class);
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
            }
        });
        rvRestaurants.setAdapter(adapter);
        new PagerSnapHelper().attachToRecyclerView(rvRestaurants);

        rvRestaurants.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (lm != null) {
                        int pos = lm.findFirstCompletelyVisibleItemPosition();
                        if (pos != -1 && pos < restaurantList.size()) {
                            Restaurant r = restaurantList.get(pos);
                            if (r.getLatitude() != 0) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(r.getLatitude(), r.getLongitude()), 15));
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.map = mapLibreMap;
        
        mapView.addOnDidFailLoadingMapListener(errorMessage -> {
            runOnUiThread(() -> {
                showErrorOverlay();
            });
        });

        String styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=" + MAPTILER_API_KEY;
        map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            enableLocationComponent(style);
            fetchRestaurants();
        });

        map.setOnMarkerClickListener(marker -> {
            for (int i = 0; i < restaurantList.size(); i++) {
                if (restaurantList.get(i).getName().equals(marker.getTitle())) {
                    rvRestaurants.smoothScrollToPosition(i);
                    break;
                }
            }
            return false;
        });
    }

    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            );
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void fetchRestaurants() {
        if (shimmerDiscover != null) shimmerDiscover.setVisibility(View.VISIBLE);
        if (rvRestaurants != null) rvRestaurants.setVisibility(View.GONE);
        
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        String selectedCity = SharedPrefManager.getCity(this);
        
        apiService.getRestaurants(selectedCity, null).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(Call<List<Restaurant>> call, Response<List<Restaurant>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hideErrorOverlay();
                    if (shimmerDiscover != null) shimmerDiscover.setVisibility(View.GONE);
                    List<Restaurant> list = response.body();
                    
                    if (userLocation != null) {
                        calculateDistances(list);
                    }
                    
                    restaurantList.clear();
                    restaurantList.addAll(list);
                    adapter.notifyDataSetChanged();
                    
                    if (rvRestaurants != null) rvRestaurants.setVisibility(View.VISIBLE);
                    displayRestaurantsOnMap(list);

                    if (focusRestaurantId != -1) {
                        handleFocusRestaurant();
                    }
                } else {
                    showErrorOverlay();
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant>> call, Throwable t) {
                showErrorOverlay();
            }
        });
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            if (adapter != null) {
                adapter.setLocationPermissionDenied(false);
            }
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    if (!restaurantList.isEmpty()) {
                        calculateDistances(restaurantList);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void calculateDistances(List<Restaurant> restaurants) {
        if (userLocation == null) return;
        for (Restaurant r : restaurants) {
            if (r != null && r.getLatitude() != 0 && r.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        r.getLatitude(), r.getLongitude(), results);
                r.setDistance(results[0] / 1000.0);
            }
        }
    }

    private void displayRestaurantsOnMap(List<Restaurant> restaurants) {
        if (map == null) return;
        
        if (restaurants.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int pointsCount = 0;

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getLatitude() != 0 && restaurant.getLongitude() != 0) {
                LatLng position = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
                map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(restaurant.getName())
                    .snippet(restaurant.getAddress()));
                
                builder.include(position);
                pointsCount++;
            }
        }

        if (pointsCount > 0) {
            LatLngBounds bounds = builder.build();
            map.setLatLngBoundsForCameraTarget(bounds);
            map.setMinZoomPreference(11);
            map.setMaxZoomPreference(18);
            
            if (pointsCount == 1) {
                for (Restaurant r : restaurants) {
                    if (r.getLatitude() != 0 && r.getLongitude() != 0) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(r.getLatitude(), r.getLongitude()), 14));
                        break;
                    }
                }
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            }
        } else {
            if (map.getLocationComponent().getLastKnownLocation() != null) {
                android.location.Location lastLoc = map.getLocationComponent().getLastKnownLocation();
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude()), 12));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (adapter != null) {
                    adapter.setLocationPermissionDenied(false);
                }
                getCurrentLocation();
                if (map != null && map.getStyle() != null) {
                    enableLocationComponent(map.getStyle());
                }
            } else {
                if (adapter != null) {
                    adapter.setLocationPermissionDenied(true);
                }
            }
        }
    }

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override
    protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
