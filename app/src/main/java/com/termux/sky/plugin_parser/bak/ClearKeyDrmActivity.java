//package com.termux.sky.txplayer;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.ViewGroup;
//
//import androidx.annotation.OptIn;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.media3.common.C;
//import androidx.media3.common.MediaItem;
//import androidx.media3.common.MimeTypes;
//import androidx.media3.common.PlaybackException;
//import androidx.media3.common.util.UnstableApi;
//import androidx.media3.datasource.DefaultHttpDataSource;
//import androidx.media3.exoplayer.ExoPlayer;
//import androidx.media3.exoplayer.analytics.AnalyticsListener;
//import androidx.media3.exoplayer.dash.DashMediaSource;
//import androidx.media3.exoplayer.util.EventLogger;
//import androidx.media3.ui.PlayerView;
//
//@OptIn(markerClass = UnstableApi.class)
//public class ClearKeyDrmActivity extends AppCompatActivity {
//
//    private static final String TAG = "DRM_PLAYER_LOGS";
//    private PlayerView playerView;
//    private ExoPlayer player;
//
//    private static final String DASH_MANIFEST_URL = "https://mdmhrSkaWT09";
//    private static final String CLEARKEY_LICENSE_URL = "https://mi";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        playerView = new PlayerView(this);
//        playerView.setLayoutParams(new ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        ));
//        setContentView(playerView);
//
//        initializePlayer();
//    }
//
//    private void initializePlayer() {
//        Log.d(TAG, "Initializing ExoPlayer...");
//
//        player = new ExoPlayer.Builder(this).build();
//        playerView.setPlayer(player);
//
//        player.addAnalyticsListener(new EventLogger(null, TAG + "_EVENT"));
//
//        player.addAnalyticsListener(new AnalyticsListener() {
//            @Override
//            public void onDrmKeysLoaded(EventTime eventTime) {
//                Log.i(TAG, "SUCCESS: ClearKey DRM Keys successfully fetched and loaded!");
//            }
//
//            @Override
//            public void onDrmSessionManagerError(EventTime eventTime, Exception error) {
//                Log.e(TAG, "FATAL: DRM Session Manager Error!", error);
//            }
//
//            @Override
//            public void onPlayerError(EventTime eventTime, PlaybackException error) {
//                Log.e(TAG, "FATAL: General Player Error!", error);
//            }
//
//            @Override
//            public void onPlaybackStateChanged(EventTime eventTime, int state) {
//                switch (state) {
//                    case ExoPlayer.STATE_BUFFERING:
//                        Log.d(TAG, "State: BUFFERING");
//                        break;
//                    case ExoPlayer.STATE_READY:
//                        Log.d(TAG, "State: READY (Playback can start)");
//                        break;
//                    case ExoPlayer.STATE_ENDED:
//                        Log.d(TAG, "State: ENDED");
//                        break;
//                    case ExoPlayer.STATE_IDLE:
//                        Log.d(TAG, "State: IDLE");
//                        break;
//                }
//            }
//        });
//
//        MediaItem.DrmConfiguration drmConfig = new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
//            .setLicenseUri(CLEARKEY_LICENSE_URL)
//            // Multi-session helps prevent session closure issues on continuous live streams
//            .setMultiSession(true)
//            .build();
//

//        MediaItem mediaItem = new MediaItem.Builder()
//            .setUri(DASH_MANIFEST_URL)
//            .setMimeType(MimeTypes.APPLICATION_MPD)
//            .setDrmConfiguration(drmConfig)
//            .build();
//        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
//        DashMediaSource mediaSource = new DashMediaSource.Factory(dataSourceFactory)
//            .createMediaSource(mediaItem);
//
//        Log.d(TAG, "Preparing Player with DASH Source...");
//
//        player.setMediaSource(mediaSource);
//        player.prepare();
//        player.setPlayWhenReady(true);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (player != null) {
//            player.release();
//            player = null;
//            Log.d(TAG, "Player Released to free up memory/decoders.");
//        }
//    }
//}
