package com.ps.dinear.auth;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ProfileResponse implements Serializable {
    private int id;
    private String email;
    
    @SerializedName("first_name")
    private String firstName;
    
    @SerializedName("last_name")
    private String lastName;
    
    @SerializedName("full_name")
    private String fullName;
    
    @SerializedName("phone_number")
    private String phoneNumber;
    
    @SerializedName("profile_picture")
    private String profilePicture;
    
    @SerializedName("is_verified")
    private boolean isVerified;
    
    @SerializedName("auth_provider")
    private String authProvider;

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getProfilePicture() { return profilePicture; }
    public boolean isVerified() { return isVerified; }
    public String getAuthProvider() { return authProvider; }
}
