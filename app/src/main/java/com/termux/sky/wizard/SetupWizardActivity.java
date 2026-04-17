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
import com.termux.sky.TxUtils;

public class SetupWizardActivity extends AppCompatActivity {

    ViewFlipper flipper;
    Button btnNext, btnBack, btnGrantStorage, btnGrantOverlay;
        TextView statusStorage, statusOverlay;
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

        btnGrantStorage = findViewById(R.id.btnGrantStorage);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        statusStorage = findViewById(R.id.txtStorageStatus);
        statusOverlay = findViewById(R.id.txtOverlayStatus);



        setupDots();
        updateUI();

        // STORAGE PERMISSION
        storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    nextStep();
                }
            });

        btnGrantStorage.setOnClickListener(v -> requestStorage());
        btnGrantOverlay.setOnClickListener(v -> requestOverlay());

        btnNext.setOnClickListener(v -> handleNext());
        btnBack.setOnClickListener(v -> backStep());
    }

    // MAIN FLOW CONTROLLER
    private void handleNext() {
        nextStep();
    }

    // STORAGE
    @SuppressLint("ObsoleteSdkInt")
    private void requestStorage() {

        // Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (hasStoragePermission()) {
                TxUtils.showCustomToast(this, "Storage already granted");
                updatePermissionStatus();
                return;
            }

            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception ex) {
                    openAppSettings(); // TV fallback
                }
            }
            return;
        }

        // Android 6–10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (hasStoragePermission()) {
                TxUtils.showCustomToast(this, "Storage already granted");
                updatePermissionStatus();
                return;
            }

            try {
                requestPermissions(
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    101
                );
            } catch (Exception e) {
                openAppSettings(); // TV fallback
            }
            return;
        }

        // Android 5 and below
        TxUtils.showCustomToast(this, "Storage available by default");
    }

    // OVERLAY
    private void requestOverlay() {

        // Already granted
        if (hasOverlayPermission()) {
            TxUtils.showCustomToast(this, "Overlay already granted");
            updatePermissionStatus();
            return;
        }

        // Try normal flow (phones/tablets)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback (TV / Fire TV)
        try {
            openAppSettings();
            TxUtils.showCustomToast(this, "Enable 'Display over apps' if available");
        } catch (Exception e) {
            TxUtils.showCustomToast(this, "Overlay setting not available on this device");
        }
    }



    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePermissionStatus();

//        if (step == 2 && Settings.canDrawOverlays(this)) {
//            nextStep();
//        }
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

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }


    private void updatePermissionStatus() {

        if (statusStorage != null) {
            boolean granted = hasStoragePermission();
            statusStorage.setText(granted ? "✅ Granted" : "❌ Not Granted");
            statusStorage.setTextColor(granted ? 0xFF4ADE80 : 0xFFF87171);
            btnGrantStorage.setVisibility(hasStoragePermission() ? View.GONE : View.VISIBLE);
        }

        if (statusOverlay != null) {

            boolean granted = hasOverlayPermission();

            if (!granted && isAndroidTV()) {
                statusOverlay.setText("⚠️ Try enabling in App Settings");
                statusOverlay.setTextColor(0xFFFBBF24);
            } else {
                statusOverlay.setText(granted ? "✅ Granted" : "❌ Not Granted");
                statusOverlay.setTextColor(granted ? 0xFF4ADE80 : 0xFFF87171);
            }

            btnGrantOverlay.setVisibility(granted ? View.GONE : View.VISIBLE);
        }
    }

    private boolean isAndroidTV() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
