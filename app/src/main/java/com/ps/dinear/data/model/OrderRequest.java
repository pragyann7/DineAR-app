package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class OrderRequest implements Serializable {
    private int restaurant;
    
    @SerializedName("delivery_address")
    private String deliveryAddress;
    
    @SerializedName("contact_number")
    private String contactNumber;
    
    private List<OrderItemRequest> items;

    public OrderRequest(int restaurant, String deliveryAddress, String contactNumber, List<OrderItemRequest> items) {
        this.restaurant = restaurant;
        this.deliveryAddress = deliveryAddress;
        this.contactNumber = contactNumber;
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
