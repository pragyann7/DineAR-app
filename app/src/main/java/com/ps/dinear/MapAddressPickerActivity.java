package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapAddressPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private MapLibreMap map;
    private TextView tvCurrentAddress;
    private LatLng selectedLatLng;
    private LatLngBounds restrictedBounds;
    private String currentAddressLine = "";
    private String currentCity = "";
    private String currentDistrict = "";
    
    private static final String MAPTILER_API_KEY = "CqA49jSeY7aVGUdjsSmL";
    private FusedLocationProviderClient fusedLocationClient;
    private String selectedCity;
    private String selectedDistrict;
    
    private static final int REQ_CHECK_SETTINGS = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map_address_picker);

        selectedCity = SharedPrefManager.getCity(this);
        selectedDistrict = SharedPrefManager.getDistrict(this);
        
        mapView = findViewById(R.id.mapViewPicker);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.btnBackMapPicker).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> confirmLocation());
        findViewById(R.id.fabCurrentLocation).setOnClickListener(v -> getCurrentLocation());
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.map = mapLibreMap;
        
        String styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=" + MAPTILER_API_KEY;
        map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            applyMapBounds();
            enableLocationComponent(style);
            checkLocationSettings();
        });

        map.addOnCameraIdleListener(() -> {
            CameraPosition position = map.getCameraPosition();
            selectedLatLng = position.target;
            reverseGeocode(selectedLatLng);
        });
    }

    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            );
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.NONE); // We handle camera movement ourselves
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> getCurrentLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MapAddressPickerActivity.this, REQ_CHECK_SETTINGS);
                } catch (android.content.IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    private void applyMapBounds() {
        String query = "";
        if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
            query = selectedDistrict.toLowerCase();
        } else if (selectedCity != null && !selectedCity.isEmpty()) {
            query = selectedCity.toLowerCase();
        }

        if (query.isEmpty()) return;

        LatLngBounds bounds = null;
        switch (query) {
            case "chitwan":
            case "bharatpur":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(27.50, 84.15))
                        .include(new LatLng(27.85, 84.65))
                        .build();
                break;
            case "kathmandu":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(27.60, 85.15))
                        .include(new LatLng(27.85, 85.55))
                        .build();
                break;
            case "pokhara":
            case "kaski":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(28.10, 83.80))
                        .include(new LatLng(28.40, 84.15))
                        .build();
                break;
            case "lalitpur":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(27.60, 85.25))
                        .include(new LatLng(27.75, 85.40))
                        .build();
                break;
            case "bhaktapur":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(27.62, 85.38))
                        .include(new LatLng(27.75, 85.52))
                        .build();
                break;
            case "butwal":
            case "rupandehi":
                bounds = new LatLngBounds.Builder()
                        .include(new LatLng(27.55, 83.35))
                        .include(new LatLng(27.85, 83.60))
                        .build();
                break;
        }

        if (bounds != null) {
            this.restrictedBounds = bounds;
            map.setLatLngBoundsForCameraTarget(bounds);
            map.setMinZoomPreference(11);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 13));
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    moveToLocation(location.getLatitude(), location.getLongitude());
                } else {
                    // If last location is null, try to get it from LocationComponent
                    LocationComponent locationComponent = map.getLocationComponent();
                    if (locationComponent.isLocationComponentActivated() && locationComponent.getLastKnownLocation() != null) {
                        android.location.Location lastKnown = locationComponent.getLastKnownLocation();
                        moveToLocation(lastKnown.getLatitude(), lastKnown.getLongitude());
                    } else {
                        Toast.makeText(this, "Fetching fresh location...", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void moveToLocation(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        if (restrictedBounds != null) {
            if (restrictedBounds.contains(latLng)) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            } else {
                Toast.makeText(this, "You are currently outside the selected district", Toast.LENGTH_SHORT).show();
            }
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
    }

    private void reverseGeocode(LatLng latLng) {
        if (!Geocoder.isPresent()) {
            tvCurrentAddress.setText("Geocoding service unavailable");
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentAddressLine = address.getAddressLine(0);
                currentCity = address.getLocality();
                currentDistrict = address.getSubAdminArea();
                
                if (currentCity == null) currentCity = address.getSubLocality();
                if (currentCity == null) currentCity = "";
                if (currentDistrict == null) currentDistrict = "";

                tvCurrentAddress.setText(currentAddressLine);
            } else {
                tvCurrentAddress.setText("Unknown Location");
            }
        } catch (IOException e) {
            tvCurrentAddress.setText("Location selected");
        }
    }

    private void confirmLocation() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Please select a location on map", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.putExtra("latitude", selectedLatLng.getLatitude());
        intent.putExtra("longitude", selectedLatLng.getLongitude());
        intent.putExtra("addressLine", currentAddressLine);
        intent.putExtra("city", currentCity);
        intent.putExtra("district", currentDistrict);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location services are required to find you on map", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
