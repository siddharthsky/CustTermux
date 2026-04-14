package com.termux.sky;

import android.content.Context;
import android.content.SharedPreferences;

public class SkySharedPref {

    private static final String PREF_NAME = "sky_prefs";
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
}
