package com.termux.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.termux.R;
import com.termux.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckForUpdateTask {

    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/siddharthsky/CustTermux-JioTVGo/releases/latest";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private Context context;
    private ExecutorService executorService;
    private String downloadUrl;
    private long downloadFileSize;
    private Button updateButton;

    public CheckForUpdateTask(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void execute() {
        executorService.submit(() -> {
            try {
                String latestVersion = checkForUpdate();
                if (latestVersion != null) {
                    String currentVersion = Utils.getCurrentVersion(context);
                    if (isNewerVersion(currentVersion, latestVersion)) {
                        ((Activity) context).runOnUiThread(() -> {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                                // Request permission
                                ActivityCompat.requestPermissions((Activity) context,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_WRITE_STORAGE);
                            } else {
                                // Permission is already granted
                                File apkFile = new File(getDownloadDirectory(), "update.apk");
                                if (apkFile.exists() && apkFile.length() == downloadFileSize) {
                                    // File exists and has correct size, show the button
                                    showUpdateButton();
                                } else {
                                    // File does not exist or has incorrect size, download the APK
                                    new DownloadAndInstallApkTask(context, downloadUrl, downloadedFile -> {
                                        if (downloadedFile != null && downloadedFile.exists()) {
                                            showUpdateButton();
                                        } else {
                                            Log.e("CheckForUpdateTask", "Failed to download APK file.");
                                        }
                                    }).execute();
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void showUpdateButton() {
        ((Activity) context).runOnUiThread(() -> {
            updateButton = ((Activity) context).findViewById(R.id.button6_5);
            if (updateButton != null) {
                updateButton.setVisibility(View.VISIBLE);
                updateButton.setOnClickListener(v -> {
                    File file = new File(getDownloadDirectory(), "update.apk");
                    if (file.exists()) {
                        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);

                    } else {
                        Log.e("CheckForUpdateTask", "Failed to find APK file for installation.");
                    }
                });
            }
        });
    }


    private String checkForUpdate() throws Exception {
        HttpURLConnection connection = null;
        BufferedInputStream reader = null;

        try {
            URL url = new URL(GITHUB_RELEASES_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            reader = new BufferedInputStream(inputStream);

            StringBuilder response = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(reader));
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            String tagName = jsonResponse.getString("tag_name");

            // Find the APK download URL based on system architecture
            String arch = getSystemArchitecture();
            JSONArray assets = jsonResponse.getJSONArray("assets");

            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.getString("name");

                if (assetName.contains(arch)) {
                    downloadUrl = asset.getString("browser_download_url");
                    downloadFileSize = asset.getLong("size");
                    Log.d("DL_link", downloadUrl);
                    return tagName;
                }
            }

            throw new Exception("No matching APK found for architecture: " + arch);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        // Example comparison logic
        String LTX = "x0.125.3";
        String latestVersionPart = LTX.replaceFirst("^x0\\.", "");
        String currentVersionPart = currentVersion.replaceFirst("^0\\.", "");
        Log.d("DIX1lat", latestVersionPart);
        Log.d("DIX2var", currentVersionPart);
        return latestVersionPart.compareTo(currentVersionPart) > 0;
    }

    private String getSystemArchitecture() {
        String arch = "universal"; // Fallback

        if (Build.SUPPORTED_ABIS.length > 0) {
            String firstAbi = Build.SUPPORTED_ABIS[0];
            if (firstAbi.equals("x86_64")) {
                arch = "x86_64";
            } else if (firstAbi.equals("x86")) {
                arch = "x86";
            } else if (firstAbi.equals("armeabi-v7a")) {
                arch = "armeabi-v7a";
            } else if (firstAbi.equals("arm64-v8a")) {
                arch = "arm64-v8a";
            }
        }
        Log.d("DIX-ARCH", arch);
        return arch;
    }

    private File getDownloadDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API level 30) and above, use the MediaStore API
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API level 29), use getExternalFilesDir
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        } else {
            // For Android 9 (API level 28) and below
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
    }

    private class DownloadAndInstallApkTask {
        private Context context;
        private String fileUrl;
        private DownloadCallback callback;

        public DownloadAndInstallApkTask(Context context, String fileUrl, DownloadCallback callback) {
            this.context = context;
            this.fileUrl = fileUrl;
            this.callback = callback;
        }

        public void execute() {
            ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
            downloadExecutor.submit(() -> {
                File downloadedFile = downloadFile(fileUrl);
                if (callback != null) {
                    callback.onDownloadComplete(downloadedFile);
                }
            });
        }

        private File downloadFile(String fileUrl) {
            HttpURLConnection connection = null;
            BufferedInputStream reader = null;
            File file = null;

            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int fileLength = connection.getContentLength();
                Log.d("DownloadProgress", "File size: " + fileLength + " bytes");

                File downloadDir = getDownloadDirectory();
                file = new File(downloadDir, "update.apk");

                InputStream inputStream = connection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream);

                byte[] buffer = new byte[8192];
                int len;
                long totalBytesRead = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                    totalBytesRead += len;

                    // Log progress
                    int progress = (int) ((totalBytesRead * 100) / fileLength);
                    Log.d("DownloadProgress", "Download progress: " + progress + "%");
                }

                outputStream.close();
                inputStream.close();

                Log.d("DownloadProgress", "Download complete");

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DownloadProgress", "Error downloading file: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return file;
        }
    }

    private interface DownloadCallback {
        void onDownloadComplete(File file);
    }
}
