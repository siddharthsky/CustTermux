package com.termux;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Utils {
    public static void showCustomToast(Context context, String message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast2, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
//    public static void changeIconToSecond(Context context) {
//        // disables the first icon
//        PackageManager packageManager = context.getPackageManager();
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivity"),
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP);
//
//        // enables the second icon
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivityAlias"),
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP);
//    }
//
//    public static void changeIconTOFirst(Context context) {
//        // disables the first icon
//        PackageManager packageManager = context.getPackageManager();
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivity"),
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP);
//
//        // enables the second icon
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivityAlias"),
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP);
//    }

    public static void changeIconToSecond(Context context) {
        // disables the first icon
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStart"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);

        // enables the second icon
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStartAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);
    }

    public static void changeIconTOFirst(Context context) {
        // disables the first icon
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStart"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);

        // enables the second icon
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStartAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }

    public static void sky_rerun(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            Log.d("SkyLog", "Out Of The App");
            context.startActivity(intent);
            System.exit(0);
        }
    }

    public static void sky_update(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_reinstall(Context context) {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        preferenceManager.setKey("isServerSetupDone", null);
        preferenceManager.setKey("server_setup_isLoginCheck", "Yes");
        preferenceManager.setKey("server_setup_isAutoboot", "No");
        preferenceManager.setKey("server_setup_isLocal", "No");
        preferenceManager.setKey("app_name", "null");
        preferenceManager.setKey("isExit", "noExit");
        preferenceManager.setKey("server_setup_isEPG", "Yes");
        preferenceManager.setKey("server_setup_isGenericBanner", "No");
        preferenceManager.setKey("server_setup_isSSH", "No");

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"reinstall2"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void lake_alert_confirmation(Context context) {
        // Create an AlertDialog Builder
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        // Set the message and the title
        builder.setMessage("Do you want to proceed?\n[Note: To exit press back button, reopen]")
            .setTitle("Confirmation");

        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sky_terminal(context);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        android.app.AlertDialog dialog = builder.create();

        // Show the AlertDialog
        dialog.show();
    }

    private static void sky_terminal(Context context) {
        TerminalView terminalView = ((Activity) context).findViewById(R.id.terminal_view);

        // Change focusable properties
        terminalView.setFocusableInTouchMode(true);
        terminalView.setFocusable(true);
    }


    public static void sky_epg_on(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"epg_on"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_epg_off(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"epg_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void showAlertbox_playlist(Context context, DialogInterface.OnClickListener localListener,DialogInterface.OnClickListener publicListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle("Download playlist")
            .setMessage("Select whether this playlist is for local use only or for other device in network (e.g., TV). \nIf using for other devices, ensure this device has a fixed IP.")
            .setPositiveButton("This device only", localListener)
            .setNegativeButton("For other devices", publicListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showAlertbox(Context context,String title ,String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void sky_ssh_on(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"ssh_on"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_ssh_off(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"ssh_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }


    public static void updateCustTermux(Context context) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/siddharthsky/CustTermux-JioTVGo/releases"));
        context.startActivity(browserIntent);
    }
}


