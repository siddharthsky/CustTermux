package com.termux.sky.hanaplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ChannelDataManager {

    private static final String TAG = "ChannelDataManager";
    private static final String PREF_NAME = "channel_cache_prefs";
    private static final String KEY_LAST_UPDATE = "last_channels_fetch_time";
    private static final String CACHE_FILE_NAME = "channels_cache.json";
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    private static HashMap<String, Boolean> catchupCache = null;

    public static void syncChannelsIfNeeded(Context context, String pluginPort) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();

        File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);

        if (!cacheFile.exists() || (currentTime - lastUpdate) > ONE_DAY_MILLIS) {
            Log.d(TAG, "Cache expired or missing. Fetching fresh /channels...");
            downloadAndSaveChannels(context, pluginPort);
        } else {
            Log.d(TAG, "Channels cache is fresh.");
        }
    }

//    private static void downloadAndSaveChannels(Context context, String pluginPort) {
//        try {
//            String urlString = "http://localhost:" + pluginPort + "/channels";
//            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
//            conn.setRequestMethod("GET");
//            conn.setConnectTimeout(6000);
//            conn.setReadTimeout(6000);
//
//            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//                reader.close();
//
//                FileOutputStream fos = context.openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE);
//                fos.write(sb.toString().getBytes());
//                fos.close();
//
//                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//                    .edit()
//                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
//                    .apply();
//
//                catchupCache = null;
//                Log.d(TAG, "Channels JSON successfully cached locally.");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to download /channels: ", e);
//        }
//    }

    private static void downloadAndSaveChannels(Context context, String pluginPort) {
        HttpURLConnection conn = null;
        try {
            String urlString = "https://localhost:" + pluginPort + "/channels";
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            if (conn instanceof HttpsURLConnection) {
                bypassSslForLocalhost((HttpsURLConnection) conn);
            }

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                FileOutputStream fos = context.openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE);
                fos.write(sb.toString().getBytes());
                fos.close();

                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply();

                catchupCache = null;
                Log.d(TAG, "Channels JSON successfully cached locally.");
            } else {
                Log.e(TAG, "Failed to download /channels. HTTP Code: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download /channels: ", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void bypassSslForLocalhost(HttpsURLConnection httpsConn) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());

        httpsConn.setSSLSocketFactory(sc.getSocketFactory());
        httpsConn.setHostnameVerifier((hostname, session) ->
            "localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname)
        );
    }

    public static boolean isCatchupAvailable(Context context, String channelId) {
        if (catchupCache == null) {
            catchupCache = new HashMap<>();
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
            if (cacheFile.exists()) {
                try {
                    FileInputStream fis = context.openFileInput(CACHE_FILE_NAME);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject rootObject = new JSONObject(sb.toString());
                    JSONArray channelsArray = rootObject.getJSONArray("result");

                    for (int i = 0; i < channelsArray.length(); i++) {
                        JSONObject item = channelsArray.getJSONObject(i);
                        if (item.has("channel_id")) {
                            catchupCache.put(item.getString("channel_id"), item.optBoolean("isCatchupAvailable", false));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading cached channels: ", e);
                }
            }
        }
        return Boolean.TRUE.equals(catchupCache.get(channelId));
    }
}
