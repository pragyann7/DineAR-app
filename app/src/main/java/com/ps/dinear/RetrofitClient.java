package com.ps.dinear;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static String baseUrl;

    public static void initialize(String url) {
        baseUrl = url;
        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(
                        GsonConverterFactory.create()
                )
                .build();
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static Retrofit getClient() {

        if (retrofit == null) {
            throw new RuntimeException(
                    "Retrofit not initialized"
            );
        }

        return retrofit;
    }
}