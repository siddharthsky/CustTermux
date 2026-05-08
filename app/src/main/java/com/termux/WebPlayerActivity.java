package com.termux;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebPlayerActivity extends AppCompatActivity {

    private static final String TAG = "WebPlayerActivity";
    private WebView webView;
    private ProgressBar loadingSpinner;
    private String DEFAULT_URL;
    private int playerUrlCount = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        SkySharedPref preferenceManager = new SkySharedPref(this);
        String BASE_URL = preferenceManager.getKey("isLocalPORT");
        String CONFIGPART_URL = preferenceManager.getKey("isWEBTVconfig");
        DEFAULT_URL = BASE_URL + CONFIGPART_URL;

        Log.d(TAG, "URL: " + DEFAULT_URL);

        // Always landscape and fullscreen
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setFullScreenMode();

        initializeWebView();
        loadUrl();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        webView = findViewById(R.id.webview);
        loadingSpinner = findViewById(R.id.loading_spinner);

        // Enable hardware acceleration for faster video playback and rendering
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new CustomWebViewClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.flush();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

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
    }

    private void loadUrl() {
        if (DEFAULT_URL != null) {
            webView.loadUrl(DEFAULT_URL);
        }
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        setFullScreenMode();
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            String currentUrl = webView.getUrl();
            assert currentUrl != null;

            if (currentUrl.contains("/player/")) {
                playerUrlCount++;
                if (playerUrlCount >= 3) {
                    webView.loadUrl(DEFAULT_URL);
                } else {
                    webView.goBack();
                }
            } else if (webView.canGoBack()) {
                playerUrlCount++;
                if (playerUrlCount >= 6) {
                    finish();
                } else {
                    webView.goBack();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null && url.contains("/play/")) {
                String newUrl = url.replace("/play/", "/mpd/");
                Log.d(TAG, "Redirecting to mpd: " + newUrl);
                view.loadUrl(newUrl);
                return true;
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
//            if (loadingSpinner != null) {
//                loadingSpinner.setVisibility(View.VISIBLE);
//            }
            moveSearchInput(view);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            if (loadingSpinner != null) {
//                loadingSpinner.setVisibility(View.GONE);
//            }

            if (url.contains("/mpd/")) {
                setFullScreenMode();
                view.loadUrl("javascript:(function() { " +
                    "var video = document.getElementsByTagName('video')[0]; " +
                    "if (video) { " +
                    "  video.style.width = '100vw'; " +
                    "  video.style.height = '100vh'; " +
                    "  video.style.objectFit = 'contain'; " +
                    "  video.play(); " +
                    "} " +
                    "})()");
            } else {
                moveSearchInput(view);
            }
        }
    }

    private void moveSearchInput(WebView view) {
        view.loadUrl("javascript:(function() { " +
            "var searchButton = document.getElementById('portexe-search-button'); " +
            "var searchInput = document.getElementById('portexe-search-input'); " +
            "if (searchButton && searchInput) { " +
            "  searchButton.parentNode.insertBefore(searchInput, searchButton.nextSibling); " +
            "} " +
            "})()");
    }
}
