package com.termux;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.termux.app.TermuxActivity;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SkySharedPref preferenceManager = new SkySharedPref(context);

            String isAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
            String isAutobootBG = preferenceManager.getKey("server_setup_isAutobootBG");

            if ("Yes".equals(isAutoboot)) {
                if ("Yes".equals(isAutobootBG)) {
                    Log.d("BootReceiver", "Starting server in background");

                    Toast.makeText(context, "[CTx] Running Server in Background", Toast.LENGTH_SHORT).show();

                    // Prepare the intent to run the command in background
                    Intent serviceIntent = new Intent();
                    serviceIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
                    serviceIntent.setAction("com.termux.RUN_COMMAND");

                    // Command details and options
                    serviceIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                    serviceIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"TheShowRunner2_nologin"});
                    serviceIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                    serviceIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
                    serviceIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "1");

                    // Start service based on Android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } else {
                    // If not background, launch in the foreground
                    Log.d("BootReceiver", "Starting server in foreground");

                    Toast.makeText(context, "[CTx] Running Server in Foreground", Toast.LENGTH_SHORT).show();

                    // Launch Termux activity in the foreground
                    Intent activityIntent = new Intent(context, TermuxActivity.class);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(activityIntent);
                }
            }
        }
    }
}
