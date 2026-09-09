package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Review {
    private int id;
    private UserInfo user;
    
    @SerializedName("restaurant")
    private Integer restaurantId;
    
    @SerializedName("food_item")
    private Integer foodItemId;
    
    private int rating;
    
    @SerializedName("comment")
    private String content;
    
    @SerializedName("ar_match_percent")
    private Integer arMatchPercent;
    
    @SerializedName("helpful_count")
    private int helpfulCount;
    
    @SerializedName("is_helpful")
    private boolean isHelpful;
    
    @SerializedName("is_own_review")
    private boolean isOwnReview;
    
    @SerializedName("is_verified_purchase")
    private boolean isVerified;
    
    private List<ReviewImage> images;
    private RestaurantResponse response;
    
    @SerializedName("created_at")
    private String date;

    public static class UserInfo {
        private String name;
        private String avatar;
        
        public String getName() { return name; }
        public String getAvatar() { return avatar; }
    }

    public static class ReviewImage {
        private int id;
        private String image;
        
        public String getImage() { return image; }
    }

    public static class RestaurantResponse {
        private int id;
        private String comment;
        @SerializedName("created_at")
        private String createdAt;
        
        public String getComment() { return comment; }
    }

    @SerializedName("food_item_name")
    private String foodItemName;

    @SerializedName("restaurant_name")
    private String restaurantName;

    public Review() {}

    public int getId() { return id; }
    public String getFoodItemName() { return foodItemName; }
    public String getRestaurantName() { return restaurantName; }
    public UserInfo getUser() { return user; }
    public String getUserName() { return user != null ? user.getName() : "Anonymous"; }
    public String getUserAvatar() { return user != null ? user.getAvatar() : null; }
    public String getDate() {
        if (date == null) return "";
        try {
            if (date.contains("T")) {
                return date.split("T")[0];
            }
            return date;
        } catch (Exception e) {
            return date;
        }
    }

    public float getRating() { return rating; }
    public String getContent() { return content; }
    public Integer getArMatchPercent() { return arMatchPercent; }
    public int getHelpfulCount() { return helpfulCount; }
    public boolean isHelpful() { return isHelpful; }
    public boolean isOwnReview() { return isOwnReview; }
    public void setHelpfulCount(int count) { this.helpfulCount = count; }
    public void setHelpful(boolean helpful) { this.isHelpful = helpful; }
    public boolean isVerified() { return isVerified; }
    public List<ReviewImage> getImages() { return images; }
    public RestaurantResponse getResponse() { return response; }
    public String getManagementResponse() { return response != null ? response.getComment() : null; }

    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }
    public void setFoodItemId(Integer foodItemId) { this.foodItemId = foodItemId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setContent(String content) { this.content = content; }
    public void setArMatchPercent(Integer arMatchPercent) { this.arMatchPercent = arMatchPercent; }
}
