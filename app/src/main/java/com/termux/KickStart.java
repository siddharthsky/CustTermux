package com.termux;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.termux.app.TermuxActivity;

public class KickStart extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(KickStart.this, "KickStart", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, TermuxActivity.class);
        startActivity(intent);
        finish();
    }
}
