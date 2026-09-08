package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class OrderRequest implements Serializable {
    private int restaurant;
    
    @SerializedName("delivery_address")
    private String deliveryAddress;

    private Double latitude;
    private Double longitude;
    
    @SerializedName("contact_number")
    private String contactNumber;
    
    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("delivery_option")
    private String deliveryOption;
    
    private List<OrderItemRequest> items;

    public OrderRequest(int restaurant, String deliveryAddress, Double latitude, Double longitude, 
                        String contactNumber, String paymentMethod, String deliveryOption, 
                        List<OrderItemRequest> items) {
        this.restaurant = restaurant;
        this.deliveryAddress = deliveryAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactNumber = contactNumber;
        this.paymentMethod = paymentMethod;
        this.deliveryOption = deliveryOption;
        this.items = items;
    }

    public static class OrderItemRequest implements Serializable {
        @SerializedName("food_item_id")
        private int foodItemId;
        
        private int quantity;

        public OrderItemRequest(int foodItemId, int quantity) {
            this.foodItemId = foodItemId;
            this.quantity = quantity;
        }
    }
}
