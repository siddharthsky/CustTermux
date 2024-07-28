package com.termux;

import android.os.Handler;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginStatusChecker {

    private Handler handler;
    private Runnable runnable;
    private TextView loginStatusTextView;

    public LoginStatusChecker(TextView serverStatusTextView) {
        this.handler = new Handler();
        this.loginStatusTextView = serverStatusTextView;
    }

    public void startChecking() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
                handler.postDelayed(this, 5000); // Check every 5 seconds
            }
        };
        handler.post(runnable);
    }

    public void stopChecking() {
        handler.removeCallbacks(runnable);
    }

    private void checkServerStatus() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:5001/live/144.m3u8");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000); // 3 seconds timeout
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == 200) {
                    updateStatus("Running");
                } else if (responseCode == 500) {
                    handleLogout(); // Handle logout on 500 response code
                } else {
                    updateStatus("Error"); // For other response codes
                }
            } catch (IOException e) {
                updateStatus("Error");
            }
        }).start();
    }

    private void updateStatus(final String status) {
        loginStatusTextView.post(() -> loginStatusTextView.setText(status));
    }

    private void handleLogout() {
        // Implement logout logic here
        // For example, you might want to start a new activity or show a dialog
        // Update the status to indicate the logout
        updateStatus("Logged Out");
    }
}
