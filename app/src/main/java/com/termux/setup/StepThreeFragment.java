package com.termux.setup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.app.TermuxActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class StepThreeFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String DOWNLOAD_URL = "http://localhost:5001/playlist.m3u";


    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_3, container, false);

        // Initialize UI elements
        Button downloadButton = view.findViewById(R.id.download_button);

        // Set onClick listener for the download button
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadButtonClick(v);
            }
        });

        return view;
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission((getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText((getActivity()), "Storage permission is required. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions((getActivity()), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void downloadFile(String fileUrl, String extraString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                    // Define the file name with the extra string
                    String fileName = "playlist";
                    if (extraString != null && !extraString.isEmpty()) {
                        fileName += "-" + extraString;
                    }
                    fileName += ".m3u";

                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();

                    // Update UI on the main thread
                    if (getActivity() != null) {
                        String finalFileName = fileName;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Playlist downloaded to Downloads folder as " + finalFileName, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Update UI on the main thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Download failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }


    // Method to handle download button click
    public void onDownloadButtonClick(View view) {
        if (checkPermission()) {
            SkySharedPref preferenceManager = new SkySharedPref(getActivity());
            String isLocal = preferenceManager.getKey("server_setup_isLocal");

            if (isLocal != null && !isLocal.isEmpty()) {
                if (isLocal.equals("No")) {
                    downloadFile(DOWNLOAD_URL,"local");
                } else {
                    String wifiIpAddress = getWifiIpAddress();
                    String DOWNLOAD_URL = "http://"+wifiIpAddress+":5001/playlist.m3u";
                    downloadFile(DOWNLOAD_URL,wifiIpAddress);
                }
            } else {
                String wifiIpAddress = getWifiIpAddress();
                String DOWNLOAD_URL = "http://"+wifiIpAddress+":5001/playlist.m3u";
                downloadFile(DOWNLOAD_URL,wifiIpAddress);
            }

        } else {
            requestPermission();
        }
    }

    public String getWifiIpAddress() {
        if (getContext() == null) {
            return "Context is null";
        }

        WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "Wi-Fi Manager is null";
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return "Wi-Fi Info is null";
        }

        int ipAddress = wifiInfo.getIpAddress();
        return Formatter.formatIpAddress(ipAddress);
    }








}
