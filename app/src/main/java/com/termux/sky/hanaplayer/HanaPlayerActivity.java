package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

    private ImageButton btnSearch;
    private EditText searchBox;
    private String currentSearchQuery = "";

    private SharedPreferences prefs;

    private boolean isLaunch = true;
    private boolean isFirstLaunch = true;

    private static final String UI_PREFS = "settings";
    private static final String SELECTED_CHIPS_KEY = "selected_chips";
    private static final String SELECTED_GROUPS_KEY = "selected_groups";

    private ChipGroup groupChipGroup;
    private Set<String> selectedGroups = new HashSet<>(Collections.singletonList("All"));
    private List<ChannelModel> currentPortChannels = new ArrayList<>();
    private boolean isUpdatingGroupChips = false;

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
        header.setPadding(10, 5, 10, 5);

        TextView titleBar = new TextView(this);
        titleBar.setText("🌸 Hana Player");
        titleBar.setTextSize(22);
        titleBar.setTypeface(null, Typeface.BOLD);
        titleBar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleBar.setLayoutParams(titleParams);

        header.addView(titleBar);

        // Search Button ---
        btnSearch = new ImageButton(this);
        btnSearch.setImageResource(R.drawable.tx_search);
        btnSearch.setBackgroundResource(R.drawable.img_btn_selector);
        btnSearch.setColorFilter(Color.WHITE);
        btnSearch.setOnClickListener(v -> toggleSearch());
        header.addView(btnSearch);

        //Menu Button ---
        btnMenu = new ImageButton(this);
        btnMenu.setImageResource(R.drawable.tx_more);
        btnMenu.setBackgroundResource(R.drawable.img_btn_selector);
        btnMenu.setColorFilter(Color.WHITE);
        btnMenu.setOnClickListener(this::showPopupMenu);
        header.addView(btnMenu);

        root.addView(header);

        //Search Box
        searchBox = new EditText(this);
        searchBox.setHint("Search channels...");
        searchBox.setHintTextColor(Color.LTGRAY);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setSingleLine(true);
        searchBox.setPadding(30, 20, 30, 20);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#1E2738"));
        searchBg.setCornerRadius(50f);
        searchBox.setBackground(searchBg);
        searchBox.setVisibility(View.GONE);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        searchParams.setMargins(16, 16, 16, 16);
        searchBox.setLayoutParams(searchParams);


        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyGroupFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        root.addView(searchBox);

        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setPadding(10, 0, 10, 0);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        chipGroup = new ChipGroup(this);
        chipGroup.setSingleSelection(false);
        chipGroup.setSingleLine(true);
        chipGroup.setChipSpacingHorizontal(12);
        chipScroll.addView(chipGroup);
        root.addView(chipScroll);

        HorizontalScrollView groupChipScroll = new HorizontalScrollView(this);
        groupChipScroll.setPadding(10, 10, 10, 10);
        groupChipScroll.setHorizontalScrollBarEnabled(false);
        groupChipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        groupChipGroup = new ChipGroup(this);
        groupChipGroup.setSingleSelection(false);
        groupChipGroup.setSingleLine(true);
        groupChipGroup.setChipSpacingHorizontal(12);
        groupChipScroll.addView(groupChipGroup);
        root.addView(groupChipScroll);

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
        selectedGroups = new HashSet<>(Objects.requireNonNull(uiPrefs.getStringSet(SELECTED_GROUPS_KEY, new HashSet<>(Collections.singletonList("All")))));

        List<Plugin> plugins = PluginStorage.load(this);

        addChip("All", "All");
        addChip("Favorites", "Favorites");

        String activePort = "";
        for (String s : selectedPorts) {
            if (!s.equals("All") && !s.equals("Favorites")) {
                activePort = s;
                break;
            }
        }

        Plugin activePlugin = null;
        List<Plugin> others = new ArrayList<>();
        for (Plugin p : plugins) {
            if (String.valueOf(p.port).equals(activePort)) {
                activePlugin = p;
            } else {
                others.add(p);
            }
        }

        if (activePlugin != null) {
            addChip(activePlugin.title, String.valueOf(activePlugin.port));
        }
        for (Plugin p : others) {
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

        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(65f);
        chip.setChipStartPadding(16f);
        chip.setChipEndPadding(16f);

        chip.setChipBackgroundColor(getChipBackgroundStates());
        chip.setTextColor(Color.WHITE);
        chip.setChipStrokeWidth(0);


        GradientDrawable focusedDrawable = new GradientDrawable();
        focusedDrawable.setShape(GradientDrawable.RECTANGLE);
        focusedDrawable.setCornerRadius(50);
        focusedDrawable.setStroke(4, Color.parseColor("#FFD700"));
        focusedDrawable.setColor(Color.TRANSPARENT);

        InsetDrawable insetFocused = new InsetDrawable(focusedDrawable, 2, 4, 2, 4);
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
                .putStringSet(SELECTED_CHIPS_KEY, new HashSet<>(selectedPorts)).apply();

            loadActiveData();

            isUpdatingChips = false;
        });

        chipGroup.addView(chip);
    }

    private ColorStateList getChipBackgroundStates() {
        int[][] states = new int[][]{
            new int[]{android.R.attr.state_checked},
            new int[]{}
        };

        int[] colors = new int[]{
            Color.parseColor("#304FFE"),
            Color.parseColor("#1E2738")
        };

        return new ColorStateList(states, colors);
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

            currentPortChannels = masterList;

            Set<String> groups = new TreeSet<>();
            for (ChannelModel cm : currentPortChannels) {
                if (cm.group != null && !cm.group.trim().isEmpty()) {
                    groups.add(cm.group.trim());
                }
            }

            if (!selectedGroups.contains("All")) {
                selectedGroups.retainAll(groups);
                if (selectedGroups.isEmpty()) {
                    selectedGroups.add("All");
                }
            }

            final List<String> availableGroups = new ArrayList<>(groups);

            new Handler(Looper.getMainLooper()).post(() -> {

                getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
                    .putStringSet(SELECTED_GROUPS_KEY, selectedGroups).apply();

                updateGroupChipsUI(availableGroups);

                applyGroupFilter();

                if (isLaunch && isFirstLaunch && !displayList.isEmpty()) {
                    isFirstLaunch = false;
                    onChannelClick(displayList.get(0));
                }
            });
        }).start();
    }

    private void applyGroupFilter() {
        List<ChannelModel> filteredList = new ArrayList<>();
        String query = currentSearchQuery.toLowerCase().trim();

        for (ChannelModel cm : currentPortChannels) {
            boolean matchesGroup = selectedGroups.contains("All") ||
                (cm.group != null && selectedGroups.contains(cm.group.trim()));

            boolean matchesSearch = query.isEmpty() ||
                (cm.name != null && cm.name.toLowerCase().contains(query));

            if (matchesGroup && matchesSearch) {
                filteredList.add(cm);
            }
        }

        displayList.clear();
        displayList.addAll(filteredList);
        adapter.updateList(displayList);
    }

    private void toggleSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (searchBox.getVisibility() == View.VISIBLE) {
            searchBox.setVisibility(View.GONE);
            searchBox.setText("");
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
            }
        } else {
            searchBox.setVisibility(View.VISIBLE);
            searchBox.requestFocus();
            if (imm != null) {
                imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void updateGroupChipsUI(List<String> groups) {
        groupChipGroup.removeAllViews();

        addGroupChip("All");

        List<String> selectedOnes = new ArrayList<>();
        List<String> others = new ArrayList<>();

        for (String groupName : groups) {
            if (selectedGroups.contains(groupName)) {
                selectedOnes.add(groupName);
            } else {
                others.add(groupName);
            }
        }

        for (String g : selectedOnes) {
            addGroupChip(g);
        }

        for (String g : others) {
            addGroupChip(g);
        }
    }

    private void addGroupChip(String groupName) {
        Chip chip = new Chip(this);
        chip.setText(groupName);
        chip.setTag(groupName);
        chip.setCheckable(true);
        chip.setFocusable(true);
        chip.setClickable(true);

        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(65f);
        chip.setChipStartPadding(16f);
        chip.setChipEndPadding(16f);

        chip.setChipBackgroundColor(getChipBackgroundStates());
        chip.setTextColor(Color.WHITE);
        chip.setChipStrokeWidth(0);

        GradientDrawable focusedDrawable = new GradientDrawable();
        focusedDrawable.setShape(GradientDrawable.RECTANGLE);
        focusedDrawable.setCornerRadius(50);
        focusedDrawable.setStroke(4, Color.parseColor("#FFD700"));
        focusedDrawable.setColor(Color.TRANSPARENT);

        InsetDrawable insetFocused = new InsetDrawable(focusedDrawable, 2, 4, 2, 4);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, insetFocused);
        states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));

        chip.setForeground(states);

        if (selectedGroups.contains(groupName)) {
            chip.setChecked(true);
        }

        chip.setOnClickListener(v -> {
            if (isUpdatingGroupChips) return;
            isUpdatingGroupChips = true;

            boolean isChecked = chip.isChecked();

            if (groupName.equals("All")) {
                if (isChecked) {
                    selectedGroups.clear();
                    selectedGroups.add("All");

                    for (int i = 0; i < groupChipGroup.getChildCount(); i++) {
                        Chip c = (Chip) groupChipGroup.getChildAt(i);
                        if (!c.getTag().toString().equals("All")) {
                            c.setChecked(false);
                        }
                    }
                } else {
                    if (selectedGroups.contains("All")) {
                        chip.setChecked(true);
                    }
                }
            } else {
                if (isChecked) {
                    selectedGroups.add(groupName);
                    selectedGroups.remove("All");

                    for (int i = 0; i < groupChipGroup.getChildCount(); i++) {
                        Chip c = (Chip) groupChipGroup.getChildAt(i);
                        if (c.getTag().toString().equals("All")) {
                            c.setChecked(false);
                            break;
                        }
                    }
                } else {
                    selectedGroups.remove(groupName);

                    if (selectedGroups.isEmpty()) {
                        selectedGroups.add("All");
                        for (int i = 0; i < groupChipGroup.getChildCount(); i++) {
                            Chip c = (Chip) groupChipGroup.getChildAt(i);
                            if (c.getTag().toString().equals("All")) {
                                c.setChecked(true);
                                break;
                            }
                        }
                    }
                }
            }

            isUpdatingGroupChips = false;

            getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
                .putStringSet(SELECTED_GROUPS_KEY, new HashSet<>(selectedGroups)).apply();

            applyGroupFilter();
        });

        groupChipGroup.addView(chip);
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
