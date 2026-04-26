package com.termux.sky.txplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.termux.sky.playlistmanager.PlaylistManager;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class ExoPlayerActivityDRM extends ComponentActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    String port_no;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        playerView = new PlayerView(this);
        setContentView(playerView);
        hideSystemUI();

        port_no = getIntent().getStringExtra("plugin_port");

        String rawUrl = getIntent().getStringExtra("url");
        String licenseUrl = getIntent().getStringExtra("license_key");
        String intentUserAgent = getIntent().getStringExtra("user_agent");

        if (rawUrl != null) {
            String v_url = rawUrl;
            String origin = null;
            String referer = null;
            String userAgent = intentUserAgent;

            try {
                v_url = URLDecoder.decode(v_url, "UTF-8");

                if (v_url.contains("|")) {
                    String[] parts = v_url.split("\\|");
                    v_url = parts[0].trim();
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i].trim();
                        String lowerPart = part.toLowerCase();
                        if (lowerPart.startsWith("origin=")) origin = part.substring(7);
                        else if (lowerPart.startsWith("referer=")) referer = part.substring(8);
                        else if (lowerPart.startsWith("user-agent=")) userAgent = part.substring(11);
                    }
                }

                v_url = v_url.replaceAll("&$", "");

            } catch (Exception e) {
                Log.e("DRM_PLAYER", "URL Decoding failed", e);
            }

            checkStatusAndPlay(v_url, licenseUrl, userAgent, origin, referer);

            
        }
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), playerView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void checkStatusAndPlay(String videoUrl, String licenseUrl, String userAgent, String origin, String referer) {
        new Thread(() -> {
            try {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(videoUrl).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // --- HEADERS---
                if (userAgent != null && !userAgent.isEmpty()) {
                    connection.setRequestProperty("User-Agent", userAgent);
                } else {
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                }
                if (origin != null && !origin.isEmpty()) {
                    connection.setRequestProperty("Origin", origin);
                }
                if (referer != null && !referer.isEmpty()) {
                    connection.setRequestProperty("Referer", referer);
                }
                // -----------------------

                int responseCode = connection.getResponseCode();
                Log.d("DRM_PLAYER", "Curl Status Code: " + responseCode);

                if ((responseCode >= 200 && responseCode < 400) && !port_no.equals("5007")) {
                    runOnUiThread(() -> initializePlayer(videoUrl, licenseUrl, userAgent, origin, referer));
                } else {

                    Log.d("DRM_DEBUG", "Main Intent Port: " + port_no);

                    Intent intent = new Intent(this, WebViewPlayerActivity.class);
                    intent.putExtra("url", videoUrl);
                    intent.putExtra("port", port_no);

                    // These flags clear the previous activities from the stack
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                    this.startActivity(intent);
                    this.finish();

                    Log.d("DRM_PLAYER", "Server rejected the curl check. Code: " + responseCode);
                    Toast.makeText(this, "Stream unavailable", Toast.LENGTH_SHORT).show();




                }

            } catch (Exception e) {
                Log.e("DRM_PLAYER", "Network error during curl check", e);
            }
        }).start();
    }

    private void initializePlayer(String videoUrl, String licenseUrl, String userAgent, String origin, String referer) {

        Log.d("DRM_PLAYER", "videoUrl=" + videoUrl +
            ", licenseUrl=" + licenseUrl +
            ", userAgent=" + userAgent +
            ", origin=" + origin +
            ", referer=" + referer);

        Map<String, String> headers = new HashMap<>();
        if (userAgent != null && !userAgent.isEmpty()) headers.put("User-Agent", userAgent);
        if (origin != null && !origin.isEmpty()) headers.put("Origin", origin);
        if (referer != null && !referer.isEmpty()) headers.put("Referer", referer);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers);

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(Uri.parse(videoUrl));

        MediaSource mediaSource;
        if (videoUrl.toLowerCase().contains(".m3u8") || videoUrl.contains("index.php") || videoUrl.contains("stream.php")) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItemBuilder.build());
        } else {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD);
            if (licenseUrl != null && !licenseUrl.isEmpty()) {
                mediaItemBuilder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(licenseUrl)
                    .setLicenseRequestHeaders(headers)
                    .build());
            }
            mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItemBuilder.build());
        }

        DefaultRenderersFactory renderersFactory =
            new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);


        player = new ExoPlayer.Builder(this, renderersFactory).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                // Log the error for debugging
                Log.e("DRM_DEBUG", "ExoPlayer failed to play: " + error.getMessage() + ". Rerouting to WebPlayer.");
                Toast.makeText(ExoPlayerActivityDRM.this, "Stream error, trying Web Player...", Toast.LENGTH_SHORT).show();

                Toast.makeText(ExoPlayerActivityDRM.this, "Retrying on Web Player", Toast.LENGTH_LONG).show();

                // Instead of only checking for 403, we switch to WebView for ANY playback failure
                switchToWebView(videoUrl);
            }
        });

