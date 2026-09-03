package com.ps.dinear;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Review;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class ReviewsListingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews_listing);

        findViewById(R.id.btnBackReviewsListing).setOnClickListener(v -> finish());

        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));

        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review("Elena Rostova", "Yesterday", 5.0f, 
            "The golden-seared crust on these jumbo sea scallops is sublime...", 
            "AR Match: 100% Correct", true));
        reviews.add(new Review("Thomas Keller", "4 days ago", 5.0f, 
            "Flawless caramelization. The natural sweetness of the scallops...", 
            "Great Service", true));
        reviews.add(new Review("Clara S.", "1 week ago", 4.0f,
            "Served on a real scallop shell nestled over warm sea salt...",
            "Good AR Match", true));
        
        ReviewAdapter adapter = new ReviewAdapter(reviews);
        rvReviews.setAdapter(adapter);

        findViewById(R.id.btnWriteReviewBottom).setOnClickListener(v -> {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            startActivity(intent);
        });
    }
}