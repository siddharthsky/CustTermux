package com.termux.app;


import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.SkySharedPref;

public class AnotherActivityCast extends AppCompatActivity {

    private CastHelper castHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout._castdemo);

        castHelper = new CastHelper(this);

        // Example usage: Change media when a button is clicked
        Button changeMediaButton1 = findViewById(R.id.button1z);
        changeMediaButton1.setOnClickListener(v -> {
            // Provide the new media URL and title
            String newMediaUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4";
            String newTitle = "THE CHROMA CASTER";
            castHelper.loadMedia(castHelper.getCastSession(), newMediaUrl, newTitle);
        });

        Button changeMediaButton2 = findViewById(R.id.button2z);
        changeMediaButton2.setOnClickListener(v -> {
            // Provide the new media URL and title
            SkySharedPref preferenceManager = new SkySharedPref(this);
            String ipAddress = preferenceManager.getKey("server_setup_wifiip");
            String port = preferenceManager.getKey("isLocalPORTonly");
            String Channelid = "player/143";


            String F1URL = "http://" + ipAddress + ":" + port + "/" + Channelid;

            String newMediaUrl = F1URL;
            String newTitle = "THE M3U URL";
            castHelper.loadMedia(castHelper.getCastSession(), newMediaUrl, newTitle);
        });

        Button changeMediaButton3 = findViewById(R.id.button3z);
        changeMediaButton3.setOnClickListener(v -> {
            // Provide the new media URL and title
            String newMediaUrl = "https://www.youtube.com/watch?v=QPLy0vHEXSA";
            String newTitle = "THE YT WAY";
            castHelper.loadMedia(castHelper.getCastSession(), newMediaUrl, newTitle);
        });
    }
}
