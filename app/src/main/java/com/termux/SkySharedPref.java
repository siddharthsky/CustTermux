package com.termux;

import android.content.Context;
import android.content.SharedPreferences;

public class SkySharedPref {

    private static final String PREF_NAME = "SkySharedPref";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SkySharedPref(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Method to save key-value pairs
    public void setKey(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    // Method to retrieve values by key
    public String getKey(String key) {
        return sharedPreferences.getString(key, null);
    }
}



