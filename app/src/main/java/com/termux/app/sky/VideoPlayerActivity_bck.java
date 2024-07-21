package com.termux.app.sky;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;


public class VideoPlayerActivity_bck extends AppCompatActivity {

    private String videoUrl1 = "http://localhost:5001/player/153"; // Replace with your video URL
    private String videoUrl2 = "http://localhost:5001/player/153"; // Replace with your video URL
    private String videoUrl3 = "http://localhost:5001/player/153"; // Replace with your video URL


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tv);

    }
}
