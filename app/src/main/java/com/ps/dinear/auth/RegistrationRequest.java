package com.ps.dinear.auth;

import com.google.gson.annotations.SerializedName;

public class RegistrationRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("password")
    private String password;

    public RegistrationRequest(String username, String email, String phoneNumber, String firstName, String lastName, String password) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }
}
