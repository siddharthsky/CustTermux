package com.termux.sky.iptv;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppPickerActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    AppAdapter adapter;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iptv_manager_activity);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppAdapter(this::onAppSelected);
        recyclerView.setAdapter(adapter);

        loadAppsAsync();

        LinearLayout layoutAutoStart = findViewById(R.id.layoutAutoStart);
        LinearLayout layoutMinimize = findViewById(R.id.layoutMinimize);
        LinearLayout layoutBOOTbg = findViewById(R.id.layoutBOOTbg);

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchAutoStart = findViewById(R.id.switchAutoStart);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchMinimize = findViewById(R.id.switchMinimize);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchBOOTbg = findViewById(R.id.switchBOOTbg);

        LinearLayout autoOptions = findViewById(R.id.autoStartOptions);
        SeekBar seekDelay = findViewById(R.id.seekDelay);
        TextView txtDelay = findViewById(R.id.txtDelay);

        applySwitchColors(switchAutoStart);
        applySwitchColors(switchMinimize);
        applySwitchColors(switchBOOTbg);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Load saved values
        boolean autoStart = prefs.getBoolean("auto_start", false);
        int delay = prefs.getInt("delay", 2);

        boolean isAutoStartActive = prefs.getBoolean("auto_start", false);
        switchAutoStart.setChecked(isAutoStartActive);
        switchMinimize.setChecked(prefs.getBoolean("minimize", false));
        switchBOOTbg.setChecked(prefs.getBoolean("boot_start_app", false));

        autoOptions.setVisibility(isAutoStartActive ? View.VISIBLE : View.GONE);
        layoutMinimize.setFocusable(isAutoStartActive);
        layoutBOOTbg.setFocusable(isAutoStartActive);
        seekDelay.setFocusable(isAutoStartActive);


        // Set seekbar (map 2–10 sec → 0–8)
        seekDelay.setProgress(delay - 2);
        txtDelay.setText(delay + " sec");

        switchMinimize.setEnabled(autoStart);
        switchBOOTbg.setEnabled(autoStart);


        layoutAutoStart.setOnClickListener(v -> switchAutoStart.toggle());
        layoutMinimize.setOnClickListener(v -> {
            if (switchAutoStart.isChecked()) switchMinimize.toggle();
        });
        layoutBOOTbg.setOnClickListener(v -> {
            if (switchAutoStart.isChecked()) switchBOOTbg.toggle();
        });



        // Toggle
        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                autoOptions.setVisibility(View.VISIBLE);
                autoOptions.setAlpha(0f);
                autoOptions.setScaleX(0.95f);
                autoOptions.setScaleY(0.95f);
                autoOptions.setTranslationY(-30f);

                autoOptions.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                    .withEndAction(seekDelay::requestFocus)
                    .start();
            } else {
                autoOptions.animate()
                    .alpha(0f)
                    .translationY(-30f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        autoOptions.setVisibility(View.GONE);
                        switchAutoStart.requestFocus();
                    })
                    .start();
            }

            layoutMinimize.setFocusable(isChecked);
            layoutBOOTbg.setFocusable(isChecked);
            seekDelay.setFocusable(isChecked);

            if (!isChecked) {
                switchMinimize.setChecked(false);
                switchBOOTbg.setChecked(false);
                prefs.edit()
                    .putBoolean("auto_start", false)
                    .putBoolean("minimize", false)
                    .putBoolean("boot_start_app", false)
                    .apply();
            } else {
                prefs.edit().putBoolean("auto_start", true).apply();
            }
        });

        switchMinimize.setOnCheckedChangeListener((b, isChecked) ->
            prefs.edit().putBoolean("minimize", isChecked).apply());

        switchBOOTbg.setOnCheckedChangeListener((b, isChecked) ->
            prefs.edit().putBoolean("boot_start_app", isChecked).apply());

        seekDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 2;
                txtDelay.setText(seconds + " sec");
                prefs.edit().putInt("delay", seconds).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void applySwitchColors(Switch toggle) {
        int[][] states = new int[][] {
            new int[] {android.R.attr.state_checked},
            new int[] {-android.R.attr.state_checked}
        };

        int[] thumbColors = new int[] {
            Color.parseColor("#38BDF8"),
            Color.parseColor("#94A3B8")
        };

        int[] trackColors = new int[] {
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#475569")
        };

        toggle.setThumbTintList(new android.content.res.ColorStateList(states, thumbColors));
        toggle.setTrackTintList(new android.content.res.ColorStateList(states, trackColors));
    }

    private void loadAppsAsync() {
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            List<AppModel> tempList = new ArrayList<>();
            PackageManager pm = getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

            for (ResolveInfo info : resolveInfos) {
                tempList.add(new AppModel(
                    info.loadLabel(pm).toString(),
                    info.activityInfo.packageName,
                    info.activityInfo.name
                ));
            }

            Collections.sort(tempList, (a, b) -> a.appName.compareToIgnoreCase(b.appName));

            AppModel internalPlayer = new AppModel(
                "🌸 Hana Player",
                "hana_player",
                "internal_activity"
            );
            tempList.add(0, internalPlayer);

            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                adapter.submitList(tempList);
            });
        });
    }

    private void onAppSelected(AppModel app) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        prefs.edit()
            .putString("pkg", app.packageName)
            .putString("activity", app.activityName)
            .apply();

        Toast.makeText(this, "Saved: " + app.appName, Toast.LENGTH_SHORT).show();
        finish();
    }

}
