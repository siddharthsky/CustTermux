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
import com.google.android.exoplayer2.util.MimeTypes;
import com.termux.R;
import com.termux.sky.plugins.Plugin;
import com.termux.sky.plugins.PluginStorage;
import com.termux.sky.tv_home_preview.RecentChannelsManager;

import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

    private int currentZapToken = 0;

    String port_no;

    private String lastAttemptedUrl = null;
    private int currentFormatTrackIndex = 0;
    private static final int FORMAT_DASH = 0;
    private static final int FORMAT_HLS = 1;
    private static final int FORMAT_PROGRESSIVE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        disableSSLCertificateChecking();

        Intent intent = getIntent();
        Uri data = intent.getData();

        String videoUrl = null;
        String videoName = null;
        String licenseUrl = null;
        String intentUserAgent = null;
        String intentCookie = null;
        String playlistType = null;

        // Check if launched from Home Screen Deep Link
        boolean isFromHome = data != null && "hanaplayer".equals(data.getScheme());

        if (isFromHome) {
            videoUrl = data.getQueryParameter("url");
            videoName = data.getQueryParameter("name");
            licenseUrl = data.getQueryParameter("license");
            intentUserAgent = data.getQueryParameter("user_agent");
            intentCookie = data.getQueryParameter("cookie");
            playlistType = data.getQueryParameter("playlist_type");
            getIntent().putExtra("show_banner", true);

            if (port_no == null && videoUrl != null) {
                port_no = videoUrl.contains("5007") ? "5007" : "0";
            }
        } else {
            // Standard internal intent
            videoUrl = intent.getStringExtra("url");
            videoName = intent.getStringExtra("name");
            licenseUrl = intent.getStringExtra("license_key");
            intentUserAgent = intent.getStringExtra("user_agent");
            intentCookie = intent.getStringExtra("cookie");

            if (port_no == null && videoUrl != null) {
                port_no = videoUrl.contains("5007") ? "5007" : "0";
            }
        }

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
        errorOverlay.setTextSize(14);
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
            (int) (screenWidth * 0.6),
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

        if (!isFromHome) {
            if (PlaylistManager.currentList == null || PlaylistManager.currentList.isEmpty()) {
                setupNormalPlaylist(videoUrl, videoName);
            }
        } else {
            if ("favorites".equals(playlistType)) {
                setupFavoritesPlaylist();
            } else if ("recents".equals(playlistType)) {
                setupRecentsPlaylist();
            } else {
                setupNormalPlaylist(videoUrl, videoName);
            }
        }

        if (videoUrl != null) {
            syncPlaylistIndex(videoUrl, videoName);
            processIncomingUrl(videoUrl, licenseUrl, intentUserAgent, intentCookie, isFromHome);
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

    // Helper method to Ping the URL
    private int pingUrl(String videoUrl, String userAgent, String origin, String referer) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(videoUrl).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : "Mozilla/5.0";
            connection.setRequestProperty("User-Agent", ua);

            if (origin != null && !origin.isEmpty()) connection.setRequestProperty("Origin", origin);
            if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);

            return connection.getResponseCode();
        } catch (Exception e) {
            return -1; // Network error / timeout
        }
    }

    private void checkStatusAndPlay(String videoUrl, String licenseUrl, String userAgent, String origin, String referer, String cookie, boolean isFromHome) {
        if (currentCheckThread != null && currentCheckThread.isAlive()) {
            currentCheckThread.interrupt();
        }

        final int myZapToken = currentZapToken;

        currentCheckThread = new Thread(() -> {
            if (!isFromHome) {
                runOnUiThread(() -> {
                    if (myZapToken != currentZapToken) return;
                    if (errorOverlay != null) errorOverlay.setVisibility(View.GONE);
                    initializePlayer(videoUrl, licenseUrl, userAgent, origin, referer, cookie);
                });
                return;
            }

            int initialCode = pingUrl(videoUrl, userAgent, origin, referer); // Standard validation ping
            boolean isAlive = (initialCode >= 200 && initialCode < 400);

            if (Thread.currentThread().isInterrupted()) return;

            if (isAlive) {
                runOnUiThread(() -> {
                    if (myZapToken != currentZapToken) return;
                    if (errorOverlay != null) errorOverlay.setVisibility(View.GONE);
                    initializePlayer(videoUrl, licenseUrl, userAgent, origin, referer, cookie);
                });
            } else {
                runOnUiThread(() -> {
                    if (myZapToken != currentZapToken) return;
                    if (errorOverlay != null) {
                        errorOverlay.setText("⏳ Initializing Server...\nPlease wait a moment.");
                        errorOverlay.setVisibility(View.VISIBLE);
                    }
                });

                ensureServerIsRunning(ExoPlayerActivityDRM.this);

                int maxRetries = 5;
                boolean retrySuccess = false;

                for (int i = 0; i < maxRetries; i++) {
                    if (Thread.currentThread().isInterrupted()) return;
                    try { Thread.sleep(2000); } catch (InterruptedException e) { return; }

                    int retryCode = pingUrl(videoUrl, userAgent, origin, referer);
                    if (retryCode >= 200 && retryCode < 400) {
                        retrySuccess = true;
                        break;
                    }
                }

                if (Thread.currentThread().isInterrupted()) return;

                if (retrySuccess) {
                    runOnUiThread(() -> {
                        if (myZapToken != currentZapToken) return;
                        if (errorOverlay != null) errorOverlay.setVisibility(View.GONE);
                        initializePlayer(videoUrl, licenseUrl, userAgent, origin, referer, cookie);
                    });
                } else {
                    showErrorOrFallback(videoUrl, myZapToken, initialCode);
                }
            }
        });
        currentCheckThread.start();
    }

    private void showErrorOrFallback(String videoUrl, int zapToken, int responseCode) {
        runOnUiThread(() -> {
            if (zapToken != currentZapToken) return;
            if (!videoUrl.contains("/live/")) {
                if (errorOverlay != null) {
                    String msg = (responseCode == -1) ? "⚠️ SERVER TIMEOUT\nTermux or Server is not responding." :
                        "⚠️ CONNECTION ISSUE\nHTTP CODE: " + responseCode;
                    errorOverlay.setText(msg);
                    errorOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                switchToWebView(videoUrl);
            }
        });
    }

    private void initializePlayer(String videoUrl, String licenseUrl, String userAgent, String origin, String referer, String cookie) {
        Log.d("DRM_PLAYER", "videoUrl=" + videoUrl + ", licenseUrl=" + licenseUrl);

        if (videoUrl != null) {
            boolean containsLive = videoUrl.toLowerCase().contains("live");
            List<String> numberList = Arrays.asList("676", "678", "682", "684", "729", "733", "744", "747", "895", "896", "897", "898", "899", "900", "901", "1662", "1669", "1754", "2424", "3088");
            String joinedNumbers = String.join("|", numberList);
            String regexPattern = ".*\\b(" + joinedNumbers + ")\\b.*";
            boolean containsExactNumber = videoUrl.matches(regexPattern);
            if (containsLive && containsExactNumber) {
                Log.d("DRM_PLAYER", "URL contains 'live' and an exact target number. Switching to WebView.");
                switchToWebView(videoUrl);
                return;
            }
        }


        if (lastAttemptedUrl == null || !lastAttemptedUrl.equals(videoUrl)) {
            lastAttemptedUrl = videoUrl;
            currentFormatTrackIndex = 0;
        }

        Map<String, String> headers = new HashMap<>();
        if (userAgent != null && !userAgent.isEmpty()) headers.put("User-Agent", userAgent);
        if (origin != null && !origin.isEmpty()) headers.put("Origin", origin);
        if (referer != null && !referer.isEmpty()) headers.put("Referer", referer);

        if (cookie != null && !cookie.isEmpty()) {
            headers.put("Cookie", cookie);
            Log.d("DRM_PLAYER", "Dynamic Cookie Injected: " + cookie);
        }

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers);

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(Uri.parse(videoUrl));
        MediaSource mediaSource;

        if (currentFormatTrackIndex == 0) {
            boolean initialCheckIsHls = videoUrl.toLowerCase().contains(".m3u8")
                || videoUrl.contains("index.php")
                || videoUrl.contains("stream.php");

            currentFormatTrackIndex = initialCheckIsHls ? FORMAT_HLS : FORMAT_DASH;
        }

        switch (currentFormatTrackIndex) {
            case FORMAT_DASH:
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD);
                if (licenseUrl != null && !licenseUrl.isEmpty()) {
                    mediaItemBuilder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                        .setLicenseUri(licenseUrl)
                        .setLicenseRequestHeaders(headers)
                        .build());
                }
                mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItemBuilder.build());
                Log.d("DRM_PLAYER", "Attempting playback as: DASH (MPD)");
                break;

            case FORMAT_HLS:
            default:
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItemBuilder.build());
                Log.d("DRM_PLAYER", "Attempting playback as: HLS (M3U8)");
                break;

