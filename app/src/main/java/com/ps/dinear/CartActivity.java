package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
    private RecyclerView rvCartItems;
    private CartAdapter adapter;
    private TextView tvTotal;
    private LinearLayout llEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        rvCartItems = findViewById(R.id.rvCartItems);
        tvTotal = findViewById(R.id.tvCartTotal);
        llEmpty = findViewById(R.id.llEmptyCart);

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        
        findViewById(R.id.btnBackCart).setOnClickListener(v -> finish());
        findViewById(R.id.btnStartShopping).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnCheckout).setOnClickListener(v -> checkout());

        updateUI();
    }

    private void updateUI() {
        List<CartItem> items = CartManager.getInstance().getItems();
        if (items.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvCartItems.setVisibility(View.GONE);
            findViewById(R.id.cvCheckout).setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.VISIBLE);
            findViewById(R.id.cvCheckout).setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new CartAdapter(this, items, new CartAdapter.CartUpdateListener() {
                    @Override
                    public void onQuantityChanged(int foodItemId, int newQuantity) {
                        CartManager.getInstance().updateQuantity(foodItemId, newQuantity);
                        updateUI();
                    }

                    @Override
                    public void onItemRemoved(int foodItemId) {
                        CartManager.getInstance().removeItem(foodItemId);
                        updateUI();
                    }
                });
                rvCartItems.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }

            tvTotal.setText("Rs. " + (int)CartManager.getInstance().getTotalPrice());
        }
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
                    startActivity(new Intent(CartActivity.this, OrdersActivity.class));
                    finish();
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
