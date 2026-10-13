package com.termux.sky.iptv;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.inmobi.ads.InMobiBanner;


import com.termux.R;
import com.termux.sky.TxVerify;
import com.termux.sky.hanaplayer.HanaPlayerActivity;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;

public class AutoAppRedirectDialog {

    private boolean isRedirecting = false;
    private static final String TAG = "AutoAppRedirect";
    private AlertDialog dialog;
    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

//    private Banner preloadedBanner;

    private InMobiBanner preloadedBanner;


    @SuppressLint("UseCompatLoadingForDrawables")
    public void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        isRedirecting = false;

        SharedPreferences prefs = activity.getSharedPreferences("settings", MODE_PRIVATE);

        boolean autoStart = prefs.getBoolean("auto_start", false);
        int delay = prefs.getInt("delay", 3);
        String pkg = prefs.getString("pkg", null);
        String cls = prefs.getString("activity", null);
        boolean minimize = prefs.getBoolean("minimize", false);

        if (!autoStart || pkg == null || cls == null) return;

        try {
            Drawable icon;
            String appName;

            if ("hana_player".equals(pkg)) {
                icon = ContextCompat.getDrawable(activity, R.drawable.tx_hana_player);
                appName = "Hana Player";
            } else {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                icon = pm.getApplicationIcon(info);
                appName = pm.getApplicationLabel(info).toString();
            }

            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_auto_redirect, null);

            ImageView iconView = view.findViewById(R.id.app_icon);
            TextView titleView = view.findViewById(R.id.app_name);
            TextView timerView = view.findViewById(R.id.countdown);
            Button cancelBtn = view.findViewById(R.id.cancel_btn);

            iconView.setImageDrawable(icon);
            titleView.setText(String.format("Opening %s", appName));

            FrameLayout bannerContainer =
                view.findViewById(R.id.banner_container);

            boolean premium =
                TxVerify.isPremium(activity);

            if (premium) {
                bannerContainer.setVisibility(View.GONE);
            } else {
                bannerContainer.setVisibility(View.VISIBLE);

                // 1. Define strict layout params for the Ad view
                FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                );

                if (preloadedBanner != null) {
                    if (preloadedBanner.getParent() != null) {
                        ((ViewGroup) preloadedBanner.getParent()).removeView(preloadedBanner);
                    }
                    // 2. Pass the params here
                    bannerContainer.addView(preloadedBanner, adParams);
                } else {
                    InMobiBanner fallbackBanner = new InMobiBanner(activity, 10000798264L);
                    fallbackBanner.setBannerSize(320, 50);
                    fallbackBanner.setRefreshInterval(60);
                    // 3. And pass the params here
                    bannerContainer.addView(fallbackBanner, adParams);
                    fallbackBanner.load();
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setView(view);
            builder.setCancelable(true);

            dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

            dialog.setCanceledOnTouchOutside(false);

            if (!activity.isFinishing() && !activity.isDestroyed()) {
                dialog.show();
            }

            dialog.setOnDismissListener(dialogInterface -> {
                if (!isRedirecting) {
                    cancel();
                }
            });

            cancelBtn.setBackground(ContextCompat.getDrawable(activity, R.drawable.tv_plugin_add_btn_sk));
            cancelBtn.setBackgroundTintList(null);

            cancelBtn.setFocusable(true);
            cancelBtn.setFocusableInTouchMode(true);
            cancelBtn.requestFocus();

            cancelBtn.setOnClickListener(v -> cancel());

            long total = delay * 1000L;

            timer = new CountDownTimer(total, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerView.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    isRedirecting = true;
                    dialog.dismiss();
                    launch(activity, pkg, cls, minimize);
                }
            }.start();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing redirect dialog", e);
        }
    }

    public boolean isFilePresentInHome(Context context, String fileName) {
        File homeDir = new File(context.getFilesDir(), "home");
        File file = new File(homeDir, fileName);
        return file.exists() && file.isFile();
    }
    public void preloadBanner(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        preloadedBanner = new InMobiBanner(activity, 10000798264L);
        preloadedBanner.setBannerSize(320, 50);
        preloadedBanner.setRefreshInterval(60);

        preloadedBanner.setListener(new BannerAdEventListener() {
            @Override
            public void onAdLoadSucceeded(@NonNull InMobiBanner inMobiBanner, @NonNull AdMetaInfo adMetaInfo) {
                Log.d(TAG, "InMobi Banner preloaded successfully");
            }

            @Override
            public void onAdLoadFailed(@NonNull InMobiBanner inMobiBanner, @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
                Log.e(TAG, "InMobi Banner failed to preload: " + inMobiAdRequestStatus.getMessage());
                preloadedBanner = null;
            }
        });

        preloadedBanner.load();
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
                    Intent intent;
                    if ("hana_player".equals(pkg)) {
                        intent = new Intent(context, HanaPlayerActivity.class);
                    } else {
                        intent = new Intent();
                        intent.setComponent(new ComponentName(pkg, cls));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Delayed launch failed", e);
                }
            }, 200);

        } catch (Exception e) {
            Log.e(TAG, "Initial launch setup failed", e);
        }
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
        }
        // CRITICAL: Prevent the handler from firing if the dialog is cancelled during the 200ms delay
        handler.removeCallbacksAndMessages(null);

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
