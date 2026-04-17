package com.termux.sky;

import android.content.Context;
import android.content.SharedPreferences;

public class SkySharedPref {

    private static final String PREF_NAME = "settings";
    private static final String KEY_AUTOSTART = "auto_start_boot";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // SAVE
    public static void setAutoStart(Context context, boolean value) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_AUTOSTART, value)
            .apply();
    }

    // READ
    public static boolean isAutoStartEnabled(Context context) {
        return getPrefs(context)
            .getBoolean(KEY_AUTOSTART, false); // default OFF
    }

    public static void setAutoStartMode(Context context, String mode) {

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        prefs.edit()
            .putString("auto_start_mode", mode)
            .apply();
    }

    public static String getAutoStartMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getString("auto_start_mode", "disabled");
    }

    public static int getVersionCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getInt("version_code", 0);
    }

    public static void setVersionCode(Context context, int version) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putInt("version_code", version).apply();
    }
}
