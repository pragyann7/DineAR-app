package com.ps.dinear.auth;

import com.google.gson.annotations.SerializedName;

public class RegistrationResponse {
    private String message;
    private String email;
    @SerializedName("first_name")
    private String firstName;
    @SerializedName("last_name")
    private String lastName;

    public String getMessage() { return message; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}
