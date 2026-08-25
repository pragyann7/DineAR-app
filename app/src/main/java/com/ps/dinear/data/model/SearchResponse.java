package com.ps.dinear.data.model;

import com.ps.dinear.MenuItem;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {
    @SerializedName("restaurants")
    private List<Restaurant> restaurants;

    @SerializedName("food_items")
    private List<MenuItem> foodItems;

    public List<Restaurant> getRestaurants() { return restaurants; }
    public List<MenuItem> getFoodItems() { return foodItems; }
}