//        player.addListener(new Player.Listener() {
//            @Override
//            public void onPlayerError(@NonNull PlaybackException error) {
//                Throwable cause = error.getCause();
//
//                if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
//                    HttpDataSource.InvalidResponseCodeException httpError =
//                        (HttpDataSource.InvalidResponseCodeException) cause;
//
//                    int responseCode = httpError.responseCode;
//                    Log.e("DRM_DEBUG", "HTTP Error Detected! Code: " + responseCode);
//
//                    if (responseCode == 403) {
//                        Log.e("DRM_DEBUG", "Access Denied (403). Check Headers or Token.");
//                        Toast.makeText(ExoPlayerActivityDRM.this, "Server Access Denied (403)", Toast.LENGTH_LONG).show();
//
//                        switchToWebView(videoUrl);
//                    }
//                } else {
//                    Log.e("DRM_DEBUG", "General Player Error: " + error.getMessage());
//                }
//            }
//        });

        playerView.setPlayer(player);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void switchToWebView(String url) {
        Intent intent = new Intent(this, WebViewPlayerActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("port", port_no);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                changeChannel(1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                changeChannel(-1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void changeChannel(int direction) {
        if (PlaylistManager.currentList == null || PlaylistManager.currentList.isEmpty()) return;

        // Calculate next index with wrap-around
        int newIndex = PlaylistManager.currentIndex + direction;
        if (newIndex < 0) newIndex = PlaylistManager.currentList.size() - 1;
        if (newIndex >= PlaylistManager.currentList.size()) newIndex = 0;

        PlaylistManager.currentIndex = newIndex;
        ChannelModel nextChannel = PlaylistManager.currentList.get(newIndex);

        // Update port_no globally
        port_no = nextChannel.originPort != null ? nextChannel.originPort : (nextChannel.url.contains("5007") ? "5007" : "0");

//        Toast.makeText(this, "Playing: " + nextChannel.name, Toast.LENGTH_SHORT).show();

        // Stop current playback
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        // Decode new URL and start playback
        String vUrl = nextChannel.url;
        String origin = null;
        String referer = null;
        String userAgent = nextChannel.userAgent;

        try {
            vUrl = java.net.URLDecoder.decode(vUrl, "UTF-8");
            if (vUrl.contains("|")) {
                String[] parts = vUrl.split("\\|");
                vUrl = parts[0].trim();
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i].trim();
                    String lowerPart = part.toLowerCase();
                    if (lowerPart.startsWith("origin=")) origin = part.substring(7);
                    else if (lowerPart.startsWith("referer=")) referer = part.substring(8);
                    else if (lowerPart.startsWith("user-agent=")) userAgent = part.substring(11);
                }
            }
            vUrl = vUrl.replaceAll("&$", "");
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "URL Decoding failed", e);
        }

        checkStatusAndPlay(vUrl, nextChannel.licenseKey, userAgent, origin, referer);
    }

}
