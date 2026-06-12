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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.startapp.sdk.ads.banner.Banner;
import com.termux.R;
import com.termux.sky.TxVerify;
import com.termux.sky.txplayer.PlaylistManager;
import com.termux.sky.plugins.Plugin;
import com.termux.sky.plugins.PluginStorage;
import com.termux.sky.tv_home_preview.FavoriteChannelsManager;
import com.termux.sky.tv_home_preview.RecentChannelsManager;
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
    private int currentSortMode = 0;
    private boolean isRearrangeMode = false;
    private int selectedMovePosition = -1;
    private TextView rearrangeBanner;
    private RecyclerView recyclerView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        isLaunch = prefs.getBoolean("auto_launch_channel", false);
        currentSortMode = prefs.getInt("sort_mode", 0);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E1628"));

        root.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        try {
            Window window = getWindow();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                // for status bar area
                return insets;
            });

        } catch (Exception e) {
            Log.d("HANA_PLAYER", "SystemUI flags error");
        }

        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(10, 5, 0, 5);

        TextView titleBar = new TextView(this);
        titleBar.setText("🌸 Hana Player");
        titleBar.setTextSize(22);
        titleBar.setTypeface(null, Typeface.BOLD);
        titleBar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleBar.setLayoutParams(titleParams);

        header.addView(titleBar);

        if (TxVerify.isPremium(this)) {
            TextView proBadge = new TextView(this);
            proBadge.setText("PRO");
            proBadge.setTextSize(10);
            proBadge.setTypeface(null, Typeface.BOLD);
            proBadge.setTextColor(Color.BLACK);
            proBadge.setPadding(12, 2, 12, 2);
            proBadge.setGravity(Gravity.CENTER);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor("#FFD700"));
            badgeBg.setCornerRadius(10f);
            proBadge.setBackground(badgeBg);

            LinearLayout.LayoutParams proParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            proParams.setMargins(15, 0, 0, 0);
            proBadge.setLayoutParams(proParams);

            header.addView(proBadge);
        }

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1.0f));

        // Search Button ---
        btnSearch = new ImageButton(this);
        btnSearch.setImageResource(R.drawable.tx_search);
        btnSearch.setBackgroundResource(R.drawable.img_btn_selector);
        btnSearch.setColorFilter(Color.WHITE);
        btnSearch.setPadding(10, 10, 10, 10);
        btnSearch.setOnClickListener(v -> toggleSearch());
        header.addView(btnSearch);

        //Menu Button ---
        btnMenu = new ImageButton(this);
        btnMenu.setImageResource(R.drawable.tx_more);
        btnMenu.setBackgroundResource(R.drawable.img_btn_selector);
        btnMenu.setColorFilter(Color.WHITE);
        btnMenu.setPadding(10, 10, 10, 10);
        btnMenu.setOnClickListener(this::showPopupMenu);
        header.addView(btnMenu);

        root.addView(header);

        //Banner
        rearrangeBanner = new TextView(this);
        rearrangeBanner.setText("Rearrange Mode ON: Long-press to drag (Phone) or select to move (TV)");
        rearrangeBanner.setBackgroundColor(Color.parseColor("#FFD700"));
        rearrangeBanner.setTextColor(Color.BLACK);
        rearrangeBanner.setTextSize(15);
        rearrangeBanner.setPadding(20, 15, 20, 15);
        rearrangeBanner.setGravity(Gravity.CENTER);
        rearrangeBanner.setTypeface(null, Typeface.BOLD);
        rearrangeBanner.setVisibility(View.GONE);
        root.addView(rearrangeBanner);

        //Search Box
        float density = getResources().getDisplayMetrics().density;
        FrameLayout searchContainer = new FrameLayout(this);
        searchContainer.setVisibility(View.GONE);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int horizontalMargin = (int) (16 * density);
        int topMargin = (int) (8 * density);
        int bottomMargin = (int) (16 * density);
        containerParams.setMargins(horizontalMargin, topMargin, horizontalMargin, bottomMargin);
        searchContainer.setLayoutParams(containerParams);

        searchBox = new EditText(this);
        searchBox.setHint("Search channels...");
        searchBox.setHintTextColor(Color.LTGRAY);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);

        int paddingSides = (int) (20 * density);
        int paddingTopBottom = (int) (12 * density);
        int paddingRightClear = (int) (50 * density);
        searchBox.setPadding(paddingSides, paddingTopBottom, paddingRightClear, paddingTopBottom);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#1E2738"));
        searchBg.setCornerRadius(50f * density);
        searchBox.setBackground(searchBg);

        ImageButton btnClear = new ImageButton(this);
        btnClear.setImageResource(R.drawable.tx_cancel_r);
        btnClear.setColorFilter(Color.GRAY);
        btnClear.setBackgroundResource(R.drawable.img_btn_selector);
        btnClear.setVisibility(View.GONE);

        int btnSize = (int) (40 * density);
        FrameLayout.LayoutParams clearBtnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        clearBtnParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        clearBtnParams.rightMargin = (int) (8 * density); // Small offset from the edge
        btnClear.setLayoutParams(clearBtnParams);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyGroupFilter();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        btnClear.setOnClickListener(v -> searchBox.setText(""));
        searchContainer.addView(searchBox);
        searchContainer.addView(btnClear);
        root.addView(searchContainer);


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


        recyclerView = new RecyclerView(this);
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

        androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                    androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT, 0) {

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    int from = viewHolder.getAdapterPosition();
                    int to = target.getAdapterPosition();

                    Collections.swap(displayList, from, to);
                    adapter.notifyItemMoved(from, to);
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // Not using swipe actions
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    if (selectedPorts.contains("Favorites") && currentSortMode == 0) {
                        saveCustomFavoritesOrder();
                    }
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return isRearrangeMode && selectedPorts.contains("Favorites") && currentSortMode == 0 && currentSearchQuery.isEmpty();
                }
            });
        touchHelper.attachToRecyclerView(recyclerView);

        FrameLayout adContainer = new FrameLayout(this);
        adContainer.setId(R.id.ad_container);
        adContainer.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(adContainer);

        setContentView(root);

        hideSystemUI();
        initChips();
        loadActiveData();
        setupAds();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isRearrangeMode && selectedMovePosition != -1 && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            int newPos = selectedMovePosition;

            int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
            int itemWidthPx = (int) (120 * getResources().getDisplayMetrics().density);
            int spanCount = Math.max(2, screenWidthPx / itemWidthPx);

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                newPos--;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                newPos++;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                newPos -= spanCount;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                newPos += spanCount;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                selectedMovePosition = -1; // Drop item
                adapter.setMovingPosition(-1);
                saveCustomFavoritesOrder();
                Toast.makeText(this, "New arrangement saved", Toast.LENGTH_SHORT).show();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                selectedMovePosition = -1; // Cancel move
                adapter.setMovingPosition(-1);
                Toast.makeText(this, "Move cancelled", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (newPos >= 0 && newPos < displayList.size() && newPos != selectedMovePosition) {
                Collections.swap(displayList, selectedMovePosition, newPos);
                adapter.notifyItemMoved(selectedMovePosition, newPos);
                selectedMovePosition = newPos;
                adapter.setMovingPosition(selectedMovePosition);
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(selectedMovePosition);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void saveCustomFavoritesOrder() {
        List<String> orderedUrls = new ArrayList<>();
        for (ChannelModel cm : displayList) {
            if (cm.url != null) {
                orderedUrls.add(cm.url);
            }
        }
        prefs.edit().putString("fav_order", android.text.TextUtils.join(",", orderedUrls)).apply();
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.hana_menu, popup.getMenu());

        MenuItem autoPlayItem = popup.getMenu().findItem(R.id.menu_auto_play);
        boolean isAutoPlayEnabled = prefs.getBoolean("auto_launch_channel", false);
        if (autoPlayItem != null) autoPlayItem.setChecked(isAutoPlayEnabled);

        MenuItem exportFavItem = popup.getMenu().findItem(R.id.menu_export_fav);
        if (exportFavItem != null) {
            exportFavItem.setVisible(selectedPorts.contains("Favorites"));
        }

        final int REARRANGE_ID = 1001;
        MenuItem rearrangeItem = popup.getMenu().add(0, REARRANGE_ID, 0, "Edit: Rearrange Favorites");
        rearrangeItem.setCheckable(true);
        rearrangeItem.setChecked(isRearrangeMode);
        rearrangeItem.setVisible(selectedPorts.contains("Favorites") && currentSortMode == 0);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == REARRANGE_ID) {
                isRearrangeMode = !isRearrangeMode;

                rearrangeBanner.setVisibility(isRearrangeMode ? View.VISIBLE : View.GONE);

                if (!isRearrangeMode) {
                    selectedMovePosition = -1;
                    adapter.setMovingPosition(-1);
                }

                String msg = isRearrangeMode ? "Rearrange Mode ON" : "Rearrange Mode OFF";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (id == R.id.menu_auto_play) {
                boolean newState = !item.isChecked();
                item.setChecked(newState);
                prefs.edit().putBoolean("auto_launch_channel", newState).apply();
                Toast.makeText(this, "Auto-Play: " + (newState ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                return true;

            } else if (id == R.id.menu_export_fav) {
                exportFavoritesToM3U();
                return true;

            } else if (id == R.id.menu_sort_default) {
                setSortMode(0);
                return true;
            } else if (id == R.id.menu_sort_name) {
                setSortMode(1);
                return true;
            } else if (id == R.id.menu_sort_number) {
                setSortMode(2);
                return true;
            } else if (id == R.id.menu_demo) {
                Toast.makeText(this, "Demo Menu...", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void exportFavoritesToM3U() {
        List<Plugin> plugins = PluginStorage.load(this);
        List<ChannelModel> allFavs = new ArrayList<>();

        for (Plugin p : plugins) {
            if (p.tool != null && p.tool) continue;
            String portStr = String.valueOf(p.port);

            if (M3UParser.existsInPrefs(this, portStr)) {
                List<ChannelModel> channels = M3UParser.getFromPrefs(this, portStr);
                for (ChannelModel cm : channels) {
                    if (cm.isFavorite) {
                        allFavs.add(cm);
                    }
                }
            }
        }

        if (allFavs.isEmpty()) {
            Toast.makeText(this, "No favorites to export!", Toast.LENGTH_SHORT).show();
            return;
        }

        applySort(allFavs);

        StringBuilder m3u = getStringBuilder(allFavs);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
        String currentDateAndTime = sdf.format(new java.util.Date());
        String fileName = "CTx_Favourites_" + currentDateAndTime + ".m3u";

        try {
            java.io.OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentResolver resolver = getContentResolver();
                android.content.ContentValues contentValues = new android.content.ContentValues();
                contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/x-mpegurl");
                contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (fileUri == null) throw new java.io.IOException("Failed to create new MediaStore record.");
                fos = resolver.openOutputStream(fileUri);
            } else {
                java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                java.io.File file = new java.io.File(downloadsDir, fileName);
                fos = new java.io.FileOutputStream(file);
            }

            if (fos != null) {
                fos.write(m3u.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.close();
                Toast.makeText(this, "Saved: Downloads/" + fileName, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e("HANA_PLAYER", "Failed to save direct M3U", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private static StringBuilder getStringBuilder(List<ChannelModel> allFavs) {
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");

        for (ChannelModel cm : allFavs) {
            m3u.append("#EXTINF:-1");
            if (cm.id != null && !cm.id.trim().isEmpty()) m3u.append(" tvg-id=\"").append(cm.id).append("\"");
            if (cm.logo != null && !cm.logo.trim().isEmpty()) m3u.append(" tvg-logo=\"").append(cm.logo).append("\"");
            if (cm.group != null && !cm.group.trim().isEmpty()) m3u.append(" group-title=\"").append(cm.group).append("\"");
            m3u.append(",").append(cm.name != null ? cm.name : "Unknown Channel").append("\n");

            if (cm.licenseType != null && !cm.licenseType.trim().isEmpty()) {
                m3u.append("#KODIPROP:inputstream.adaptive.license_type=").append(cm.licenseType).append("\n");
            }
            if (cm.licenseKey != null && !cm.licenseKey.trim().isEmpty()) {
                m3u.append("#KODIPROP:inputstream.adaptive.license_key=").append(cm.licenseKey).append("\n");
            }
            if (cm.userAgent != null && !cm.userAgent.trim().isEmpty()) {
                m3u.append("#EXTVLCOPT:http-user-agent=").append(cm.userAgent).append("\n");
            }

            m3u.append(cm.url).append("\n");
            m3u.append("\n");
        }
        return m3u;
    }

    private void setSortMode(int mode) {
        currentSortMode = mode;
        prefs.edit().putInt("sort_mode", mode).apply();

        applyGroupFilter();

        String[] modeNames = {"Default", "Name", "Number"};
        Toast.makeText(this, "Sorted by: " + modeNames[mode], Toast.LENGTH_SHORT).show();
    }

    private void applySort(List<ChannelModel> list) {
        if (currentSortMode == 0 && selectedPorts.contains("Favorites")) {
            // Apply Custom Drag-and-Drop Sort
            String orderStr = prefs.getString("fav_order", "");
            if (!orderStr.isEmpty()) {
                List<String> order = Arrays.asList(orderStr.split(","));
                Collections.sort(list, (c1, c2) -> {
                    int idx1 = order.indexOf(c1.url);
                    int idx2 = order.indexOf(c2.url);
                    if (idx1 == -1) idx1 = Integer.MAX_VALUE;
                    if (idx2 == -1) idx2 = Integer.MAX_VALUE;
                    return Integer.compare(idx1, idx2);
                });
            }
        } else if (currentSortMode == 1) {
            // Sort by Name (A-Z)
            Collections.sort(list, (c1, c2) -> {
                String n1 = c1.name != null ? c1.name : "";
                String n2 = c2.name != null ? c2.name : "";
                return n1.compareToIgnoreCase(n2);
            });
        } else if (currentSortMode == 2) {
            // Sort by Number
            Collections.sort(list, (c1, c2) -> {
                int num1 = extractChannelNumber(c1);
                int num2 = extractChannelNumber(c2);
                return Integer.compare(num1, num2);
            });
        }
    }

    private int extractChannelNumber(ChannelModel cm) {
        if (cm.id != null) {
            String idString = cm.id.trim();
            StringBuilder numStr = new StringBuilder();

            for (char c : idString.toCharArray()) {
                if (Character.isDigit(c)) {
                    numStr.append(c);
                } else if (numStr.length() > 0) {
                    break;
                }
            }

            if (numStr.length() > 0) {
                try {
                    return Integer.parseInt(numStr.toString());
                } catch (NumberFormatException ignored) {}
            }
        }

        return Integer.MAX_VALUE;
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
            boolean isTool = (p.tool != null && p.tool);
            if (isTool) {
                continue;
            }

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

        float density = getResources().getDisplayMetrics().density;

        float chipHeightPx = 30f * density;
        float paddingPx = 12f * density;

        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(chipHeightPx);
        chip.setChipStartPadding(paddingPx);
        chip.setChipEndPadding(paddingPx);

        chip.setCheckedIconVisible(false);

        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f);

        chip.setTextStartPadding(4f * density);
        chip.setTextEndPadding(4f * density);

        chip.setChipBackgroundColor(getChipBackgroundStates());
        chip.setTextColor(Color.WHITE);
        chip.setChipStrokeWidth(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            chip.setForeground(getTvFocusBorder(this));
        }

        if (selectedPorts.contains(portValue)) {
            chip.setChecked(true);
        }

        chip.setOnClickListener(v -> {
            if (isUpdatingChips) return;
            isUpdatingChips = true;

            if (!portValue.equals("Favorites") && isRearrangeMode) {
                isRearrangeMode = false;
                if (rearrangeBanner != null) rearrangeBanner.setVisibility(View.GONE);
                selectedMovePosition = -1;
                if (adapter != null) adapter.setMovingPosition(-1);
            }

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

    private StateListDrawable getTvFocusBorder(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        int strokeWidthPx = (int) (3 * density);
        int cornerRadiusPx = (int) (50 * density);

        int insetHPx = (int) (2 * density);
        int insetVPx = (int) (2 * density);

        GradientDrawable focusedDrawable = new GradientDrawable();
        focusedDrawable.setShape(GradientDrawable.RECTANGLE);
        focusedDrawable.setCornerRadius(cornerRadiusPx);
        focusedDrawable.setStroke(strokeWidthPx, Color.parseColor("#FFD700"));
        focusedDrawable.setColor(Color.TRANSPARENT);

        InsetDrawable insetFocused = new InsetDrawable(focusedDrawable, insetHPx, insetVPx, insetHPx, insetVPx);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, insetFocused);
        states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));

        return states;
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
                for (Plugin p : plugins) {
                    boolean isTool = (p.tool != null && p.tool);
                    if (isTool) continue;

                    targetPlugins.add(p);
                }
            } else {
                for (Plugin p : plugins) {
                    boolean isTool = (p.tool != null && p.tool);
                    if (isTool) continue;

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

        applySort(filteredList);

        displayList.clear();
        displayList.addAll(filteredList);
        adapter.updateList(displayList);
    }

    private void toggleSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View container = (View) searchBox.getParent();

        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
            searchBox.setText("");
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
            }
        } else {
            container.setVisibility(View.VISIBLE);
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

        float density = getResources().getDisplayMetrics().density;

        float chipHeightPx = 30f * density;
        float paddingPx = 12f * density;

        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(chipHeightPx);
        chip.setChipStartPadding(paddingPx);
        chip.setChipEndPadding(paddingPx);

        chip.setCheckedIconVisible(false);

        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f);

        chip.setTextStartPadding(4f * density);
        chip.setTextEndPadding(4f * density);

        chip.setChipBackgroundColor(getChipBackgroundStates());
        chip.setTextColor(Color.WHITE);
        chip.setChipStrokeWidth(0);;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            chip.setForeground(getTvFocusBorder(this));
        }

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
        int maxRetries = 3;
        int delayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            StringBuilder sb = new StringBuilder();
            java.net.HttpURLConnection conn = null;

            try {
                java.net.URL url = new java.net.URL(urlString);
                conn = (java.net.HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();

                if (responseCode >= 200 && responseCode < 400) {
                    java.io.InputStream in = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }

                    reader.close();
                    in.close();
                    return sb.toString();
                } else {
                    Log.w("PlugDRM", "Attempt " + attempt + " returned HTTP " + responseCode);
                }

            } catch (Exception e) {
                Log.e("PlugDRM", "Download attempt " + attempt + " failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "";
                }
            }
        }

        return "";
    }

    private void onChannelClick(ChannelModel channel) {

        if (isRearrangeMode && isTv(this) && selectedPorts.contains("Favorites") && currentSortMode == 0) {
            if (selectedMovePosition == -1) {
                selectedMovePosition = displayList.indexOf(channel);
                adapter.setMovingPosition(selectedMovePosition);
                Toast.makeText(this, "Moving " + channel.name + "... Use D-pad. Press OK to drop.", Toast.LENGTH_SHORT).show();
            } else {
                selectedMovePosition = -1;
                adapter.setMovingPosition(-1);
                saveCustomFavoritesOrder();
                Toast.makeText(this, "Position saved", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        PlaylistManager.currentList = displayList;
        PlaylistManager.currentIndex = displayList.indexOf(channel);

        if (isTv(this)) {
            RecentChannelsManager.addChannelToHome(this, channel);
        }

        String activePort = channel.originPort;
        if (activePort == null || activePort.trim().isEmpty()) {
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

    }

    public static boolean isTv(Context context) {
        android.app.UiModeManager uiModeManager = (android.app.UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void onChannelLongClick(ChannelModel channel) {

        if (isRearrangeMode && selectedPorts.contains("Favorites") && currentSortMode == 0) {
            if (isTv(this)) {
                if (selectedMovePosition == -1) {
                    selectedMovePosition = displayList.indexOf(channel);
                    adapter.setMovingPosition(selectedMovePosition);
                    Toast.makeText(this, "Use D-pad to move " + channel.name + ". Press Enter to drop.", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        channel.isFavorite = !channel.isFavorite;

        if (channel.originPort != null) {
            List<ChannelModel> portList = M3UParser.getFromPrefs(this, channel.originPort);
            boolean updatedPrefs = false;

            for (ChannelModel portCh : portList) {
                if (portCh.url != null && portCh.url.equals(channel.url) &&
                    portCh.name != null && portCh.name.equals(channel.name)) {

                    portCh.isFavorite = channel.isFavorite;
                    updatedPrefs = true;
                    break;
                }
            }

            if (updatedPrefs) {
                M3UParser.saveToPrefs(this, channel.originPort, portList);
            }
        }

        int position = displayList.indexOf(channel);
        if (position != -1) {
            if (selectedPorts.contains("Favorites") && !channel.isFavorite) {
                displayList.remove(position);
                adapter.notifyItemRemoved(position);
            } else {
                adapter.notifyItemChanged(position);
            }
        }

        if (isTv(this)) {
            FavoriteChannelsManager.syncFavoritesToHome(this, currentPortChannels);
        }

        String msg = channel.isFavorite ? "Added to Favorites" : "Removed from Favorites";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    private void setupAds() {
        FrameLayout adContainer = findViewById(R.id.ad_container);

        if (adContainer != null) {
            if (!TxVerify.isPremium(this)) {
                Banner banner = new Banner(this);
                FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                bannerParams.gravity = Gravity.CENTER;
                banner.setLayoutParams(bannerParams);
                adContainer.addView(banner);
                adContainer.setVisibility(View.VISIBLE);
            } else {
                adContainer.setVisibility(View.GONE);
            }
        }
    }
}
