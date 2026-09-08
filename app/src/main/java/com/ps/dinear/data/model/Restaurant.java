package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import com.ps.dinear.MenuItem;
import java.util.List;

public class Restaurant {
    private int id;
    private String name;
    private String slug;
    private String description;
    private String email;
    
    @SerializedName("phone_number")
    private String phoneNumber;
    
    @SerializedName("categories")
    private List<Category> categories;
    
    @SerializedName("location")
    private Location location;
    
    @SerializedName("price_range")
    private String priceRange;
    
    private double distance;
    private double rating;
    
    @SerializedName("logo")
    private String imageUrl;

    @SerializedName("banner_image")
    private String bannerImage;
    
    @SerializedName("delivery_time")
    private String deliveryTime;

    @SerializedName("address")
    private String address;

    @SerializedName("opening_time")
    private String openingTime;

    @SerializedName("closing_time")
    private String closingTime;

    @SerializedName("is_featured")
    private boolean isFeatured;

    @SerializedName("delivery_charge")
    private double deliveryCharge;

    private double latitude;
    private double longitude;

    @SerializedName("menu_items")
    private List<MenuItem> menuItems;

    public Restaurant() {
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    
    public String getCuisine() {
        if (categories == null || categories.isEmpty()) return "General";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(categories.get(i).getName());
            if (i < categories.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
    
    public String getPriceRange() { return priceRange != null ? priceRange : "$$"; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public double getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getBannerImage() { return bannerImage; }
    public String getDeliveryTime() { return deliveryTime != null ? deliveryTime : "20-30 MIN"; }
    public String getAddress() { return address; }
    public Location getLocation() { return location; }
    public String getOpeningTime() { return openingTime; }
    public String getClosingTime() { return closingTime; }
    public boolean isFeatured() { return isFeatured; }
    public double getDeliveryCharge() { return deliveryCharge; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<MenuItem> getMenuItems() { return menuItems; }

    public static class Category {
        private int id;
        private String name;
        private String icon;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
    }

    public static class Location {
        private int id;
        private String district;
        private String city;

        public int getId() { return id; }
        public String getDistrict() { return district; }
        public String getCity() { return city; }
    }
}
