package com.ps.dinear.auth;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    private String access;
    private String refresh;
    private String role;
    
    @SerializedName("first_name")
    private String firstName;
    
    @SerializedName("last_name")
    private String lastName;

    public String getAccess() { return access; }
    public String getRefresh() { return refresh; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    
    public String getFullName() {
        if (firstName == null) return "";
        return (firstName + " " + (lastName != null ? lastName : "")).trim();
    }
}