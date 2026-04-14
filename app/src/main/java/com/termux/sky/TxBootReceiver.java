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
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            boolean enabled = SkySharedPref.isAutoStartEnabled(context);

            if (enabled) {
                Log.d("BootReceiver", "Starting server in background");

                Toast.makeText(context, "[CTx] Running Server in Background", Toast.LENGTH_SHORT).show();

                Intent serviceIntent = new Intent();
                serviceIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
                serviceIntent.setAction("com.termux.RUN_COMMAND");

                serviceIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.init_run.sh");
                serviceIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                serviceIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
                serviceIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "1");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

            }

//            else {
//                Log.d("BootReceiver", "Starting server in foreground");
//                Toast.makeText(context, "[CTx] Running Server in Foreground", Toast.LENGTH_SHORT).show();
//                Intent activityIntent = new Intent(context, TermuxActivity.class);
//                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(activityIntent);
//            }

        }
    }
}
