package com.termux.sky.play_master;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.util.EventLogger;

import com.termux.sky.plugin_parser.ChannelModel;
import com.termux.sky.plugin_parser.M3UParser;
import com.termux.sky.plugins_utils.Plugin;
import com.termux.sky.plugins_utils.PluginStorage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

@OptIn(markerClass = UnstableApi.class)
public class DrmActivationService extends Service {

    private static final String TAG = "DRM_SERVICE_LOGS";
    private ExoPlayer player;
    private Handler mainHandler;
    private boolean isRunning = false;
    private int targetPort = 8180;
    private String targetPlaylistUrl = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started. Sequence initiated.");

        if (intent != null) {
            targetPort = intent.getIntExtra("port", 8180);

            if (intent.getBooleanExtra("toast", false)) {
                Toast.makeText(this, "Activating playlist : " + targetPort, Toast.LENGTH_SHORT).show();
            }
        }

        List<Plugin> plugins = PluginStorage.load(this);
        for (Plugin plugin : plugins) {
            if (plugin.port == targetPort) {
                targetPlaylistUrl = plugin.playlist;
                break;
            }
        }

        if (targetPlaylistUrl == null || targetPlaylistUrl.isEmpty()) {
            Log.e(TAG, "Plugin with port " + targetPort + " not found or has no playlist URL. Stopping.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (isRunning) {
            Log.d(TAG, "Sequence is already running. Ignoring duplicate start.");
            return START_NOT_STICKY;
        }

        if (!isNetworkAvailable()) {
            Log.e(TAG, "No internet connection available. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        isRunning = true;
        Log.d(TAG, "Internet is good. Starting Phase 1: Activate existing channel.");
        phase1ActivateExisting();

        return START_NOT_STICKY;
    }

    
    private void phase1ActivateExisting() {
        SharedPreferences prefs = getSharedPreferences("port_" + targetPort, Context.MODE_PRIVATE);
        String dashManifestUrl = prefs.getString("ch_0_url", "");
        String clearKeyLicenseUrl = prefs.getString("ch_0_lic_key", "");

        if (dashManifestUrl.isEmpty() || clearKeyLicenseUrl.isEmpty()) {
            Log.e(TAG, "Phase 1: Existing URLs missing. Moving to Phase 2 directly.");
            phase2FetchNewData();
        } else {
            Log.d(TAG, "Phase 1: Found existing URLs. Activating...");
            startPlaybackSequence(dashManifestUrl, clearKeyLicenseUrl, this::phase2FetchNewData);
        }
    }


    private void phase2FetchNewData() {
        Log.d(TAG, "Phase 2: Fetching new playlist data in background...");
        Executors.newSingleThreadExecutor().execute(() -> {

            String playlistUrl = targetPlaylistUrl;
            String portStr = String.valueOf(targetPort);

            String content = downloadUrl(playlistUrl);

            mainHandler.post(() -> {
                if (content != null && !content.isEmpty()) {
                    List<ChannelModel> channels = M3UParser.parse(content);
                    if (!channels.isEmpty()) {
                        M3UParser.saveToPrefs(DrmActivationService.this, portStr, channels);
                        Log.d(TAG, "Phase 2: Refresh successful! Data saved. Moving to Phase 3.");
                        phase3ActivateNewData();
                        return;
                    }
                }

                Log.e(TAG, "Phase 2: Failed to fetch or parse playlist. Stopping.");
                stopSelf();
            });
        });
    }

    
    private void phase3ActivateNewData() {
        SharedPreferences prefs = getSharedPreferences("port_" + targetPort, Context.MODE_PRIVATE);
        String dashManifestUrl = prefs.getString("ch_0_url", "");
        String clearKeyLicenseUrl = prefs.getString("ch_0_lic_key", "");

        if (dashManifestUrl.isEmpty() || clearKeyLicenseUrl.isEmpty()) {
            Log.e(TAG, "Phase 3: New URLs missing. Stopping service.");
            stopSelf();
        } else {
            Log.d(TAG, "Phase 3: Found new URLs. Activating...");
            
            startPlaybackSequence(dashManifestUrl, clearKeyLicenseUrl, this::stopSelf);
        }
    }

    
    private void startPlaybackSequence(String dashManifestUrl, String clearKeyLicenseUrl, Runnable onComplete) {
        releasePlayer(); 

        player = new ExoPlayer.Builder(this).build();
        player.setVolume(0f); 
        player.addAnalyticsListener(new EventLogger(null, TAG + "_EVENT"));

        
        Runnable completeAction = new Runnable() {
            boolean isCalled = false;
            @Override
            public void run() {
                if (!isCalled) {
                    isCalled = true;
                    releasePlayer();
                    onComplete.run();
                }
            }
        };

        
        
        Runnable failsafeRunnable = () -> {
            Log.w(TAG, "Playback timed out before reaching READY state. Aborting this phase.");
            completeAction.run();
        };
        mainHandler.postDelayed(failsafeRunnable, 15000);

        player.addListener(new Player.Listener() {
            boolean hasStarted15SecTimer = false;

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && !hasStarted15SecTimer) {
                    hasStarted15SecTimer = true;
                    Log.d(TAG, "Playback is READY. Keeping alive for exactly 15 seconds...");

                    
                    mainHandler.removeCallbacks(failsafeRunnable);

                    
                    mainHandler.postDelayed(() -> {
                        Log.d(TAG, "15 seconds completed. Ending playback phase.");
                        completeAction.run();
                    }, 15000);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player Error during sequence", error);
                mainHandler.removeCallbacks(failsafeRunnable);
                completeAction.run();
            }
        });

        try {
            MediaItem.DrmConfiguration drmConfig = new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                .setLicenseUri(clearKeyLicenseUrl)
                .setMultiSession(true)
                .build();

            MediaItem mediaItem = new MediaItem.Builder()
                .setUri(dashManifestUrl)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .setDrmConfiguration(drmConfig)
                .build();

            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            DashMediaSource mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);

            player.setMediaSource(mediaSource);
            player.prepare();
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            Log.e(TAG, "Exception initializing ExoPlayer", e);
            mainHandler.removeCallbacks(failsafeRunnable);
            completeAction.run();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private String downloadUrl(String urlString) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                reader.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading playlist: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result.toString();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy called. Cleaning up all resources...");
        isRunning = false;

        
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        releasePlayer();
    }
}
