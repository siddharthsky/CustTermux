package com.termux.tv_ui;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.termux.R;


public class TVPlayer extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tv);
    }
}
