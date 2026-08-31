package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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
    private ProgressBar progressBar;
    private RecyclerView rvRestaurants;
    private RestaurantAdapter adapter;
    private View fabToggleList;
    private boolean isListVisible = true;
    private List<Restaurant> restaurantList = new java.util.ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private static final String MAPTILER_API_KEY = "CqA49jSeY7aVGUdjsSmL";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_discover);

        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBarDiscover);
        rvRestaurants = findViewById(R.id.rvDiscoverRestaurants);
        fabToggleList = findViewById(R.id.fabToggleList);
        
        setupRecyclerView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        fabToggleList.setOnClickListener(v -> toggleRestaurantList());

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.btnBackDiscover).setOnClickListener(v -> finish());
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
                // When a card is clicked, move the map to it
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
                if (restaurant.getLocation() != null) {
                    intent.putExtra("city", restaurant.getLocation().getCity());
                    intent.putExtra("district", restaurant.getLocation().getDistrict());
                }
                startActivity(intent);
            }
        });
        rvRestaurants.setAdapter(adapter);

        // Snap effect to center the cards
        new PagerSnapHelper().attachToRecyclerView(rvRestaurants);

        // Sync map when scrolling through cards
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
        
        // Add error listener to debug the 403 Forbidden issue
        mapView.addOnDidFailLoadingMapListener(errorMessage -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Map error: " + errorMessage, Toast.LENGTH_LONG).show();
            });
        });

        // Trying basic style as a test
        String styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=" + MAPTILER_API_KEY;
        map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            enableLocationComponent(style);
            fetchRestaurants();
        });

        map.setOnMarkerClickListener(marker -> {
            // Find restaurant by title and scroll to it in the list
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
        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        
        String selectedCity = SharedPrefManager.getCity(this);
        Log.d("DiscoverActivity", "Fetching restaurants for city: " + selectedCity);
        
        apiService.getRestaurants(selectedCity, null).enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(Call<List<Restaurant>> call, Response<List<Restaurant>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> list = response.body();
                    Log.d("DiscoverActivity", "Successfully fetched " + list.size() + " restaurants");
                    
                    if (userLocation != null) {
                        calculateDistances(list);
                    }
                    
                    restaurantList.clear();
                    restaurantList.addAll(list);
                    adapter.notifyDataSetChanged();
                    
                    displayRestaurantsOnMap(list);
                } else {
                    Log.e("DiscoverActivity", "Failed to fetch restaurants. Code: " + response.code());
                    Toast.makeText(DiscoverActivity.this, "Failed to load restaurants", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("DiscoverActivity", "Network error while fetching restaurants", t);
                Toast.makeText(DiscoverActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            if (r.getLatitude() != 0 && r.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        r.getLatitude(), r.getLongitude(), results);
                r.setDistance(results[0] / 1000.0); // Convert to km
            }
        }
    }

    private void displayRestaurantsOnMap(List<Restaurant> restaurants) {
        if (map == null) return;
        
        if (restaurants.isEmpty()) {
            Log.d("DiscoverActivity", "No restaurants found in the response");
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int pointsCount = 0;

        for (Restaurant restaurant : restaurants) {
            Log.d("DiscoverActivity", "Restaurant: " + restaurant.getName() + " at " + restaurant.getLatitude() + ", " + restaurant.getLongitude());
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
            Log.d("DiscoverActivity", "Zooming to " + pointsCount + " points");
            LatLngBounds bounds = builder.build();
            
            // Restrict camera to this district/area to reduce tile usage
            map.setLatLngBoundsForCameraTarget(bounds);
            map.setMinZoomPreference(11); // Prevent zooming out to see the whole world
            map.setMaxZoomPreference(18); // Limit high-res tile requests
            
            if (pointsCount == 1) {
                // If only one point, find it and zoom manually to avoid Builder.build() exception
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
            Log.w("DiscoverActivity", "No valid restaurant points to zoom to. Defaulting to user location if available.");
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
