package com.termux;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.app.TermuxActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebPlayerActivity extends AppCompatActivity {

    private static final String TAG = "RIX";

    private WebView webView;
    private ProgressBar loadingSpinner;
    private List<String> channelNumbers;

    private String url;
    private String BASE_URL;
    private String CONFIGPART_URL;
    private String DEFAULT_URL;
    private String initURL;

    private String currentPlayId;
    private String currentLogoUrl;
    private String currentChannelName;

    private static final String RECENT_CHANNELS_KEY = "recent_channels";
    private final List<Channel> recentChannels = new ArrayList<>();

//    SkySharedPref preferenceManager = new SkySharedPref(WebPlayerActivity.this); // This is valid in an Activity


    private class Channel {
        String playId;
        String logoUrl;
        String channelName;

        Channel(String playId, String logoUrl, String channelName) {
            this.playId = playId;
            this.logoUrl = logoUrl;
            this.channelName = channelName;
        }
    }






    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_player);

        SkySharedPref preferenceManager = new SkySharedPref(this);
        BASE_URL = preferenceManager.getKey("isLocalPORT");
        String PORTx = preferenceManager.getKey("isLocalPORTonly");
        CONFIGPART_URL = preferenceManager.getKey("isWEBTVconfig");
        DEFAULT_URL = BASE_URL + CONFIGPART_URL;

        Log.d(TAG, "URL: " + DEFAULT_URL);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setFullScreenMode();

        initializeWebView();
        loadUrl();
