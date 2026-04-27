package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.termux.R;
import com.termux.sky.playlistmanager.PlaylistManager;
import com.termux.sky.plugins.Plugin;
import com.termux.sky.plugins.PluginStorage;
import com.termux.sky.txplayer.ChannelModel;
import com.termux.sky.txplayer.ExoPlayerActivityDRM;
import com.termux.sky.txplayer.M3UParser;

import java.net.URL;
import java.util.*;

public class HanaPlayerActivity extends AppCompatActivity {

    private LinearLayout root;
    private ChipGroup chipGroup;
    private HanaChannelAdapter adapter;
    private List<ChannelModel> displayList = new ArrayList<>();
    private Set<String> selectedPorts = new HashSet<>();
    private boolean isUpdatingChips = false;
    private ImageButton btnMenu;
    private ProgressBar progressBar;

    private SharedPreferences prefs;

    private boolean isLaunch = true;
    private boolean isFirstLaunch = true;

    private static final String UI_PREFS = "settings";
    private static final String SELECTED_CHIPS_KEY = "selected_chips";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        isLaunch = prefs.getBoolean("auto_launch_channel", false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E1628"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(10, 0, 0, 0);
//        header.setPadding(40, 60, 40, 20); // Top padding for status bar area

        TextView titleBar = new TextView(this);
        titleBar.setText("🌸 Hana Player");
        titleBar.setTextSize(22);
        titleBar.setTypeface(null, Typeface.BOLD);
        titleBar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleBar.setLayoutParams(titleParams);

        header.addView(titleBar);

        btnMenu = new ImageButton(this);
        btnMenu.setImageResource(R.drawable.tx_more);
        btnMenu.setBackgroundResource(R.drawable.img_btn_selector);
        btnMenu.setColorFilter(Color.WHITE);

        btnMenu.setOnClickListener(this::showPopupMenu);

        header.addView(btnMenu);

        root.addView(header);

        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setPadding(10, 20, 10, 20);
        chipScroll.setHorizontalScrollBarEnabled(false);

        chipGroup = new ChipGroup(this);
        chipGroup.setSingleSelection(false);
        chipScroll.addView(chipGroup);
        root.addView(chipScroll);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(progressParams);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        RecyclerView recyclerView = new RecyclerView(this);
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int itemWidthPx = (int) (120 * getResources().getDisplayMetrics().density);
        int spanCount = Math.max(2, screenWidthPx / itemWidthPx);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter = new HanaChannelAdapter(displayList, this::onChannelClick, this::onChannelLongClick);
        recyclerView.setAdapter(adapter);
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1.0f);
        recyclerView.setLayoutParams(rvParams);
        root.addView(recyclerView);

        setContentView(root);

        hideSystemUI();
        initChips();
        loadActiveData();
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.hana_menu, popup.getMenu());

        MenuItem autoPlayItem = popup.getMenu().findItem(R.id.menu_auto_play);
        boolean isAutoPlayEnabled = prefs.getBoolean("auto_launch_channel", false);
        autoPlayItem.setChecked(isAutoPlayEnabled);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_auto_play) {
                boolean newState = !item.isChecked();
                item.setChecked(newState);
                prefs.edit().putBoolean("auto_launch_channel", newState).apply();
                Toast.makeText(this, "Auto-Play: " + (newState ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                return true;

            } else if (id == R.id.menu_demo) {
                Toast.makeText(this, "Demo Menu...", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), root);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void initChips() {
        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS, MODE_PRIVATE);
        selectedPorts = new HashSet<>(Objects.requireNonNull(uiPrefs.getStringSet(SELECTED_CHIPS_KEY, new HashSet<>(Collections.singletonList("All")))));

        List<Plugin> plugins = PluginStorage.load(this);

        addChip("All", "All");
        addChip("Favorites", "Favorites");
        for (Plugin p : plugins) {
            addChip(p.title, String.valueOf(p.port));
        }
    }

    private void addChip(String label, String portValue) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTag(portValue);
        chip.setCheckable(true);
        chip.setFocusable(true);
        chip.setClickable(true);

        GradientDrawable focusedDrawable = new GradientDrawable();
        focusedDrawable.setShape(GradientDrawable.RECTANGLE);

        focusedDrawable.setCornerRadius(50);

        focusedDrawable.setStroke(4, Color.parseColor("#FFD700"));
        focusedDrawable.setColor(Color.TRANSPARENT);

        InsetDrawable insetFocused = new InsetDrawable(
            focusedDrawable,
            2, 4, 2, 4
        );

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, insetFocused);
        states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));

        chip.setForeground(states);

        if (selectedPorts.contains(portValue)) {
            chip.setChecked(true);
        }

        chip.setOnClickListener(v -> {
            if (isUpdatingChips) return;
            isUpdatingChips = true;

            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip child = (Chip) chipGroup.getChildAt(i);
                child.setChecked(false);
            }

            chip.setChecked(true);

            selectedPorts.clear();
            selectedPorts.add(portValue);

            getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
                .putStringSet(SELECTED_CHIPS_KEY, selectedPorts).apply();

            loadActiveData();

            isUpdatingChips = false;
        });

