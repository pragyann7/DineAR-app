package com.ps.dinear.data.model;

import com.ps.dinear.MenuItem;
import java.io.Serializable;

public class CartItem implements Serializable {
    private MenuItem menuItem;
    private int quantity;
    private int restaurantId;

    public CartItem(MenuItem menuItem, int quantity, int restaurantId) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.restaurantId = restaurantId;
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getRestaurantId() { return restaurantId; }
}