//        loadRecentChannels();

    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        webView = findViewById(R.id.webview);
        loadingSpinner = findViewById(R.id.loading_spinner);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setMediaPlaybackRequiresUserGesture(false); // Allow autoplay
    }

    private void loadUrl() {
        if (DEFAULT_URL != null) {
            initURL = DEFAULT_URL;
            webView.loadUrl(DEFAULT_URL);
        }
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void extractChannelNumbers() {
        webView.evaluateJavascript("Array.from(document.querySelectorAll('.card')).map(card => card.getAttribute('href').match(/\\/play\\/(\\d+)/)[1])", result -> {
            if (result != null && !result.isEmpty()) {
                result = result.replace("[", "").replace("]", "").replace("\"", "");
                channelNumbers = Arrays.asList(result.split(","));
                Log.d(TAG, "Channel Numbers: " + channelNumbers);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && webView.getUrl() != null && webView.getUrl().contains("/player/")) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    navigateToNextChannel();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    navigateToPreviousChannel();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void navigateToNextChannel() {
        navigateChannel(1);
    }

    private void navigateToPreviousChannel() {
        navigateChannel(-1);
    }

    private void navigateChannel(int direction) {
        if (channelNumbers == null || channelNumbers.isEmpty()) {
            Log.d(TAG, "No channel numbers available.");
            return;
        }

        Log.d(TAG, "Total channels available: " + channelNumbers.size());

        String currentUrl = webView.getUrl();
        assert currentUrl != null;

        int queryIndex = currentUrl.indexOf('?');
        String currentNumber;

        if (queryIndex != -1) {
            currentNumber = currentUrl.substring(currentUrl.lastIndexOf('/') + 1, queryIndex);
        } else {
            currentNumber = currentUrl.substring(currentUrl.lastIndexOf('/') + 1);
        }

        int index = channelNumbers.indexOf(currentNumber);

        if (index >= 0) {
            int newIndex = (index + direction + channelNumbers.size()) % channelNumbers.size();
            String newNumber = channelNumbers.get(newIndex);

            String newUrl;
            if (queryIndex != -1) {
                newUrl = currentUrl.replace("/" + currentNumber + "?", "/" + newNumber + "?");
            } else {
                newUrl = currentUrl.replace("/" + currentNumber, "/" + newNumber);
            }

            Log.d(TAG, "Navigating to Channel: " + newUrl);
            webView.loadUrl(newUrl);
        } else {
            Log.d(TAG, "Current number not found in channel numbers.");
        }
    }

    private int playerUrlCount = 0;

    @Override
    public void onBackPressed() {
        if (webView != null) {
            String currentUrl = webView.getUrl();
            assert currentUrl != null;

            if (currentUrl.contains("/player/")) {
                playerUrlCount++;
                if (playerUrlCount >= 3) {
                    webView.loadUrl(initURL);
                } else {
                    webView.goBack();
                }
            } else if (webView.canGoBack()) {
                playerUrlCount++;
                if (playerUrlCount >= 6) {
                    finish();
                } else {
                    webView.goBack();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("/play/")) {
                // Save the current URL before navigating to the player
                initURL = webView.getUrl();

                // Extract the play ID from the URL
//                String playId = url.substring(url.lastIndexOf("/play/") + 6); // Extracting play ID

                String playId = url.matches(".*/play/([^/]+).*") ? url.replaceAll(".*/play/([^/]+).*", "$1") : null;


                Log.d("WB", String.valueOf(playId));

                // Use JavaScript to extract the channel logo and name
                view.evaluateJavascript(
                    "(function() { " +
                        "try { " +
                        "    var channelCard = document.querySelector('a[href*=\"/play/" + playId + "\"]'); " +
                        "    if (channelCard) { " +
                        "        var logoElement = channelCard.querySelector('img'); " +
                        "        var nameElement = channelCard.querySelector('span'); " +
                        "        var logoUrl = logoElement ? logoElement.getAttribute('src') : null; " +
                        "        var channelName = nameElement ? nameElement.innerText : null; " +
                        "        return JSON.stringify({playId: '" + playId + "', logoUrl: logoUrl, channelName: channelName}); " +
                        "    } else { " +
                        "        return null; " +
                        "    } " +
                        "} catch (error) { " +
                        "    return null; " +
                        "} " +
                        "})();", result -> {
                        if (result != null && !result.equals("null")) {
                            try {
                                // Remove any extra quotes surrounding the JSON result
                                String jsonString = result.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                                JSONObject jsonResult = new JSONObject(jsonString);
                                currentPlayId = jsonResult.getString("playId");
                                currentLogoUrl = jsonResult.getString("logoUrl");
                                currentChannelName = jsonResult.getString("channelName");

                                Log.d(TAG, "Channel Clicked: " + currentChannelName + " (Play ID: " + currentPlayId + ")");
                                saveRecentChannel(currentPlayId, currentLogoUrl, currentChannelName);
                            } catch (JSONException e) {
                                Log.d(TAG, "JSON parsing error: " + e.getMessage());
                            }
                        } else {
                            Log.d(TAG, "No channel data extracted.");
                        }
                    });



                // Replace "/play/" with "/player/" to load the player view
                String newUrl = url.replace("/play/", "/player/");
                Log.d(TAG, "Loading new player URL: " + newUrl);
                webView.loadUrl(newUrl);

//                Intent intent = new Intent(WebPlayerActivity.this, KickStartWEB.class);
//                startActivity(intent);


                return true; // URL has been overridden
            } else if (url.contains(initURL)) {
                initURL = url;
                return false;
            }
            return false; // URL has not been overridden
        }


        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            loadingSpinner.setVisibility(View.GONE);
            if (url.contains("/player/")) {
                setFullScreenMode();
                view.loadUrl("javascript:(function() { " +
                    "var video = document.getElementsByTagName('video')[0]; " +
                    "if (video) { " +
                    "  video.style.width = '100vw'; " +
                    "  video.style.height = '100vh'; " +
                    "  video.style.objectFit = 'contain'; " +
                    "  video.play(); " +
                    "} " +
                    "})()");
//            } else if (url.equals(DEFAULT_URL)) {
//                moveSearchInput(view);
//                extractChannelNumbers();
            } else {
                moveSearchInput(view);
                extractChannelNumbers();
                loadRecentChannels();
//                injectTVChannel("GOOGLE TV", "0", "https://via.placeholder.com/100");
//                injectTVChannel("STAR PLUS3", "1433", "https://upload.wikimedia.org/wikipedia/en/d/d7/StarPlus_Logo.png");

            }
        }

        private void moveSearchInput(WebView view) {
            view.loadUrl("javascript:(function() { " +
                "var searchButton = document.getElementById('portexe-search-button'); " +
                "var searchInput = document.getElementById('portexe-search-input'); " +
                "if (searchButton && searchInput) { " +
                "  searchButton.parentNode.insertBefore(searchInput, searchButton.nextSibling); " +
                "} " +
                "})()");
        }
    }

    private void injectTVChannel(String channelName, String playId, String logoUrl) {

        String jsCode = "javascript:(function() {" +
            "console.log('Starting channel injection process...');" +

            "var channelGrid = document.querySelector('.grid.grid-cols-2');" +
            "console.log('Attempting to find the channel grid:', channelGrid);" +

            "if (channelGrid) {" +
            "  console.log('Channel grid found:', channelGrid);" +
            "  var existingChannel = document.querySelector('a[href=\"/play/" + playId + "\"]');" +
            "  console.log('Checking for existing channel with playId:', '" + playId + "');" +

            "  if (existingChannel) {" +
            "    console.log('Channel with playId ' + '" + playId + "' + ' already exists, skipping injection.');" +
            "  } else {" +
            "    console.log('Channel does not exist. Proceeding with channel injection...');" +
            "    var newChannel = document.createElement('a');" +
            "    newChannel.href = '/play/" + playId + "';" +
            "    newChannel.className = 'card border-2 border-gold shadow-lg hover:shadow-xl hover:bg-base-300 transition-all duration-200 ease-in-out scale-100 hover:scale-105';" +
            "    var cardContent = `<div class=\"flex flex-col items-center p-2 sm:p-4\">" +
            "      <img src=\"" + logoUrl + "\" loading=\"lazy\" alt=\"" + channelName + "\" class=\"h-14 w-14 sm:h-16 sm:w-16 md:h-18 md:w-18 lg:h-20 lg:w-20 rounded-full bg-gray-200\" />" +
            "      <span class=\"text-lg font-bold mt-2\">" + channelName + "</span>" +
            "      <div class=\"absolute top-2 right-2\">" +
            "        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"gold\" viewBox=\"0 -960 960 960\">" +
            "        <path d=\"M480-269 314-169q-11 7-23 6t-21-8q-9-7-14-17.5t-2-23.5l44-189-147-127q-10-9-12.5-20.5T140-571q4-11 12-18t22-9l194-17 75-178q5-12 15.5-18t21.5-6q11 0 21.5 6t15.5 18l75 178 194 17q14 2 22 9t12 18q4 11 1.5 22.5T809-528L662-401l44 189q3 13-2 23.5T690-171q-9 7-21 8t-23-6L480-269Z\"/>  " +
            "        </svg>" +
            "      </div>" +
            "    </div>`;" +

            "    newChannel.innerHTML = cardContent;" +
            "    channelGrid.insertBefore(newChannel, channelGrid.firstChild);" +
            "    console.log('Successfully injected new channel:', newChannel);" +
            "  }" +
            "} else {" +
            "  console.log('Failed to find the channel grid. Injection skipped.');" +
            "}" +
            "})()";

        webView.evaluateJavascript(jsCode, null);
        Log.d("ChannelInjection", "JavaScript code injected into the WebView.");
    }









    private void saveRecentChannel(String playId, String logoUrl, String channelName) {
        SkySharedPref preferenceManager = new SkySharedPref(this);

        // Load existing recent channels from preferenceManager
        String recentChannelsJson = preferenceManager.getKey(RECENT_CHANNELS_KEY);
        if (recentChannelsJson != null && !recentChannelsJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(recentChannelsJson);
                recentChannels.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    recentChannels.add(new Channel(
                        jsonObject.getString("playId"),
                        jsonObject.getString("logoUrl"),
                        jsonObject.getString("channelName")
                    ));
                }
            } catch (JSONException e) {
                Log.d(TAG, "Error loading recent channels: " + e);
            }
        }

        // Check if the channel with the given playId already exists and remove it if found
        recentChannels.removeIf(channel -> channel.channelName.equals(channelName));
        recentChannels.add(0, new Channel(playId, logoUrl, channelName));

        // Keep only the latest 5 channels
        if (recentChannels.size() > 5) {
            recentChannels.remove(recentChannels.size() - 1);
        }

        // Convert updated list to JSON array and save back to preferences
        JSONArray jsonArray = new JSONArray();
        for (Channel channel : recentChannels) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("playId", channel.playId);
                jsonObject.put("logoUrl", channel.logoUrl);
                jsonObject.put("channelName", channel.channelName);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                Log.d(TAG, String.valueOf(e));
            }
        }

        preferenceManager.setKey(RECENT_CHANNELS_KEY, jsonArray.toString());
    }


    private void loadRecentChannels() {
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String channelData = preferenceManager.getKey(RECENT_CHANNELS_KEY);

        Log.d(TAG, "Channel Data from Shared Preferences: " + channelData);

        if (channelData != null && !channelData.isEmpty()) {

            recentChannels.clear(); // Clear existing list
            Log.d("RIX", "I WAS HERE");
            try {
                JSONArray jsonArray = new JSONArray(channelData);

//                // Start iterating
//                for (int i = 0; i < jsonArray.length(); i++) {
                
                // Iterate in reverse
                for (int i = jsonArray.length() - 1; i >= 0; i--) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String playId = jsonObject.getString("playId");
                    String logoUrl = jsonObject.getString("logoUrl");
                    String channelName = jsonObject.getString("channelName");

                    // Log each channel's details to confirm parsing
                    Log.d(TAG, "Parsed Channel - Play ID: " + playId + ", Logo URL: " + logoUrl + ", Name: " + channelName);

                    recentChannels.add(new Channel(playId, logoUrl, channelName));
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error in loadRecentChannels: " + e.getMessage());
            }
        }

        // Inject each recent channel into the UI
        for (Channel channel : recentChannels) {
            String formattedPlayId;
            if (channel.playId != null && !channel.playId.endsWith("//")) {
                formattedPlayId = channel.playId + "//";
            } else {
                formattedPlayId = channel.playId;
            }

            Log.d(TAG, "Injecting Channel into WebView - Name: " + channel.channelName + ", Play ID: " + formattedPlayId);

            if (formattedPlayId != null) {
                injectTVChannel(channel.channelName, formattedPlayId, channel.logoUrl);
            }
        }


    }





}
