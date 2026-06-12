package com.termux.sky;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import com.termux.R;

public class TxStartupChecker {

    private final Activity activity;

    private static final String PREF = "settings";
    private static final String KEY_OVERLAY_CANCEL = "overlay_cancel_count";
    private static final String KEY_STORAGE_CANCEL = "storage_cancel_count";
    private static final int MAX_CANCELS = 3;

    private AlertDialog dialog;

    public TxStartupChecker(Activity activity) {
        this.activity = activity;
    }

    public void startPermissionFlow() {
        SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
        boolean isSetupDone = prefs.getBoolean("setup_done", false);

        if (isSetupDone) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    checkOverlayPermission();
                }
            }, 1000);
        } else {
            Log.d("SkyLog", "Setup not done, skipping permission flow");
        }
    }

    private void applyCustomTheming(AlertDialog dialog) {
        dialog.show();
        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(android.graphics.Color.WHITE);
            posButton.setFocusable(true);
            posButton.requestFocus();
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(android.graphics.Color.WHITE);
            negButton.setFocusable(true);
        }
    }

    /* ================= OVERLAY (WIZARD STYLE) ================= */

    private void checkOverlayPermission() {
        if (hasOverlayPermission()) {
            checkStoragePermission();
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
        int cancelCount = prefs.getInt(KEY_OVERLAY_CANCEL, 0);

        if (cancelCount >= MAX_CANCELS) {
            checkStoragePermission();
            return;
        }

        showOverlayDialog(cancelCount);
    }

    private void showOverlayDialog(int cancelCount) {
        if (dialog != null && dialog.isShowing()) return;

        dialog = new AlertDialog.Builder(activity, R.style.GoldenFocusDialogTheme)
            .setTitle("Overlay Permission")
            .setMessage("Required to run terminal sessions properly.")
            .setCancelable(false)
            .setPositiveButton("Allow", (d, w) -> requestOverlay())
            .setNegativeButton("Cancel", (d, w) -> {
                SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
                prefs.edit().putInt(KEY_OVERLAY_CANCEL, cancelCount + 1).apply();
                checkStoragePermission();
            })
            .create();

        applyCustomTheming(dialog);
    }

    private void requestOverlay() {
        
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                activity.startActivity(intent);
            } catch (Exception e2) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                } catch (Exception e3) {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    activity.startActivity(intent);
                }
            }
        }
    }

    /* ================= STORAGE (WIZARD STYLE) ================= */

    private void checkStoragePermission() {
        if (hasStoragePermission()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
        int cancelCount = prefs.getInt(KEY_STORAGE_CANCEL, 0);

        if (cancelCount >= MAX_CANCELS) return;

        showStorageDialog(cancelCount);
    }

    private void showStorageDialog(int cancelCount) {
        if (dialog != null && dialog.isShowing()) return;

        dialog = new AlertDialog.Builder(activity, R.style.GoldenFocusDialogTheme)
            .setTitle("Storage Permission")
            .setMessage("Needed to access Termux files.")
            .setCancelable(false)
            .setPositiveButton("Allow", (d, w) -> requestStorage())
            .setNegativeButton("Cancel", (d, w) -> {
                SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
                prefs.edit().putInt(KEY_STORAGE_CANCEL, cancelCount + 1).apply();
            })
            .create();

        applyCustomTheming(dialog);
    }

    @SuppressLint("ObsoleteSdkInt")
    private void requestStorage() {

        
        if (isAndroidTV() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasStoragePermission()) {
                return;
            }
            activity.requestPermissions(
                new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                101
            );
            return;
        }
        

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (Environment.isExternalStorageManager()) {
                return;
            }

            Intent intent = new Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
                return;
            }

            intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
                return;
            }

            openAppSettings();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                activity.requestPermissions(
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    101);
            }
        }
    }

    /* ================= HELPERS ================= */

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            
            if (Environment.isExternalStorageManager()) {
                return true;
            }
            
            if (isAndroidTV()) {
                return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            }
            return false;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(activity);
        }
        return true;
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    private boolean isAndroidTV() {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    public void onResumeCheck() {
        startPermissionFlow();
    }
}
