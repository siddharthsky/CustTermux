package com.termux.sky;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.termux.sky.wizard.AutoAppRedirectDialog;

public class TxBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) return;

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        String mode = SkySharedPref.getAutoStartMode(context);

        SharedPreferences prefs = context.getSharedPreferences("settings", MODE_PRIVATE);

        boolean autoStart = prefs.getBoolean("auto_start", false);
        boolean boot_start_app = prefs.getBoolean("boot_start_app", false);

        String pkg = prefs.getString("pkg", null);
        String cls = prefs.getString("activity", null);


        Log.d("BootReceiver", "AutoStart mode: " + mode);

        switch (mode) {

            case "background":
                startTermuxService(context, true, autoStart, boot_start_app, pkg, cls);
                break;

            case "foreground":
                startTermuxActivity(context);
                break;

            case "disabled":
            default:
                Log.d("BootReceiver", "AutoStart disabled");
                break;
        }
    }

    private void startTermuxService(Context context, boolean background, boolean autoStart, boolean boot_start_app, String pkg, String cls) {

        Toast.makeText(context, "[CTx] Running in background.", Toast.LENGTH_LONG).show();

        Intent serviceIntent = new Intent();
        serviceIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        serviceIntent.setAction("com.termux.RUN_COMMAND");

        serviceIntent.putExtra("com.termux.RUN_COMMAND_PATH",
            "/data/data/com.termux/files/home/.skyutilz.sh");

        serviceIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
            new String[]{"--run", "boot"});

        serviceIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR",
            "/data/data/com.termux/files/home");

        serviceIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", background);
        serviceIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "1");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        if (autoStart) {
            if (boot_start_app) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(pkg, cls));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        } else {
            Log.d("BOOT","SKIP");
        }



    }

    private void startTermuxActivity(Context context) {
        Intent activityIntent = new Intent();
        activityIntent.setClassName("com.termux", "com.termux.app.TermuxActivity");
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }
}
