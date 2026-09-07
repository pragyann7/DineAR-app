package com.ps.dinear;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Order;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends AppCompatActivity {
    private RecyclerView rvOrders;
    private ProgressBar pbOrders;
    private View llNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        rvOrders = findViewById(R.id.rvOrders);
        pbOrders = findViewById(R.id.pbOrders);
        llNoOrders = findViewById(R.id.llNoOrders);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        // Handle navigation bar insets for list
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rvOrders, (v, insets) -> {
            androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            float density = getResources().getDisplayMetrics().density;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), (int) (40 * density) + navBars.bottom);
            return insets;
        });
        
        findViewById(R.id.btnBackOrders).setOnClickListener(v -> finish());

        // loadOrders(); // Called in onResume
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        pbOrders.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        llNoOrders.setVisibility(View.GONE);

        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getOrders(token).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                pbOrders.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Order> orders = response.body();
                    if (orders.isEmpty()) {
                        llNoOrders.setVisibility(View.VISIBLE);
                    } else {
                        rvOrders.setVisibility(View.VISIBLE);
                        rvOrders.setAdapter(new OrdersAdapter(OrdersActivity.this, orders));
                    }
                } else {
                    Toast.makeText(OrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                pbOrders.setVisibility(View.GONE);
                Toast.makeText(OrdersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
