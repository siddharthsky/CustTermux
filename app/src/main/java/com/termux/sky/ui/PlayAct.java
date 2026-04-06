package com.termux.sky.ui;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class PlayAct extends AppCompatActivity {

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        hideSystemUI();

        setContentView(R.layout.activity_new);

        videoView = findViewById(R.id.videoView);

        // 🔥 Built-in controls (like ExoPlayer UI)
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        String videoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";

        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(videoUrl));

        // ✅ Autoplay
        videoView.setOnPreparedListener(mp -> {
            mp.start();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(); // re-apply
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
