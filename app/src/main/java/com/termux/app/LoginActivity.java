package com.termux;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private WebView webView;
    private TextView loadingMessage;
    private String url;
    private static final String DEFAULT_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a LinearLayout to hold the WebView and TextView
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        // Create and configure the loading message TextView
        loadingMessage = new TextView(this);
        loadingMessage.setText("Loading...");
        loadingMessage.setTextSize(18);
        loadingMessage.setGravity(Gravity.CENTER);
        loadingMessage.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        layout.addView(loadingMessage);

        // Create and configure the WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        layout.addView(webView);

        setContentView(layout);

        url = DEFAULT_URL;
        // Load the initial URL
        loadUrl();
    }

    private void loadUrl() {
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private void setModal() {
        if (webView != null) {
            String jsCode = "document.getElementById('login_modal').showModal();"+
                "document.body.innerHTML = '<div id=\"login_modal\">' + document.getElementById('login_modal').outerHTML + '</div>';";
            webView.evaluateJavascript(jsCode, null);
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            // Hide loading message and show WebView
            loadingMessage.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            setModal();
        }

        }
    }


