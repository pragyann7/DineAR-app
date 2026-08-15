package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.ps.dinear.ApiService;
import com.ps.dinear.FilterAdapter;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private MenuFoodAdapter foodAdapter;
    private FilterAdapter tabAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        String restaurantName = getIntent().getStringExtra("restaurantName");
        ((TextView) findViewById(R.id.tvRestaurantTitle)).setText(restaurantName);

        findViewById(R.id.btnBackMenu).setOnClickListener(v -> finish());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshMenu);
        swipeRefreshLayout.setOnRefreshListener(() -> setupMenu());

        setupTabs();
        setupMenu();
    }

    private void setupTabs() {
        RecyclerView rvTabs = findViewById(R.id.rvMenuTabs);
        List<String> tabs = new ArrayList<>();
        tabs.add("Appetizers");
        tabs.add("Mains");
        tabs.add("Drinks");
        tabs.add("Desserts");

        tabAdapter = new FilterAdapter(this, tabs);
        rvTabs.setAdapter(tabAdapter);
    }

    private void setupMenu() {
        RecyclerView rvMenu = findViewById(R.id.rvMenuItems);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMenu().enqueue(new Callback<List<MenuItem>>() {
            @Override
            public void onResponse(Call<List<MenuItem>> call, Response<List<MenuItem>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    foodAdapter = new MenuFoodAdapter(MenuActivity.this, response.body(), item -> openFoodDetails(item));
                    rvMenu.setAdapter(foodAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<MenuItem>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MenuActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFoodDetails(MenuItem item) {
        Intent intent = new Intent(this, FoodDetailsActivity.class);
        intent.putExtra("foodName", item.getName());
        intent.putExtra("foodPrice", item.getPrice());
        intent.putExtra("foodDesc", item.getDescription());
        intent.putExtra("foodImage", item.getImageUrl());
        intent.putExtra("modelUrl", item.getModelUrl());
        intent.putExtra("modelName", item.getModelName());
        intent.putExtra("modelVersion", item.getModelVersion());
        intent.putExtra("tag1", item.getTag1());
        intent.putExtra("tag2", item.getTag2());
        startActivity(intent);
    }
}