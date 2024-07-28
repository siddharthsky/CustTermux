package com.termux.setup;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.R;
import com.termux.SkyActionActivity;

public class StepThreeFragment extends Fragment {

    private WebView webView;
    private TextView loadingMessage;
    private String url;
    private static final String DEFAULT_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_3, container, false);

        Toast.makeText(getActivity(), "Login Service Error.", Toast.LENGTH_SHORT).show();

        // Initialize UI elements
        loadingMessage = view.findViewById(R.id.loading_message);
        webView = view.findViewById(R.id.webview);

        // Configure the WebView
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

        // Set URL and load
        url = DEFAULT_URL;
        loadUrl();

        return view;
    }

    private void loadUrl() {
        if (url != null && webView != null) {
            webView.loadUrl(url);
        }
    }

    private void setModal() {
        if (webView != null) {
            String jsCode = "document.getElementById('login_modal').showModal();" +
                "document.body.innerHTML = '<div id=\"login_modal\">' + document.getElementById('login_modal').outerHTML + '</div>';";
            webView.evaluateJavascript(jsCode, null);
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            // Hide loading message and show WebView
            if (loadingMessage != null) {
                loadingMessage.setVisibility(View.GONE);
            }
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
                setModal();
            }
        }
    }
}
