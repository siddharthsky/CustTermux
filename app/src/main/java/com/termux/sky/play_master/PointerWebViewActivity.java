package com.termux.sky.play_master;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;

public class PointerWebViewActivity extends AppCompatActivity {

    private FrameLayout rootLayout;
    private WebView webView;
    private ImageView cursorView;

    
    private boolean isVirtualMouseMode = true; 
    private float cursorX = 0f;
    private float cursorY = 0f;
    private final float CURSOR_SPEED = 15f; 

    
    private long[] clickTimes = new long[3];

    
    private final Handler fadeHandler = new Handler(Looper.getMainLooper());
    private final Runnable fadeRunnable = () -> {
        if (cursorView != null) {
            cursorView.animate().alpha(0f).setDuration(400).start();
        }
    };

    private int backCount = 0;
    private static final int MAX_BACK = 5;
    private String port_no = null;

    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    
    private static final String[] AD_HOSTS = {
        "googleads", "doubleclick.net", "adsystem", "adsafeprotected",
        "adnexus", "criteo", "taboola", "outbrain", "adform",
        "rubiconproject", "pubmatic", "advertising.com", "amazon-adsystem",
        "googlesyndication.com", "scorecardresearch", "analytics", "tracking"
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        } catch (Exception e) {
            Log.d("DRM_PLAYER_WEB", "SystemUI flags error");
        }

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        setupWebView();
        setupVirtualCursor();

