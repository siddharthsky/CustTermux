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
            //preferenceManager.setKey("server_setup_isAutobootBG","Yes");
            String isAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
            String isAutobootBG = preferenceManager.getKey("server_setup_isAutobootBG");
            if (isAutoboot != null && isAutoboot.equals("Yes")) {
                if (isAutobootBG != null && isAutobootBG.equals("Yes")) {
                    Log.d("DIX2","BG service");
                    Toast.makeText(context, "[CTx] Running Server in Background", Toast.LENGTH_SHORT).show();
                    Intent intentC = new Intent();
                    intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                    intentC.setAction("com.termux.RUN_COMMAND");
                    intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                    intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"TheShowRunner2_nologin"});
                    intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                    intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
                    intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "1");
                    //context.startService(intentC);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intentC);
                    } else {
                        context.startService(new Intent(intentC));
                    }
                } else{
                    Log.d("DIX2","NOT BG service");
                    Toast.makeText(context, "[CTx] Running Server in Foreground", Toast.LENGTH_SHORT).show();
                    Intent activityIntent = new Intent(context, TermuxActivity.class);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(activityIntent);
                }
            }
        }
    }
}
