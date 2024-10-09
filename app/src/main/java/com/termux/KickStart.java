package com.termux;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.app.TermuxActivity;

public class KickStart extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the TermuxActivity and finish KickStart to remove it from the back stack
        Intent intent = new Intent(KickStart.this, TermuxActivity.class);
        startActivity(intent);
        finish();
    }
}
