package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.RatingBar;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.ps.dinear.data.model.Review;
import com.ps.dinear.data.model.ReviewListResponse;
import com.ps.dinear.data.model.ReviewSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewsListingActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private List<Review> reviewsList = new ArrayList<>();
    private Integer restaurantId;
    private Integer foodItemId;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;
    private String currentOrdering = "-created_at";
    private String currentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews_listing);

        findViewById(R.id.btnBackReviewsListing).setOnClickListener(v -> finish());

        restaurantId = getIntent().getIntExtra("restaurantId", -1);
        if (restaurantId == -1) restaurantId = null;
        
        MenuItem foodItem;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            foodItem = getIntent().getSerializableExtra("selectedItem", MenuItem.class);
        } else {
            foodItem = (MenuItem) getIntent().getSerializableExtra("selectedItem");
        }
        
        if (foodItem != null) {
            foodItemId = foodItem.getId();
        }

        String restaurantName = getIntent().getStringExtra("restaurantName");
        if (restaurantName != null) {
            ((TextView) findViewById(R.id.tvReviewsTitle)).setText(restaurantName + " Reviews");
            ((TextView) findViewById(R.id.tvRestaurantListing)).setText(restaurantName);
        }

        if (foodItem != null) {
            ((TextView) findViewById(R.id.tvFoodNameListing)).setText(foodItem.getName());
            ((TextView) findViewById(R.id.tvPriceListing)).setText(String.format(Locale.US, "%s %d", foodItem.getCurrency(), (int)foodItem.getPrice()));
            
            String fullImageUrl = RetrofitClient.getFullUrl(this, foodItem.getImageUrl());
            Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivFoodListing));
        } else {
            findViewById(R.id.tvFoodNameListing).setVisibility(View.GONE);
            findViewById(R.id.tvPriceListing).setVisibility(View.GONE);
            
            String restaurantImage = getIntent().getStringExtra("imageUrl");
            if (restaurantImage != null) {
                String fullImageUrl = RetrofitClient.getFullUrl(this, restaurantImage);
                Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivFoodListing));
            }
        }

        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReviewAdapter(reviewsList);
        rvReviews.setAdapter(adapter);

        TextView tvSortedBy = findViewById(R.id.tvSortedBy);
        if (tvSortedBy != null) {
            tvSortedBy.setOnClickListener(this::showSortMenu);
        }

        findViewById(R.id.chipFilterAll).setOnClickListener(v -> applyFilter(null));
        findViewById(R.id.chipFilterPhotos).setOnClickListener(v -> applyFilter("has_photos"));
        findViewById(R.id.chipFilterAR).setOnClickListener(v -> applyFilter("has_ar"));

        NestedScrollView scrollView = findViewById(R.id.nestedScrollViewReviews);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                if (!isLoading && hasNextPage) {
                    fetchReviews(currentPage + 1);
                }
            }
        });

        findViewById(R.id.btnWriteReviewBottom).setOnClickListener(v -> {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            if (restaurantId != null) intent.putExtra("restaurantId", restaurantId);
            if (restaurantName != null) intent.putExtra("restaurantName", restaurantName);
            if (foodItem != null) intent.putExtra("selectedItem", foodItem);
            
            String imgUrl = getIntent().getStringExtra("imageUrl");
            if (imgUrl != null) intent.putExtra("restaurantImage", imgUrl);
            
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReviews(1);
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Newest First");
        popup.getMenu().add(0, 4, 1, "Most Helpful");
        popup.getMenu().add(0, 2, 2, "Highest Rating");
        popup.getMenu().add(0, 3, 3, "Lowest Rating");
        
        popup.setOnMenuItemClickListener(item -> {
            String newOrdering;
            String label;
            switch (item.getItemId()) {
                case 1: newOrdering = "-created_at"; label = "Newest First"; break;
                case 4: newOrdering = "-helpful"; label = "Most Helpful"; break;
                case 2: newOrdering = "-rating"; label = "Highest Rating"; break;
                case 3: newOrdering = "rating"; label = "Lowest Rating"; break;
                default: return false;
            }
            
            if (!newOrdering.equals(currentOrdering)) {
                currentOrdering = newOrdering;
                ((TextView) v).setText("Sorted by: " + label);
                fetchReviews(1);
            }
            return true;
        });
        popup.show();
    }

    private void applyFilter(String filter) {
        if ((filter == null && currentFilter == null) || (filter != null && filter.equals(currentFilter))) {
            return;
        }
        currentFilter = filter;
        updateChipStyles();
        fetchReviews(1);
    }

    private void updateChipStyles() {
        Chip chipAll = findViewById(R.id.chipFilterAll);
        Chip chipPhotos = findViewById(R.id.chipFilterPhotos);
        Chip chipAR = findViewById(R.id.chipFilterAR);

        updateSingleChipStyle(chipAll, currentFilter == null);
        updateSingleChipStyle(chipPhotos, "has_photos".equals(currentFilter));
        updateSingleChipStyle(chipAR, "has_ar".equals(currentFilter));
    }

    private void updateSingleChipStyle(Chip chip, boolean isSelected) {
        if (isSelected) {
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange_primary)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            chip.setChipStrokeWidth(0f);
            if (chip.getChipIcon() != null) {
                chip.setChipIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            }
        } else {
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.gray_text));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.divider)));
            if (chip.getChipIcon() != null) {
                chip.setChipIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_text)));
            }
        }
    }

    private void fetchReviews(int page) {
        if (isLoading) return;
        isLoading = true;
        
        if (page > 1) {
            findViewById(R.id.pbLoadMore).setVisibility(View.VISIBLE);
        }

        String token = SharedPrefManager.getAccessToken(this);
        String authHeader = token != null ? "Bearer " + token : null;

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getReviews(authHeader, restaurantId, foodItemId, page == 1, page, currentOrdering, currentFilter, null).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> response) {
                isLoading = false;
                findViewById(R.id.pbLoadMore).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ReviewListResponse listResponse = response.body();
                    
                    if (page == 1) {
                        reviewsList.clear();
                    }
                    
                    if (listResponse.getResults() != null && !listResponse.getResults().isEmpty()) {
                        reviewsList.addAll(listResponse.getResults());
                        findViewById(R.id.llEmptyReviewsListing).setVisibility(View.GONE);
                        findViewById(R.id.rvReviews).setVisibility(View.VISIBLE);
                        currentPage = page;
                    } else if (page == 1) {
                        findViewById(R.id.llEmptyReviewsListing).setVisibility(View.VISIBLE);
                        findViewById(R.id.rvReviews).setVisibility(View.GONE);
                    }
                    
                    hasNextPage = listResponse.getNext() != null;
                    adapter.notifyDataSetChanged();
                    
                    if (listResponse.getSummary() != null) {
                        updateSummary(listResponse.getSummary());
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewListResponse> call, Throwable t) {
                isLoading = false;
                findViewById(R.id.pbLoadMore).setVisibility(View.GONE);
                Toast.makeText(ReviewsListingActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary(ReviewSummary summary) {
        if (summary.getTotalReviews() > 0) {
            ((TextView) findViewById(R.id.tvAvgRatingListing)).setText(String.format(Locale.US, "%.1f / 5", summary.getAverageRating()));
            ((RatingBar) findViewById(R.id.rbAvgRatingListing)).setRating(summary.getAverageRating());
            ((TextView) findViewById(R.id.tvRatingBadgeListing)).setText(String.format(Locale.US, "%.1f", summary.getAverageRating()));
        } else {
            ((TextView) findViewById(R.id.tvAvgRatingListing)).setText("N/A");
            ((RatingBar) findViewById(R.id.rbAvgRatingListing)).setRating(0f);
            ((TextView) findViewById(R.id.tvRatingBadgeListing)).setText("N/A");
        }

        ((TextView) findViewById(R.id.tvTotalReviewsListing)).setText(summary.getTotalReviews() + " Reviews");

        if (summary.getArAccuracy() > 0) {
            ((TextView) findViewById(R.id.tvARAccuracyListing)).setText(Math.round(summary.getArAccuracy()) + "% AR Match Accuracy");
        } else {
            ((TextView) findViewById(R.id.tvARAccuracyListing)).setText("N/A AR Match Accuracy");
        }

        ((Chip) findViewById(R.id.chipFilterAll)).setText("All (" + summary.getTotalReviews() + ")");
        ((Chip) findViewById(R.id.chipFilterPhotos)).setText("With Photos (" + summary.getPhotoCount() + ")");
        ((Chip) findViewById(R.id.chipFilterAR)).setText("AR Match (" + summary.getArMatchCount() + ")");
        
        updateChipStyles();
        
        Map<String, Integer> dist = summary.getRatingDistribution();
        if (dist != null) {
            updateRatingRow(findViewById(R.id.row5), 5, dist.getOrDefault("5", 0), summary.getTotalReviews());
            updateRatingRow(findViewById(R.id.row4), 4, dist.getOrDefault("4", 0), summary.getTotalReviews());
            updateRatingRow(findViewById(R.id.row3), 3, dist.getOrDefault("3", 0), summary.getTotalReviews());
            updateRatingRow(findViewById(R.id.row2), 2, dist.getOrDefault("2", 0), summary.getTotalReviews());
            updateRatingRow(findViewById(R.id.row1), 1, dist.getOrDefault("1", 0), summary.getTotalReviews());
        }
    }

    private void updateRatingRow(View row, int value, int count, int total) {
        ((TextView) row.findViewById(R.id.tvRatingValue)).setText(String.valueOf(value));
        int percent = total > 0 ? (count * 100 / total) : 0;
        ((ProgressBar) row.findViewById(R.id.pbRating)).setProgress(percent);
        ((TextView) row.findViewById(R.id.tvRatingCount)).setText(String.valueOf(count));
    }
}