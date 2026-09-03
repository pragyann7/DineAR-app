package com.ps.dinear.data.model;

public class Review {
    private String id;
    private String userName;
    private String userAvatar;
    private String date;
    private float rating;
    private String content;
    private String arAccuracy;
    private boolean isVerified;
    private int helpfulCount;
    private String imageUrl;
    private String managementResponse;

    public Review(String userName, String date, float rating, String content, String arAccuracy, boolean isVerified) {
        this.userName = userName;
        this.date = date;
        this.rating = rating;
        this.content = content;
        this.arAccuracy = arAccuracy;
        this.isVerified = isVerified;
    }

    // Getters and Setters
    public String getUserName() { return userName; }
    public String getDate() { return date; }
    public float getRating() { return rating; }
    public String getContent() { return content; }
    public String getArAccuracy() { return arAccuracy; }
    public boolean isVerified() { return isVerified; }
    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getManagementResponse() { return managementResponse; }
    public void setManagementResponse(String managementResponse) { this.managementResponse = managementResponse; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
}