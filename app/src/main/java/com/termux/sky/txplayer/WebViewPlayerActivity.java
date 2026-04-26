package com.termux.sky.txplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.exoplayer2.ui.PlayerView;
import com.termux.sky.playlistmanager.PlaylistManager;

public class WebViewPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private int backCount = 0;
    private static final int MAX_BACK = 5;

    String port_no = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        webView = new WebView(this);
        setContentView(webView);
        hideSystemUI();

        String url = getIntent().getStringExtra("url");
        port_no = getIntent().getStringExtra("port");

        Log.d("DRM_PLAYER_WEB", "url received: " + url+" on port: " + port_no);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.flush();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    runOnUiThread(() -> {
                        for (String resource : request.getResources()) {

                            if (android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                                request.grant(new String[]{resource});
                                return;
                            }
                        }

                        request.deny();
                    });
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String finalUrl = rewriteUrl(url);
                view.loadUrl(finalUrl);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();

                backCount = 0;
            }
        });

        if (url != null) {
            webView.loadUrl(rewriteUrl(url));
        }
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), webView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private String rewriteUrl(String url) {
        if (url == null) return null;

        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();

            if (port_no != null && !port_no.isEmpty()) {

                assert path != null;
                if (path.contains("/live/")) {
                    Log.d("DRM_PLAYER_WEB", "Port 5006,5007 logic applied");
                    path = path.replace("/live/", "/mpd/");

                    if (path.endsWith(".m3u8")) {
                        path = path.substring(0, path.length() - ".m3u8".length());
                    }
                }
            }
            

            return uri.buildUpon()
                .path(path)
                .build()
                .toString();

        } catch (Exception e) {
            Log.e("DRM_PLAYER_WEB", String.valueOf(e));
            return url;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            backCount++;

            if (backCount >= MAX_BACK) {
                finish();
            }

        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.clearHistory();
            webView.clearCache(true);
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        // Intercept the key press before the WebView can consume it for scrolling
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                changeChannel(1);
                return true; // Return true so the WebView doesn't scroll
            } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                changeChannel(-1);
                return true; // Return true so the WebView doesn't scroll
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void changeChannel(int direction) {
        if (PlaylistManager.currentList == null || PlaylistManager.currentList.isEmpty()) return;

        // Calculate next index with wrap-around
        int newIndex = PlaylistManager.currentIndex + direction;
        if (newIndex < 0) newIndex = PlaylistManager.currentList.size() - 1;
        if (newIndex >= PlaylistManager.currentList.size()) newIndex = 0;

        PlaylistManager.currentIndex = newIndex;
        ChannelModel nextChannel = PlaylistManager.currentList.get(newIndex);

        String activePort = nextChannel.originPort != null ? nextChannel.originPort : (nextChannel.url.contains("5007") ? "5007" : "0");

//        Toast.makeText(this, "Trying ExoPlayer: " + nextChannel.name, android.widget.Toast.LENGTH_SHORT).show();

        // INSTEAD of loading in WebView, we launch ExoPlayerActivityDRM again
        Intent intent = new Intent(this, ExoPlayerActivityDRM.class)
            .putExtra("url", nextChannel.url)
            .putExtra("name", nextChannel.name)
            .putExtra("logo_url", nextChannel.logo)
            .putExtra("license_key", nextChannel.licenseKey)
            .putExtra("license_type", nextChannel.licenseType)
            .putExtra("user_agent", nextChannel.userAgent)
            .putExtra("manifest_type", nextChannel.manifestType)
            .putExtra("plugin_port", activePort);

        // Clear the WebView from the back stack so the user doesn't get stuck in a loop
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);

        // Clean up and close the WebView Activity
        if (webView != null) {
            webView.stopLoading();
            webView.onPause();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
        }
    }
}
