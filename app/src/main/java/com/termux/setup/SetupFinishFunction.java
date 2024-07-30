package com.termux.setup;

import android.content.Context;
import android.content.Intent;

public class SetupFinishFunction {

    private Context context;


    public SetupFinishFunction(Context context) {
        this.context = context;
    }

    public static void XReStartTermux(Context context) {
        // Start the Termux service
        Intent termuxServiceStartIntent = new Intent();
        termuxServiceStartIntent.setClassName("com.termux", "com.termux.app.TermuxService");
        termuxServiceStartIntent.setAction("com.termux.service_execute");
        context.startService(termuxServiceStartIntent);
    }


    public static void XStartTermux(Context context) {
        // Start the Termux service
        Intent termuxServiceStartIntent = new Intent();
        termuxServiceStartIntent.setClassName("com.termux", "com.termux.app.TermuxService");
        termuxServiceStartIntent.setAction("com.termux.service_execute");
        context.startService(termuxServiceStartIntent);
    }

    public void DownloadPlaylist() {

    }

    public void onPause() {

    }

}
