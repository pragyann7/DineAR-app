package com.ps.dinear;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "server_config";

    private static final String KEY_IP = "server_ip";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_DISTRICT = "selected_district";
    private static final String KEY_CITY = "selected_city";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_ONBOARDING_FINISHED = "onboarding_finished";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    public static void saveServer(
            Context context,
            String ip,
            String port
    ) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putString(KEY_IP, ip)
                .putString(KEY_PORT, port)
                .apply();
    }

    public static void saveLocation(Context context, String district, String city) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putString(KEY_DISTRICT, district)
                .putString(KEY_CITY, city)
                .apply();
    }

    public static String getIp(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return prefs.getString(KEY_IP, null);
    }

    public static String getPort(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return prefs.getString(KEY_PORT, "8000");
    }

    public static String getDistrict(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return prefs.getString(KEY_DISTRICT, null);
    }

    public static String getCity(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return prefs.getString(KEY_CITY, null);
    }

    public static void setIsGuest(Context context, boolean isGuest) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply();
    }

    public static boolean isGuest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    public static void setIsLoggedIn(Context context, boolean isLoggedIn) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void setOnboardingFinished(Context context, boolean finished) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_FINISHED, finished).apply();
    }

    public static boolean hasFinishedOnboarding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_FINISHED, false);
    }

    public static void saveTokens(Context context, String access, String refresh) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, access)
                .putString(KEY_REFRESH_TOKEN, refresh)
                .apply();
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public static String getRefreshToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public static void saveUserInfo(Context context, String name, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "Guest");
    }

    public static String getUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public static void clearAuth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}