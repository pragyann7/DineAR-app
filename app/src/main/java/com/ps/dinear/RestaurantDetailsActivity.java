package com.ps.dinear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.ps.dinear.menu.MenuActivity;
import com.ps.dinear.data.model.Review;
import com.ps.dinear.SharedPrefManager;
import com.ps.dinear.data.model.ReviewListResponse;
import com.ps.dinear.data.model.ReviewSummary;
import android.widget.LinearLayout;
import java.util.List;

import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private int restaurantId;
    private String restaurantSlug;
    private String name;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);
        
        super.setContentView(R.layout.activity_restaurant_details);

        restaurantId = getIntent().getIntExtra("restaurantId", -1);
        restaurantSlug = getIntent().getStringExtra("restaurantSlug");
        name = getIntent().getStringExtra("restaurantName");
        String cuisine = getIntent().getStringExtra("cuisine");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String description = getIntent().getStringExtra("description");
        String deliveryTime = getIntent().getStringExtra("deliveryTime");
        double distance = getIntent().getDoubleExtra("distance", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String bannerImage = getIntent().getStringExtra("bannerImage");
        String address = getIntent().getStringExtra("address");
        String city = getIntent().getStringExtra("city");
        String district = getIntent().getStringExtra("district");
        boolean isFeatured = getIntent().getBooleanExtra("isFeatured", false);
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        
        if (rating > 0) {
            ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
            ((TextView) findViewById(R.id.tvAvgRatingRest)).setText(String.valueOf(rating));
            ((android.widget.RatingBar) findViewById(R.id.rbRestDetails)).setRating((float) rating);
            ((TextView) findViewById(R.id.tvAvgRatingRestSubtext)).setText("out of 5");
        } else {
            ((TextView) findViewById(R.id.tvRestRatingDetails)).setText("N/A");
            ((TextView) findViewById(R.id.tvAvgRatingRest)).setText("N/A");
            ((android.widget.RatingBar) findViewById(R.id.rbRestDetails)).setRating(0f);
            ((TextView) findViewById(R.id.tvAvgRatingRestSubtext)).setText("No Ratings");
        }

        ((TextView) findViewById(R.id.tvRestTimeDetails)).setText(deliveryTime != null ? deliveryTime : "20-30 min");
        ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(description);
        
        findViewById(R.id.tvPromoBadgeDetails).setVisibility(isFeatured ? View.VISIBLE : View.GONE);

        com.google.android.material.chip.ChipGroup cgCuisines = findViewById(R.id.cgCuisines);
        if (cuisine != null && !cuisine.isEmpty()) {
            String[] parts = cuisine.split(", ");
            for (String part : parts) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(part);
                chip.setChipBackgroundColorResource(R.color.gray_light);
                chip.setChipStrokeWidth(0f);
                chip.setTextColor(getResources().getColor(R.color.gray_text));
                chip.setTextSize(13f);
                cgCuisines.addView(chip);
            }
        }

        StringBuilder fullAddress = new StringBuilder();
        if (address != null && !address.isEmpty()) fullAddress.append(address);
        
        StringBuilder areaInfo = new StringBuilder();
        if (city != null && !city.isEmpty()) areaInfo.append(city);
        if (district != null && !district.isEmpty()) {
            if (areaInfo.length() > 0) areaInfo.append(", ");
            areaInfo.append(district);
        }

        if (fullAddress.length() > 0 && areaInfo.length() > 0) {
            fullAddress.append(", ").append(areaInfo);
        } else if (areaInfo.length() > 0) {
            fullAddress.append(areaInfo);
        }

        TextView tvAddress = findViewById(R.id.tvRestAddressDetails);
        if (fullAddress.length() > 0) {
            tvAddress.setText(fullAddress.toString());
        } else {
            tvAddress.setText("Location details unavailable");
            ((ImageView) findViewById(R.id.ivMapIcon)).setImageResource(R.drawable.icon_nolocation);
        }
        
        if (distance > 0) {
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText(String.format(Locale.US, "%.1f km", distance));
            ((ImageView) findViewById(R.id.ivDistanceIcon)).setImageResource(R.drawable.icon_walk);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ((TextView) findViewById(R.id.tvRestDistanceDetails)).setText("No GPS");
            ((ImageView) findViewById(R.id.ivDistanceIcon)).setImageResource(R.drawable.icon_nolocation);
        } else {
            findViewById(R.id.llDistanceDetails).setVisibility(View.GONE);
        }

        String displayImageUrl = (bannerImage != null && !bannerImage.isEmpty()) ? bannerImage : imageUrl;
        String fullImageUrl = RetrofitClient.getFullUrl(this, displayImageUrl);
        Glide.with(this).load(fullImageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        String fullLogoUrl = RetrofitClient.getFullUrl(this, imageUrl);
        Glide.with(this).load(fullLogoUrl).into((ImageView) findViewById(R.id.ivRestLogoDetails));

        findViewById(R.id.btnBackRestDetails).setOnClickListener(v -> finish());

        findViewById(R.id.cvAddressCard).setOnClickListener(v -> {
            if (latitude != 0 && longitude != 0) {
                Intent discoverIntent = new Intent(this, DiscoverActivity.class);
                discoverIntent.putExtra("focusRestaurantId", restaurantId);
                discoverIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(discoverIntent);
            } else {
                Toast.makeText(this, "Coordinates not available for this restaurant", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView btnFavorite = findViewById(R.id.btnFavoriteRest);
        
        if (FavoritesManager.getInstance().isRestaurantFavorite(restaurantId)) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
        }

        btnFavorite.setOnClickListener(v -> {
            FavoritesManager.getInstance().toggleRestaurantFavorite(this, restaurantId, new FavoritesManager.ToggleCallback() {
                @Override
                public void onStateChanged(boolean isFavorite) {
                    if (isFavorite) {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                        btnFavorite.setColorFilter(getResources().getColor(R.color.red_600));
                    } else {
                        btnFavorite.setImageResource(R.drawable.icon_favorite);
                        btnFavorite.setColorFilter(getResources().getColor(R.color.black));
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(RestaurantDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnViewMenu).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantSlug", restaurantSlug);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });

        findViewById(R.id.cvARExplore).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantSlug", restaurantSlug);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });

        findViewById(R.id.btnWriteReviewRest).setOnClickListener(v -> {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantName", name);
            intent.putExtra("restaurantImage", imageUrl);
            startActivity(intent);
        });

        findViewById(R.id.btnSeeAllReviewsRest).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsListingActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantName", name);
            intent.putExtra("imageUrl", imageUrl);
            startActivity(intent);
        });

        TextView tvDescription = findViewById(R.id.tvRestDescriptionDetails);
        if (description != null && description.length() > 150) {
            String truncated = description.substring(0, 150) + "... ";
            android.text.SpannableString ss = new android.text.SpannableString(truncated + "Read more");
            android.text.style.ForegroundColorSpan fcs = new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.orange_primary));
            android.text.style.StyleSpan bold = new android.text.style.StyleSpan(android.graphics.Typeface.BOLD);
            ss.setSpan(fcs, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(bold, truncated.length(), ss.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvDescription.setText(ss);
            tvDescription.setOnClickListener(v -> tvDescription.setText(description));
        } else {
            tvDescription.setText(description);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReviews(restaurantId);
    }

    private void fetchReviews(int restaurantId) {
        String token = SharedPrefManager.getAccessToken(this);
        String authHeader = token != null ? "Bearer " + token : null;

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getReviews(authHeader, restaurantId, null, true, 1, "-helpful", null).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewListResponse listResponse = response.body();
                    if (listResponse.getSummary() != null) {
                        updateReviewSummary(listResponse.getSummary());
                    }
                    if (listResponse.getResults() != null && !listResponse.getResults().isEmpty()) {
                        updateTopReviews(listResponse.getResults());
                        findViewById(R.id.llEmptyReviewsRest).setVisibility(View.GONE);
                        findViewById(R.id.tvTopReviewHeaderRest).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.llTopReviewsContainerRest).setVisibility(View.GONE);
                        findViewById(R.id.llEmptyReviewsRest).setVisibility(View.VISIBLE);
                        findViewById(R.id.tvTopReviewHeaderRest).setVisibility(View.GONE);
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
            ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.format(Locale.US, "%.1f", summary.getAverageRating()));
            ((TextView) findViewById(R.id.tvAvgRatingRest)).setText(String.format(Locale.US, "%.1f", summary.getAverageRating()));
            ((android.widget.RatingBar) findViewById(R.id.rbRestDetails)).setRating(summary.getAverageRating());
            ((TextView) findViewById(R.id.tvAvgRatingRestSubtext)).setText("out of 5");
            ((TextView) findViewById(R.id.tvReviewsCountRest)).setText("(" + summary.getTotalReviews() + " reviews)");
            findViewById(R.id.btnSeeAllReviewsRest).setVisibility(View.VISIBLE);
            ((androidx.appcompat.widget.AppCompatButton) findViewById(R.id.btnSeeAllReviewsRest)).setText("See All " + summary.getTotalReviews() + " Reviews →");

            Map<String, Integer> dist = summary.getRatingDistribution();
            if (dist != null) {
                updateRatingRow(findViewById(R.id.row5_rest), 5, dist.getOrDefault("5", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row4_rest), 4, dist.getOrDefault("4", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row3_rest), 3, dist.getOrDefault("3", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row2_rest), 2, dist.getOrDefault("2", 0), summary.getTotalReviews());
                updateRatingRow(findViewById(R.id.row1_rest), 1, dist.getOrDefault("1", 0), summary.getTotalReviews());
            }
        } else {
            ((TextView) findViewById(R.id.tvRestRatingDetails)).setText("N/A");
            ((TextView) findViewById(R.id.tvAvgRatingRest)).setText("N/A");
            ((android.widget.RatingBar) findViewById(R.id.rbRestDetails)).setRating(0f);
            ((TextView) findViewById(R.id.tvAvgRatingRestSubtext)).setText("No Ratings");
            ((TextView) findViewById(R.id.tvReviewsCountRest)).setText("(0 reviews)");
            findViewById(R.id.btnSeeAllReviewsRest).setVisibility(View.GONE);
        }

        if (summary.getArAccuracy() > 0) {
            findViewById(R.id.llARAccuracyRest).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvARAccuracyRest)).setText(Math.round(summary.getArAccuracy()) + "% AR Match Accuracy");
        } else {
            findViewById(R.id.llARAccuracyRest).setVisibility(View.GONE);
        }
    }

    private void updateRatingRow(View row, int value, int count, int total) {
        ((TextView) row.findViewById(R.id.tvRatingValue)).setText(String.valueOf(value));
        int percent = total > 0 ? (count * 100 / total) : 0;
        ((android.widget.ProgressBar) row.findViewById(R.id.pbRating)).setProgress(percent);
        ((TextView) row.findViewById(R.id.tvRatingCount)).setText(String.valueOf(count));
    }

    private void updateTopReviews(List<Review> reviews) {
        LinearLayout container = findViewById(R.id.llTopReviewsContainerRest);
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        int count = 0;
        for (Review review : reviews) {
            if (count >= 3) break;

            View topReviewView = getLayoutInflater().inflate(R.layout.item_review, container, false);
            ((TextView) topReviewView.findViewById(R.id.tvReviewerName)).setText(review.getUserName());
            ((TextView) topReviewView.findViewById(R.id.tvReviewDate)).setText(review.getDate());
            ((TextView) topReviewView.findViewById(R.id.tvReviewContent)).setText(review.getContent());
            ((android.widget.RatingBar) topReviewView.findViewById(R.id.ratingBarItem)).setRating(review.getRating());

            if (review.getArMatchPercent() != null) {
                ((TextView) topReviewView.findViewById(R.id.tvARPortionMatch)).setText("AR Match: " + review.getArMatchPercent() + "% Correct");
                ((View) topReviewView.findViewById(R.id.tvARPortionMatch).getParent()).setVisibility(View.VISIBLE);
            } else {
                ((View) topReviewView.findViewById(R.id.tvARPortionMatch).getParent()).setVisibility(View.GONE);
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
            
            container.addView(topReviewView);
            
            TextView tvHelpful = topReviewView.findViewById(R.id.tvHelpfulCount);
            ImageView ivHelpful = (ImageView) ((android.view.ViewGroup) topReviewView.findViewById(R.id.btnHelpful)).getChildAt(0);
            
            updateHelpfulView(review, tvHelpful, ivHelpful);

            if (review.isOwnReview()) {
                topReviewView.findViewById(R.id.btnHelpful).setEnabled(false);
                tvHelpful.setAlpha(0.6f);
                ivHelpful.setAlpha(0.4f);
            } else {
                topReviewView.findViewById(R.id.btnHelpful).setEnabled(true);
                tvHelpful.setAlpha(1.0f);
                ivHelpful.setAlpha(1.0f);
                topReviewView.findViewById(R.id.btnHelpful).setOnClickListener(v -> toggleHelpful(review, tvHelpful, ivHelpful));
            }

            topReviewView.findViewById(R.id.btnShare).setOnClickListener(v -> shareReview(review));

            count++;
        }
    }

    private void updateHelpfulView(Review review, TextView tv, ImageView iv) {
        tv.setText("Helpful" + (review.getHelpfulCount() > 0 ? " (" + review.getHelpfulCount() + ")" : ""));
        int colorRes = review.isHelpful() ? R.color.orange_primary : R.color.gray_text;
        int color = androidx.core.content.ContextCompat.getColor(this, colorRes);
        tv.setTextColor(color);
        iv.setColorFilter(color);
    }

    private void toggleHelpful(Review review, TextView tv, ImageView iv) {
        String token = SharedPrefManager.getAccessToken(this);
        if (token == null) {
            Toast.makeText(this, "Please login to like reviews", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.toggleHelpful("Bearer " + token, review.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isHelpful = (boolean) response.body().get("is_helpful");
                    int count = ((Double) response.body().get("helpful_count")).intValue();
                    review.setHelpful(isHelpful);
                    review.setHelpfulCount(count);
                    updateHelpfulView(review, tv, iv);
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void shareReview(Review review) {
        String shareBody = "Check out this review by " + review.getUserName() + " on DineAR: \"" + review.getContent() + "\"";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Share Review via"));
    }
}
