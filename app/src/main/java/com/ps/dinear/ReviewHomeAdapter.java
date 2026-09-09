package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewHomeAdapter extends RecyclerView.Adapter<ReviewHomeAdapter.ViewHolder> {

    private List<Review> reviews = new ArrayList<>();
    private final Context context;

    public ReviewHomeAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        
        holder.tvUserName.setText(review.getUserName());
        holder.rbRating.setRating(review.getRating());
        holder.tvSnippet.setText(review.getContent());
        
        String target = "on " + (review.getFoodItemName() != null ? review.getFoodItemName() : (review.getRestaurantName() != null ? review.getRestaurantName() : "DineAR"));
        holder.tvTarget.setText(target);

        // Logic for user avatar if available in review object
        holder.ivUser.setImageResource(R.drawable.ic_person);
    }

    @Override
    public int getItemCount() {
        return Math.min(reviews.size(), 6);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUser;
        TextView tvUserName, tvSnippet, tvTarget;
        RatingBar rbRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUser = itemView.findViewById(R.id.ivUserReview);
            tvUserName = itemView.findViewById(R.id.tvUserNameReview);
            tvSnippet = itemView.findViewById(R.id.tvReviewSnippetHome);
            tvTarget = itemView.findViewById(R.id.tvReviewTargetHome);
            rbRating = itemView.findViewById(R.id.rbReviewHome);
        }
    }
}
