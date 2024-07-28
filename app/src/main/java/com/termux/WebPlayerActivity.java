package com.termux;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private String url;
    private static final String DEFAULT_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

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

    private void setDarkTheme() {
        if (webView != null) {
            String jsCode = "document.getElementsByTagName('html')[0].setAttribute('data-theme', 'dark');" +
                "localStorage.setItem('theme', 'dark');";
            webView.evaluateJavascript(jsCode, null);
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    //    @Override
//    public void onBackPressed() {
//        if (webView != null && !webView.getUrl().equals(DEFAULT_URL)) {
//            webView.loadUrl(DEFAULT_URL);
//        } else {
//            super.onBackPressed();
//        }
//    }


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
            // Apply dark theme after the page finishes loading
            //setDarkTheme();
            // Autoplay video
            webView.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");
        }
    }
}
