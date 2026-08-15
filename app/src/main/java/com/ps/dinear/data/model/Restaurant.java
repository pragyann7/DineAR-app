package com.ps.dinear.data.model;

public class Restaurant {
    private int id;
    private String name;
    private String description;
    private String cuisine;
    private String priceRange;
    private double distance;
    private double rating;
    private String imageUrl;
    private String deliveryTime;

    public Restaurant(int id, String name, String description, String cuisine, String priceRange, double distance, double rating, String imageUrl, String deliveryTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cuisine = cuisine;
        this.priceRange = priceRange;
        this.distance = distance;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.deliveryTime = deliveryTime;
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
}