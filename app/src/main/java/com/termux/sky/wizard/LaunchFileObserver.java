package com.termux.sky.wizard;
import android.app.Activity;
import android.os.FileObserver;
import android.util.Log;

import com.termux.app.TermuxActivity;

import java.io.File;

public class LaunchFileObserver extends FileObserver {

    private final TermuxActivity activity;
    private final File targetFile;

    public LaunchFileObserver(TermuxActivity activity, File homeDir) {
        super(homeDir.getAbsolutePath(), CREATE | MOVED_TO | CLOSE_WRITE);
        this.activity = activity;
        this.targetFile = new File(homeDir, ".launch");
    }

    @Override
    public void onEvent(int event, String path) {

        if (path == null) return;

        if (targetFile.exists()) {
            Log.d("LaunchObserver", "Launch detected");

            activity.showRedirectOnce();
        }
    }
}
