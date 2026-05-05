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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.termux.R;
import com.termux.sky.playlistmanager.PlaylistManager;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExoPlayerActivityDRM extends ComponentActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private ChannelBannerManager bannerManager;
    private DefaultTrackSelector trackSelector;

    private android.os.Handler zapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable zapRunnable;
    private static final long ZAP_DELAY_MS = 400;

    private TextView errorOverlay;

    private SharedPreferences prefs;
    private Thread currentCheckThread;

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

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(30f);
        shape.setColor(android.graphics.Color.parseColor("#D91A1A1A"));
        shape.setStroke(3, android.graphics.Color.parseColor("#FF4444"));
        errorOverlay = new TextView(this);
        errorOverlay.setBackground(shape);
        errorOverlay.setTextColor(android.graphics.Color.WHITE);
        errorOverlay.setGravity(android.view.Gravity.CENTER);
        errorOverlay.setTextSize(14); // Slightly smaller text
        errorOverlay.setPadding(40, 40, 40, 40);
        errorOverlay.setLineSpacing(0, 1.1f);
        errorOverlay.setVisibility(View.GONE);


        playerView = new PlayerView(this);
        playerView.setKeepScreenOn(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        root.addView(playerView);


        playerView.setControllerAutoShow(false);
        playerView.hideController();

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
                showCustomVideoTrackSelector());

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

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams errorParams = new FrameLayout.LayoutParams(
            (int) (screenWidth * 0.6), // 60% width
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        errorParams.gravity = android.view.Gravity.CENTER;

        root.addView(errorOverlay, errorParams);


        setContentView(root);

        bannerManager = new ChannelBannerManager(root);

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

        if (currentCheckThread != null && currentCheckThread.isAlive()) {
            currentCheckThread.interrupt();
        }

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
                if (Thread.currentThread().isInterrupted()) return;
                Log.d("DRM_PLAYER", "Response Code: " + responseCode);

                boolean isUrlValid = (responseCode >= 200 && responseCode < 400);
                boolean isSpecialPort = "5007".equals(port_no);

                runOnUiThread(() -> {

                    if (Thread.currentThread().isInterrupted()) return;

                    if (isUrlValid && !isSpecialPort) {
                        initializePlayer(videoUrl, licenseUrl, ua, origin, referer);
                    } else {
                        if (!videoUrl.contains("/live/")) {
                            if (errorOverlay != null) {
                                String sb = "⚠️ CONNECTION ISSUE\n" +
                                    "────────────────\n" +
                                    "HTTP CODE: " + responseCode + "\n" +
                                    "STATUS: " + (responseCode == 403 ? "Access Denied" : "Server Error") + "\n";

                                errorOverlay.setText(sb);
                                errorOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            switchToWebView(videoUrl);
                        }
                    }
                });

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) return;

                Log.e("DRM_PLAYER", "Network error during check", e);

                runOnUiThread(() -> {
                    String errorMsg = e instanceof java.net.SocketTimeoutException ?
                        "Connection Timeout: Server not responding" :
                        "Network error: " + e.getMessage();

                    if (errorOverlay != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("⚠️ NETWORK FAILURE\n");
                        sb.append("────────────────\n");
                        sb.append("TYPE: ").append(e.getClass().getSimpleName()).append("\n");
                        sb.append("INFO: ").append(errorMsg);

                        errorOverlay.setText(sb.toString());
                        errorOverlay.setVisibility(View.VISIBLE);
                    }
                });
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
        trackSelector.setParameters(trackSelector.buildUponParameters()
            .setForceHighestSupportedBitrate(false)
            .build());
        applySavedTrackPreferences();

        player = new ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {

                if (errorOverlay != null && !videoUrl.contains("/live/") ) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("⚠️ ERROR DETAILS\n");
                    sb.append("────────────────\n");
                    sb.append("CODE: ").append(error.errorCode).append("\n");
                    sb.append("TYPE: ").append(error.getErrorCodeName()).append("\n");

                    if (error.getMessage() != null) {
                        sb.append("Details: ").append(error.getMessage()).append("\n");
                    }

                    if (error.getCause() != null) {
                        sb.append("INFO: ").append(error.getCause().getClass().getSimpleName());
                    }

                    errorOverlay.setText(sb.toString());
                    errorOverlay.setVisibility(View.VISIBLE);
                } else {
                    switchToWebView(videoUrl);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (errorOverlay != null && state == Player.STATE_READY) {
                    errorOverlay.setVisibility(View.GONE);
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onTracksChanged(@NonNull TrackGroupArray trackGroups, com.google.android.exoplayer2.trackselection.TrackSelectionArray trackSelections) {
                // Intentionally left blank
            }
        });

        playerView.setPlayer(player);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        playerView.post(() -> playerView.hideController());
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
            player.stop();
            player.clearMediaItems();
            player.setPlayWhenReady(false);
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_S) {
            showSettingsMenu();
            return true;
        }

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

        if (player != null) {
            player.stop();
        }

        if (zapRunnable != null) zapHandler.removeCallbacks(zapRunnable);
        if (currentCheckThread != null) currentCheckThread.interrupt();

        int newIndex = PlaylistManager.currentIndex + direction;
        if (newIndex < 0) newIndex = PlaylistManager.currentList.size() - 1;
        if (newIndex >= PlaylistManager.currentList.size()) newIndex = 0;

        PlaylistManager.currentIndex = newIndex;
        ChannelModel nextChannel = PlaylistManager.currentList.get(newIndex);

        bannerManager.show(nextChannel.name, nextChannel.logo);
        port_no = nextChannel.originPort != null ? nextChannel.originPort : (nextChannel.url.contains("5007") ? "5007" : "0");

        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.GONE);
        }

        if (player != null) {
            player.stop();
        }

        if (zapRunnable != null) {
            zapHandler.removeCallbacks(zapRunnable);
        }

        zapRunnable = () -> {
            killExoPlayer();
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
                        showCustomVideoTrackSelector(); // Call the new custom dialog
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

        int rendererIndex = -1;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == trackType) {
                rendererIndex = i;
                break;
            }
        }

        if (rendererIndex == -1) return;

        TrackSelectionDialogBuilder builder = new TrackSelectionDialogBuilder(
            this, title, trackSelector, rendererIndex
        );


        builder.setAllowAdaptiveSelections(false);

        builder.setShowDisableOption(true);
        builder.setTheme(android.R.style.Theme_DeviceDefault_Dialog_Alert);

        Dialog dialog = builder.build();

        if (trackType == C.TRACK_TYPE_AUDIO) {
            dialog.setOnDismissListener(d -> {
                if (playerView != null) {
                    playerView.postDelayed(() -> {
                        if (player != null) {
                            @SuppressWarnings("deprecation")
                            Format format = player.getAudioFormat();
                            if (format != null && format.language != null) {
                                prefs.edit().putString("pref_audio_lang", format.language).apply();
                            }
                        }
                    }, 300);
                }
            });
        }

        dialog.show();
    }

    private void showCustomVideoTrackSelector() {
        if (player == null || trackSelector == null) return;

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;

        int rendererIndex = -1;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                rendererIndex = i;
                break;
            }
        }

        if (rendererIndex == -1) return;

        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
        DefaultTrackSelector.Parameters currentParams = trackSelector.getParameters();

        List<String> trackNames = new ArrayList<>();
        List<DefaultTrackSelector.SelectionOverride> overrides = new ArrayList<>();
        List<Integer> groupIndices = new ArrayList<>();

        // Add "Auto" option at the top
        trackNames.add("Auto");
        overrides.add(null);
        groupIndices.add(-1);

        DefaultTrackNameProvider nameProvider = new DefaultTrackNameProvider(getResources());
        int checkedItem = 0;

        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);

                // FILTER: Only show tracks with a bitrate above 400 kbps (or unknown)
                if (format.bitrate == Format.NO_VALUE || format.bitrate > 400000) {

                    trackNames.add(nameProvider.getTrackName(format));

                    DefaultTrackSelector.SelectionOverride override =
                        new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex);

                    overrides.add(override);
                    groupIndices.add(groupIndex);

                    // Check if this track is the one currently selected by the user
                    DefaultTrackSelector.SelectionOverride currentOverride =
                        currentParams.getSelectionOverride(rendererIndex, trackGroups);

                    if (currentOverride != null &&
                        currentOverride.groupIndex == groupIndex &&
                        currentOverride.containsTrack(trackIndex)) {
                        checkedItem = trackNames.size() - 1;
                    }
                }
            }
        }

        final int finalRendererIndex = rendererIndex;
        String[] options = trackNames.toArray(new String[0]);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Select Video Quality")
            .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                DefaultTrackSelector.ParametersBuilder builder = trackSelector.buildUponParameters();

                if (which == 0) {
                    // "Auto" selected: clear user overrides
                    builder.clearSelectionOverrides(finalRendererIndex);
                } else {
                    // Specific quality selected
                    builder.clearSelectionOverrides(finalRendererIndex);
                    builder.setSelectionOverride(
                        finalRendererIndex,
                        trackGroups,
                        overrides.get(which)
                    );
                }

                trackSelector.setParameters(builder.build());
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
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
