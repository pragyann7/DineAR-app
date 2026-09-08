package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private int id;
    private int restaurant;
    
    @SerializedName("restaurant_details")
    private Restaurant restaurantDetails;
    
    private String status;
    
    @SerializedName("total_price")
    private double totalPrice;
    
    @SerializedName("delivery_address")
    private String deliveryAddress;
    
    @SerializedName("contact_number")
    private String contactNumber;

    @SerializedName("delivery_option")
    private String deliveryOption;

    @SerializedName("delivery_charge")
    private double deliveryCharge;

    private List<OrderItem> items;
    
    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public Restaurant getRestaurantDetails() { return restaurantDetails; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getContactNumber() { return contactNumber; }
    public String getDeliveryOption() { return deliveryOption; }
    public double getDeliveryCharge() { return deliveryCharge; }
    public List<OrderItem> getItems() { return items; }
    public String getCreatedAt() { return createdAt; }

    public static class OrderItem implements Serializable {
        private int id;
        
        @SerializedName("food_item")
        private int foodItemId;
        
        @SerializedName("food_item_details")
        private com.ps.dinear.MenuItem foodItemDetails;
        
        private int quantity;
        private double price;

        public int getId() { return id; }
        public com.ps.dinear.MenuItem getFoodItemDetails() { return foodItemDetails; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
    }
}
