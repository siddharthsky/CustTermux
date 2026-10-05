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

public class ChannelDataManager {

    private static final String TAG = "ChannelDataManager";
    private static final String PREF_NAME = "channel_cache_prefs";
    private static final String KEY_LAST_UPDATE = "last_channels_fetch_time";
    private static final String CACHE_FILE_NAME = "channels_cache.json";
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000; 

    public static void syncChannelsIfNeeded(Context context, String pluginPort) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();

        File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);

        
        if (!cacheFile.exists() || (currentTime - lastUpdate) > ONE_DAY_MILLIS) {
            Log.d(TAG, "Cache expired or missing. Fetching fresh /channels...");
            downloadAndSaveChannels(context, pluginPort);
        } else {
            Log.d(TAG, "Channels cache is fresh (less than 24 hours old). Skipping fetch.");
        }
    }

    private static void downloadAndSaveChannels(Context context, String pluginPort) {
        try {
            String urlString = "http://localhost:" + pluginPort + "/channels";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
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

                Log.d(TAG, "Channels JSON successfully cached locally.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download /channels: ", e);
        }
    }

    public static boolean isCatchupAvailable(Context context, String channelId) {
        File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
        if (!cacheFile.exists()) {
            return false;
        }

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
                if (item.has("channel_id") && item.getString("channel_id").equals(channelId)) {
                    return item.optBoolean("isCatchupAvailable", false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cached channels: ", e);
        }

        return false;
    }
}
