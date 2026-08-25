package com.ps.dinear;

import android.app.Application;

public class DineARApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Retrofit on app start
        RetrofitClient.initialize(this);
    }
}
