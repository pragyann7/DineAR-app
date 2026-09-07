package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Restaurant;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rv;
    private View shimmer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getResources().getColor(R.color.white));
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        super.setContentView(R.layout.activity_category);

        findViewById(R.id.btnBackCategory).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvCategoryList);
        shimmer = findViewById(R.id.shimmerCategories);
        
        loadCategories();
    }

    private void loadCategories() {
        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
        if (rv != null) rv.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getCategories().enqueue(new Callback<List<Restaurant.Category>>() {
            @Override
            public void onResponse(Call<List<Restaurant.Category>> call, Response<List<Restaurant.Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (shimmer != null) shimmer.setVisibility(View.GONE);
                    List<String> categoryNames = new ArrayList<>();
                    for (Restaurant.Category cat : response.body()) {
                        categoryNames.add(cat.getName());
                    }
                    
                    FilterAdapter adapter = new FilterAdapter(CategoryActivity.this, categoryNames, R.layout.item_filter_grid, categoryName -> {
                        Intent data = new Intent();
                        data.putExtra("selected_category", categoryName);
                        setResult(RESULT_OK, data);
                        finish();
                    });
                    if (rv != null) {
                        rv.setAdapter(adapter);
                        rv.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(CategoryActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<Restaurant.Category>> call, Throwable t) {
                Toast.makeText(CategoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
