package com.termux;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.termux.app.TermuxActivity;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SkySharedPref preferenceManager = new SkySharedPref(context);
            String isAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
            if (isAutoboot != null && isAutoboot.equals("Yes")) {
                Intent activityIntent = new Intent(context, TermuxActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        }
    }
}
