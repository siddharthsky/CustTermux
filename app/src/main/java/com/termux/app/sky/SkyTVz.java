package com.termux.app.sky;



import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import android.widget.VideoView;
import android.widget.MediaController;

import com.termux.R;


public class SkyTVz extends AppCompatActivity {



    private String call;

    private VideoView videoView;
    private String videoUrl = "http://localhost:5001/player/153"; // Replace with your video URL

    private String videoUrl2 = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termux_tv);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.termux.SKYTVUI")) {
                // Extract the phone number from the Intent
                String int_var = intent.getStringExtra("call");
                if (int_var != null && !int_var.isEmpty()) {
                    switch (int_var) {
                        case "start":
                            start_ui();
                            finish();
                            break;
                        default:
                            finish_ui();
                            finish();
                            break;
                    }
                }
            }
        }

    }

    private void finish_ui() {
        finish();
    }


    private void start_ui() {
        Log.d("d","Work of Art");

        videoView = findViewById(R.id.videoView);
        Uri uri = Uri.parse(videoUrl2);

        // Set the video URL and start the video
        videoView.setVideoURI(uri);
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
    }


}
