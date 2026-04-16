package com.termux.sky;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.termux.app.TermuxActivity;
import com.termux.sky.ui.WebViewLoginActivity;
import com.termux.sky.wizard.SetupWizardActivity;

public class TxStartupChecker {

    private final Activity activity;

    private static final String PREF = "sky_permission_pref";
    private static final String KEY_OVERLAY_CANCEL = "overlay_cancel_count";
    private static final String KEY_STORAGE_CANCEL = "storage_cancel_count";
    private static final int MAX_CANCELS = 3;

    private AlertDialog dialog;

    public TxStartupChecker(Activity activity) {
        this.activity = activity;
    }

    /* START FLOW */
    public void startPermissionFlow() {

//        Intent intent = new Intent(activity, WebViewPlayerActivity.class);
//        intent.putExtra("url", "http://localhost:5350/play/143");
//        activity.startActivity(intent);

        SharedPreferences prefs = activity.getSharedPreferences("app", MODE_PRIVATE);
        boolean isSetupDone = prefs.getBoolean("setup_done", false);

        if (isSetupDone) {
            checkOverlayPermission();
        } else {
            Log.d("SkyLog", "Setup not done, skipping permission flow");

        }
    }

    /* ================= OVERLAY ================= */

    private void checkOverlayPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            checkStoragePermission();
            return;
        }

        if (Settings.canDrawOverlays(activity)) {
            Log.d("SkyLog", "Overlay granted");
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

        dialog = new AlertDialog.Builder(activity)
            .setTitle("Overlay Permission Required")
            .setMessage("Required to run terminal sessions properly.")
            .setCancelable(false)

            .setPositiveButton("Allow", (d, w) -> {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName())
                );
                activity.startActivity(intent);
            })

            .setNegativeButton("Cancel", (d, w) -> {
                SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
                prefs.edit().putInt(KEY_OVERLAY_CANCEL, cancelCount + 1).apply();
                checkStoragePermission();
            })
            .create();

        dialog.show();
    }

    /* ================= STORAGE ================= */

    private void checkStoragePermission() {

        SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
        int cancelCount = prefs.getInt(KEY_STORAGE_CANCEL, 0);

        if (cancelCount >= MAX_CANCELS) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (Environment.isExternalStorageManager()) {
                Log.d("SkyLog", "Storage granted");
                return;
            }

            showStorageDialog(cancelCount);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                return;
            }

            showStorageDialog(cancelCount);
        }
    }

    private void showStorageDialog(int cancelCount) {

        if (dialog != null && dialog.isShowing()) return;

        dialog = new AlertDialog.Builder(activity)
            .setTitle("Storage Permission Required")
            .setMessage("Needed to access Termux files.")
            .setCancelable(false)

            .setPositiveButton("Allow", (d, w) -> {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivity(intent);
                    }
                } else {
                    activity.requestPermissions(
                        new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        101
                    );
                }
            })

            .setNegativeButton("Cancel", (d, w) -> {
                SharedPreferences prefs = activity.getSharedPreferences(PREF, MODE_PRIVATE);
                prefs.edit().putInt(KEY_STORAGE_CANCEL, cancelCount + 1).apply();
            })
            .create();

        dialog.show();
    }

    /* ================= RETURN FROM SETTINGS ================= */

    public void onResumeCheck() {
        startPermissionFlow();
    }
}
