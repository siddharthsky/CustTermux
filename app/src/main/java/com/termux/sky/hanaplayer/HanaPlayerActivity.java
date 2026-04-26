package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import com.termux.sky.plugins.Plugin;
import com.termux.sky.plugins.PluginStorage;
import com.termux.sky.txplayer.ChannelModel;
import com.termux.sky.txplayer.ExoPlayerActivityDRM;
import com.termux.sky.txplayer.M3UParser;

import java.util.*;

public class HanaPlayerActivity extends AppCompatActivity {

    private LinearLayout root;
    private ChipGroup chipGroup;
    private HanaChannelAdapter adapter;
    private List<ChannelModel> displayList = new ArrayList<>();
    private Set<String> selectedPorts = new HashSet<>();
    private boolean isUpdatingChips = false;
    private ImageButton btnMenu;

    private static final String UI_PREFS = "hana_player_ui";
    private static final String SELECTED_CHIPS_KEY = "selected_chips";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E1628"));

        // --- 1. Create a Header Container (Horizontal) ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
//        header.setPadding(40, 60, 40, 20); // Top padding for status bar area

        // --- 2. Create the Title ---
        TextView titleBar = new TextView(this);
        titleBar.setText("🌸 Hana Player");
        titleBar.setTextSize(22);
        titleBar.setTypeface(null, Typeface.BOLD);
        titleBar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleBar.setLayoutParams(titleParams);

        header.addView(titleBar);

        // --- 3. Create the Menu Button Programmatically ---
        btnMenu = new ImageButton(this);
        btnMenu.setImageResource(R.drawable.tx_more);
        btnMenu.setBackgroundResource(R.drawable.img_btn_selector);
        btnMenu.setColorFilter(Color.WHITE);

        // Set the click listener here directly
        btnMenu.setOnClickListener(this::showPopupMenu);

        header.addView(btnMenu);

        // Add the header to the root
        root.addView(header);

        // --- 4. The rest of your UI ---
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setPadding(10, 20, 10, 20);
        chipScroll.setHorizontalScrollBarEnabled(false);

        chipGroup = new ChipGroup(this);
        chipGroup.setSingleSelection(false);
        chipScroll.addView(chipGroup);
        root.addView(chipScroll);

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new HanaChannelAdapter(displayList, this::onChannelClick, this::onChannelLongClick);
        recyclerView.setAdapter(adapter);
        root.addView(recyclerView);

        setContentView(root);

        hideSystemUI();
        initChips();
        loadActiveData();
    }

    private void showPopupMenu(View view) {
       PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.hana_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_auto_play) {
                Toast.makeText(this, "Auto-Play Setting...", Toast.LENGTH_SHORT).show();
                Log.d("PlugDRM","Cleared fav.");
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

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingChips) return;
            isUpdatingChips = true;

            if (isChecked) {
                if (portValue.equals("All") || portValue.equals("Favorites")) {
                    selectedPorts.clear();
                    selectedPorts.add(portValue);
                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
                        Chip c = (Chip) chipGroup.getChildAt(i);
                        if (!c.getTag().toString().equals(portValue)) {
                            c.setChecked(false);
                        }
                    }
                } else {
                    selectedPorts.add(portValue);
                    selectedPorts.remove("All");
                    selectedPorts.remove("Favorites");
                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
                        Chip c = (Chip) chipGroup.getChildAt(i);
                        String tag = c.getTag().toString();
                        if (tag.equals("All") || tag.equals("Favorites")) {
                            c.setChecked(false);
                        }
                    }
                }
            } else {
                selectedPorts.remove(portValue);
                if (selectedPorts.isEmpty()) {
                    selectedPorts.add("All");
                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
                        Chip c = (Chip) chipGroup.getChildAt(i);
                        if (c.getTag().toString().equals("All")) {
                            c.setChecked(true);
                        }
                    }
                }
            }

            isUpdatingChips = false;
            getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
                .putStringSet(SELECTED_CHIPS_KEY, selectedPorts).apply();

            loadActiveData();
        });

        chipGroup.addView(chip);
    }

    private void loadActiveData() {
        displayList.clear();
        List<Plugin> plugins = PluginStorage.load(this);

        if (selectedPorts.contains("All") || selectedPorts.contains("Favorites")) {
            for (Plugin p : plugins) {
                List<ChannelModel> channels = M3UParser.getFromPrefs(this, String.valueOf(p.port));
                for(ChannelModel cm : channels) cm.originPort = String.valueOf(p.port);
                displayList.addAll(channels);
            }
        } else {
            for (String portStr : selectedPorts) {
                List<ChannelModel> channels = M3UParser.getFromPrefs(this, portStr);
                for(ChannelModel cm : channels) cm.originPort = portStr;
                displayList.addAll(channels);
            }
        }

        if (selectedPorts.contains("Favorites")) {
            List<ChannelModel> favoritesOnly = new ArrayList<>();
            for (ChannelModel cm : displayList) {
                if (cm.isFavorite) favoritesOnly.add(cm);
            }
            displayList = favoritesOnly;
        }

        adapter.updateList(displayList);
    }

    private void onChannelClick(ChannelModel channel) {
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

        Toast.makeText(this, "Playing: " + channel.name, Toast.LENGTH_SHORT).show();
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
