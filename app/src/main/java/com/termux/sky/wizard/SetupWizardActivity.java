package com.termux.sky.wizard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class SetupWizardActivity extends AppCompatActivity {

    ViewFlipper flipper;
    Button btnNext, btnBack;
    LinearLayout dots;

    int step = 0;
    int totalSteps = 4;

    ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);

        flipper = findViewById(R.id.flipper);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        dots = findViewById(R.id.dots);

        setupDots();
        updateUI();

        // STORAGE PERMISSION
        storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    nextStep();
                }
            });

        btnNext.setOnClickListener(v -> handleNext());
        btnBack.setOnClickListener(v -> backStep());
    }

    // MAIN FLOW CONTROLLER
    private void handleNext() {

        if (step == 0) {
            nextStep();
            return;
        }

        if (step == 1) {
            requestStorage();
            return;
        }

        if (step == 2) {
            requestOverlay();
            return;
        }

        nextStep();
    }

    // STORAGE
    @SuppressLint("ObsoleteSdkInt")
    private void requestStorage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (android.os.Environment.isExternalStorageManager()) {
                nextStep();
                return;
            }

            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                nextStep();
                return;
            }

            requestPermissions(
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                101
            );

            return;
        }

        nextStep();
    }

    // OVERLAY
    private void requestOverlay() {

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        } else {
            nextStep();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (step == 2 && Settings.canDrawOverlays(this)) {
            nextStep();
        }
    }

    private void nextStep() {
        if (step < totalSteps - 1) {
            step++;
            flipper.showNext();
            updateUI();
        } else {
            finishSetup();
        }
    }

    private void backStep() {
        if (step > 0) {
            step--;
            flipper.showPrevious();
            updateUI();
        }
    }

    private void updateUI() {
        btnBack.setVisibility(step == 0 ? View.GONE : View.VISIBLE);
        btnNext.setText(step == totalSteps - 1 ? "Finish" : "Next");
        updateDots();
    }

    private void setupDots() {
        dots.removeAllViews();

        for (int i = 0; i < totalSteps; i++) {
            TextView dot = new TextView(this);
            dot.setText("•");
            dot.setTextSize(22);
            dot.setPadding(6, 0, 6, 0);
            dot.setTextColor(i == 0 ? 0xFFFFFFFF : 0x55FFFFFF);
            dots.addView(dot);
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.getChildCount(); i++) {
            TextView dot = (TextView) dots.getChildAt(i);
            dot.setTextColor(i == step ? 0xFFFFFFFF : 0x55FFFFFF);
        }
    }

    private void finishSetup() {
        getSharedPreferences("app", MODE_PRIVATE)
            .edit()
            .putBoolean("setup_done", true)
            .apply();

        finish();
    }
}
