package com.termux.sky.iptv;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchAutoStart = findViewById(R.id.switchAutoStart);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchMinimize = findViewById(R.id.switchMinimize);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchBOOTbg = findViewById(R.id.switchBOOTbg);

        LinearLayout autoOptions = findViewById(R.id.autoStartOptions);
        SeekBar seekDelay = findViewById(R.id.seekDelay);
        TextView txtDelay = findViewById(R.id.txtDelay);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Load saved values
        boolean autoStart = prefs.getBoolean("auto_start", false);
        int delay = prefs.getInt("delay", 2);

        boolean minimize = prefs.getBoolean("minimize", false);
        switchMinimize.setChecked(minimize);

        boolean boot_start_app = prefs.getBoolean("boot_start_app", false);
        switchBOOTbg.setChecked(boot_start_app);

        switchAutoStart.setChecked(autoStart);
        autoOptions.setVisibility(autoStart ? View.VISIBLE : View.GONE);



        // Set seekbar (map 2–10 sec → 0–8)
        seekDelay.setProgress(delay - 2);
        txtDelay.setText(delay + " sec");

        switchMinimize.setEnabled(autoStart);
        switchBOOTbg.setEnabled(autoStart);

        // Toggle
        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {

            autoOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            switchMinimize.setEnabled(isChecked);
            switchBOOTbg.setEnabled(isChecked);

            if (!isChecked) {

                switchMinimize.setOnCheckedChangeListener(null);
                switchBOOTbg.setOnCheckedChangeListener(null);

                switchMinimize.setChecked(false);
                switchBOOTbg.setChecked(false);

                switchMinimize.setOnCheckedChangeListener((b, v) ->
                    prefs.edit().putBoolean("minimize", v).apply()
                );

                switchBOOTbg.setOnCheckedChangeListener((b, v) ->
                    prefs.edit().putBoolean("boot_start_app", v).apply()
                );

                prefs.edit()
                    .putBoolean("auto_start", false)
                    .putBoolean("minimize", false)
                    .putBoolean("boot_start_app", false)
                    .apply();
            } else {
                prefs.edit().putBoolean("auto_start", true).apply();
            }
        });

        switchMinimize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("minimize", isChecked).apply();
        });

        switchBOOTbg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("boot_start_app", isChecked).apply();
        });

        // Slider change
        seekDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 2; // map 0→2, 8→10
                txtDelay.setText(seconds + " sec");

                prefs.edit().putInt("delay", seconds).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
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
