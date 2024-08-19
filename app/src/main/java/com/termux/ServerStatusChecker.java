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

    private static String URL_LINK ;
    private static String urlString ;
    private Handler handler;
    private Runnable runnable;
    private TextView serverStatusTextView;
    private Context context;

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
                handler.postDelayed(this, 7000); // Check every 5 seconds
            }
        };
        handler.post(runnable);
    }

    public void stopChecking() {
        handler.removeCallbacks(runnable);
    }

    private void checkServerStatus() {
        // Initialize SkySharedPref and get the port from preferences
        SkySharedPref preferenceManager = new SkySharedPref(context);
        urlString = preferenceManager.getKey("isLocalPORT");
        new Thread(() -> {
            URL_LINK = urlString;
            try {
                URL url = new URL(URL_LINK);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000); // 3 seconds timeout
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    updateStatus("Running");
                } else {
                    updateStatus("Stopped");
                }
            } catch (IOException e) {
                updateStatus("Stopped");
            }
        }).start();
        Log.d("TAG","Checked Server");
    }

    private void updateStatus(final String status) {
        serverStatusTextView.post(() -> {
            serverStatusTextView.setText(status);

            SkySharedPref preferenceManager = new SkySharedPref(context);
            preferenceManager.setKey("isServerRunning", status);

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
