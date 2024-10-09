package com.termux;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerStatusChecker {

    private String urlLink;
    private final Handler handler;
    private Runnable runnable;
    private final TextView serverStatusTextView;
    private final Context context;

    public ServerStatusChecker(Context context, TextView serverStatusTextView) {
        this.handler = new Handler();
        this.serverStatusTextView = serverStatusTextView;
        this.context = context;
    }

    public void startChecking() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
                handler.postDelayed(this, 7000); // Check every 7 seconds
            }
        };
        handler.post(runnable);
    }

    public void stopChecking() {
        handler.removeCallbacks(runnable);
    }

    private void checkServerStatus() {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        urlLink = preferenceManager.getKey("isLocalPORT");

        if (urlLink == null || urlLink.isEmpty()) {
            updateStatus("Error: Invalid URL");
            Log.e("ServerStatusChecker", "URL is invalid or not set in preferences");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(urlLink);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000); // 3 seconds timeout
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == 200) {
                    updateStatus("Running");
                } else if (responseCode == 500) {
                    updateStatus("Stopped");
                } else {
                    updateStatus("Stopped");
                }
            } catch (IOException e) {
                updateStatus("Error");
                Log.e("ServerStatusChecker", "IOException occurred while checking server status", e);
            }
        }).start();

        Log.d("ServerStatusChecker", "Checked Server Status");
    }

    private void updateStatus(final String status) {
        serverStatusTextView.post(() -> {
            serverStatusTextView.setText(status);

            // Save server status in shared preferences
            SkySharedPref preferenceManager = new SkySharedPref(context);
            preferenceManager.setKey("isServerRunning", status);

            // Change color based on status
            int color;
            if ("Running".equals(status)) {
                color = ContextCompat.getColor(context, R.color.status_running);
            } else {
                color = ContextCompat.getColor(context, R.color.status_stopped);
            }
            serverStatusTextView.setTextColor(color);
        });
    }
}
