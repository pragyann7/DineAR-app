package com.ps.dinear;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class MenuItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String slug;
    
    @SerializedName("restaurant_ids")
    private List<Integer> restaurantIds;

    @SerializedName("restaurant_name")
    private String restaurantName;

    @SerializedName("restaurant_slug")
    private String restaurantSlug;

    @SerializedName("name")
    private String name;

    @SerializedName("price")
    private double price;

    @SerializedName("discount_price")
    private Double discountPrice;

    @SerializedName("currency")
    private String currency;

    private String description;
    
    @SerializedName("primary_image")
    private String primaryImage;
    
    @SerializedName("images")
    private List<FoodImage> images;
    
    @SerializedName("ar_mapping")
    private ARMapping arMapping;
    
    @SerializedName("category_id")
    private Integer categoryId;

    @SerializedName("category")
    private String category;
    
    private String status;

    @SerializedName("approval_status")
    private String approvalStatus;

    @SerializedName("is_featured")
    private boolean isFeatured;

    public MenuItem() {}

    public MenuItem(int id, String name, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.primaryImage = imageUrl;
    }

    public int getId() { return id; }
    public String getSlug() { return slug; }
    public List<Integer> getRestaurantIds() { return restaurantIds; }

    public Integer getRestaurantId() {
        if (restaurantIds != null && !restaurantIds.isEmpty()) {
            return restaurantIds.get(0);
        }
        return null;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantSlug() {
        return restaurantSlug;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public Double getDiscountPrice() { return discountPrice; }
    public String getCurrency() { return currency != null ? currency : "NPR"; }
    public String getDescription() {
        return (description != null && !description.isEmpty()) ? description : "No description available.";
    }
    public String getImageUrl() {
        if (primaryImage != null && !primaryImage.isEmpty()) return primaryImage;
        if (images != null && !images.isEmpty()) {
            for (FoodImage img : images) {
                if (img.isPrimary) return img.image;
            }
            return images.get(0).image;
        }
        return null;
    }
    
    public String getModelUrl() {
        if (arMapping != null) {
            if (arMapping.arAsset != null) return arMapping.arAsset.modelFile;
            if (arMapping.pendingArAsset != null) return arMapping.pendingArAsset.modelFile;
        }
        return null;
    }
    
    public String getModelName() {
        if (arMapping != null) {
            if (arMapping.arAsset != null) return arMapping.arAsset.name;
            if (arMapping.pendingArAsset != null) return arMapping.pendingArAsset.name;
        }
        return null;
    }
    
    public String getModelVersion() {
        if (arMapping != null) {
            if (arMapping.arAsset != null) return String.valueOf(arMapping.arAsset.version);
            if (arMapping.pendingArAsset != null) return String.valueOf(arMapping.pendingArAsset.version);
        }
        return "1";
    }
    
    public Integer getCategoryId() { return categoryId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean has3d() { return getModelUrl() != null; }
    public String getStatus() { return status; }
    public String getApprovalStatus() { return approvalStatus; }
    public boolean isFeatured() { return isFeatured; }

    public String getTag1() { return "DineAR"; }
    public String getTag2() { return isFeatured ? "Featured" : "Food"; }

    public static class ARMapping implements Serializable {
        @SerializedName("ar_asset")
        public ARAsset arAsset;
        @SerializedName("pending_ar_asset")
        public ARAsset pendingArAsset;
        @SerializedName("assigned_at")
        public String assignedAt;
    }

    public static class ARAsset implements Serializable {
        public int id;
        public String name;
        @SerializedName("model_file")
        public String modelFile;
        public String format;
        public String thumbnail;
        public int version;
        @SerializedName("is_active")
        public boolean isActive;
        public String source;
        @SerializedName("approval_status")
        public String approvalStatus;
        @SerializedName("file_size")
        public long fileSize;
    }

    public static class FoodImage implements Serializable {
        public int id;
        public String image;
        @SerializedName("is_primary")
        public boolean isPrimary;
    }
}
