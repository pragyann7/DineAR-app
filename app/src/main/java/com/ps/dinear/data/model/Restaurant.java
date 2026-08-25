package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import com.ps.dinear.MenuItem;
import java.util.List;

public class Restaurant {
    private int id;
    private String name;
    private String description;
    private String cuisine;
    
    @SerializedName("price_range")
    private String priceRange;
    
    private double distance;
    private double rating;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("delivery_time")
    private String deliveryTime;

    private String category;

    @SerializedName("menu_items")
    private List<MenuItem> menuItems;

    public Restaurant(int id, String name, String description, String cuisine, String priceRange, double distance, double rating, String imageUrl, String deliveryTime, String category, List<MenuItem> menuItems) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cuisine = cuisine;
        this.priceRange = priceRange;
        this.distance = distance;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.deliveryTime = deliveryTime;
        this.category = category;
        this.menuItems = menuItems;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCuisine() { return cuisine; }
    public String getPriceRange() { return priceRange; }
    public double getDistance() { return distance; }
    public double getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getDeliveryTime() { return deliveryTime; }
    public String getCategory() { return category; }
    public List<MenuItem> getMenuItems() { return menuItems; }
}
