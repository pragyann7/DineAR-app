package com.ps.dinear;

public class MenuItem {
    private int id;
    private String name;
    private int price;
    private String description;
    private String imageUrl;
    private String modelUrl;
    private String modelName;
    private String modelVersion;
    private String category;
    private String tag1;
    private String tag2;

    public MenuItem(int id, String name, int price, String imageUrl, String modelUrl, String modelName, String modelVersion) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.modelUrl = modelUrl;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.description = "No description available.";
        this.category = "General";
        this.tag1 = "DineAR";
        this.tag2 = "Food";
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getModelUrl() { return modelUrl; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public String getCategory() { return category; }
    public String getTag1() { return tag1; }
    public String getTag2() { return tag2; }
}