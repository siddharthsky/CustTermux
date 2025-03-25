package com.termux;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;


public class WebViewDL extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 1;
    private WebView webView;
    private String pendingUrl;
    private String pendingFileName;
    private boolean isPendingBlob;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_dl);

        WebView webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new BlobHandler(), "AndroidBlobHandler");

        webView.setWebViewClient(new WebViewClient());
        webView.setDownloadListener(createDownloadListener(webView));

        String url = getIntent().getStringExtra("url");
        if (url != null) webView.loadUrl(url);
    }

    // Modified DownloadListener creation
    private DownloadListener createDownloadListener() {
        return (url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

            if (!fileName.toLowerCase().endsWith(".m3u")) {
                Toast.makeText(this, "Only .m3u files allowed", Toast.LENGTH_LONG).show();
                return;
            }

            this.pendingFileName = "Combined_Playlist.m3u"; // Force filename as in original
            this.pendingUrl = url;
            this.isPendingBlob = url.startsWith("blob:");

            checkStoragePermission();
        };
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
        } else {
            startDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                Toast.makeText(this,
                    "Storage permission required for downloads",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startDownload() {
        if (isPendingBlob) {
            handleBlobUrl(webView, pendingUrl, pendingFileName);
        } else {
            handleHttpUrl(pendingUrl, pendingFileName);
        }
    }

    private DownloadListener createDownloadListener(WebView webView) {
        return (url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

            if (!fileName.toLowerCase().endsWith(".m3u")) {
                Toast.makeText(this, "Only .m3u files allowed", Toast.LENGTH_LONG).show();
                return;
            }

            if (url.startsWith("blob:")) {
                handleBlobUrl(webView, url, "Combined_Playlist.m3u");
            } else {
                handleHttpUrl(url, "Combined_Playlist.m3u");
            }
        };
    }

    private void handleBlobUrl(WebView webView, String url, String fileName) {
        String jsCode = "(function(){var xhr=new XMLHttpRequest();xhr.open('GET','"+url+"',true);" +
            "xhr.responseType='blob';xhr.onload=function(){var reader=new FileReader();" +
            "reader.onloadend=function(){AndroidBlobHandler.saveBlobData(reader.result,'"+fileName+"');};" +
            "reader.readAsDataURL(xhr.response);};xhr.send();})()";
        webView.evaluateJavascript(jsCode, null);
    }

    private void handleHttpUrl(String url, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle(fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                long downloadId = dm.enqueue(request);
                registerDownloadReceiver(downloadId);
            }
        } catch (IllegalArgumentException e) {
            Log.e("WebViewDL", "Download error", e);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerDownloadReceiver(long downloadId) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    unregisterReceiver(this);
                    closeWebViewActivity();
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);
    }

    private void closeWebViewActivity() {
        finish();
    }

    class BlobHandler {
        @JavascriptInterface
        public void saveBlobData(String base64Data, String fileName) {
            try {
                byte[] decodedBytes = Base64.decode(base64Data.split(",")[1], Base64.DEFAULT);
                File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(decodedBytes);
                    runOnUiThread(() -> {
                        Toast.makeText(WebViewDL.this, "Downloaded: " + fileName, Toast.LENGTH_LONG).show();
                        closeWebViewActivity();
                    });
                }
            } catch (Exception e) {
                Log.e("BlobHandler", "File save error", e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webView);
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
