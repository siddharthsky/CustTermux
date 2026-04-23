package com.termux.sky.txplayer;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.exoplayer2.ui.PlayerView;

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

                if (port_no.equals("5006") || port_no.equals("5007") ) {
                    Log.d("DRM_PLAYER_WEB", "Port 5006,5007 logic applied");

                    if (path.contains("/live/")) {
                        path = path.replace("/live/", "/mpd/");
                    }

                    if (path.endsWith(".m3u8")) {
                        path = path.substring(0, path.length() - ".m3u8".length());
                    }

                } else if (port_no.equals("8181")) {
                    Log.d("DRM_PLAYER_WEB", "Port 8181 logic applied");
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
}
