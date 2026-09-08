package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.ps.dinear.data.model.CartItem;
import com.ps.dinear.data.model.Order;
import com.ps.dinear.data.model.OrderRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private CheckoutItemsAdapter adapter;
    private TextView tvRestaurantName, tvOrderSummaryInfo, tvPaymentVia, tvPayAmount;
    private TextView tvSubtotal, tvServiceCharge, tvDeliveryCharge, tvTotalPayable;
    private View rlDeliveryCharge;
    private TextView tvCheckoutAddress;
    private MaterialButton btnPay;
    private RadioButton rbEsewa, rbKhalti, rbCash;
    private RadioButton rbOptionDelivery, rbOptionTakeaway, rbOptionDineIn;
    private MaterialCardView cvEsewa, cvKhalti, cvCash;
    private MaterialCardView cvOptionDelivery, cvOptionTakeaway, cvOptionDineIn, cvPickAddress;
    private LinearLayout llAddressPicker;
    
    private String selectedPaymentMethod = "eSewa";
    private String selectedDeliveryOption = "DELIVERY";
    private String currentGpsAddress = "Locating...";
    private double currentLat = 0, currentLng = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQ_MAP_PICKER = 102;
    private static final int REQ_LOCATION_PERMISSION = 1001;
    private static final int REQ_CHECK_SETTINGS = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupOrderSummary();
        setupPaymentMethods();
        setupDeliveryOptions();

        findViewById(R.id.btnBackCheckout).setOnClickListener(v -> finish());
        btnPay.setOnClickListener(v -> processPayment());
        
        checkLocationPermission();
    }

    private void initViews() {
        rvItems = findViewById(R.id.rvOrderItems);
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvOrderSummaryInfo = findViewById(R.id.tvOrderSummaryInfo);
        tvPaymentVia = findViewById(R.id.tvPaymentViaLabel);
        tvPayAmount = findViewById(R.id.tvPayAmount);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceCharge = findViewById(R.id.tvServiceCharge);
        tvDeliveryCharge = findViewById(R.id.tvDeliveryCharge);
        rlDeliveryCharge = findViewById(R.id.rlDeliveryCharge);
        tvTotalPayable = findViewById(R.id.tvTotalPayable);
        btnPay = findViewById(R.id.btnPay);
        
        rbEsewa = findViewById(R.id.rbEsewa);
        rbKhalti = findViewById(R.id.rbKhalti);
        rbCash = findViewById(R.id.rbCash);
        cvEsewa = findViewById(R.id.cvEsewa);
        cvKhalti = findViewById(R.id.cvKhalti);
        cvCash = findViewById(R.id.cvCash);
        
        rbOptionDelivery = findViewById(R.id.rbOptionDelivery);
        rbOptionTakeaway = findViewById(R.id.rbOptionTakeaway);
        rbOptionDineIn = findViewById(R.id.rbOptionDineIn);
        cvOptionDelivery = findViewById(R.id.cvOptionDelivery);
        cvOptionTakeaway = findViewById(R.id.cvOptionTakeaway);
        cvOptionDineIn = findViewById(R.id.cvOptionDineIn);
        
        cvPickAddress = findViewById(R.id.cvPickAddress);
        tvCheckoutAddress = findViewById(R.id.tvCheckoutAddress);
        llAddressPicker = findViewById(R.id.llAddressPicker);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION_PERMISSION);
        } else {
            checkLocationSettings();
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

        task.addOnSuccessListener(this, locationSettingsResponse -> fetchGpsLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(CheckoutActivity.this, REQ_CHECK_SETTINGS);
                } catch (android.content.IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    private void fetchGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    updateLocationData(location.getLatitude(), location.getLongitude());
                } else {
                    // Try to get a fresh location if last location is null
                    LocationRequest singleRequest = LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setNumUpdates(1)
                            .setInterval(0);
                    
                    fusedLocationClient.requestLocationUpdates(singleRequest, new com.google.android.gms.location.LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                            if (locationResult.getLastLocation() != null) {
                                updateLocationData(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                            } else {
                                tvCheckoutAddress.setText("Location not found. Tap to pick.");
                            }
                        }
                    }, Looper.getMainLooper());
                }
            });
        }
    }

    private void updateLocationData(double lat, double lng) {
        currentLat = lat;
        currentLng = lng;
        reverseGeocode(currentLat, currentLng);
    }

    private void reverseGeocode(double lat, double lng) {
        if (!Geocoder.isPresent()) return;
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentGpsAddress = address.getAddressLine(0);
                tvCheckoutAddress.setText(currentGpsAddress);
            }
        } catch (IOException e) {
            tvCheckoutAddress.setText("Location found (tap to view)");
        }
    }

    private void setupDeliveryOptions() {
        View.OnClickListener selectDelivery = v -> updateDeliverySelection("DELIVERY");
        View.OnClickListener selectTakeaway = v -> updateDeliverySelection("TAKEAWAY");
        View.OnClickListener selectDineIn = v -> updateDeliverySelection("DINE_IN");

        cvOptionDelivery.setOnClickListener(selectDelivery);
        rbOptionDelivery.setOnClickListener(selectDelivery);

        cvOptionTakeaway.setOnClickListener(selectTakeaway);
        rbOptionTakeaway.setOnClickListener(selectTakeaway);

        cvOptionDineIn.setOnClickListener(selectDineIn);
        rbOptionDineIn.setOnClickListener(selectDineIn);

        cvPickAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapAddressPickerActivity.class);
            startActivityForResult(intent, REQ_MAP_PICKER);
        });

        findViewById(R.id.ivRefreshLocation).setOnClickListener(v -> fetchGpsLocation());

        updateDeliverySelection("DELIVERY");
    }

    private void updateDeliverySelection(String option) {
        selectedDeliveryOption = option;

        rbOptionDelivery.setChecked(false);
        rbOptionTakeaway.setChecked(false);
        rbOptionDineIn.setChecked(false);

        cvOptionDelivery.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvOptionDelivery.setStrokeWidth(convertDpToPx(1));
        cvOptionTakeaway.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvOptionTakeaway.setStrokeWidth(convertDpToPx(1));
        cvOptionDineIn.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvOptionDineIn.setStrokeWidth(convertDpToPx(1));

        int orangePrimary = getResources().getColor(R.color.orange_primary);

        llAddressPicker.setVisibility(option.equals("DELIVERY") ? View.VISIBLE : View.GONE);
        
        // Show/Hide delivery charge in bill details
        if (rlDeliveryCharge != null) {
            rlDeliveryCharge.setVisibility(option.equals("DELIVERY") ? View.VISIBLE : View.GONE);
        }

        switch (option) {
            case "DELIVERY":
                rbOptionDelivery.setChecked(true);
                cvOptionDelivery.setStrokeColor(orangePrimary);
                cvOptionDelivery.setStrokeWidth(convertDpToPx(2));
                break;
            case "TAKEAWAY":
                rbOptionTakeaway.setChecked(true);
                cvOptionTakeaway.setStrokeColor(orangePrimary);
                cvOptionTakeaway.setStrokeWidth(convertDpToPx(2));
                break;
            case "DINE_IN":
                rbOptionDineIn.setChecked(true);
                cvOptionDineIn.setStrokeColor(orangePrimary);
                cvOptionDineIn.setStrokeWidth(convertDpToPx(2));
                break;
        }
        
        calculateFinalTotal();
    }

    private int calculateFinalTotal() {
        int subtotal = (int) CartManager.getInstance().getTotalPrice();
        int serviceCharge = 10;
        double charge = CartManager.getInstance().getDeliveryCharge();
        int deliveryCharge = selectedDeliveryOption.equals("DELIVERY") ? (int) charge : 0;
        int total = subtotal + serviceCharge + deliveryCharge;

        tvSubtotal.setText(String.format(Locale.getDefault(), "Rs. %d", subtotal));
        tvServiceCharge.setText(String.format(Locale.getDefault(), "Rs. %d", serviceCharge));
        if (tvDeliveryCharge != null) {
            tvDeliveryCharge.setText(String.format(Locale.getDefault(), "Rs. %d", deliveryCharge));
        }
        tvTotalPayable.setText(String.format(Locale.getDefault(), "Rs. %d", total));
        
        tvPayAmount.setText(String.format(Locale.getDefault(), "Rs. %d", total));
        if (btnPay.isEnabled()) {
            btnPay.setText(String.format(Locale.getDefault(), "Pay Rs. %d", total));
        }
        return total;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MAP_PICKER && resultCode == RESULT_OK && data != null) {
            currentGpsAddress = data.getStringExtra("addressLine");
            currentLat = data.getDoubleExtra("latitude", 0);
            currentLng = data.getDoubleExtra("longitude", 0);
            if (currentGpsAddress != null && !currentGpsAddress.isEmpty()) {
                tvCheckoutAddress.setText(currentGpsAddress);
            } else {
                tvCheckoutAddress.setText("Custom location on map");
            }
        } else if (requestCode == REQ_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                fetchGpsLocation();
            } else {
                Toast.makeText(this, "Location services are required for delivery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings();
        }
    }

    private void setupOrderSummary() {
        List<CartItem> items = CartManager.getInstance().getItems();
        String name = CartManager.getInstance().getRestaurantName();
        if (name == null) name = "DineAR Restaurant";

        tvRestaurantName.setText(name);
        tvOrderSummaryInfo.setText(String.format(Locale.getDefault(), "%d items in cart", items.size()));

        adapter = new CheckoutItemsAdapter(this, items);
        rvItems.setAdapter(adapter);

        calculateFinalTotal();
    }

    private void setupPaymentMethods() {
        View.OnClickListener selectEsewa = v -> updatePaymentSelection("eSewa");
        View.OnClickListener selectKhalti = v -> updatePaymentSelection("Khalti");
        View.OnClickListener selectCash = v -> updatePaymentSelection("Cash");

        cvEsewa.setOnClickListener(selectEsewa);
        rbEsewa.setOnClickListener(selectEsewa);
        
        cvKhalti.setOnClickListener(selectKhalti);
        rbKhalti.setOnClickListener(selectKhalti);
        
        cvCash.setOnClickListener(selectCash);
        rbCash.setOnClickListener(selectCash);
        
        updatePaymentSelection("eSewa");
    }

    private void updatePaymentSelection(String method) {
        selectedPaymentMethod = method;
        
        rbEsewa.setChecked(false);
        rbKhalti.setChecked(false);
        rbCash.setChecked(false);
        
        cvEsewa.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvEsewa.setStrokeWidth(convertDpToPx(1));
        cvKhalti.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvKhalti.setStrokeWidth(convertDpToPx(1));
        cvCash.setStrokeColor(Color.parseColor("#E0E0E0"));
        cvCash.setStrokeWidth(convertDpToPx(1));

        int orangePrimary = getResources().getColor(R.color.orange_primary);

        switch (method) {
            case "eSewa":
                rbEsewa.setChecked(true);
                cvEsewa.setStrokeColor(orangePrimary);
                cvEsewa.setStrokeWidth(convertDpToPx(2));
                break;
            case "Khalti":
                rbKhalti.setChecked(true);
                cvKhalti.setStrokeColor(orangePrimary);
                cvKhalti.setStrokeWidth(convertDpToPx(2));
                break;
            case "Cash":
                rbCash.setChecked(true);
                cvCash.setStrokeColor(orangePrimary);
                cvCash.setStrokeWidth(convertDpToPx(2));
                break;
        }
        tvPaymentVia.setText(String.format("Payment via: %s", selectedPaymentMethod));
    }

    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void processPayment() {
        if ("DELIVERY".equals(selectedDeliveryOption)) {
            if (currentLat == 0 || currentLng == 0 || currentGpsAddress == null || "Locating...".equals(currentGpsAddress)) {
                Toast.makeText(this, "Please select a valid delivery address", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if ("eSewa".equals(selectedPaymentMethod)) {
            initiateEsewaPayment();
        } else {
            placeOrder();
        }
    }

    private void initiateEsewaPayment() {
        btnPay.setEnabled(false);
        btnPay.setText("Initiating eSewa...");

        List<CartItem> items = CartManager.getInstance().getItems();
        List<OrderRequest.OrderItemRequest> itemRequests = new ArrayList<>();
        for (CartItem item : items) {
            itemRequests.add(new OrderRequest.OrderItemRequest(item.getMenuItem().getId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                CartManager.getInstance().getRestaurantId(),
                currentGpsAddress != null ? currentGpsAddress : "No Address",
                currentLat != 0 ? currentLat : null,
                currentLng != 0 ? currentLng : null,
                "9800000000",
                "ESEWA",
                selectedDeliveryOption,
                itemRequests
        );

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.placeOrder(token, request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int orderId = response.body().getId();
                    getEsewaFormData(orderId);
                } else {
                    btnPay.setEnabled(true);
                    btnPay.setText(String.format(Locale.getDefault(), "Pay Rs. %d", calculateFinalTotal()));
                    
                    String error = "Order failed";
                    try {
                        if (response.errorBody() != null) {
                            error += ": " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(CheckoutActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                btnPay.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getEsewaFormData(int orderId) {
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        com.ps.dinear.data.model.PaymentRequest request = new com.ps.dinear.data.model.PaymentRequest(orderId, "ESEWA");
        
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.initiateEsewaPayment(token, request).enqueue(new Callback<com.ps.dinear.data.model.EsewaInitiateResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.ps.dinear.data.model.EsewaInitiateResponse> call, @NonNull Response<com.ps.dinear.data.model.EsewaInitiateResponse> response) {
                btnPay.setEnabled(true);
                btnPay.setText(String.format(Locale.getDefault(), "Pay Rs. %d", calculateFinalTotal()));
                
                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(CheckoutActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra("esewaData", response.body());
                    intent.putExtra("method", "eSewa");
                    startActivity(intent);
                } else {
                    String error = "Failed to initiate eSewa payment";
                    try {
                        if (response.errorBody() != null) {
                            error += ": " + response.errorBody().string();
                        }
                    } catch (IOException ignored) {}
                    Toast.makeText(CheckoutActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<com.ps.dinear.data.model.EsewaInitiateResponse> call, Throwable t) {
                btnPay.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void placeOrder() {
        btnPay.setEnabled(false);
        btnPay.setText("Processing...");

        List<CartItem> items = CartManager.getInstance().getItems();
        List<OrderRequest.OrderItemRequest> itemRequests = new ArrayList<>();
        for (CartItem item : items) {
            itemRequests.add(new OrderRequest.OrderItemRequest(item.getMenuItem().getId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                CartManager.getInstance().getRestaurantId(),
                currentGpsAddress != null ? currentGpsAddress : "No Address",
                currentLat != 0 ? currentLat : null,
                currentLng != 0 ? currentLng : null,
                "9800000000",
                selectedPaymentMethod.toUpperCase(),
                selectedDeliveryOption,
                itemRequests
        );

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.placeOrder(token, request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful()) {
                    CartManager.getInstance().clear();
                    Toast.makeText(CheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    
                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    btnPay.setEnabled(true);
                    btnPay.setText(String.format(Locale.getDefault(), "Pay Rs. %d", calculateFinalTotal()));
                    Toast.makeText(CheckoutActivity.this, "Failed to place order: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                btnPay.setEnabled(true);
                btnPay.setText("Pay Rs. " + (int)CartManager.getInstance().getTotalPrice());
                Toast.makeText(CheckoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
