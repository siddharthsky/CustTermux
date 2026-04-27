package com.termux.sky.iptv;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.sky.hanaplayer.HanaPlayerActivity;

import java.io.File;
import java.util.Objects;

public class AutoAppRedirectDialog {

    private AlertDialog dialog;
    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void show(Activity activity) {

        SharedPreferences prefs = activity.getSharedPreferences("settings", MODE_PRIVATE);

        boolean autoStart = prefs.getBoolean("auto_start", false);
        int delay = prefs.getInt("delay", 3);

        String pkg = prefs.getString("pkg", null);
        String cls = prefs.getString("activity", null);

        Boolean minimize = prefs.getBoolean("minimize", false);

        /// /////////////////

        boolean exists = isFilePresentInHome(activity,".launch");

        /// /////////////////

        if (!autoStart || pkg == null || cls == null) return;

        try {
            Drawable icon;
            String appName;

            if ("hana_player".equals(pkg)) {
                icon = activity.getDrawable(R.drawable.tx_hana_player);
                appName = "Hana Player";
            } else {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                icon = pm.getApplicationIcon(info);
                appName = pm.getApplicationLabel(info).toString();
            }

            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_auto_redirect
                , null);

            ImageView iconView = view.findViewById(R.id.app_icon);
            TextView titleView = view.findViewById(R.id.app_name);
            TextView timerView = view.findViewById(R.id.countdown);
            Button cancelBtn = view.findViewById(R.id.cancel_btn);

            if ("hana_player".equals(pkg)) {
                iconView.setColorFilter(android.graphics.Color.parseColor("#F47983"));
            }

            iconView.setImageDrawable(icon);
            titleView.setText("Opening " + appName);

            AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);
            builder.setView(view);
            builder.setCancelable(true);

            dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            dialog.setOnCancelListener(dialogInterface -> {
                cancel();
                dialog.dismiss();
            });

            cancelBtn.setFocusable(true);
            cancelBtn.setFocusableInTouchMode(true);
            cancelBtn.requestFocus();

            cancelBtn.setOnClickListener(v -> {
                cancel();
                dialog.dismiss();
            });

            long total = delay * 1000L;

            timer = new CountDownTimer(total, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerView.setText((millisUntilFinished / 1000) + "s");
                }

                @Override
                public void onFinish() {
                    dialog.dismiss();
                    launch(activity, pkg, cls, minimize);
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean isFilePresentInHome(Activity activity, String fileName) {
        File homeDir = new File(activity.getFilesDir(), "home");
        File file = new File(homeDir, fileName);

        return file.exists() && file.isFile();
    }

    public void launch(Context context, String pkg, String cls, Boolean minimize) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("settings", MODE_PRIVATE);
            prefs.edit().putBoolean("temp_minimize", minimize).apply();

            if (minimize && context instanceof Activity) {
                ((Activity) context).moveTaskToBack(true);
            }

            handler.postDelayed(() -> {
                try {

                    if ("hana_player".equals(pkg)) {
                        Intent intent = new Intent(context, HanaPlayerActivity.class);
                        context.startActivity(intent);
                    } else {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(pkg, cls));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }

                } catch (Exception e) {
                    Log.e("TxAutoAppRedirect", "Delayed launch failed", e);
                }
            }, 200);

        } catch (Exception e) {
            Log.e("TxAutoAppRedirect", "Initial launch setup failed", e);
        }
    }

    public void cancel() {
        if (timer != null) timer.cancel();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