        setContentView(rootLayout);
        hideSystemUI();

        
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                cursorX = rootLayout.getWidth() / 2f;
                cursorY = rootLayout.getHeight() / 2f;
                cursorView.setX(cursorX);
                cursorView.setY(cursorY);
                resetFadeTimer();
            }
        });

        String url = getIntent().getStringExtra("url");
        port_no = getIntent().getStringExtra("port");

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = new WebView(this);
        rootLayout.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null); 
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); 
        settings.setDatabaseEnabled(true);

        
        settings.setUseWideViewPort(true);       
        settings.setLoadWithOverviewMode(true);  
        settings.setSupportZoom(true);           
        settings.setBuiltInZoomControls(true);   
        settings.setDisplayZoomControls(false);  
        webView.setInitialScale(1);              

        settings.setMediaPlaybackRequiresUserGesture(false);

        
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {

            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;

                
                rootLayout.addView(customVideoView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

                
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customVideoView == null) return;

                
                customVideoView.setVisibility(View.GONE);
                rootLayout.removeView(customVideoView);
                customVideoView = null;
                webView.setVisibility(View.VISIBLE);

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }

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
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url = request.getUrl().toString();

                    
                    if (url.startsWith("blob:") || url.startsWith("data:")) {
                        return super.shouldInterceptRequest(view, request);
                    }

                    
                    Map<String, String> headers = request.getRequestHeaders();
                    if (headers != null) {
                        String accept = headers.get("Accept");
                        String range = headers.get("Range");

                        if ((accept != null && (accept.contains("video/") || accept.contains("audio/"))) ||
                            range != null) {
                            return super.shouldInterceptRequest(view, request);
                        }
                    }

                    
                    String lowerUrl = url.toLowerCase();
                    for (String adHost : AD_HOSTS) {
                        if (lowerUrl.contains(adHost)) {
                            Log.d("AD_BLOCKER", "Blocked ad request: " + url);
                            return new WebResourceResponse("text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes()));
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.startsWith("blob:") || url.startsWith("data:")) {
                    return super.shouldInterceptRequest(view, url);
                }

                String lowerUrl = url.toLowerCase();
                for (String adHost : AD_HOSTS) {
                    if (lowerUrl.contains(adHost)) {
                        Log.d("AD_BLOCKER", "Blocked ad request: " + lowerUrl);
                        return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                view.evaluateJavascript(
                    "var adElements = document.querySelectorAll('.ad, .ads, .ad-container, [id^=google_ads_], iframe');" +
                        "for (var i = 0; i < adElements.length; i++) {" +
                        "  adElements[i].style.display = 'none';" +
                        "}", null);
            }
        });
    }

    private void setupVirtualCursor() {
        cursorView = new ImageView(this);
        cursorView.setImageResource(android.R.drawable.ic_menu_mylocation);
        cursorView.setColorFilter(Color.RED);
        cursorView.setVisibility(isVirtualMouseMode ? View.VISIBLE : View.GONE);

        
        cursorView.setClickable(false);
        cursorView.setFocusable(false);

        FrameLayout.LayoutParams cursorParams = new FrameLayout.LayoutParams(60, 60);
        rootLayout.addView(cursorView, cursorParams);
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
                assert path != null;
                if (path.contains("/live/")) {
                    path = path.replace("/live/", "/mpd/").replace("/mpd/mpd/", "/mpd/");
                    if (path.endsWith(".m3u8")) {
                        path = path.substring(0, path.length() - ".m3u8".length());
                    }
                }
            }
            return uri.buildUpon().path(path).build().toString();
        } catch (Exception e) {
            return url;
        }
    }

    private void resetFadeTimer() {
        if (!isVirtualMouseMode) return;
        cursorView.setAlpha(1f); 
        fadeHandler.removeCallbacks(fadeRunnable);
        fadeHandler.postDelayed(fadeRunnable, 3000); 
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;

        
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (isDown) {
                
                System.arraycopy(clickTimes, 1, clickTimes, 0, clickTimes.length - 1);
                clickTimes[clickTimes.length - 1] = SystemClock.uptimeMillis();

                
                if (clickTimes[clickTimes.length - 1] - clickTimes[0] < 1000) {
                    isVirtualMouseMode = !isVirtualMouseMode;
                    cursorView.setVisibility(isVirtualMouseMode ? View.VISIBLE : View.GONE);
                    Arrays.fill(clickTimes, 0); 
                    if (isVirtualMouseMode) resetFadeTimer();
                    return true;
                }

                if (isVirtualMouseMode) {
                    simulateClick(cursorX + (cursorView.getWidth() / 2f), cursorY + (cursorView.getHeight() / 2f));
                    resetFadeTimer();
                }
            }
            return isVirtualMouseMode || super.dispatchKeyEvent(event);
        }

        
        if (isVirtualMouseMode) {
            if (isDown) {
                boolean moved = false;
                float dx = 0;
                float dy = 0;

                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        dy = -CURSOR_SPEED;
                        moved = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        dy = CURSOR_SPEED;
                        moved = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        dx = -CURSOR_SPEED;
                        moved = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        dx = CURSOR_SPEED;
                        moved = true;
                        break;
                }

                if (moved) {
                    cursorX += dx;
                    cursorY += dy;

                    int scrollX = 0;
                    int scrollY = 0;
                    float edgeThreshold = 60f; 

                    
                    if (cursorX < edgeThreshold && dx < 0) {
                        scrollX = -(int)CURSOR_SPEED;
                    }
                    if (cursorX > rootLayout.getWidth() - cursorView.getWidth() - edgeThreshold && dx > 0) {
                        scrollX = (int)CURSOR_SPEED;
                    }
                    if (cursorY < edgeThreshold && dy < 0) {
                        scrollY = -(int)(CURSOR_SPEED * 1.5f); 
                    }
                    if (cursorY > rootLayout.getHeight() - cursorView.getHeight() - edgeThreshold && dy > 0) {
                        scrollY = (int)(CURSOR_SPEED * 1.5f); 
                    }

                    
                    if (scrollX != 0 || scrollY != 0) {
                        webView.scrollBy(scrollX, scrollY);
                    }

                    
                    if (cursorX < 0) {
                        cursorX = 0;
                    } else if (cursorX > rootLayout.getWidth() - cursorView.getWidth()) {
                        cursorX = rootLayout.getWidth() - cursorView.getWidth();
                    }

                    if (cursorY < 0) {
                        cursorY = 0;
                    } else if (cursorY > rootLayout.getHeight() - cursorView.getHeight()) {
                        cursorY = rootLayout.getHeight() - cursorView.getHeight();
                    }

                    cursorView.setX(cursorX);
                    cursorView.setY(cursorY);
                    resetFadeTimer();
                    return true; 
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void simulateClick(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, x, y, 0);

        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        upEvent.recycle();
    }

    @SuppressLint("MissingSuperCall")
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
    protected void onPause() {
        if (webView != null) {
            webView.evaluateJavascript("document.querySelectorAll('video, audio').forEach(media => media.pause());", null);
            webView.onPause();
            webView.pauseTimers();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        fadeHandler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.evaluateJavascript("document.querySelectorAll('video, audio').forEach(media => media.pause());", null);
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.onPause();
            webView.pauseTimers();
            webView.clearHistory();
            webView.clearCache(true);

            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }

            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
