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
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

public class FoodDetailsActivity extends AppCompatActivity {

    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
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

        // Header Section
        ((TextView) findViewById(R.id.tvFoodCategoryDetails)).setText(item.getCategory() != null ? item.getCategory() : "Food");
        ((TextView) findViewById(R.id.tvFoodNameDetails)).setText(item.getName());

        // Price Section
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

        // Description
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

        // Chips/Tags
        ChipGroup cgCuisines = findViewById(R.id.cgCuisines);
        cgCuisines.removeAllViews();
        // Use backend category as the primary tag
        if (item.getCategory() != null) addChip(cgCuisines, item.getCategory());
        if (item.isFeatured()) addChip(cgCuisines, "Popular");
        
        // Handle Signature Tag Visibility
        findViewById(R.id.llSignatureTag).setVisibility(item.isFeatured() ? View.VISIBLE : View.GONE);
        
        // Image
        String fullImageUrl = RetrofitClient.getFullUrl(this, item.getImageUrl());
        Glide.with(this).load(fullImageUrl)
                .placeholder(R.drawable.burger)
                .into((ImageView) findViewById(R.id.ivFoodLarge));

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        // Favorite
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

        // AR Section
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

        // Quantity Logic
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

        // Add to Cart
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            int restaurantId = getIntent().getIntExtra("restaurantId", -1);
            if (restaurantId == -1 && item.getRestaurantId() != null) {
                restaurantId = item.getRestaurantId();
            }

            if (restaurantId != -1) {
                // Add with quantity
                for (int i = 0; i < quantity; i++) {
                    CartManager.getInstance().addItem(item, restaurantId);
                }
                Toast.makeText(this, "Added " + quantity + " to cart!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Restaurant ID missing", Toast.LENGTH_SHORT).show();
            }
        });
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
