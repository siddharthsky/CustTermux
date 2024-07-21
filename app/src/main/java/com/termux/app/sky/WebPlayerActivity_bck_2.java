package com.termux.app.sky;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class WebPlayerActivity_bck_2 extends AppCompatActivity {

    private WebView webView;
    private String url;
    private static final String DEFAULT_URL = "http://192.168.1.11:5001/";
    private boolean isFirstLoad = true; // Flag to check if it's the first load

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView = findViewById(R.id.webview);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
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

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        webView.onResume();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && !webView.getUrl().equals(DEFAULT_URL)) {
            webView.loadUrl(DEFAULT_URL);
        } else {
            super.onBackPressed();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("/play/")) {
                String newUrl = url.replace("/play/", "/player/");
                view.loadUrl(newUrl);
                return true; // URL has been overridden
            }
            return false; // URL has not been overridden
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (isFirstLoad) {
                // Reload to the default URL once
                view.loadUrl(DEFAULT_URL);
                isFirstLoad = false; // Set flag to false after first reload
                return; // Skip the rest of the code
            }

            // Autoplay video
            webView.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");

            // Delay focus on the first grid item
            webView.loadUrl("javascript:(function() { setTimeout(function() { document.querySelector('.grid a').focus(); }, 500); })()");
        }
    }
}
