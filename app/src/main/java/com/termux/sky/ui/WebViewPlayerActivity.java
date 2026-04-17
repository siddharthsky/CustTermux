package com.termux.sky.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewPlayerActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        String url = getIntent().getStringExtra("url");

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSaveFormData(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.flush();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                String finalUrl = rewriteUrl(url);

                // send to custom browser logic
                openInCustomTabs(WebViewPlayerActivity.this, finalUrl);

                // IMPORTANT: stop WebView from loading it
                return true;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();
            }
        });

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private void openInCustomTabs(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);

            // 1st priority: HiBrowser
            if (isAppInstalled(context, "com.hisense.odinbrowser")) {
                openWithPackage(context, uri, "com.hisense.odinbrowser");
                return;
            }

            // 2nd priority: TV Bro
            if (isAppInstalled(context, "com.phlox.tvwebbrowser")) {
                openWithPackage(context, uri, "com.phlox.tvwebbrowser");
                return;
            }

            // 3rd priority: Chrome
            if (isAppInstalled(context, "com.android.chrome")) {
                openWithPackage(context, uri, "com.android.chrome");
                return;
            }

            // Fallback: show install prompt
            showInstallDialog(context);

        } catch (Exception e) {
            e.printStackTrace();
            webView.loadUrl(url);
        }
    }

    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openWithPackage(Context context, Uri uri, String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            webView.loadUrl(uri.toString());
        }
    }

    private void showInstallDialog(Context context) {
        new AlertDialog.Builder(context)
            .setTitle("Browser Required")
            .setMessage("Please install either HiBrowser or TV Bro to open links properly.")
            .setPositiveButton("Install TV Bro", (d, w) -> openPlayStore(context, "com.phlox.tvwebbrowser"))
            .setNegativeButton("Install HiBrowser", (d, w) -> openPlayStore(context, "com.hisense.odinbrowser"))
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void openPlayStore(Context context, String packageName) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private String rewriteUrl(String url) {
        if (url == null) return null;

        try {
            Uri uri = Uri.parse(url);

            String path = uri.getPath();
            if (path != null && path.contains("/play/")) {
                path = path.replace("/play/", "/mpd/");
            }

            Uri newUri = uri.buildUpon()
                .path(path)
                .build();

            return newUri.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
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
}
