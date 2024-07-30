package com.termux;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity2 extends AppCompatActivity {

    private WebView webView;
    private TextView loadingMessage;
    private ProgressBar loadingSpinner;
    private String url;
    private static final String DEFAULT_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable the home button as an up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Create a LinearLayout to hold the WebView, ProgressBar, and TextViews
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        // Create and configure the instruction message TextView
        TextView instructionMessage = new TextView(this);
        instructionMessage.setText("After logging in please press back to exit");
        instructionMessage.setTextSize(16);
        instructionMessage.setGravity(Gravity.CENTER);
        instructionMessage.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        layout.addView(instructionMessage);

        // Create and configure the loading message TextView
        loadingMessage = new TextView(this);
        loadingMessage.setText("Loading...");
        loadingMessage.setTextSize(18);
        loadingMessage.setGravity(Gravity.CENTER);
        loadingMessage.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        layout.addView(loadingMessage);

        // Create and configure the loading spinner ProgressBar
        loadingSpinner = new ProgressBar(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.gravity = Gravity.CENTER;
        loadingSpinner.setLayoutParams(spinnerParams);
        layout.addView(loadingSpinner);

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
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webView.setVisibility(View.GONE);  // Hide WebView initially
        layout.addView(webView);

        setContentView(layout);

        // Start the flashing effect
        startFlashingEffect(instructionMessage);

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
            String jsCode = "document.getElementById('login_modal').showModal();";
            webView.evaluateJavascript(jsCode, null);
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            // Show loading message and spinner
            loadingMessage.setVisibility(View.VISIBLE);
            loadingSpinner.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Hide loading message and spinner, show WebView
            loadingMessage.setVisibility(View.GONE);
            loadingSpinner.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            setModal();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startFlashingEffect(TextView textView) {
        ObjectAnimator animator = ObjectAnimator.ofInt(textView, "textColor", Color.WHITE, Color.RED);
        animator.setDuration(750);
        animator.setEvaluator(new android.animation.ArgbEvaluator());
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();
    }
}
