package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.CartItem;
import com.ps.dinear.data.model.Order;
import com.ps.dinear.data.model.OrderRequest;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {
    private RecyclerView rvCartItems, rvOrders;
    private CartAdapter cartAdapter;
    private OrdersAdapter ordersAdapter;
    private TextView tvTotal, tvNoOrders;
    private LinearLayout llEmptyCart;
    private ProgressBar pbOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        rvCartItems = findViewById(R.id.rvCartItems);
        rvOrders = findViewById(R.id.rvOrders);
        tvTotal = findViewById(R.id.tvCartTotal);
        llEmptyCart = findViewById(R.id.llEmptyCart);
        tvNoOrders = findViewById(R.id.tvNoOrders);
        pbOrders = findViewById(R.id.pbOrders);

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        
        findViewById(R.id.btnBackCart).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckout).setOnClickListener(v -> checkout());

        updateCartUI();
        loadOrderHistory();
    }

    private void updateCartUI() {
        List<CartItem> items = CartManager.getInstance().getItems();
        if (items.isEmpty()) {
            llEmptyCart.setVisibility(View.VISIBLE);
            rvCartItems.setVisibility(View.GONE);
            findViewById(R.id.cvCheckout).setVisibility(View.GONE);
        } else {
            llEmptyCart.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.VISIBLE);
            findViewById(R.id.cvCheckout).setVisibility(View.VISIBLE);

            if (cartAdapter == null) {
                cartAdapter = new CartAdapter(this, items, new CartAdapter.CartUpdateListener() {
                    @Override
                    public void onQuantityChanged(int foodItemId, int newQuantity) {
                        CartManager.getInstance().updateQuantity(foodItemId, newQuantity);
                        updateCartUI();
                    }

                    @Override
                    public void onItemRemoved(int foodItemId) {
                        CartManager.getInstance().removeItem(foodItemId);
                        updateCartUI();
                    }
                });
                rvCartItems.setAdapter(cartAdapter);
            } else {
                cartAdapter.notifyDataSetChanged();
            }

            tvTotal.setText("Rs. " + (int)CartManager.getInstance().getTotalPrice());
        }
    }

    private void loadOrderHistory() {
        if (!SharedPrefManager.isLoggedIn(this)) {
            tvNoOrders.setText("Login to see order history");
            tvNoOrders.setVisibility(View.VISIBLE);
            return;
        }

        pbOrders.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        tvNoOrders.setVisibility(View.GONE);

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getOrders(token).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                pbOrders.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Order> orders = response.body();
                    if (orders.isEmpty()) {
                        tvNoOrders.setVisibility(View.VISIBLE);
                    } else {
                        rvOrders.setVisibility(View.VISIBLE);
                        ordersAdapter = new OrdersAdapter(CartActivity.this, orders);
                        rvOrders.setAdapter(ordersAdapter);
                    }
                } else {
                    tvNoOrders.setText("Failed to load orders");
                    tvNoOrders.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                pbOrders.setVisibility(View.GONE);
                tvNoOrders.setText("Network error");
                tvNoOrders.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkout() {
        if (!SharedPrefManager.isLoggedIn(this)) {
            Toast.makeText(this, "Please login to place an order", Toast.LENGTH_SHORT).show();
            // Optional: Redirect to login
            return;
        }

        List<CartItem> items = CartManager.getInstance().getItems();
        List<OrderRequest.OrderItemRequest> itemRequests = new ArrayList<>();
        for (CartItem item : items) {
            itemRequests.add(new OrderRequest.OrderItemRequest(item.getMenuItem().getId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
            CartManager.getInstance().getRestaurantId(),
            "My Address (Placeholder)", // In real app, collect this from user
            "9800000000", // In real app, collect this
            itemRequests
        );

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.placeOrder(token, request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful()) {
                    CartManager.getInstance().clear();
                    Toast.makeText(CartActivity.this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    updateCartUI();
                    loadOrderHistory();
                } else {
                    Toast.makeText(CartActivity.this, "Failed to place order: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
