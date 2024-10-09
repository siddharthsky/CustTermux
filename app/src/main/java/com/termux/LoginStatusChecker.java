package com.termux;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginStatusChecker {

    private String urlLink;
    private String urlString;
    private String urlChannel;
    private final Handler handler;
    private Runnable runnable;
    private final TextView loginStatusTextView;
    private final Context context;

    public LoginStatusChecker(Context context, TextView loginStatusTextView) {
        this.context = context;
        this.loginStatusTextView = loginStatusTextView;
        this.handler = new Handler();
    }

    public void startChecking() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
                handler.postDelayed(this, 10000); // Check every 10 seconds
            }
        };
        handler.post(runnable);
    }

    public void stopChecking() {
        handler.removeCallbacks(runnable);
    }

    private void checkServerStatus() {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        urlString = preferenceManager.getKey("isLocalPORT");
        urlChannel = preferenceManager.getKey("isLocalPORTchannel");

        if (urlString != null && urlChannel != null) {
            urlLink = urlString + urlChannel;

            new Thread(() -> {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) new URL(urlLink).openConnection();
                    urlConnection.setConnectTimeout(3000); // 3 seconds timeout
                    urlConnection.connect();

                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == 200) {
                        updateStatus("Logged In");
                    } else if (responseCode == 500) {
                        handleLogout(); // Handle logout on 500 response code
                    } else {
                        updateStatus("Error"); // For other response codes
                    }
                } catch (IOException e) {
                    updateStatus("Error");
                }
            }).start();

            Log.d("LoginStatusChecker", "Checked Login Status");
        } else {
            updateStatus("Error: Invalid URL");
            Log.e("LoginStatusChecker", "Error: Invalid URL or Channel");
        }
    }

    private void updateStatus(final String status) {
        loginStatusTextView.post(() -> {
            loginStatusTextView.setText(status);
            int color;
            switch (status) {
                case "Logged In":
                    color = ContextCompat.getColor(context, R.color.status_running);
                    break;
                case "Error":
                case "Error: Invalid URL":
                    color = ContextCompat.getColor(context, R.color.status_error);
                    break;
                case "Logged Out":
                    color = ContextCompat.getColor(context, R.color.status_stopped);
                    break;
                default:
                    color = ContextCompat.getColor(context, android.R.color.black);
                    break;
            }
            loginStatusTextView.setTextColor(color);
        });
    }

    private void handleLogout() {
        updateStatus("Logged Out");
    }
}
