package com.ps.dinear;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ps.dinear.data.model.Review;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteReviewActivity extends AppCompatActivity {

    private Integer restaurantId;
    private MenuItem selectedItem;
    private Integer selectedArAccuracy = null;
    private boolean isArAccuracySelected = false;
    
    private MaterialButton btnSpotOn, btnVeryAccurate, btnSlightlyOff, btnDidNotUseAR;
    private RatingBar ratingBar;
    private TextView tvRatingLabel, tvCharCount;
    private EditText etImpressions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        restaurantId = getIntent().getIntExtra("restaurantId", -1);
        if (restaurantId == -1) restaurantId = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            selectedItem = getIntent().getSerializableExtra("selectedItem", MenuItem.class);
        } else {
            selectedItem = (MenuItem) getIntent().getSerializableExtra("selectedItem");
        }

        initViews();
        setupListeners();
        setupContextUI();
    }

    private void initViews() {
        btnSpotOn = findViewById(R.id.btnSpotOn);
        btnVeryAccurate = findViewById(R.id.btnVeryAccurate);
        btnSlightlyOff = findViewById(R.id.btnSlightlyOff);
        btnDidNotUseAR = findViewById(R.id.btnDidNotUseAR);
        
        ratingBar = findViewById(R.id.ratingBarReview);
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        tvCharCount = findViewById(R.id.tvCharCount);
        etImpressions = findViewById(R.id.etImpressions);
    }

    private void setupListeners() {
        findViewById(R.id.btnBackWriteReview).setOnClickListener(v -> finish());

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> updateRatingLabel((int) rating));

        btnSpotOn.setOnClickListener(v -> selectArAccuracy(100, btnSpotOn));
        btnVeryAccurate.setOnClickListener(v -> selectArAccuracy(80, btnVeryAccurate));
        btnSlightlyOff.setOnClickListener(v -> selectArAccuracy(50, btnSlightlyOff));
        btnDidNotUseAR.setOnClickListener(v -> selectArAccuracy(null, btnDidNotUseAR));

        etImpressions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnSubmitReview).setOnClickListener(v -> submitReview());
        
        findViewById(R.id.btnAddPhoto).setOnClickListener(v -> Toast.makeText(this, "Photo upload coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void setupContextUI() {
        String restaurantName = getIntent().getStringExtra("restaurantName");
        String restaurantImage = getIntent().getStringExtra("restaurantImage");
        
        if (selectedItem != null) {
            ((TextView) findViewById(R.id.tvFoodNameReview)).setText(selectedItem.getName());
            if (restaurantName != null) {
                ((TextView) findViewById(R.id.tvRestaurantReview)).setText(restaurantName);
            }
            
            String fullImageUrl = RetrofitClient.getFullUrl(this, selectedItem.getImageUrl());
            com.bumptech.glide.Glide.with(this).load(fullImageUrl)
                .placeholder(R.drawable.burger)
                .into((android.widget.ImageView) findViewById(R.id.ivFoodReview));
                
            ((TextView) findViewById(R.id.tvWriteReviewHeaderTitle)).setText("Review " + selectedItem.getName());
        } else if (restaurantName != null) {
            ((TextView) findViewById(R.id.tvRestaurantReview)).setText(restaurantName);
            ((TextView) findViewById(R.id.tvWriteReviewHeaderTitle)).setText("Review " + restaurantName);
            ((TextView) findViewById(R.id.tvWriteReviewQuestion)).setText("How was your experience at " + restaurantName + "?");
            ((TextView) findViewById(R.id.tvTellUsMoreHeader)).setText("Tell us more about the restaurant");
            ((TextView) findViewById(R.id.tvAddPhotosHeader)).setText("Add Restaurant Photos");
            ((TextView) findViewById(R.id.tvARAccuracyQuestion)).setText("Did the AR menus help your choice?");
            
            findViewById(R.id.tvFoodNameReview).setVisibility(View.GONE);
            
            if (restaurantImage != null) {
                String fullImageUrl = RetrofitClient.getFullUrl(this, restaurantImage);
                com.bumptech.glide.Glide.with(this).load(fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into((android.widget.ImageView) findViewById(R.id.ivFoodReview));
            }
        }
    }

    private void updateRatingLabel(int rating) {
        switch (rating) {
            case 1: tvRatingLabel.setText("Disappointing"); break;
            case 2: tvRatingLabel.setText("Average"); break;
            case 3: tvRatingLabel.setText("Good"); break;
            case 4: tvRatingLabel.setText("Very Good"); break;
            case 5: tvRatingLabel.setText("Outstanding!"); break;
            default: tvRatingLabel.setText("Select Rating"); break;
        }
    }

    private void selectArAccuracy(Integer percent, MaterialButton selectedBtn) {
        selectedArAccuracy = percent;
        isArAccuracySelected = true;
        
        resetArButton(btnSpotOn);
        resetArButton(btnVeryAccurate);
        resetArButton(btnSlightlyOff);
        resetArButton(btnDidNotUseAR);
        
        int highlightColor = (percent == null) ? 
            android.graphics.Color.parseColor("#616161") : 
            android.graphics.Color.parseColor("#7E57C2");   
            
        selectedBtn.setBackgroundTintList(ColorStateList.valueOf(highlightColor));
        selectedBtn.setTextColor(android.graphics.Color.WHITE);
    }

    private void resetArButton(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray_light)));
        btn.setTextColor(getResources().getColor(R.color.black));
    }

    private void submitReview() {
        int rating = (int) ratingBar.getRating();
        String comment = etImpressions.getText().toString();
        
        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isArAccuracySelected) {
            Toast.makeText(this, "Please select AR accuracy feedback", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review();
        review.setRating(rating);
        review.setContent(comment);
        if (restaurantId != null) review.setRestaurantId(restaurantId);
        if (selectedItem != null) review.setFoodItemId(selectedItem.getId());
        
        review.setArMatchPercent(selectedArAccuracy);

        String token = SharedPrefManager.getAccessToken(this);
        if (token == null) {
            Toast.makeText(this, "Please login to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.postReview("Bearer " + token, review).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(Call<Review> call, Response<Review> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WriteReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Failed to submit review";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String errorJson = errorBody.string();
                            if (errorJson.contains("detail")) {
                                errorMsg = errorJson.split("\"detail\":\"")[1].split("\"")[0];
                            } else if (errorJson.contains("[\"")) {
                                errorMsg = errorJson.split("\\[\"")[1].split("\"]")[0];
                            }
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(WriteReviewActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Review> call, Throwable t) {
                Toast.makeText(WriteReviewActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
