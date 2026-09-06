package com.ps.dinear.menu;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.ARActivity;
import com.ps.dinear.CartManager;
import com.ps.dinear.FavoritesManager;
import com.ps.dinear.MenuItem;
import com.ps.dinear.ApiService;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.ReviewsListingActivity;
import com.ps.dinear.WriteReviewActivity;
import com.ps.dinear.data.model.Review;
import com.ps.dinear.data.model.ReviewListResponse;
import com.ps.dinear.data.model.ReviewSummary;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodDetailsActivity extends AppCompatActivity {

    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        super.setContentView(R.layout.activity_food_details);

        MenuItem item = (MenuItem) getIntent().getSerializableExtra("selectedItem");
        
        if (item == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvFoodCategoryDetails)).setText(item.getCategory() != null ? item.getCategory() : "Food");
        ((TextView) findViewById(R.id.tvFoodNameDetails)).setText(item.getName());

        TextView tvPrice = findViewById(R.id.tvFoodPriceDetails);
        TextView tvOldPrice = findViewById(R.id.tvFoodOldPriceDetails);

        if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0) {
            tvPrice.setText(String.format(Locale.US, "%s %d", item.getCurrency(), item.getDiscountPrice().intValue()));
            tvOldPrice.setText(String.format(Locale.US, "%s %d", item.getCurrency(), (int)item.getPrice()));
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvOldPrice.setVisibility(View.VISIBLE);
        } else {
            tvPrice.setText(String.format(Locale.US, "%s %d", item.getCurrency(), (int)item.getPrice()));
            tvOldPrice.setVisibility(View.GONE);
        }

        TextView tvDescription = findViewById(R.id.tvDescriptionDetails);
        String description = item.getDescription();
        if (description != null && description.length() > 140) {
            String truncated = description.substring(0, 140) + "... ";
            android.text.SpannableString ss = new android.text.SpannableString(truncated + "Read more");
            android.text.style.ForegroundColorSpan fcs = new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.orange_primary));
            android.text.style.StyleSpan bold = new android.text.style.StyleSpan(android.graphics.Typeface.BOLD);
            ss.setSpan(fcs, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(bold, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvDescription.setText(ss);
            tvDescription.setOnClickListener(v -> {
                tvDescription.setText(description);
                tvDescription.setClickable(false);
            });
        } else {
            tvDescription.setText(description);
        }

        ChipGroup cgCuisines = findViewById(R.id.cgCuisines);
        cgCuisines.removeAllViews();
        if (item.getCategory() != null) addChip(cgCuisines, item.getCategory());
        if (item.isFeatured()) addChip(cgCuisines, "Popular");
        
        findViewById(R.id.llSignatureTag).setVisibility(item.isFeatured() ? View.VISIBLE : View.GONE);
        
        String fullImageUrl = RetrofitClient.getFullUrl(this, item.getImageUrl());
        Glide.with(this).load(fullImageUrl)
                .placeholder(R.drawable.burger)
                .into((ImageView) findViewById(R.id.ivFoodLarge));

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        ImageView btnFavorite = findViewById(R.id.btnFavoriteFoodDetails);
        updateFavoriteIcon(btnFavorite, FavoritesManager.getInstance().isFoodFavorite(item.getId()));

        btnFavorite.setOnClickListener(v -> {
            FavoritesManager.getInstance().toggleFoodFavorite(this, item.getId(), new FavoritesManager.ToggleCallback() {
                @Override
                public void onStateChanged(boolean isFavorite) {
                    updateFavoriteIcon(btnFavorite, isFavorite);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(FoodDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        View arCard = findViewById(R.id.cvARExploreFood);
        if (item.has3d()) {
            arCard.setVisibility(View.VISIBLE);
            arCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, ARActivity.class);
                intent.putExtra("selectedItem", item);
                intent.putExtra("restaurantId", getIntent().getIntExtra("restaurantId", -1));
                intent.putExtra("restaurantSlug", getIntent().getStringExtra("restaurantSlug"));
                startActivity(intent);
            });
        } else {
            arCard.setVisibility(View.GONE);
        }

        TextView tvQuantity = findViewById(R.id.tvQuantity);
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            int restaurantId = getIntent().getIntExtra("restaurantId", -1);
            if (restaurantId == -1 && item.getRestaurantId() != null) {
                restaurantId = item.getRestaurantId();
            }

            if (restaurantId != -1) {
                for (int i = 0; i < quantity; i++) {
                    CartManager.getInstance().addItem(item, restaurantId);
                }
                Toast.makeText(this, "Added " + quantity + " to cart!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Restaurant ID missing", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnWriteReviewDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            intent.putExtra("selectedItem", item);
            intent.putExtra("restaurantName", getIntent().getStringExtra("restaurantName"));
            startActivity(intent);
        });

        findViewById(R.id.btnSeeAllReviewsDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsListingActivity.class);
            intent.putExtra("selectedItem", item);
            intent.putExtra("restaurantId", getIntent().getIntExtra("restaurantId", -1));
            intent.putExtra("restaurantName", getIntent().getStringExtra("restaurantName"));
            intent.putExtra("imageUrl", item.getImageUrl());
            startActivity(intent);
        });

        fetchReviews(item.getId());
    }

    private void fetchReviews(int foodItemId) {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getReviews(null, foodItemId, true, 1, null, null).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewListResponse listResponse = response.body();
                    if (listResponse.getSummary() != null) {
                        updateReviewSummary(listResponse.getSummary());
                    }
                    if (listResponse.getResults() != null && !listResponse.getResults().isEmpty()) {
                        updateTopReview(listResponse.getResults().get(0));
                        findViewById(R.id.llEmptyReviewsDetails).setVisibility(View.GONE);
                        findViewById(R.id.tvTopReviewsHeader).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.includeTopReview).setVisibility(View.GONE);
                        findViewById(R.id.llEmptyReviewsDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.tvTopReviewsHeader).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewListResponse> call, Throwable t) {
            }
        });
    }

    private void updateReviewSummary(ReviewSummary summary) {
        if (summary.getTotalReviews() > 0) {
            ((TextView) findViewById(R.id.tvFoodRatingDetails)).setText(String.format(Locale.US, "%.1f", summary.getAverageRating()));
            ((TextView) findViewById(R.id.tvAvgRatingDetailsLarge)).setText(String.format(Locale.US, "%.1f", summary.getAverageRating()));
            ((android.widget.RatingBar) findViewById(R.id.rbAvgRatingDetails)).setRating(summary.getAverageRating());
            ((TextView) findViewById(R.id.tvAvgRatingSubtext)).setText("out of 5");
            ((TextView) findViewById(R.id.tvReviewsCountDetails)).setText("(" + summary.getTotalReviews() + " dish reviews)");
            findViewById(R.id.btnSeeAllReviewsDetails).setVisibility(View.VISIBLE);
            ((androidx.appcompat.widget.AppCompatButton) findViewById(R.id.btnSeeAllReviewsDetails)).setText("See All " + summary.getTotalReviews() + " Reviews →");
        } else {
            ((TextView) findViewById(R.id.tvFoodRatingDetails)).setText("N/A");
            ((TextView) findViewById(R.id.tvAvgRatingDetailsLarge)).setText("N/A");
            ((android.widget.RatingBar) findViewById(R.id.rbAvgRatingDetails)).setRating(0f);
            ((TextView) findViewById(R.id.tvAvgRatingSubtext)).setText("New Dish");
            ((TextView) findViewById(R.id.tvReviewsCountDetails)).setText("(0 reviews)");
            findViewById(R.id.btnSeeAllReviewsDetails).setVisibility(View.GONE);
        }

        if (summary.getTotalReviews() > 0) {
            Map<String, Integer> dist = summary.getRatingDistribution();
            if (dist != null) {
                updateRatingRow(findViewById(R.id.row5_details), 5, dist.getOrDefault("5", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row4_details), 4, dist.getOrDefault("4", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row3_details), 3, dist.getOrDefault("3", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row2_details), 2, dist.getOrDefault("2", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row1_details), 1, dist.getOrDefault("1", 0), summary.getTotalReviews());
            }
        } else {
            updateRatingRow(findViewById(R.id.row5_details), 5, 0, 0);
            updateRatingRow(findViewById(R.id.row4_details), 4, 0, 0);
            updateRatingRow(findViewById(R.id.row3_details), 3, 0, 0);
            updateRatingRow(findViewById(R.id.row2_details), 2, 0, 0);
            updateRatingRow(findViewById(R.id.row1_details), 1, 0, 0);
        }

        if (summary.getArAccuracy() > 0) {
            findViewById(R.id.llARAccuracyDetails).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvARAccuracyDetails)).setText(Math.round(summary.getArAccuracy()) + "% AR Match Accuracy");
        } else {
            findViewById(R.id.llARAccuracyDetails).setVisibility(View.GONE);
        }
    }

    private void updateRatingRow(View row, int value, int count, int total) {
        ((TextView) row.findViewById(R.id.tvRatingValue)).setText(String.valueOf(value));
        int percent = total > 0 ? (count * 100 / total) : 0;
        ((android.widget.ProgressBar) row.findViewById(R.id.pbRating)).setProgress(percent);
        ((TextView) row.findViewById(R.id.tvRatingCount)).setText(String.valueOf(count));
    }

    private void updateTopReview(Review review) {
        View topReviewView = findViewById(R.id.includeTopReview);
        topReviewView.setVisibility(View.VISIBLE);
        ((TextView) topReviewView.findViewById(R.id.tvReviewerName)).setText(review.getUserName());
        ((TextView) topReviewView.findViewById(R.id.tvReviewDate)).setText(review.getDate());
        ((TextView) topReviewView.findViewById(R.id.tvReviewContent)).setText(review.getContent());
        ((android.widget.RatingBar) topReviewView.findViewById(R.id.ratingBarItem)).setRating(review.getRating());
        
        if (review.getArMatchPercent() != null) {
            ((TextView) topReviewView.findViewById(R.id.tvARPortionMatch)).setText("AR Match: " + review.getArMatchPercent() + "% Correct");
        }
        
        if (review.getUserAvatar() != null) {
            Glide.with(this).load(RetrofitClient.getFullUrl(this, review.getUserAvatar()))
                .placeholder(R.drawable.avatar_0)
                .into((ImageView) topReviewView.findViewById(R.id.ivReviewerAvatar));
        }
        
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            topReviewView.findViewById(R.id.cvReviewImage).setVisibility(View.VISIBLE);
            Glide.with(this).load(RetrofitClient.getFullUrl(this, review.getImages().get(0).getImage()))
                .into((ImageView) topReviewView.findViewById(R.id.ivReviewImage));
        } else {
            topReviewView.findViewById(R.id.cvReviewImage).setVisibility(View.GONE);
        }
    }

    private void addChip(ChipGroup group, String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setChipBackgroundColorResource(R.color.gray_light);
        chip.setChipStrokeWidth(0f);
        chip.setTextColor(getResources().getColor(R.color.gray_text));
        chip.setTextSize(12f);
        group.addView(chip);
    }

    private void updateFavoriteIcon(ImageView view, boolean isFavorite) {
        if (isFavorite) {
            view.setImageResource(R.drawable.ic_favorite_filled);
            view.setColorFilter(getResources().getColor(R.color.red_600));
        } else {
            view.setImageResource(R.drawable.icon_favorite);
            view.setColorFilter(getResources().getColor(R.color.black));
        }
    }
}
