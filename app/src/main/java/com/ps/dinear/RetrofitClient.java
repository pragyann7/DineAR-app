package com.ps.dinear;

import android.content.Context;
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

    public static void initialize(Context context) {
        String ip = SharedPrefManager.getIp(context);
        String port = SharedPrefManager.getPort(context);
        
        if (ip == null) {
            ip = "192.168.1.66"; // Default fallback
        }
        
        String url = "http://" + ip + ":" + port + "/";
        initialize(url);
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static String getBaseUrl(Context context) {
        if (baseUrl == null) {
            initialize(context);
        }
        return baseUrl;
    }

    public static String getFullUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.startsWith("http")) {
            return relativeUrl;
        }
        String base = getBaseUrl();
        if (base == null) return null;

        if (base.endsWith("/") && relativeUrl.startsWith("/")) {
            return base + relativeUrl.substring(1);
        }
        return base + relativeUrl;
    }

    public static String getFullUrl(Context context, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return null;
        }
        if (relativeUrl.startsWith("http")) {
            return relativeUrl;
        }
        String base = getBaseUrl(context);
        if (base == null) return null;

        if (base.endsWith("/") && relativeUrl.startsWith("/")) {
            return base + relativeUrl.substring(1);
        }
        if (!base.endsWith("/") && !relativeUrl.startsWith("/")) {
            return base + "/" + relativeUrl;
        }
        return base + relativeUrl;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            initialize(context);
        }
        return retrofit;
    }

    // Deprecated: Use getClient(Context) to ensure initialization
    public static Retrofit getClient() {
        if (retrofit == null) {
            throw new RuntimeException("Retrofit not initialized. Use getClient(Context)");
        }
        return retrofit;
    }
}
