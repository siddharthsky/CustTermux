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
import com.termux.SkySharedPref;

public class WebPlayerActivity_bck extends AppCompatActivity {

    private WebView webView;
    private int urlIndex = 1;
    private static String BASE_URL;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        // Initialize SkySharedPref and other member variables
        SkySharedPref preferenceManager = new SkySharedPref(this);
        BASE_URL = preferenceManager.getKey("isLocalPORT");

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView = findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // Allow autoplay

        webView.setWebViewClient(new WebViewClient() {
            // autoplay when finished loading via javascript injection
            public void onPageFinished(WebView view, String url) { webView.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()"); }
        });



        // Load the initial URL
        loadUrl();
    }

    private void loadUrl() {
        webView.loadUrl(BASE_URL);
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
}
