package com.termux;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WebPlayerActivity extends AppCompatActivity {

    private static final String TAG = "RIX";

    private WebView webView;
    private ProgressBar loadingSpinner;
    private List<String> channelNumbers;

    private String url;
    private String BASE_URL;
    private String CONFIGPART_URL;
    private String DEFAULT_URL;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        SkySharedPref preferenceManager = new SkySharedPref(this);
        BASE_URL = preferenceManager.getKey("isLocalPORT");
        String PORTx = preferenceManager.getKey("isLocalPORTonly");
        CONFIGPART_URL = preferenceManager.getKey("isWEBTVconfig");
        DEFAULT_URL = BASE_URL + CONFIGPART_URL;

        Log.d(TAG, "URL: " + DEFAULT_URL);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setFullScreenMode();

        initializeWebView();
        loadUrl();
    }

    private void initializeWebView() {
        webView = findViewById(R.id.webview);
        loadingSpinner = findViewById(R.id.loading_spinner);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setMediaPlaybackRequiresUserGesture(false); // Allow autoplay
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
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void extractChannelNumbers() {
        webView.evaluateJavascript("Array.from(document.querySelectorAll('.card')).map(card => card.getAttribute('href').match(/\\/play\\/(\\d+)/)[1])", result -> {
            if (result != null && !result.isEmpty()) {
                result = result.replace("[", "").replace("]", "").replace("\"", "");
                channelNumbers = Arrays.asList(result.split(","));
                Log.d(TAG, "Channel Numbers: " + channelNumbers);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && webView.getUrl() != null && webView.getUrl().contains("/player/")) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    navigateToNextChannel();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    navigateToPreviousChannel();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void navigateToNextChannel() {
        navigateChannel(1);
    }

    private void navigateToPreviousChannel() {
        navigateChannel(-1);
    }

    private void navigateChannel(int direction) {
        if (channelNumbers == null || channelNumbers.isEmpty()) {
            Log.d(TAG, "No channel numbers available.");
            return;
        }

        String currentUrl = webView.getUrl();
        assert currentUrl != null;

        String currentNumber = currentUrl.substring(currentUrl.lastIndexOf('/') + 1, currentUrl.indexOf('?'));
        int index = channelNumbers.indexOf(currentNumber);
        if (index >= 0) {
            int newIndex = (index + direction + channelNumbers.size()) % channelNumbers.size();
            String newNumber = channelNumbers.get(newIndex);
            String newUrl = currentUrl.replace("/" + currentNumber + "?", "/" + newNumber + "?");
            Log.d(TAG, "Navigating to Channel: " + newUrl);
            webView.loadUrl(newUrl);
        } else {
            Log.d(TAG, "Current number not found in channel numbers.");
        }
    }

    private int playerUrlCount = 0;

    @Override
    public void onBackPressed() {
        if (webView != null) {
            String currentUrl = webView.getUrl();
            assert currentUrl != null;

            if (currentUrl.contains("/player/")) {
                playerUrlCount++;
                webView.loadUrl(DEFAULT_URL);
            } else if (currentUrl.equals(DEFAULT_URL)) {
                playerUrlCount++;
                if (playerUrlCount >= 3) {
                    finish();
                }
            } else if (webView.canGoBack()) {
                webView.goBack();
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
            if (url.contains("/play/")) {
                String newUrl = url.replace("/play/", "/player/");
                Log.d(TAG, "Loading new player URL: " + newUrl);
                webView.loadUrl(newUrl);
                return true; // URL has been overridden
            }
            return false; // URL has not been overridden
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            loadingSpinner.setVisibility(View.GONE);
            if (url.contains("/player/")) {
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
            } else if (url.equals(DEFAULT_URL)) {
                moveSearchInput(view);
                extractChannelNumbers();
            } else {
                moveSearchInput(view);
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
}
