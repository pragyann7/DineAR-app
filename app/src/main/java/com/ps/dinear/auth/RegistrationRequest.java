package com.ps.dinear.auth;

import com.google.gson.annotations.SerializedName;

public class RegistrationRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("password")
    private String password;

    public RegistrationRequest(String email, String firstName, String lastName, String password) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }
}
