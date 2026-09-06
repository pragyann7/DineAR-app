package com.ps.dinear;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Review;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvReviewerName.setText(review.getUserName());
        holder.tvReviewDate.setText(review.getDate());
        holder.ratingBar.setRating(review.getRating());
        holder.tvReviewContent.setText(review.getContent());
        
        if (review.getArMatchPercent() != null) {
            holder.tvARPortionMatch.setVisibility(View.VISIBLE);
            holder.tvARPortionMatch.setText("AR Match: " + review.getArMatchPercent() + "% Correct");
        } else {
            holder.tvARPortionMatch.setVisibility(View.GONE);
        }
        
        if (review.getManagementResponse() != null) {
            holder.llManagementResponse.setVisibility(View.VISIBLE);
            holder.tvManagementResponse.setText(review.getManagementResponse());
        } else {
            holder.llManagementResponse.setVisibility(View.GONE);
        }

        if (review.getUserAvatar() != null) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(RetrofitClient.getFullUrl(holder.itemView.getContext(), review.getUserAvatar()))
                .placeholder(R.drawable.avatar_0)
                .into(holder.ivReviewerAvatar);
        }
        
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            holder.cvReviewImage.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(RetrofitClient.getFullUrl(holder.itemView.getContext(), review.getImages().get(0).getImage()))
                .into(holder.ivReviewImage);
        } else {
            holder.cvReviewImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerName, tvReviewDate, tvReviewContent, tvARPortionMatch, tvManagementResponse;
        RatingBar ratingBar;
        View llManagementResponse, cvReviewImage;
        ImageView ivReviewerAvatar, ivReviewImage;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewContent = itemView.findViewById(R.id.tvReviewContent);
            tvARPortionMatch = itemView.findViewById(R.id.tvARPortionMatch);
            tvManagementResponse = itemView.findViewById(R.id.tvManagementResponse);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
            llManagementResponse = itemView.findViewById(R.id.llManagementResponse);
            cvReviewImage = itemView.findViewById(R.id.cvReviewImage);
            ivReviewerAvatar = itemView.findViewById(R.id.ivReviewerAvatar);
            ivReviewImage = itemView.findViewById(R.id.ivReviewImage);
        }
    }
}