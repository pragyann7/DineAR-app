package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import com.ps.dinear.MenuItem;
import java.util.List;

public class Restaurant {
    private int id;
    private String name;
    private String description;
    
    @SerializedName("cuisine_names")
    private List<String> cuisineNames;
    
    @SerializedName("price_range")
    private String priceRange;
    
    private double distance;
    private double rating;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("delivery_time")
    private String deliveryTime;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("menu_items")
    private List<MenuItem> menuItems;

    public Restaurant() {
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    
    public List<String> getCuisineNames() { return cuisineNames; }
    
    public String getCuisine() {
        if (cuisineNames == null || cuisineNames.isEmpty()) return "International";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cuisineNames.size(); i++) {
            sb.append(cuisineNames.get(i));
            if (i < cuisineNames.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
    
    public String getPriceRange() { return priceRange; }
    public double getDistance() { return distance; }
    public double getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getDeliveryTime() { return deliveryTime; }
    public String getCategory() { return categoryName; }
    public List<MenuItem> getMenuItems() { return menuItems; }
}
