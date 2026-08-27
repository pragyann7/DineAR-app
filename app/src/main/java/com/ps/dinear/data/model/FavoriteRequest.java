package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteRequest {
    @SerializedName("type")
    private String type; // "restaurant" or "food"

    @SerializedName("item_id")
    private int itemId;

    public FavoriteRequest(String type, int itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    public String getType() { return type; }
    public int getItemId() { return itemId; }
}
