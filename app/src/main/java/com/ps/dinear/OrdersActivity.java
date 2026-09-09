package com.ps.dinear;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.Order;
import com.ps.dinear.data.model.OrderListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private OrdersAdapter adapter;
    private View shimmer;
    private ProgressBar pbLoadMore;
    private View llNoOrders;
    
    private List<Order> ordersList = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_orders);

        // Handle Status Bar Insets
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            if (v.getLayoutParams().height != statusBars.top) {
                v.getLayoutParams().height = statusBars.top;
                v.requestLayout();
            }
            return insets;
        });

        findViewById(R.id.btnBackOrders).setOnClickListener(v -> finish());

        rvOrders = findViewById(R.id.rvOrders);
        shimmer = findViewById(R.id.shimmerOrders);
        pbLoadMore = findViewById(R.id.pbOrders);
        llNoOrders = findViewById(R.id.llNoOrders);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdersAdapter(this, ordersList);
        rvOrders.setAdapter(adapter);

        rvOrders.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == ordersList.size() - 1) {
                    if (!isLoading && hasNextPage) {
                        fetchOrders(currentPage + 1);
                    }
                }
            }
        });

        fetchOrders(1);
    }

    private void fetchOrders(int page) {
        if (isLoading) return;
        isLoading = true;

        if (page == 1) {
            if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
            llNoOrders.setVisibility(View.GONE);
        } else {
            if (pbLoadMore != null) pbLoadMore.setVisibility(View.VISIBLE);
        }

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.getOrders(token, page).enqueue(new Callback<OrderListResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderListResponse> call, @NonNull Response<OrderListResponse> response) {
                isLoading = false;
                if (shimmer != null) shimmer.setVisibility(View.GONE);
                if (pbLoadMore != null) pbLoadMore.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    OrderListResponse listResponse = response.body();
                    List<Order> orders = listResponse.getResults();

                    if (page == 1) {
                        ordersList.clear();
                    }

                    if (orders != null && !orders.isEmpty()) {
                        ordersList.addAll(orders);
                        adapter.notifyDataSetChanged();
                        rvOrders.setVisibility(View.VISIBLE);
                        currentPage = page;
                        hasNextPage = listResponse.getNext() != null;
                    } else if (page == 1) {
                        llNoOrders.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(OrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderListResponse> call, @NonNull Throwable t) {
                isLoading = false;
                if (shimmer != null) shimmer.setVisibility(View.GONE);
                if (pbLoadMore != null) pbLoadMore.setVisibility(View.GONE);
                Toast.makeText(OrdersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
