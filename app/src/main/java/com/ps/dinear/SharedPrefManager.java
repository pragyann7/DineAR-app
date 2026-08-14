package com.ps.dinear;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "server_config";

    private static final String KEY_IP = "server_ip";
    private static final String KEY_PORT = "server_port";

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
}