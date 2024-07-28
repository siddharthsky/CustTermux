package com.termux;

import android.os.Handler;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerStatusChecker {

    private Handler handler;
    private Runnable runnable;
    private TextView serverStatusTextView;

    public ServerStatusChecker(TextView serverStatusTextView) {
        this.handler = new Handler();
        this.serverStatusTextView = serverStatusTextView;
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
                URL url = new URL("http://localhost:5001");
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
    }

    private void updateStatus(final String status) {
        serverStatusTextView.post(() -> serverStatusTextView.setText(status));
    }
}
