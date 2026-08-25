package com.ps.dinear.auth;

public class RegistrationRequest {
    private String username;
    private String email;
    private String password;
    private String phone_number;

    public RegistrationRequest(String username, String email, String password, String phone_number) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
    }
}