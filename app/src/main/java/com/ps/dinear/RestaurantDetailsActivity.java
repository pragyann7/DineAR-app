package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.ps.dinear.menu.MenuActivity;

public class RestaurantDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        int restaurantId = getIntent().getIntExtra("restaurantId", -1);
        String name = getIntent().getStringExtra("restaurantName");
        String cuisine = getIntent().getStringExtra("cuisine");
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String description = getIntent().getStringExtra("description");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        ((TextView) findViewById(R.id.tvRestNameDetails)).setText(name);
        ((TextView) findViewById(R.id.tvRestRatingDetails)).setText(String.valueOf(rating));
        ((TextView) findViewById(R.id.tvRestCuisineDetails)).setText(cuisine);
        ((TextView) findViewById(R.id.tvRestDescriptionDetails)).setText(description);
        ((TextView) findViewById(R.id.tvRestAddressDetails)).setText("Bharatpur, Chitwan, Nepal");

        Glide.with(this).load(imageUrl).into((ImageView) findViewById(R.id.ivRestaurantHeader));

        findViewById(R.id.btnBackRestDetails).setOnClickListener(v -> finish());

        findViewById(R.id.btnViewMenu).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("restaurantName", name);
            startActivity(intent);
        });
    }
}