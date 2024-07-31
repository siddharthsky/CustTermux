package com.termux;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TermuxActivityResume {

    private Context context;
    private int openIptvCount = 0;
    private final int maxOpenIptvCalls = 10;
    private final Handler taskHandler = new Handler(Looper.getMainLooper());
    private Runnable iptvCheckTask;
    private AlertDialog iptvAlertDialog;
    private CountDownTimer countdownTimer;

    public TermuxActivityResume(Context context) {
        this.context = context;
    }

    public void onResume() {
        if (openIptvCount < maxOpenIptvCalls) {
            scheduleIptvCheckTask(2000);
        }
    }

    public void onPause() {
        // Cancel any ongoing tasks and alert dialogs
        taskHandler.removeCallbacks(iptvCheckTask);
        if (iptvAlertDialog != null && iptvAlertDialog.isShowing()) {
            iptvAlertDialog.dismiss();
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    private void scheduleIptvCheckTask(final int delayMillis) {
        iptvCheckTask = new Runnable() {
            @Override
            public void run() {
                if (openIptvCount < maxOpenIptvCalls) {
                    String url = "http://localhost:5001/live/144.m3u8";
                    new CheckStatusTask().execute(url);
                }
            }
        };
        taskHandler.postDelayed(iptvCheckTask, delayMillis);
    }

    private class CheckStatusTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                return connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode != null) {
                handleResponse(responseCode);
            } else {
                System.out.println("TermuxActivity: Error occurred while checking status code.");
                Toast.makeText(context, "Checking Server Status", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleResponse(int responseCode) {
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                System.out.println("TermuxActivity: The webpage is accessible.");
                checkIptvStatus();
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                System.out.println("TermuxActivity: The webpage was not found.");
                break;
            default:
                System.out.println("Response code: " + responseCode);
                if (responseCode == 500) {
                    System.out.println("TermuxActivity: The webpage was not found. 500");
                }
                break;
        }
    }

    private void checkIptvStatus() {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        String iptvChecker = preferenceManager.getKey("app_name");

        if (iptvChecker != null && !iptvChecker.isEmpty()) {
            if (iptvChecker.equals("null")) {
                System.out.println("IPTV, null!");
            } else if (iptvChecker.equals("sky_web_tv")) {
                System.out.println("IPTV, webTV!");
            } else {
                System.out.println("IPTV, found!");
                showIptvAlertDialog();
                openIptvCount++;
            }
        } else {
            System.out.println("IPTV, null!");
        }
    }

    private synchronized void showIptvAlertDialog() {
        if (iptvAlertDialog != null && iptvAlertDialog.isShowing()) {
            return; // Another alert is already showing, so do nothing.
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.iptv_alert, null);

        SkySharedPref preferenceManager = new SkySharedPref(context);
        String iconBase64 = preferenceManager.getKey("app_icon");

        ImageView iconImageView = dialogView.findViewById(R.id.iptv_icon);
        if (iconBase64 != null) {
            Bitmap iconBitmap = base64ToBitmap(iconBase64);
            iconImageView.setImageBitmap(iconBitmap);
        } else {
            iconImageView.setImageResource(R.mipmap.ic_launcher2);
        }

        String packageName = preferenceManager.getKey("app_name");
        String appName = getAppNameFromPackageName(packageName);

        TextView iptvNameTextView = dialogView.findViewById(R.id.iptv_name);
        iptvNameTextView.setText("Opening " + appName);

        TextView countdownTextView = dialogView.findViewById(R.id.countdown_timer);
        final int countdownDuration = 6000; // 5 seconds
        countdownTextView.setText((countdownDuration / 1000) + "s");

        iptvAlertDialog = builder.create();
        iptvAlertDialog.setView(dialogView);

        Button dismissButton = dialogView.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(v -> iptvAlertDialog.dismiss());

        iptvAlertDialog.show();
        dismissButton.post(() -> dismissButton.requestFocus());

        countdownTimer = new CountDownTimer(countdownDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                if (iptvAlertDialog != null && iptvAlertDialog.isShowing()) {
                    iptvAlertDialog.dismiss();
                    onDialogTimeout();
                }
            }
        }.start();
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private String getAppNameFromPackageName(String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName;
        }
    }

    private void onDialogTimeout() {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        String appPkg = preferenceManager.getKey("app_name");
        String appClass = preferenceManager.getKey("app_launchactivity");

        if (appPkg != null && !appPkg.isEmpty()) {
            if (appPkg.equals("null")) {
                System.out.println("IPTV, null!");
            } else if (appPkg.equals("sky_web_tv")) {
                System.out.println("IPTV, webTV!");
            } else {
                System.out.println("IPTV, found!");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(appPkg, appClass));
                context.startActivity(intent);
                openIptvCount++;
            }
        } else {
            System.out.println("IPTV, null!");
        }
    }
}
