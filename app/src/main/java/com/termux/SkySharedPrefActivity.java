package com.termux;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SkySharedPrefActivity extends Activity {

    private static final String TAG = "SkySharedPrefActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SkySharedPref skySharedPref = new SkySharedPref(this);
        Intent intent = getIntent();

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "com.termux.SaveReceiver":
                        handleSaveAction(intent, skySharedPref);
                        break;
                    case "com.termux.GetReceiver":
                        handleGetAction(intent, skySharedPref);
                        break;
                    default:
                        Log.w(TAG, "Unknown action received: " + action);
                        break;
                }
            }
        }
        finish(); // End activity after intent processing
    }

    private void handleSaveAction(Intent intent, SkySharedPref skySharedPref) {
        String key = intent.getStringExtra("key");
        String value = intent.getStringExtra("value");

        if (key != null && value != null) {
            // Save the key-value pair to shared preferences
            skySharedPref.setKey(key, value);
            logData("Saved", key, value);
        } else {
            Log.e(TAG, "Save action failed: Key or Value is null.");
        }
    }

    private void handleGetAction(Intent intent, SkySharedPref skySharedPref) {
        String key = intent.getStringExtra("key");

        if (key != null) {
            String value = skySharedPref.getKey(key);
            if (value == null) {
                value = "null"; // Treat null values as "null" string
            }
            // Send the retrieved data back via broadcast
            Intent responseIntent = new Intent("com.termux.GetResponse");
            responseIntent.putExtra("key", key);
            responseIntent.putExtra("value", value);
            sendBroadcast(responseIntent);

            logData("Retrieved", key, value);
        } else {
            Log.e(TAG, "Get action failed: Key is null.");
        }
    }

    private void logData(String action, String key, String value) {
        // Centralized logging for better readability and consistency
        String logMessage = action + " key: " + key + ", value: " + value;
        System.out.println(logMessage);
        System.err.println(logMessage);
        Log.d(TAG, logMessage);
        Log.e(TAG, logMessage);
    }
}
