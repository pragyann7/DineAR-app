package com.ps.dinear;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.ps.dinear.data.model.CartItem;
import com.ps.dinear.data.model.Order;
import com.ps.dinear.data.model.OrderRequest;

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
    private TextView tvSubtotal, tvServiceCharge, tvTotalPayable;
    private MaterialButton btnPay;
    private RadioButton rbEsewa, rbKhalti, rbCash;
    private MaterialCardView cvEsewa, cvKhalti, cvCash;
    private String selectedPaymentMethod = "eSewa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initViews();
        setupOrderSummary();
        setupPaymentMethods();

        findViewById(R.id.btnBackCheckout).setOnClickListener(v -> finish());
        btnPay.setOnClickListener(v -> processPayment());
    }

    private void initViews() {
        rvItems = findViewById(R.id.rvOrderItems);
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvOrderSummaryInfo = findViewById(R.id.tvOrderSummaryInfo);
        tvPaymentVia = findViewById(R.id.tvPaymentViaLabel);
        tvPayAmount = findViewById(R.id.tvPayAmount);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceCharge = findViewById(R.id.tvServiceCharge);
        tvTotalPayable = findViewById(R.id.tvTotalPayable);
        btnPay = findViewById(R.id.btnPay);
        rbEsewa = findViewById(R.id.rbEsewa);
        rbKhalti = findViewById(R.id.rbKhalti);
        rbCash = findViewById(R.id.rbCash);
        cvEsewa = findViewById(R.id.cvEsewa);
        cvKhalti = findViewById(R.id.cvKhalti);
        cvCash = findViewById(R.id.cvCash);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupOrderSummary() {
        List<CartItem> items = CartManager.getInstance().getItems();
        String restaurantName = CartManager.getInstance().getRestaurantName();
        if (restaurantName == null) restaurantName = "DineAR Restaurant";

        tvRestaurantName.setText(restaurantName);
        tvOrderSummaryInfo.setText(items.size() + " items in cart");

        adapter = new CheckoutItemsAdapter(this, items);
        rvItems.setAdapter(adapter);

        int subtotal = (int) CartManager.getInstance().getTotalPrice();
        int serviceCharge = 10; // Placeholder as in screenshot
        int total = subtotal + serviceCharge;
        
        tvSubtotal.setText("Rs. " + subtotal);
        tvServiceCharge.setText("Rs. " + serviceCharge);
        tvTotalPayable.setText("Rs. " + total);
        
        tvPayAmount.setText("Rs. " + total);
        btnPay.setText("Pay Rs. " + total);
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
        
        // Set initial state
        updatePaymentSelection("eSewa");
    }

    private void updatePaymentSelection(String method) {
        selectedPaymentMethod = method;
        
        // Reset all
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
        tvPaymentVia.setText("Payment via: " + selectedPaymentMethod);
    }

    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void processPayment() {
        if (selectedPaymentMethod.equals("eSewa")) {
            initiateEsewaPayment();
        } else {
            // For Cash/Demo, we use the old logic or a simplified version
            placeOrder(selectedPaymentMethod);
        }
    }

    private void initiateEsewaPayment() {
        btnPay.setEnabled(false);
        btnPay.setText("Initiating eSewa...");

        // 1. First place the order
        List<CartItem> items = CartManager.getInstance().getItems();
        List<OrderRequest.OrderItemRequest> itemRequests = new ArrayList<>();
        for (CartItem item : items) {
            itemRequests.add(new OrderRequest.OrderItemRequest(item.getMenuItem().getId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                CartManager.getInstance().getRestaurantId(),
                "My Address (Placeholder)",
                "9800000000",
                "ESEWA",
                itemRequests
        );

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.placeOrder(token, request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int orderId = response.body().getId();
                    getEsewaFormData(orderId);
                } else {
                    btnPay.setEnabled(true);
                    btnPay.setText("Pay Rs. " + (int)CartManager.getInstance().getTotalPrice());
                    Toast.makeText(CheckoutActivity.this, "Failed to create order", Toast.LENGTH_SHORT).show();
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
            public void onResponse(Call<com.ps.dinear.data.model.EsewaInitiateResponse> call, Response<com.ps.dinear.data.model.EsewaInitiateResponse> response) {
                btnPay.setEnabled(true);
                btnPay.setText("Pay Rs. " + (int)CartManager.getInstance().getTotalPrice());
                
                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(CheckoutActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra("esewaData", response.body());
                    intent.putExtra("method", "eSewa");
                    startActivity(intent);
                } else {
                    Toast.makeText(CheckoutActivity.this, "Failed to initiate eSewa payment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.ps.dinear.data.model.EsewaInitiateResponse> call, Throwable t) {
                btnPay.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void placeOrder(String method) {
        btnPay.setEnabled(false);
        btnPay.setText("Processing...");

        List<CartItem> items = CartManager.getInstance().getItems();
        List<OrderRequest.OrderItemRequest> itemRequests = new ArrayList<>();
        for (CartItem item : items) {
            itemRequests.add(new OrderRequest.OrderItemRequest(item.getMenuItem().getId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                CartManager.getInstance().getRestaurantId(),
                "My Address (Placeholder)",
                "9800000000",
                selectedPaymentMethod,
                itemRequests
        );

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.placeOrder(token, request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful()) {
                    CartManager.getInstance().clear();
                    Toast.makeText(CheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    
                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    btnPay.setEnabled(true);
                    btnPay.setText("Pay Rs. " + (int)CartManager.getInstance().getTotalPrice());
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
