package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ReviewSummary {
    @SerializedName("average_rating")
    private float averageRating;
    
    @SerializedName("total_reviews")
    private int totalReviews;
    
    @SerializedName("rating_distribution")
    private Map<String, Integer> ratingDistribution;
    
    @SerializedName("ar_accuracy")
    private float arAccuracy;
    
    @SerializedName("ar_match_count")
    private int arMatchCount;
    
    @SerializedName("photo_count")
    private int photoCount;

    public float getAverageRating() { return averageRating; }
    public int getTotalReviews() { return totalReviews; }
    public Map<String, Integer> getRatingDistribution() { return ratingDistribution; }
    public float getArAccuracy() { return arAccuracy; }
    public int getArMatchCount() { return arMatchCount; }
    public int getPhotoCount() { return photoCount; }
}
