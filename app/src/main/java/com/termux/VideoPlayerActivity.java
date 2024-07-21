package com.termux;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class VideoPlayerActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tv);
    }
}
