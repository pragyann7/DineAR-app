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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh favorite status when returning from FoodDetailsActivity
        if (foodAdapter != null) {
            foodAdapter.notifyDataSetChanged();
        }
    }

    private void setupTabs() {
        RecyclerView rvTabs = findViewById(R.id.rvMenuTabs);
        List<String> tabs = new ArrayList<>();
        tabs.add("All");
        tabs.add("Popular");
        tabs.add("Italian");
        tabs.add("Burgers");
        tabs.add("Desserts");
        tabs.add("Drinks");

        tabAdapter = new FilterAdapter(this, tabs, category -> filterMenuByCategory(category));
        rvTabs.setAdapter(tabAdapter);
    }

    private List<MenuItem> fullMenuList = new ArrayList<>();

    private void setupMenu() {
        RecyclerView rvMenu = findViewById(R.id.rvMenuItems);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        int restaurantId = getIntent().getIntExtra("restaurantId", -1);
        swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getMenu(restaurantId).enqueue(new Callback<List<MenuItem>>() {
            @Override
            public void onResponse(Call<List<MenuItem>> call, Response<List<MenuItem>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    fullMenuList = response.body();
                    if (fullMenuList.isEmpty()) {
                        Toast.makeText(MenuActivity.this, "This restaurant has no menu items yet.", Toast.LENGTH_LONG).show();
                    }
                    displayMenuItems(fullMenuList);
                } else {
                    Toast.makeText(MenuActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MenuItem>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MenuActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMenuByCategory(String category) {
        if (category.equals("All")) {
            displayMenuItems(fullMenuList);
        } else {
            List<MenuItem> filtered = new ArrayList<>();
            for (MenuItem item : fullMenuList) {
                if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(item);
                }
            }
            displayMenuItems(filtered);
        }
    }

    private void displayMenuItems(List<MenuItem> items) {
        RecyclerView rvMenu = findViewById(R.id.rvMenuItems);
        foodAdapter = new MenuFoodAdapter(this, items, item -> openFoodDetails(item));
        rvMenu.setAdapter(foodAdapter);
    }

    private void openFoodDetails(MenuItem item) {
        Intent intent = new Intent(this, FoodDetailsActivity.class);
        intent.putExtra("selectedItem", item);
        // No longer passing fullMenuList to avoid Intent size limits
        startActivity(intent);
    }
}