//        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isUpdatingChips) return;
//            isUpdatingChips = true;
//
//            if (isChecked) {
//                if (portValue.equals("All") || portValue.equals("Favorites")) {
//                    selectedPorts.clear();
//                    selectedPorts.add(portValue);
//                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
//                        Chip c = (Chip) chipGroup.getChildAt(i);
//                        if (!c.getTag().toString().equals(portValue)) {
//                            c.setChecked(false);
//                        }
//                    }
//                } else {
//                    selectedPorts.add(portValue);
//                    selectedPorts.remove("All");
//                    selectedPorts.remove("Favorites");
//                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
//                        Chip c = (Chip) chipGroup.getChildAt(i);
//                        String tag = c.getTag().toString();
//                        if (tag.equals("All") || tag.equals("Favorites")) {
//                            c.setChecked(false);
//                        }
//                    }
//                }
//            } else {
//                selectedPorts.remove(portValue);
//                if (selectedPorts.isEmpty()) {
//                    selectedPorts.add("All");
//                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
//                        Chip c = (Chip) chipGroup.getChildAt(i);
//                        if (c.getTag().toString().equals("All")) {
//                            c.setChecked(true);
//                        }
//                    }
//                }
//            }
//
//            isUpdatingChips = false;
//            getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
//                .putStringSet(SELECTED_CHIPS_KEY, selectedPorts).apply();
//
//            loadActiveData();
//        });

        chipGroup.addView(chip);
    }

    private void loadActiveData() {
        new Thread(() -> {
            List<Plugin> plugins = PluginStorage.load(this);
            List<ChannelModel> masterList = new ArrayList<>();

            List<Plugin> targetPlugins = new ArrayList<>();
            if (selectedPorts.contains("All") || selectedPorts.contains("Favorites")) {
                targetPlugins.addAll(plugins);
            } else {
                for (Plugin p : plugins) {
                    if (selectedPorts.contains(String.valueOf(p.port))) {
                        targetPlugins.add(p);
                    }
                }
            }

            for (Plugin p : targetPlugins) {
                String portStr = String.valueOf(p.port);
                List<ChannelModel> channels;

                if (M3UParser.existsInPrefs(this, portStr)) {
                    channels = M3UParser.getFromPrefs(this, portStr);
                } else {
                    runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
                    String content = p.playlist;
                    if (content != null && content.startsWith("http")) {
                        content = downloadUrl(content);
                    }
                    channels = M3UParser.parse(content);
                    M3UParser.saveToPrefs(this, portStr, channels);
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }

                for (ChannelModel cm : channels) {
                    cm.originPort = portStr;
                }
                masterList.addAll(channels);
            }

            if (selectedPorts.contains("Favorites")) {
                List<ChannelModel> favoritesOnly = new ArrayList<>();
                for (ChannelModel cm : masterList) {
                    if (cm.isFavorite) favoritesOnly.add(cm);
                }
                masterList = favoritesOnly;
            }

            final List<ChannelModel> resultList = masterList;
            new Handler(Looper.getMainLooper()).post(() -> {
                displayList.clear();
                displayList.addAll(resultList);
                adapter.updateList(displayList);

                // Auto-launch logic
                if (isLaunch && isFirstLaunch && !displayList.isEmpty()) {
                    isFirstLaunch = false;
                    onChannelClick(displayList.get(0));
                }
            });
        }).start();
    }

    private String downloadUrl(String urlString) {
        try {
            Scanner s = new Scanner(new URL(urlString).openStream(), "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (Exception e) { return ""; }
    }

    private void onChannelClick(ChannelModel channel) {

        PlaylistManager.currentList = displayList;
        PlaylistManager.currentIndex = displayList.indexOf(channel);

        String activePort = channel.originPort;
        if (activePort == null || channel.url.contains("5007")) {
            activePort = channel.url.contains("5007") ? "5007" : "0";
        }

        Intent intent = new Intent(this, ExoPlayerActivityDRM.class)
            .putExtra("url", channel.url)
            .putExtra("name", channel.name)
            .putExtra("logo_url", channel.logo)
            .putExtra("license_key", channel.licenseKey)
            .putExtra("license_type", channel.licenseType)
            .putExtra("user_agent", channel.userAgent)
            .putExtra("manifest_type", channel.manifestType)
            .putExtra("plugin_port", activePort);

        startActivity(intent);

//        Toast.makeText(this, "Playing: " + channel.name, Toast.LENGTH_SHORT).show();
    }

    private void onChannelLongClick(ChannelModel channel) {
        channel.isFavorite = !channel.isFavorite;

        if (channel.originPort != null) {
            List<ChannelModel> portList = M3UParser.getFromPrefs(this, channel.originPort);
            for (ChannelModel portCh : portList) {
                if (portCh.url.equals(channel.url)) {
                    portCh.isFavorite = channel.isFavorite;
                    M3UParser.saveToPrefs(this, channel.originPort, portList);
                    break;
                }
            }
        }

        String msg = channel.isFavorite ? "Added to Favorites" : "Removed from Favorites";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        loadActiveData();
    }
}
