package com.ps.dinear.data.model;

import com.google.gson.annotations.SerializedName;
import com.ps.dinear.MenuItem;
import java.io.Serializable;
import java.util.List;

public class RestaurantMenuResponse implements Serializable {
    @SerializedName("restaurant")
    private String restaurantName;
    
    private String slug;
    
    @SerializedName("marker_database")
    private String markerDatabase;
    
    private List<CategoryGroup> categories;

    public String getRestaurantName() { return restaurantName; }
    public String getSlug() { return slug; }
    public String getMarkerDatabase() { return markerDatabase; }
    public List<CategoryGroup> getCategories() { return categories; }

    public static class CategoryGroup implements Serializable {
        private int id;
        private String name;
        private String description;
        private int order;
        
        @SerializedName("foods")
        private List<MenuItem> menuItems;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getOrder() { return order; }
        public List<MenuItem> getMenuItems() { return menuItems; }
    }
}
