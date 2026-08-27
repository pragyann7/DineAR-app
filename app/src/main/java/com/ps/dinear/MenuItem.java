package com.ps.dinear;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Serializable;

public class MenuItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    
    @SerializedName("restaurant_id")
    private Integer restaurantId;

    @SerializedName(value = "restaurant_name", alternate = {"restaurant_title", "restaurantName"})
    private String restaurantName;

    @SerializedName("restaurant")
    private JsonElement restaurant;
    
    private String name;
    private double price;
    private String description;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("model_url")
    private String modelUrl;
    
    @SerializedName("model_name")
    private String modelName;
    
    @SerializedName("model_version")
    private String modelVersion;
    
    @SerializedName("category_name")
    private String categoryName;
    
    private String tag1;
    private String tag2;
    
    @SerializedName("is_available")
    private boolean isAvailable;

    @SerializedName("has_3d")
    private boolean has3d;

    private String status;

    public MenuItem(int id, String name, double price, String imageUrl, String modelUrl, String modelName, String modelVersion) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.modelUrl = modelUrl;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.description = "No description available.";
        this.categoryName = "General";
        this.tag1 = "DineAR";
        this.tag2 = "Food";
        this.isAvailable = true;
    }

    public int getId() { return id; }
    public Integer getRestaurantId() {
        if (restaurantId != null) return restaurantId;
        if (restaurant != null && restaurant.isJsonPrimitive() && restaurant.getAsJsonPrimitive().isNumber()) {
            return restaurant.getAsInt();
        }
        return null;
    }
    public String getRestaurantName() {
        if (restaurantName != null && !restaurantName.isEmpty()) {
            return restaurantName;
        }
        if (restaurant != null) {
            if (restaurant.isJsonPrimitive()) {
                if (restaurant.getAsJsonPrimitive().isString()) {
                    return restaurant.getAsString();
                }
            } else if (restaurant.isJsonObject()) {
                JsonObject obj = restaurant.getAsJsonObject();
                if (obj.has("name")) {
                    return obj.get("name").getAsString();
                } else if (obj.has("restaurant_name")) {
                    return obj.get("restaurant_name").getAsString();
                } else if (obj.has("restaurant")) {
                    JsonElement nested = obj.get("restaurant");
                    if (nested.isJsonPrimitive()) return nested.getAsString();
                }
            }
        }
        return null;
    }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getModelUrl() { return modelUrl; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public String getCategory() { return categoryName; }
    public String getTag1() { return tag1; }
    public String getTag2() { return tag2; }
    public boolean isAvailable() { return isAvailable; }
    public boolean has3d() { return has3d; }
    public String getStatus() { return status; }
}
