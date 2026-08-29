package com.ps.dinear;

import android.content.Context;
import com.ps.dinear.auth.TokenAuthenticator;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static String baseUrl;

    public static void initialize(String url, Context context) {
        baseUrl = url;
        
        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(new TokenAuthenticator(context))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
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
        initialize(url, context);
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
