package com.termux;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SkySharedPrefActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SkySharedPref skySharedPref = new SkySharedPref(this);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();

            if ("com.termux.SaveReceiver".equals(action)) {
                String key = intent.getStringExtra("key");
                String value = intent.getStringExtra("value");

                if (key != null && value != null) {
                    skySharedPref.setKey(key, value);
                    Log.d("SkySharedPrefActivity", "Saved key: " + key + ", value: " + value);
                }
            } else if ("com.termux.GetReceiver".equals(action)) {
                String key = intent.getStringExtra("key");

                if (key != null) {
                    String value = skySharedPref.getKey(key);
                    if (value == null) {
                        value = "null";
                    }

                    // Send response
                    Intent responseIntent = new Intent();
                    responseIntent.setAction("com.termux.GetResponse");
                    responseIntent.putExtra("key", key);
                    responseIntent.putExtra("value", value);
                    sendBroadcast(responseIntent);

                    Log.d("SkySharedPrefActivity", "Retrieved key: " + key + ", value: " + value);
                }
            }
        }

        // Finish the activity after processing the intent
        finish();
    }
}
