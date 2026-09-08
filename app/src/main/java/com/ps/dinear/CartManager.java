package com.ps.dinear;

import com.ps.dinear.data.model.CartItem;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems = new ArrayList<>();
    private String restaurantName;
    private double deliveryCharge = 50.00; // Default
    private List<CartListener> listeners = new ArrayList<>();

    public interface CartListener {
        void onCartChanged();
    }

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addListener(CartListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CartListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (CartListener listener : listeners) {
            listener.onCartChanged();
        }
    }

    public void addItem(MenuItem item, int restaurantId, String restaurantName, double deliveryCharge) {
        // If adding from a different restaurant, clear cart first (standard food delivery rule)
        if (!cartItems.isEmpty() && cartItems.get(0).getRestaurantId() != restaurantId) {
            cartItems.clear();
        }
        
        this.restaurantName = restaurantName;
        this.deliveryCharge = deliveryCharge;

        boolean found = false;
        for (CartItem cartItem : cartItems) {
            if (cartItem.getMenuItem().getId() == item.getId()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(new CartItem(item, 1, restaurantId));
        }
        notifyListeners();
    }

    public void removeItem(int foodItemId) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getMenuItem().getId() == foodItemId) {
                cartItems.remove(i);
                notifyListeners();
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
                    notifyListeners();
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
        notifyListeners();
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
    
    public String getRestaurantName() {
        return restaurantName;
    }

    public double getDeliveryCharge() {
        return deliveryCharge;
    }

    public int getItemCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }
}
