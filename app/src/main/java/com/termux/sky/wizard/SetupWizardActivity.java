package com.termux.sky.wizard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
    Button btnNext, btnBack, btnGrantStorage, btnGrantOverlay, btnGrantBattery;
        TextView statusStorage, statusOverlay, statusBattery;
    LinearLayout dots;

    int step = 0;
    int totalSteps = 5;


    ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_wizard_activity);

        flipper = findViewById(R.id.flipper);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        dots = findViewById(R.id.dots);

        btnGrantStorage = findViewById(R.id.btnGrantStorage);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        statusStorage = findViewById(R.id.txtStorageStatus);
        statusOverlay = findViewById(R.id.txtOverlayStatus);
        btnGrantBattery = findViewById(R.id.btnGrantBattery);
        statusBattery = findViewById(R.id.txtBatteryStatus);



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
        btnGrantBattery.setOnClickListener(v -> requestBatteryOptimization());

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

        if (hasOverlayPermission()) {
            TxUtils.showCustomToast(this, "Overlay already granted");
            updatePermissionStatus();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            TxUtils.showCustomToast(this, "Overlay permission is managed by the system on this Android version");
            return;
        }

        try {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please allow 'Display over other apps'");
            return;
        } catch (Exception e1) {
            // Failed. Likely an Android TV or custom ROM that doesn't support the specific package URI.
        }

        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please find CTx Engine and allow 'Display over other apps'");
            return;
        } catch (Exception e2) {
            // Failed. The device entirely lacks the dedicated overlay settings menu.
        }

        try {
            Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please enable 'Display over other apps' in Permissions/Advanced settings");
            return;
        } catch (Exception e3) {
            // Failed.
        }

        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please navigate to Apps -> Permissions and enable Overlay");
        } catch (Exception e4) {
            TxUtils.showCustomToast(this, "Overlay setting not available on this device");
        }
    }


    // BATTERY
    private boolean isBatteryOptimized() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return !pm.isIgnoringBatteryOptimizations(getPackageName());
            }
        }
        return false;
    }

    private void requestBatteryOptimization() {
        if (!isBatteryOptimized()) {
            TxUtils.showCustomToast(this, "Optimization already disabled");
            updatePermissionStatus();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            TxUtils.showCustomToast(this, "Not required on this Android version");
            return;
        }


        try {
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        } catch (Exception e1) {
            // Failed.
        }

        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please find CTx Engine and set to 'Don't Optimize' or 'Unrestricted'");
            return;
        } catch (Exception e2) {
            // Failed.
        }


        try {
            Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please find 'Energy' or 'Power' settings and unrestrict CTx Engine");
            return;
        } catch (Exception e3) {
            try {
                Intent intentSaver = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                startActivity(intentSaver);
                TxUtils.showCustomToast(this, "Check 'Energy Saver' settings to unrestrict CTx Engine");
                return;
            } catch (Exception e3b) {
                // Both TV Power fallbacks failed.
            }
        }

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please check 'Battery' or 'Energy' settings in this menu and set to Unrestricted");
            return;
        } catch (Exception e4) {
            // Failed.
        }

        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            TxUtils.showCustomToast(this, "Please navigate to Battery or Energy settings and remove restrictions");
        } catch (Exception e5) {
            TxUtils.showCustomToast(this, "Optimization settings not accessible on this device");
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

            flipper.setInAnimation(this, R.anim.slide_in_right);
            flipper.setOutAnimation(this, R.anim.slide_out_left);

            flipper.showNext();
            updateUI();
        } else {
            finishSetup();
        }
    }

    private void backStep() {
        if (step > 0) {
            step--;

            flipper.setInAnimation(this, R.anim.slide_in_left);
            flipper.setOutAnimation(this, R.anim.slide_out_right);

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
        getSharedPreferences("settings", MODE_PRIVATE)
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

        boolean isPreM = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
        boolean isTV = isAndroidTV();

        if (statusStorage != null && btnGrantStorage != null) {
            if (isPreM) {
                statusStorage.setText("✅ Granted (Default)");
                statusStorage.setTextColor(0xFF4ADE80);
                btnGrantStorage.setVisibility(View.GONE);
            } else {
                boolean granted = hasStoragePermission();
                statusStorage.setText(granted ? "✅ Granted" : "❌ Not Granted");
                statusStorage.setTextColor(granted ? 0xFF4ADE80 : 0xFFF87171);
                btnGrantStorage.setVisibility(granted ? View.GONE : View.VISIBLE);
            }
        }

        // 2. OVERLAY
        if (statusOverlay != null && btnGrantOverlay != null) {
            if (isPreM) {
                statusOverlay.setText("✅ Granted (System Managed)");
                statusOverlay.setTextColor(0xFF4ADE80);
                btnGrantOverlay.setVisibility(View.GONE);
            } else {
                boolean granted = hasOverlayPermission();

                if (!granted && isTV) {
                    statusOverlay.setText("⚠️ Check App Settings");
                    statusOverlay.setTextColor(0xFFFBBF24);
                } else {
                    statusOverlay.setText(granted ? "✅ Granted" : "❌ Not Granted");
                    statusOverlay.setTextColor(granted ? 0xFF4ADE80 : 0xFFF87171);
                }

                btnGrantOverlay.setVisibility(granted ? View.GONE : View.VISIBLE);
            }
        }

        if (statusBattery != null && btnGrantBattery != null) {
            if (isPreM) {
                statusBattery.setText("✅ Not Applicable (Android 5)");
                statusBattery.setTextColor(0xFF4ADE80); // Green
                btnGrantBattery.setVisibility(View.GONE);
            } else {
                boolean isIgnoring = !isBatteryOptimized();

                String typeLabel = isTV ? "Energy" : "Battery";

                if (isIgnoring) {
                    statusBattery.setText("✅ " + typeLabel + " Unrestricted");
                    statusBattery.setTextColor(0xFF4ADE80);
                } else {
                    if (isTV) {
                        statusBattery.setText("⚠️ " + typeLabel + " Restricted (Check Settings)");
                        statusBattery.setTextColor(0xFFFBBF24);
                    } else {
                        statusBattery.setText("❌ " + typeLabel + " Optimized (Restricted)");
                        statusBattery.setTextColor(0xFFF87171);
                    }
                }

                btnGrantBattery.setVisibility(isIgnoring ? View.GONE : View.VISIBLE);
            }
        }
    }

    private boolean isAndroidTV() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
