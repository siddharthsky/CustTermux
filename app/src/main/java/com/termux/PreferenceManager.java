package com.termux;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "MyPrefs";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Boolean flag methods
    public void setBooleanFlag(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBooleanFlag(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    // Integer flag methods
    public void setIntFlag(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public int getIntFlag(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    // String flag methods
    public void setStringFlag(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getStringFlag(String key) {
        return sharedPreferences.getString(key, null);
    }

}

//    private void sky_terminal() {
//        TerminalView terminalView = findViewById(R.id.terminal_view);
//
//        // Change focusable properties
//        terminalView.setFocusableInTouchMode(true);
//        terminalView.setFocusable(true);
//
//        // Set multiple flags
//        PreferenceManager preferenceManager = new PreferenceManager(this);
//        preferenceManager.setBooleanFlag("isFlagSet1", true);
//        preferenceManager.setBooleanFlag("isFlagSet2", false);
//        preferenceManager.setIntFlag("intFlag", 123);
//        preferenceManager.setStringFlag("stringFlag", "Some value");
//    }


//    private void someOtherMethod() {
//        PreferenceManager preferenceManager = new PreferenceManager(this);
//
//        boolean isFlagSet1 = preferenceManager.getBooleanFlag("isFlagSet1");
//        boolean isFlagSet2 = preferenceManager.getBooleanFlag("isFlagSet2");
//        int intFlag = preferenceManager.getIntFlag("intFlag");
//        String stringFlag = preferenceManager.getStringFlag("stringFlag");
//
//        if (isFlagSet1) {
//            // Flag is set, perform the necessary actions
//        }
//
//        if (isFlagSet2) {
//            // Another flag is set, perform the necessary actions
//        }
//
//        // Use intFlag and stringFlag as needed
//    }

