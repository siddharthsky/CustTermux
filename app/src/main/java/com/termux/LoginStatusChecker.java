package com.termux;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import androidx.core.content.ContextCompat;

public class LoginStatusChecker {

    private static String URL_LINK ;
    private static String urlString;
    private static String urlchannel;
    private Handler handler;
    private Runnable runnable;
    private TextView loginStatusTextView;
    private Context context;

    public LoginStatusChecker(Context context, TextView serverStatusTextView) {
        this.handler = new Handler();
        this.loginStatusTextView = serverStatusTextView;
        this.context = context;
    }

    public void startChecking() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
                handler.postDelayed(this, 10000); // Check every 5 seconds
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
        urlchannel = preferenceManager.getKey("isLocalPORTchannel");


        new Thread(() -> {
            URL_LINK = urlString+urlchannel;
            try {
                URL url = new URL(URL_LINK);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
        Log.d("TAG","Checked Login");
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
        // Update the status to indicate the logout
        updateStatus("Logged Out");
    }
}
