package com.termux.sky.txplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.termux.R;
import com.termux.sky.playlistmanager.PlaylistManager;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class ExoPlayerActivityDRM extends ComponentActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private ChannelBannerManager bannerManager;
    private DefaultTrackSelector trackSelector;

    private android.os.Handler zapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable zapRunnable;
    private static final long ZAP_DELAY_MS = 400;

    private SharedPreferences prefs;

    String port_no;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        } catch (Exception e) {
            Log.d("DRM_PLAYER", "SystemUI flags error");
        }

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        playerView = new PlayerView(this);
        playerView.setKeepScreenOn(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        root.addView(playerView);

        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        ImageButton playBtn = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_play);
        ImageButton pauseBtn = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_pause);

        if (playBtn != null) {
            playBtn.setImageResource(R.drawable.tx_play_exo);
            playBtn.setBackgroundResource(R.drawable.img_btn_selector);
            playBtn.setColorFilter(android.graphics.Color.WHITE);
        }

        if (pauseBtn != null) {
            pauseBtn.setImageResource(R.drawable.tx_pause_exo);
            pauseBtn.setBackgroundResource(R.drawable.img_btn_selector);
            pauseBtn.setColorFilter(android.graphics.Color.WHITE);
        }


        View nextButton = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_next);
        if (nextButton != null && nextButton.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup controlGroup = (android.view.ViewGroup) nextButton.getParent();
            int nextButtonIndex = controlGroup.indexOfChild(nextButton);

            ImageButton btnResize = createCustomControl(nextButton, R.drawable.tx_resize, v ->
                showScreenScaleMenu());

            ImageButton btnAudio = createCustomControl(nextButton, R.drawable.tx_audio, v ->
                showTrackSelector(C.TRACK_TYPE_AUDIO, "Select Audio Track"));

            ImageButton btnVideo = createCustomControl(nextButton, R.drawable.tx_videohd, v ->
                showTrackSelector(C.TRACK_TYPE_VIDEO, "Select Video Quality"));

            ImageButton btnCC = createCustomControl(nextButton, R.drawable.tx_closed_caption, v ->
                showTrackSelector(C.TRACK_TYPE_TEXT, "Select Subtitles / CC"));


            controlGroup.addView(btnResize, nextButtonIndex + 1);
            controlGroup.addView(btnAudio, nextButtonIndex + 2);
            controlGroup.addView(btnVideo, nextButtonIndex + 3);
            controlGroup.addView(btnCC, nextButtonIndex + 4);
        }


        int savedScale = prefs.getInt("global_screen_scale", AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setResizeMode(savedScale);

        playerView.setOnLongClickListener(v -> {
            showSettingsMenu();
            return true;
        });

        setContentView(root);

        bannerManager = new ChannelBannerManager(root);

        // Check intent for "show_banner" flag
        boolean shouldShow = getIntent().getBooleanExtra("show_banner", true);
        String name = getIntent().getStringExtra("name");
        String logo_url = getIntent().getStringExtra("logo_url");

        if (shouldShow && name != null) {
            bannerManager.show(name, logo_url);
        }

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

    @NonNull
    private ImageButton createCustomControl(View template, int iconResId, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);

        button.setImageResource(iconResId);
        button.setBackgroundResource(R.drawable.img_btn_selector);
        button.setColorFilter(android.graphics.Color.WHITE);
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);

        button.setLayoutParams(template.getLayoutParams());
        button.setPadding(
            template.getPaddingLeft(),
            template.getPaddingTop(),
            template.getPaddingRight(),
            template.getPaddingBottom()
        );

        button.setOnClickListener(listener);
        button.setFocusable(true);

        return button;
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

                String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
                connection.setRequestProperty("User-Agent", ua);

                if (origin != null && !origin.isEmpty()) connection.setRequestProperty("Origin", origin);
                if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);

                int responseCode = connection.getResponseCode();
                Log.d("DRM_PLAYER", "Response Code: " + responseCode);

                // Logic Check
                boolean isUrlValid = (responseCode >= 200 && responseCode < 400);
                boolean isSpecialPort = "5007".equals(port_no);

                if (isUrlValid && !isSpecialPort) {
                    runOnUiThread(() -> initializePlayer(videoUrl, licenseUrl, ua, origin, referer));
                } else {
                    runOnUiThread(() -> {
                        if (!isUrlValid) {
                            Toast.makeText(this, "Stream connection issues, trying WebView...", Toast.LENGTH_SHORT).show();
                        }
                        switchToWebView(videoUrl);
                    });
                }

            } catch (Exception e) {
                Log.e("DRM_PLAYER", "Network error during check", e);
                runOnUiThread(() -> switchToWebView(videoUrl));
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
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true);

        trackSelector = new DefaultTrackSelector(this);
        applySavedTrackPreferences();

        player = new ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .build();

