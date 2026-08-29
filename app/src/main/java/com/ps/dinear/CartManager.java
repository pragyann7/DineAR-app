package com.ps.dinear;

import com.ps.dinear.data.model.CartItem;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems = new ArrayList<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addItem(MenuItem item, int restaurantId) {
        // If adding from a different restaurant, clear cart first (standard food delivery rule)
        if (!cartItems.isEmpty() && cartItems.get(0).getRestaurantId() != restaurantId) {
            cartItems.clear();
        }

        for (CartItem cartItem : cartItems) {
            if (cartItem.getMenuItem().getId() == item.getId()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                return;
            }
        }
        cartItems.add(new CartItem(item, 1, restaurantId));
    }

    public void removeItem(int foodItemId) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getMenuItem().getId() == foodItemId) {
                cartItems.remove(i);
                return;
            }
        }
    }

    public void updateQuantity(int foodItemId, int quantity) {
        for (CartItem cartItem : cartItems) {
            if (cartItem.getMenuItem().getId() == foodItemId) {
                if (quantity <= 0) {
                    removeItem(foodItemId);
                } else {
                    cartItem.setQuantity(quantity);
                }
                return;
            }
        }
    }

    public List<CartItem> getItems() {
        return cartItems;
    }

    public void clear() {
        cartItems.clear();
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            double price = item.getMenuItem().getPrice();
            if (item.getMenuItem().getDiscountPrice() != null) {
                price = item.getMenuItem().getDiscountPrice();
            }
            total += price * item.getQuantity();
        }
        return total;
    }

    public int getRestaurantId() {
        if (cartItems.isEmpty()) return -1;
        return cartItems.get(0).getRestaurantId();
    }
    
    public int getItemCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }
}
