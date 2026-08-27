package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class FavoriteIdsResponse {
    @SerializedName("restaurants")
    private List<Integer> restaurantIds = new ArrayList<>();

    @SerializedName("foods")
    private List<Integer> foodIds = new ArrayList<>();

    public List<Integer> getRestaurantIds() { return restaurantIds; }
    public List<Integer> getFoodIds() { return foodIds; }
}