//        player.addListener(new Player.Listener() {
//            @Override
//            public void onPlayerError(@NonNull PlaybackException error) {
//                // Log the error for debugging
//                Log.e("DRM_DEBUG", "ExoPlayer failed to play: " + error.getMessage() + ". Rerouting to WebPlayer.");
//                Toast.makeText(ExoPlayerActivityDRM.this, "Stream error, trying Web Player...", Toast.LENGTH_SHORT).show();
//
//                Toast.makeText(ExoPlayerActivityDRM.this, "Retrying on Web Player", Toast.LENGTH_LONG).show();
//
//                // Instead of only checking for 403, we switch to WebView for ANY playback failure
//                switchToWebView(videoUrl);
//            }
//        });

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
        killExoPlayer();
    }

    @Override
    protected void onDestroy() {
        killExoPlayer();
        super.onDestroy();
    }

    private void killExoPlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {

        // --- NEW: Open settings on Menu button or 'S' key ---
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_S) {
            showSettingsMenu();
            return true;
        }

//        // --- NEW: Long Press DPAD Center (OK) or Enter ---
//        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
//            // A repeat count of 1 means the button has been held down.
//            // A repeat count of 0 is a normal, quick press.
//            if (event.getRepeatCount() == 1) {
//                showSettingsMenu();
//                return true;
//            }
//        }

        if (playerView != null && !playerView.isControllerVisible()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

                playerView.showController();

                return true;
            }
        }


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

        int newIndex = PlaylistManager.currentIndex + direction;
        if (newIndex < 0) newIndex = PlaylistManager.currentList.size() - 1;
        if (newIndex >= PlaylistManager.currentList.size()) newIndex = 0;

        PlaylistManager.currentIndex = newIndex;
        ChannelModel nextChannel = PlaylistManager.currentList.get(newIndex);

        bannerManager.show(nextChannel.name, nextChannel.logo);
        port_no = nextChannel.originPort != null ? nextChannel.originPort : (nextChannel.url.contains("5007") ? "5007" : "0");

        if (player != null) {
            player.stop();
        }

        if (zapRunnable != null) {
            zapHandler.removeCallbacks(zapRunnable);
        }

        zapRunnable = () -> {
            if (player != null) {
                player.release();
                player = null;
            }
            processAndPlayChannel(nextChannel);
        };

        zapHandler.postDelayed(zapRunnable, ZAP_DELAY_MS);
    }

    private void processAndPlayChannel(ChannelModel nextChannel) {
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

    private void showSettingsMenu() {
        if (player == null) return;

        String[] options = {"⚙️ Video Quality", "🎵 Audio Track", "📺 Screen Scale"};

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Player Settings")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showTrackSelector(C.TRACK_TYPE_VIDEO, "Select Video Quality");
                        break;
                    case 1:
                        showTrackSelector(C.TRACK_TYPE_AUDIO, "Select Audio Track");
                        break;
                    case 2:
                        showScreenScaleMenu();
                        break;
                }
            })
            .show();
    }

    private void showTrackSelector(int trackType, String title) {
        if (player == null || trackSelector == null) return;

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
            trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null) return;

        boolean hasTracksOfType = false;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == trackType) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                if (trackGroups.length > 0) {
                    hasTracksOfType = true;
                    break;
                }
            }
        }

        if (!hasTracksOfType) {
            String typeName = (trackType == C.TRACK_TYPE_TEXT) ? "Subtitles" : "Options";
            Toast.makeText(this, "No " + typeName + " available", Toast.LENGTH_SHORT).show();
            return;
        }

        TrackSelectionDialogBuilder builder = new TrackSelectionDialogBuilder(
            this, title, trackSelector, trackType
        );
        builder.setTheme(android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setShowDisableOption(false); //for none option


        // After the dialog updates the trackSelector, we save the preference
        Dialog dialog = builder.build();
        dialog.setOnDismissListener(d -> {
            if (trackType == C.TRACK_TYPE_AUDIO) {
                // Get the currently playing format after the user made a choice
                com.google.android.exoplayer2.Format currentFormat = player.getAudioFormat();
                if (currentFormat != null && currentFormat.language != null) {
                    prefs.edit().putString("pref_audio_lang", currentFormat.language).apply();
                }
            }
        });

        dialog.show();
    }

    private void applySavedTrackPreferences() {
        if (trackSelector == null) return;

        String savedAudioLang = prefs.getString("pref_audio_lang", null);

        if (savedAudioLang != null) {
            DefaultTrackSelector.Parameters currentParameters = trackSelector.getParameters();
            DefaultTrackSelector.Parameters newParameters = currentParameters
                .buildUpon()
                .setPreferredAudioLanguage(savedAudioLang)
                .build();

            trackSelector.setParameters(newParameters);
        }
    }



    private void showScreenScaleMenu() {
        String[] options = {"Fit (Default)", "Stretch (Fill Screen)", "Zoom (Crop to Fit)"};

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Screen Scale")
            .setItems(options, (dialog, which) -> {
                int mode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                String toastMsg = "Scale: Fit";

                if (which == 1) {
                    mode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                    toastMsg = "Scale: Stretch";
                } else if (which == 2) {
                    mode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                    toastMsg = "Scale: Zoom";
                }

                playerView.setResizeMode(mode);

                prefs.edit().putInt("global_screen_scale", mode).apply();

                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            })
            .show();
    }

}
