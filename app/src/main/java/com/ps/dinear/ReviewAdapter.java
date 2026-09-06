package com.ps.dinear;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

        // Helpful Button Logic
        updateHelpfulUI(holder, review);
        if (review.isOwnReview()) {
            holder.btnHelpful.setEnabled(false);
            holder.ivHelpfulIcon.setAlpha(0.4f);
            holder.tvHelpfulCount.setAlpha(0.6f); // Slightly more visible for readability
        } else {
            holder.btnHelpful.setEnabled(true);
            holder.ivHelpfulIcon.setAlpha(1.0f);
            holder.tvHelpfulCount.setAlpha(1.0f);
            holder.btnHelpful.setOnClickListener(v -> toggleHelpful(holder, review));
        }

        // Share Button Logic
        holder.btnShare.setOnClickListener(v -> shareReview(holder.itemView.getContext(), review));
    }

    private void updateHelpfulUI(ReviewViewHolder holder, Review review) {
        holder.tvHelpfulCount.setText("Helpful" + (review.getHelpfulCount() > 0 ? " (" + review.getHelpfulCount() + ")" : ""));
        int color = review.isHelpful() ? R.color.orange_primary : R.color.gray_text;
        holder.tvHelpfulCount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));
        holder.ivHelpfulIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), color));
    }

    private void toggleHelpful(ReviewViewHolder holder, Review review) {
        String token = SharedPrefManager.getAccessToken(holder.itemView.getContext());
        if (token == null) {
            android.widget.Toast.makeText(holder.itemView.getContext(), "Please login to like reviews", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(holder.itemView.getContext()).create(ApiService.class);
        apiService.toggleHelpful("Bearer " + token, review.getId()).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isHelpful = (boolean) response.body().get("is_helpful");
                    int count = ((Double) response.body().get("helpful_count")).intValue();
                    
                    review.setHelpful(isHelpful);
                    review.setHelpfulCount(count);
                    updateHelpfulUI(holder, review);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void shareReview(android.content.Context context, Review review) {
        String shareBody = "Check out this review by " + review.getUserName() + " on DineAR: \"" + review.getContent() + "\"";
        android.content.Intent sharingIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "DineAR Review");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        context.startActivity(android.content.Intent.createChooser(sharingIntent, "Share Review via"));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerName, tvReviewDate, tvReviewContent, tvARPortionMatch, tvManagementResponse, tvHelpfulCount;
        RatingBar ratingBar;
        View llManagementResponse, cvReviewImage, btnHelpful, btnShare;
        ImageView ivReviewerAvatar, ivReviewImage, ivHelpfulIcon;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewContent = itemView.findViewById(R.id.tvReviewContent);
            tvARPortionMatch = itemView.findViewById(R.id.tvARPortionMatch);
            tvManagementResponse = itemView.findViewById(R.id.tvManagementResponse);
            tvHelpfulCount = itemView.findViewById(R.id.tvHelpfulCount);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
            llManagementResponse = itemView.findViewById(R.id.llManagementResponse);
            cvReviewImage = itemView.findViewById(R.id.cvReviewImage);
            ivReviewerAvatar = itemView.findViewById(R.id.ivReviewerAvatar);
            ivReviewImage = itemView.findViewById(R.id.ivReviewImage);
            btnHelpful = itemView.findViewById(R.id.btnHelpful);
            btnShare = itemView.findViewById(R.id.btnShare);
            ivHelpfulIcon = (ImageView) ((ViewGroup) btnHelpful).getChildAt(0);
        }
    }
}