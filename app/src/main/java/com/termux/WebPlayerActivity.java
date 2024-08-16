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

public class WebPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar loadingSpinner;
    private List<String> channelNumbers;
    private String url;
//    private static final String DEFAULT_URL = "http://localhost:5001/";
    private static final String TAG = "WebPlayerActivity";
    private String DEFAULT_URL;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        // Initialize SkySharedPref and other member variables
        SkySharedPref preferenceManager = new SkySharedPref(this);
        DEFAULT_URL = preferenceManager.getKey("isLocalPORT");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

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

        url = DEFAULT_URL;
        // Load the initial URL
        loadUrl();
    }

    private void loadUrl() {
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void setDarkTheme() {
        if (webView != null) {
            String jsCode = "document.getElementsByTagName('html')[0].setAttribute('data-theme', 'dark');" +
                "localStorage.setItem('theme', 'dark');";
            webView.evaluateJavascript(jsCode, null);
        }
    }

    private void extractChannelNumbers() {
        webView.evaluateJavascript("Array.from(document.querySelectorAll('.card')).map(card => card.getAttribute('href').match(/\\/play\\/(\\d+)/)[1])", result -> {
            if (result != null && !result.isEmpty()) {
                result = result.replace("[", "").replace("]", "").replace("\"", "");
                String[] channelNumbersArray = result.split(",");
                channelNumbers = Arrays.asList(channelNumbersArray);
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

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            if (webView.getUrl().contains("/player/")) {
//                switch (event.getKeyCode()) {
//                    case KeyEvent.KEYCODE_DPAD_RIGHT:
//                        navigateToNextChannel();
//                        return true;
//                    case KeyEvent.KEYCODE_DPAD_LEFT:
//                        navigateToPreviousChannel();
//                        return true;
//                }
//            }
//        }
//        return super.dispatchKeyEvent(event);
//    }
//
//    private void navigateToNextChannel() {
//        Log.d(TAG, "Channel Numbers: navigateToNextChannel" + channelNumbers);
//        if (channelNumbers == null || channelNumbers.isEmpty()) {
//            Log.d(TAG, "No channel numbers available.");
//            return;
//        }
//        String currentUrl = webView.getUrl();
//        String currentNumber = currentUrl.substring(currentUrl.lastIndexOf('/') + 1);
//        int index = channelNumbers.indexOf(currentNumber);
//        Log.d(TAG, "Current Channel: " + currentNumber);
//        if (index >= 0) {
//            String nextNumber = (index < channelNumbers.size() - 1) ? channelNumbers.get(index + 1) : channelNumbers.get(0);
//            String nextUrl = "http://localhost:5001/player/" + nextNumber;
//            Log.d(TAG, "Navigating to Next Channel: " + nextUrl);
//            webView.loadUrl(nextUrl);
//        } else {
//            Log.d(TAG, "No next channel available.");
//        }
//    }
//
//    private void navigateToPreviousChannel() {
//        Log.d(TAG, "Channel Numbers: navigateToPreviousChannel" + channelNumbers);
//        if (channelNumbers == null || channelNumbers.isEmpty()) {
//            Log.d(TAG, "No channel numbers available.");
//            return;
//        }
//        String currentUrl = webView.getUrl();
//        String currentNumber = currentUrl.substring(currentUrl.lastIndexOf('/') + 1);
//        int index = channelNumbers.indexOf(currentNumber);
//        Log.d(TAG, "Current Channel: " + currentNumber);
//        if (index > 0) {
//            String previousNumber = channelNumbers.get(index - 1);
//            String previousUrl = "http://localhost:5001/player/" + previousNumber;
//            Log.d(TAG, "Navigating to Previous Channel: " + previousUrl);
//            webView.loadUrl(previousUrl);
//        } else if (index == 0) {
//            String previousNumber = channelNumbers.get(channelNumbers.size() - 1);
//            String previousUrl = "http://localhost:5001/player/" + previousNumber;
//            Log.d(TAG, "Navigating to Previous Channel: " + previousUrl);
//            webView.loadUrl(previousUrl);
//        } else {
//            Log.d(TAG, "No previous channel available.");
//        }
//    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack(); // Navigate back in the WebView history
        } else {
            super.onBackPressed(); // Finish the activity if no history
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("/play/")) {
                // Clear previous channel numbers only if not already on a /player/ page
                String newUrl = url.replace("/play/", "/player/");
                webView.loadUrl(newUrl);
//                Intent intent = new Intent(WebPlayerActivity.this, VideoActivity.class);
//                intent.putExtra("videoUrl", newUrl);
//                startActivity(intent);
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
                Log.d(TAG, "Playing: " + url);
                Log.d(TAG, "Channel Numbers player: " + channelNumbers);
                setFullScreenMode();
                view.loadUrl("javascript:(function() { " +
                    "var video = document.getElementsByTagName('video')[0]; " +
                    "if (video) { " +
                    "  video.style.width = '100vw'; " +  // Use viewport width
                    "  video.style.height = '100vh'; " + // Use viewport height
                    "  video.style.objectFit = 'contain'; " + // Scale the video while preserving aspect ratio
                    "  video.play(); " +
                    "} " +
                    "})()");
//            } else if (url.contains("/play/")) {
//                // Extract channels from the latest page
//                Log.d(TAG, "Got Play: " + url);
//                Log.d(TAG, "Channel Numbers play: " + channelNumbers);
//            } else if (url.contains(DEFAULT_URL)) {
//                // Clear the channel numbers list if not on a /play/ or /player/ page
//                Log.d(TAG, "List cleared " + url);
//                // Extract channels from the latest page
//                extractChannelNumbers();
            }
        }
    }
}
