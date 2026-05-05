package com.termux.sky;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class TermuxController {

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int rerunCount = 0;
    private static final int MAX_RERUN_COUNT = 2;

    public TermuxController(Activity activity) {
        this.activity = activity;
    }

    private void delay(long ms, Runnable task) {
        handler.postDelayed(task, ms);
    }

    public void killProcess(String processPath) {
        Intent pkillIntent = new Intent();
        pkillIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        pkillIntent.setAction("com.termux.RUN_COMMAND");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_PATH",
            "/data/data/com.termux/files/usr/bin/pkill");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
            new String[]{"-f", processPath});
        pkillIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR",
            "/data/data/com.termux/files/home");
        pkillIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        pkillIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        activity.startService(pkillIntent);
    }

    public void stop() {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.TermuxService");
        intent.setAction("com.termux.service_stop");
        activity.startService(intent);
    }

    public void start() {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.TermuxService");
        intent.setAction("com.termux.service_execute");
        activity.startService(intent);
    }

    public void restartService(String processPath) {
        killProcess(processPath);

        delay(100, () -> {
            stop();

            delay(500, this::start);
        });
    }

    public void rerunCustTermux() {
        Toast.makeText(activity, "Re-Running CustTermux", Toast.LENGTH_SHORT).show();

        start();

        rerunCount++;

        if (rerunCount >= MAX_RERUN_COUNT) {
            restartApp();
            rerunCount = 0;
        }
    }

    public void restartApp() {
        restartApp(true);
    }

    public void restartApp(Boolean toast) {
        if (toast) {
            Toast.makeText(activity, "Restarting after 2 reruns", Toast.LENGTH_SHORT).show();
        }

        Intent intent = activity.getPackageManager()
            .getLaunchIntentForPackage(activity.getPackageName());

        if (intent != null) {
            activity.finish();
            Log.d("SkyLog", "Out Of The App");
            activity.startActivity(intent);
            System.exit(0);
        }
    }

    public void exitApp() {
        activity.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }


}
