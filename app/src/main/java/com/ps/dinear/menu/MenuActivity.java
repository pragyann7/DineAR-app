package com.ps.dinear.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.ps.dinear.CartManager;
import com.ps.dinear.ApiService;
import com.ps.dinear.FilterAdapter;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.data.model.RestaurantMenuResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private MenuFoodAdapter foodAdapter;
    private FilterAdapter tabAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvCartBadge;
    private String currentSearchQuery = "";
    private List<MenuItem> fullMenuList = new ArrayList<>();
    private String currentCategory = "All";
    private CartManager.CartListener cartListener = this::updateCartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_menu);

        String restaurantName = getIntent().getStringExtra("restaurantName");
        ((TextView) findViewById(R.id.tvRestaurantTitle)).setText(restaurantName);

        findViewById(R.id.btnBackMenu).setOnClickListener(v -> finish());

        tvCartBadge = findViewById(R.id.tvCartBadgeMenu);
        findViewById(R.id.btnCartMenu).setOnClickListener(v -> {
            startActivity(new Intent(this, com.ps.dinear.CartActivity.class));
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshMenu);
        swipeRefreshLayout.setOnRefreshListener(() -> setupMenu());

        setupSearchView();
        // setupTabs(); // Tabs will be setup inside setupMenu now
        setupMenu();
        CartManager.getInstance().addListener(cartListener);
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
        updateCartBadge();
        // Refresh favorite status when returning from FoodDetailsActivity
        if (foodAdapter != null) {
            foodAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeListener(cartListener);
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        int count = com.ps.dinear.CartManager.getInstance().getItemCount();
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void setupMenu() {
        RecyclerView rvMenu = findViewById(R.id.rvMenuItems);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        String restaurantSlug = getIntent().getStringExtra("restaurantSlug");
        swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        android.util.Log.d("MenuActivity", "Fetching menu for slug: " + restaurantSlug);
        apiService.getMenu(restaurantSlug).enqueue(new Callback<RestaurantMenuResponse>() {
            @Override
            public void onResponse(Call<RestaurantMenuResponse> call, Response<RestaurantMenuResponse> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    RestaurantMenuResponse body = response.body();
                    android.util.Log.d("MenuActivity", "Menu fetched successfully. Categories: " + (body.getCategories() != null ? body.getCategories().size() : 0));
                    
                    // 1. Setup Tabs
                    List<String> tabs = new ArrayList<>();
                    tabs.add("All");
                    if (body.getCategories() != null) {
                        for (RestaurantMenuResponse.CategoryGroup group : body.getCategories()) {
                            tabs.add(group.getName());
                        }
                    }
                    RecyclerView rvTabs = findViewById(R.id.rvMenuTabs);
                    tabAdapter = new FilterAdapter(MenuActivity.this, tabs, category -> filterMenuByCategory(category));
                    rvTabs.setAdapter(tabAdapter);

                    // 2. Flatten and display menu
                    List<MenuItem> flattenedList = new ArrayList<>();
                    if (body.getCategories() != null) {
                        for (RestaurantMenuResponse.CategoryGroup group : body.getCategories()) {
                            if (group.getMenuItems() != null) {
                                android.util.Log.d("MenuActivity", "Category: " + group.getName() + " has " + group.getMenuItems().size() + " foods.");
                                for (MenuItem item : group.getMenuItems()) {
                                    item.setCategory(group.getName());
                                    flattenedList.add(item);
                                }
                            }
                        }
                    }
                    
                    fullMenuList = flattenedList;
                    android.util.Log.d("MenuActivity", "Total flattened items: " + fullMenuList.size());
                    if (fullMenuList.isEmpty()) {
                        Toast.makeText(MenuActivity.this, "This restaurant has no menu items yet.", Toast.LENGTH_LONG).show();
                    }
                    displayMenuItems(fullMenuList);
                } else {
                    android.util.Log.e("MenuActivity", "Menu fetch failed with code: " + response.code());
                    Toast.makeText(MenuActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RestaurantMenuResponse> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                android.util.Log.e("MenuActivity", "Menu API error: " + t.getMessage());
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
        int restaurantId = getIntent().getIntExtra("restaurantId", -1);
        if (foodAdapter == null) {
            foodAdapter = new MenuFoodAdapter(this, new ArrayList<>(items), item -> openFoodDetails(item));
            foodAdapter.setRestaurantId(restaurantId);
            rvMenu.setAdapter(foodAdapter);
        } else {
            foodAdapter.setRestaurantId(restaurantId);
            foodAdapter.updateList(items);
        }
    }

    private void openFoodDetails(MenuItem item) {
        Intent intent = new Intent(this, FoodDetailsActivity.class);
        intent.putExtra("selectedItem", item);
        intent.putExtra("restaurantId", getIntent().getIntExtra("restaurantId", -1));
        intent.putExtra("restaurantSlug", getIntent().getStringExtra("restaurantSlug"));
        // No longer passing fullMenuList to avoid Intent size limits
        startActivity(intent);
    }
}