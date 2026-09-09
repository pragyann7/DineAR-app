package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.SearchResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllFoodsActivity extends AppCompatActivity {

    private RecyclerView rvAllFoods;
    private FoodVerticalAdapter adapter;
    private View shimmer;
    private View llNoResults;
    private List<MenuItem> foodList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_all_foods);

        findViewById(R.id.btnBackAllFoods).setOnClickListener(v -> finish());

        rvAllFoods = findViewById(R.id.rvAllFoods);
        shimmer = findViewById(R.id.shimmerAllFoods);
        llNoResults = findViewById(R.id.llNoResultsAllFoods);

        String title = getIntent().getStringExtra("title");
        if (title != null) {
            ((android.widget.TextView) findViewById(R.id.tvAllFoodsTitle)).setText(title);
        }

        rvAllFoods.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodVerticalAdapter(this, foodList);
        rvAllFoods.setAdapter(adapter);

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

        loadFoods();
    }

    private void loadFoods() {
        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
        if (rvAllFoods != null) rvAllFoods.setVisibility(View.GONE);
        if (llNoResults != null) llNoResults.setVisibility(View.GONE);

        String city = SharedPrefManager.getCity(this);
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);

        apiService.search("", city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (shimmer != null) shimmer.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<MenuItem> foods = response.body().getFoodItems();
                    if (foods == null || foods.isEmpty()) {
                        if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
                        if (rvAllFoods != null) rvAllFoods.setVisibility(View.GONE);
                    } else {
                        String filter = getIntent().getStringExtra("filter");
                        List<MenuItem> filteredFoods = new ArrayList<>();
                        
                        if ("price_drop".equals(filter)) {
                            for (MenuItem item : foods) {
                                if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0 && item.getDiscountPrice() < item.getPrice()) {
                                    filteredFoods.add(item);
                                }
                            }
                        } else if ("hot".equals(filter)) {
                            // For now, hot is just all foods (can be refined later with rating/popularity)
                            filteredFoods.addAll(foods);
                            java.util.Collections.shuffle(filteredFoods);
                        } else {
                            // Default: Trending / All
                            filteredFoods.addAll(foods);
                        }

                        if (filteredFoods.isEmpty()) {
                            if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
                            if (rvAllFoods != null) rvAllFoods.setVisibility(View.GONE);
                        } else {
                            foodList.clear();
                            foodList.addAll(filteredFoods);
                            adapter.notifyDataSetChanged();
                            if (rvAllFoods != null) rvAllFoods.setVisibility(View.VISIBLE);
                            if (llNoResults != null) llNoResults.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Toast.makeText(AllFoodsActivity.this, "Failed to load dishes", Toast.LENGTH_SHORT).show();
                    if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                if (shimmer != null) shimmer.setVisibility(View.GONE);
                Toast.makeText(AllFoodsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
            }
        });
    }
}
