package com.termux;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class LoginErrorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showAlert();

    }

    private void showAlert() {
        new AlertDialog.Builder(this,R.style.CustomAlertDialogTheme)
            .setTitle("🔐 Login")
            .setMessage("An error occurred while checking login.\nDo you want to log in again?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    sky_login();
                    finish();
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss(); // Dismiss the dialog
                    finish();

                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    finish(); // Handle canceling of the dialog

                }
            })
            .show();

    }

    private void sky_login() {
        Intent intent = new Intent(LoginErrorActivity.this, LoginActivity2.class);
        startActivity(intent);
    }

//    private void iptvrunner2() {
//        SkySharedPref preferenceManager = new SkySharedPref(LoginErrorActivity.this);
//        String apppkg = preferenceManager.getKey("app_name");
//        String appclass = preferenceManager.getKey("app_launchactivity");
//
//        if (apppkg == null || "null".equals(apppkg)) {
//            Log.d("d","no iptv");
//        } else {
//            Intent intent = new Intent();
//            intent.setComponent(new ComponentName(apppkg, appclass));
//            startActivity(intent);
//        }
//    }
}
