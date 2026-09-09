package com.ps.dinear;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class DineARApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // Initialize Retrofit on app start
        RetrofitClient.initialize(this);
    }
}
