package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_menu);

        String restaurantName = getIntent().getStringExtra("restaurantName");
        ((TextView) findViewById(R.id.tvRestaurantTitle)).setText(restaurantName);

        findViewById(R.id.btnBackMenu).setOnClickListener(v -> finish());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshMenu);
        swipeRefreshLayout.setOnRefreshListener(() -> setupMenu());

        setupSearchView();
        setupTabs();
        setupMenu();
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchViewMenu);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterMenu();
                return true;
            }
        });
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
        tabs.add("Main Course");
        tabs.add("Starter");
        tabs.add("Momo");
        tabs.add("Burger");
        tabs.add("Pizza");
        tabs.add("Drinks");
        tabs.add("Dessert");

        tabAdapter = new FilterAdapter(this, tabs, category -> filterMenuByCategory(category));
        rvTabs.setAdapter(tabAdapter);
    }

    private List<MenuItem> fullMenuList = new ArrayList<>();
    private String currentCategory = "All";

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
        currentCategory = category;
        filterMenu();
    }

    private void filterMenu() {
        List<MenuItem> filtered = new ArrayList<>();
        String query = currentSearchQuery.toLowerCase().trim();

        for (MenuItem item : fullMenuList) {
            boolean matchesCategory = currentCategory.equals("All") || 
                    (item.getCategory() != null && item.getCategory().equalsIgnoreCase(currentCategory));
            
            boolean matchesSearch = query.isEmpty() || 
                    item.getName().toLowerCase().contains(query) || 
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(query));

            if (matchesCategory && matchesSearch) {
                filtered.add(item);
            }
        }
        displayMenuItems(filtered);
    }

    private void displayMenuItems(List<MenuItem> items) {
        RecyclerView rvMenu = findViewById(R.id.rvMenuItems);
        if (foodAdapter == null) {
            foodAdapter = new MenuFoodAdapter(this, new ArrayList<>(items), item -> openFoodDetails(item));
            rvMenu.setAdapter(foodAdapter);
        } else {
            foodAdapter.updateList(items);
        }
    }

    private void openFoodDetails(MenuItem item) {
        Intent intent = new Intent(this, FoodDetailsActivity.class);
        intent.putExtra("selectedItem", item);
        // No longer passing fullMenuList to avoid Intent size limits
        startActivity(intent);
    }
}