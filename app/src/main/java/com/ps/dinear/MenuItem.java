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

    @SerializedName("name")
    private String name;

    @SerializedName("price")
    private double price;

    @SerializedName("discount_price")
    private Double discountPrice;

    @SerializedName("currency")
    private String currency;

    private String description;
    
    @SerializedName("images")
    private List<FoodImage> images;
    
    @SerializedName("ar_mapping")
    private ARMapping arMapping;
    
    @SerializedName("category_id")
    private Integer categoryId;

    @SerializedName("category")
    private String category;
    
    private String status;

    @SerializedName("is_featured")
    private boolean isFeatured;

    public MenuItem() {}

    public MenuItem(int id, String name, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        // Legacy constructor, might not set images properly but keeps compatibility
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
        return null; // Fallback to lookup in Adapter
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public Double getDiscountPrice() { return discountPrice; }
    public String getCurrency() { return currency != null ? currency : "NPR"; }
    public String getDescription() {
        return (description != null && !description.isEmpty()) ? description : "No description available.";
    }
    public String getImageUrl() {
        if (images != null && !images.isEmpty()) {
            for (FoodImage img : images) {
                if (img.isPrimary) return img.image;
            }
            return images.get(0).image;
        }
        return null;
    }
    
    public String getModelUrl() {
        if (arMapping != null && arMapping.arAsset != null) {
            return arMapping.arAsset.modelFile;
        }
        return null;
    }
    
    public String getModelName() {
        if (arMapping != null && arMapping.arAsset != null) {
            return arMapping.arAsset.name;
        }
        return null;
    }
    
    public String getModelVersion() {
        if (arMapping != null && arMapping.arAsset != null) {
            return String.valueOf(arMapping.arAsset.version);
        }
        return "1";
    }
    
    public Integer getCategoryId() { return categoryId; }
    public String getCategory() { return category; }
    public boolean has3d() { return getModelUrl() != null; }
    public String getStatus() { return status; }
    public boolean isFeatured() { return isFeatured; }

    public String getTag1() { return "DineAR"; }
    public String getTag2() { return isFeatured ? "Featured" : "Food"; }

    public static class ARMapping implements Serializable {
        @SerializedName("ar_asset")
        public ARAsset arAsset;
    }

    public static class ARAsset implements Serializable {
        public int id;
        public String name;
        @SerializedName("model_file")
        public String modelFile;
        public String format;
        public int version;
    }

    public static class FoodImage implements Serializable {
        public int id;
        public String image;
        @SerializedName("is_primary")
        public boolean isPrimary;
    }
}
