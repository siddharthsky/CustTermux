package com.termux.sky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class TxBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) return;

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        String mode = SkySharedPref.getAutoStartMode(context);

        Log.d("BootReceiver", "AutoStart mode: " + mode);

        switch (mode) {

            case "background":
                startTermuxService(context, true);
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

    private void startTermuxService(Context context, boolean background) {

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
    }

    private void startTermuxActivity(Context context) {
        Intent activityIntent = new Intent();
        activityIntent.setClassName("com.termux", "com.termux.app.TermuxActivity");
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }
}
