package com.termux.sky.txplayer.bak;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.util.EventLogger;

@OptIn(markerClass = UnstableApi.class)
public class DrmActivationService extends Service {

    private static final String TAG = "DRM_SERVICE_LOGS";
    private ExoPlayer player;
    private Handler activationHandler;
    private Runnable stopRunnable;
    private boolean isActivated = false;

    @Override
    public void onCreate() {
        super.onCreate();
        activationHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started. Fetching activation configuration dynamically...");

        if (intent != null && intent.getBooleanExtra("toast", false)) {
            Toast.makeText(this, "Activating playlist : 8180", Toast.LENGTH_SHORT).show();
        }

        initializeBackgroundPlayer();

        return START_NOT_STICKY;
    }

    private void initializeBackgroundPlayer() {
        SharedPreferences prefs = getSharedPreferences("port_8180", Context.MODE_PRIVATE);

        String dashManifestUrl = prefs.getString("ch_0_url", "");
        String clearKeyLicenseUrl = prefs.getString("ch_0_lic_key", "");

        if (dashManifestUrl.isEmpty() || clearKeyLicenseUrl.isEmpty()) {
            Log.e(TAG, "Failed to retrieve activation URLs. ch_0_url or ch_0_lic_key is missing in port_8180 preferences.");
            stopSelf();
            return;
        }

        Log.d(TAG, "URLs successfully loaded. Manifest: " + dashManifestUrl);

        player = new ExoPlayer.Builder(this).build();
        player.setVolume(0f);

        player.addAnalyticsListener(new EventLogger(null, TAG + "_EVENT"));
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onDrmKeysLoaded(EventTime eventTime) {
                Log.i(TAG, "ClearKey DRM Keys loaded inside Service.");
            }

            @Override
            public void onDrmSessionManagerError(EventTime eventTime, Exception error) {
                Log.e(TAG, "DRM Error in Service", error);
                stopSelf();
            }

            @Override
            public void onPlayerError(EventTime eventTime, PlaybackException error) {
                Log.e(TAG, "Player Error in Service", error);
                stopSelf();
            }

            @Override
            public void onPlaybackStateChanged(EventTime eventTime, int state) {
                if (state == ExoPlayer.STATE_READY && !isActivated) {
                    isActivated = true;
                    Log.d(TAG, "Playback ready in background. Timing 2 seconds...");

                    stopRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "2 seconds up. Activation complete, stopping service.");
                            stopSelf();
                        }
                    };
                    activationHandler.postDelayed(stopRunnable, 2000);
                }
            }
        });

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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy called. Cleaning up...");

        if (activationHandler != null && stopRunnable != null) {
            activationHandler.removeCallbacks(stopRunnable);
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }
}