//            case FORMAT_PROGRESSIVE:
//            default:
//                mediaSource = new com.google.android.exoplayer2.source.ProgressiveMediaSource.Factory(dataSourceFactory)
//                    .createMediaSource(mediaItemBuilder.build());
//                Log.d("DRM_PLAYER", "Attempting playback as: Progressive Container (MP4/MKV)");
//                break;
        }

        if (player == null) {
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
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
                    Throwable cause = error.getCause();

                    boolean isContainerError = (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                        || error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
                        || error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                        || cause instanceof com.google.android.exoplayer2.ParserException
                        || (cause != null && cause.getMessage() != null && cause.getMessage().contains("org.xmlpull")));

                    if (isContainerError) {
                        if (currentFormatTrackIndex == FORMAT_DASH) {
                            Log.w("DRM_PLAYER", "DASH parsing failed. Cycling to HLS layout.");
                            currentFormatTrackIndex = FORMAT_HLS;
                            retryPlaybackLoop();
                            return;
                        } else if (currentFormatTrackIndex == FORMAT_HLS) {
                            Log.w("DRM_PLAYER", "HLS parsing failed. Cycling to Progressive Media (MP4/MKV).");
                            currentFormatTrackIndex = FORMAT_PROGRESSIVE;
                            retryPlaybackLoop();
                            return;
                        } else if (currentFormatTrackIndex == FORMAT_PROGRESSIVE) {
                            Log.e("DRM_PLAYER", "All container formats failed parsing targets.");
                        }
                    }

                    if (errorOverlay != null && !videoUrl.contains("/live/")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("⚠️ ERROR DETAILS\n");
                        sb.append("────────────────\n");
                        sb.append("CODE: ").append(error.errorCode).append("\n");
                        sb.append("TYPE: ").append(error.getErrorCodeName()).append("\n");

                        if (error.getMessage() != null) {
                            sb.append("Details: ").append(error.getMessage()).append("\n");
                        }
                        errorOverlay.setText(sb.toString());
                        errorOverlay.setVisibility(View.VISIBLE);
                    } else {
                        switchToWebView(videoUrl);
                    }
                }

                private void retryPlaybackLoop() {
                    if (player != null) {
                        player.stop();
                        player.clearMediaItems();
                    }
                    initializePlayer(videoUrl, licenseUrl, userAgent, origin, referer, cookie);
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (errorOverlay != null && state == Player.STATE_READY) {
                        errorOverlay.setVisibility(View.GONE);
                    }
                }
            });

            player.setAudioAttributes(
                new com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MOVIE)
                    .build(),
                true
            );

            playerView.setPlayer(player);
        }

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

    @Override
    public void onBackPressed() {
        if (playerView != null && playerView.isControllerVisible()) {
            playerView.hideController();
            return;
        }
        super.onBackPressed();
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

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (playerView != null && playerView.isControllerVisible()) {
                playerView.hideController();
                return true;
            }
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
        if (PlaylistManager.currentList == null || PlaylistManager.currentList.isEmpty()) {
            return;
        }

        currentZapToken++;

        if (player != null) {
            player.stop();
            player.clearMediaItems();
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

        zapRunnable = () -> processAndPlayChannel(nextChannel);
        zapHandler.postDelayed(zapRunnable, ZAP_DELAY_MS);
    }


    private void processAndPlayChannel(ChannelModel nextChannel) {
        String vUrl = nextChannel.url;
        String origin = null;
        String referer = null;
        String userAgent = nextChannel.userAgent;
        String cookie = nextChannel.cookie;

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
                    else if (lowerPart.startsWith("cookie=")) cookie = part.substring(7);
                }
            }
            vUrl = vUrl.replaceAll("&$", "");
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "URL Decoding failed", e);
        }

        checkStatusAndPlay(vUrl, nextChannel.licenseKey, userAgent, origin, referer, cookie, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Uri data = intent.getData();
        if (data != null && "hanaplayer".equals(data.getScheme())) {
            String url = data.getQueryParameter("url");
            String name = data.getQueryParameter("name");
            String license = data.getQueryParameter("license");
            String ua = data.getQueryParameter("user_agent");
            String intentCookie = data.getQueryParameter("cookie");
            String playlistType = data.getQueryParameter("playlist_type");

            if (url != null) {
                port_no = url.contains("5007") ? "5007" : "0";

                if ("favorites".equals(playlistType)) {
                    setupFavoritesPlaylist();
                } else if ("recents".equals(playlistType)) {
                    setupRecentsPlaylist();
                } else {
                    setupNormalPlaylist(url, name);
                }

                syncPlaylistIndex(url, name);

                bannerManager.show(name, intent.getStringExtra("logo_url"));
                processIncomingUrl(url, license, ua, intentCookie, true);
            }
        }
    }

    private void processIncomingUrl(String rawUrl, String licenseUrl, String intentUserAgent, String intentCookie, boolean isFromHome) {
        String v_url = rawUrl;
        String origin = null;
        String referer = null;
        String userAgent = intentUserAgent;
        String cookie = intentCookie;

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
                    else if (lowerPart.startsWith("cookie=")) cookie = part.substring(7);
                }
            }
            v_url = v_url.replaceAll("&$", "");
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "URL Decoding failed", e);
        }

        checkStatusAndPlay(v_url, licenseUrl, userAgent, origin, referer, cookie, isFromHome);
    }

    private void showSettingsMenu() {
        if (player == null) return;

        String[] options = {"⚙️ Video Quality", "🎵 Audio Track", "📺 Screen Scale"};

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Player Settings")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showCustomVideoTrackSelector();
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
        builder.setTheme(R.style.GoldenFocusDialogTheme);

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

        trackNames.add("Auto");
        overrides.add(null);
        groupIndices.add(-1);

        DefaultTrackNameProvider nameProvider = new DefaultTrackNameProvider(getResources());
        int checkedItem = 0;

        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);

                if (format.bitrate == Format.NO_VALUE || format.bitrate > 400000) {
                    trackNames.add(nameProvider.getTrackName(format));
                    DefaultTrackSelector.SelectionOverride override =
                        new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex);
                    overrides.add(override);
                    groupIndices.add(groupIndex);

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

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Select Video Quality")
            .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                DefaultTrackSelector.ParametersBuilder builder = trackSelector.buildUponParameters();

                if (which == 0) {
                    builder.clearSelectionOverrides(finalRendererIndex);
                } else {
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
        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
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

    private void disableSSLCertificateChecking() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "Failed to disable SSL checking", e);
        }
    }

    private void ensureServerIsRunning(Context context) {
        try {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
            serviceIntent.setAction("com.termux.RUN_COMMAND");
            serviceIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutilz.sh");
            serviceIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"--run", "boot"});
            serviceIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
            serviceIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
            serviceIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", 1);
            context.startService(serviceIntent);
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "Could not start background server", e);
        }
    }

    private void syncPlaylistIndex(String videoUrl, String videoName) {
        if (PlaylistManager.currentList != null) {
            for (int i = 0; i < PlaylistManager.currentList.size(); i++) {
                ChannelModel cm = PlaylistManager.currentList.get(i);
                if ((videoName != null && videoName.equals(cm.name)) ||
                    (videoUrl != null && videoUrl.equals(cm.url))) {
                    PlaylistManager.currentIndex = i;
                    return;
                }
            }
        }
    }

    private void setupFavoritesPlaylist() {
        List<ChannelModel> favorites = new ArrayList<>();

        Set<String> portsToCheck = new HashSet<>();
        portsToCheck.add("0");
        portsToCheck.add("5007");
        portsToCheck.add("99000");

        try {
            List<Plugin> plugins = PluginStorage.load(this);
            if (plugins != null) {
                for (Plugin p : plugins) {
                    portsToCheck.add(String.valueOf(p.port));
                }
            }
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "Failed to load plugins for favorites", e);
        }

        for (String p : portsToCheck) {
            List<ChannelModel> channels = M3UParser.getFromPrefs(this, p);
            if (channels != null) {
                for (ChannelModel cm : channels) {
                    if (cm.isFavorite) {
                        favorites.add(cm);
                    }
                }
            }
        }

        PlaylistManager.currentList = favorites.isEmpty() ? null : favorites;
    }

    private void setupNormalPlaylist(String targetUrl, String targetName) {
        String portToLoad = (port_no != null) ? port_no : "0";
        List<ChannelModel> savedChannels = M3UParser.getFromPrefs(this, portToLoad);

        boolean found = false;
        if (savedChannels != null) {
            for (ChannelModel cm : savedChannels) {
                if ((targetName != null && targetName.equals(cm.name)) ||
                    (targetUrl != null && targetUrl.equals(cm.url))) {
                    found = true; break;
                }
            }
        }


        if (!found) {
            String altPort = "0".equals(portToLoad) ? "5007" : "0";
            List<ChannelModel> altChannels = M3UParser.getFromPrefs(this, altPort);
            if (altChannels != null) {
                savedChannels = altChannels;
                port_no = altPort;
            }
        }

        PlaylistManager.currentList = (savedChannels != null && !savedChannels.isEmpty()) ? savedChannels : null;
    }

    private void setupRecentsPlaylist() {
        try {
            List<ChannelModel> recents = RecentChannelsManager.getRecentChannels(this);

            if (recents != null && !recents.isEmpty()) {
                PlaylistManager.currentList = recents;
                return;
            }
        } catch (Exception e) {
            Log.e("DRM_PLAYER", "Failed to load recents loop", e);
        }

        PlaylistManager.currentList = null;
    }
}
