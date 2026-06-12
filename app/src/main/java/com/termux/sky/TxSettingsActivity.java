package com.termux.sky;

import static com.termux.sky.TxUtils.doClearData;
import static com.termux.sky.TxUtils.doPluginClearData;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.sky.filehandlers.FileManagerActivity;
import com.termux.sky.filehandlers.FilePickerActivity;
import com.termux.sky.iptv.AppPickerActivity;

import java.util.ArrayList;
import java.util.List;

public class TxSettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> displaySettingsList;
    private static final int REQUEST_CODE_LICENSE_FILE = 1002;
    private EditText mLicenseInput;

    public enum SettingId {
        IPTV_MANAGER, FILE_MANAGER, AUTO_START, BACKUP_RESTORE,
        REMOVE_ADS, TELEGRAM, GITHUB, ABOUT, HEADER_DANGER_ZONE,
        CLEAR_PLUGINS, RESET_APP_DATA, HEADER_EXP_ZONE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_settings);

        recyclerView = findViewById(R.id.settingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadSettingsOptions();

        adapter = new SettingsAdapter(this, displaySettingsList, item -> {
            if (item.getType() == SettingItem.ItemType.HEADER) return;

            switch (item.getId()) {
                case IPTV_MANAGER: {
                    Intent intent = new Intent(this, AppPickerActivity.class);
                    this.startActivity(intent);
                    break;
                }
                case FILE_MANAGER: {
                    Intent intent = new Intent(this, FileManagerActivity.class);
                    this.startActivity(intent);
                    break;
                }
                case AUTO_START: {
                    TxUtils.showAutoStartDialog(this);
                    break;
                }
                case BACKUP_RESTORE: {
                    TxUtils.showBackupRestoreDialog(this);
                    break;
                }
                case REMOVE_ADS: {
                    showLicenseDialog();
                    break;
                }
                case TELEGRAM: {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("tg://resolve?domain=CustTermux"));
                        startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://t.me/CustTermux"));
                        startActivity(intent);
                    }
                    break;
                }
                case GITHUB: {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/siddharthsky/CustTermux"));
                    startActivity(intent);
                    break;
                }
                case ABOUT: {
                    showAboutDialog();
                    break;
                }
                case CLEAR_PLUGINS: {
                    doPluginClearData(this);
                    break;
                }

                case RESET_APP_DATA: {
                    doClearData(this);
                    break;
                }
                default:
                    Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadSettingsOptions() {
        List<SettingItem> masterList = new ArrayList<>();

        masterList.add(new SettingItem(SettingId.IPTV_MANAGER, "IPTV Manager", R.drawable.tx_m_tv, true));
        masterList.add(new SettingItem(SettingId.AUTO_START, "Auto-Start on boot", R.drawable.tx_boot, true));
        masterList.add(new SettingItem(SettingId.BACKUP_RESTORE, "Backup and Restore", R.drawable.tx_backup, true));
        masterList.add(new SettingItem(SettingId.REMOVE_ADS, "Activate Premium", R.drawable.tx_ads, true));
        masterList.add(new SettingItem(SettingId.TELEGRAM, "Telegram: Help and Support", R.drawable.tx_tel, true));
        masterList.add(new SettingItem(SettingId.GITHUB, "Github Repository", R.drawable.tx_globe, true));
        masterList.add(new SettingItem(SettingId.ABOUT, "About CTx Engine", R.drawable.tx_error_outline, true));

        masterList.add(new SettingItem(SettingId.HEADER_EXP_ZONE, "Experimental ZONE", true));

        masterList.add(new SettingItem(SettingId.FILE_MANAGER, "Internal File Manager", R.drawable.tx_file_manager, true));

        masterList.add(new SettingItem(SettingId.HEADER_DANGER_ZONE, "Danger ZONE", true));

        masterList.add(new SettingItem(SettingId.CLEAR_PLUGINS, "Clear all plugins", R.drawable.tx_m_rerun3, true));
        masterList.add(new SettingItem(SettingId.RESET_APP_DATA, "Reset App Data", R.drawable.tx_del, true));

        displaySettingsList = new ArrayList<>();
        for (SettingItem item : masterList) {
            if (item.isVisible()) displaySettingsList.add(item);
        }
    }


    public static class SettingItem {
        public enum ItemType {ITEM, HEADER}

        private SettingId id;
        private String title;
        private int iconResId;
        private boolean isVisible;
        private ItemType type;

        public SettingItem(SettingId id, String title, int iconResId, boolean isVisible) {
            this.id = id;
            this.title = title;
            this.iconResId = iconResId;
            this.isVisible = isVisible;
            this.type = ItemType.ITEM;
        }

        public SettingItem(SettingId id, String title, boolean isVisible) {
            this.id = id;
            this.title = title;
            this.isVisible = isVisible;
            this.type = ItemType.HEADER;
            this.iconResId = 0;
        }

        public SettingId getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public int getIconResId() {
            return iconResId;
        }

        public boolean isVisible() {
            return isVisible;
        }

        public ItemType getType() {
            return type;
        }
    }


    public static class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_HEADER = 1;

        private Context context;
        private List<SettingItem> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(SettingItem item);
        }

        public SettingsAdapter(Context context, List<SettingItem> items, OnItemClickListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType() == SettingItem.ItemType.HEADER ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {

                LinearLayout layout = new LinearLayout(context);
                layout.setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

                TextView title = new TextView(context);
                title.setId(View.generateViewId());
                title.setTextSize(13);
                title.setTypeface(null, Typeface.BOLD);
                title.setAllCaps(true);
                title.setLetterSpacing(0.08f);

                layout.addView(title);
                return new HeaderViewHolder(layout, title);
            } else {

                LinearLayout layout = new LinearLayout(context);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, dpToPx(12));
                layout.setLayoutParams(params);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER_VERTICAL);
                layout.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));
                layout.setClickable(true);
                layout.setFocusable(true);


                layout.setBackgroundResource(R.drawable.button_card);

                ImageView icon = new ImageView(context);
                icon.setId(View.generateViewId());
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
                iconParams.setMargins(0, 0, dpToPx(16), 0);
                icon.setLayoutParams(iconParams);

                TextView title = new TextView(context);
                title.setId(View.generateViewId());
                title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                title.setTextSize(16);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

                layout.addView(icon);
                layout.addView(title);

                return new ItemViewHolder(layout, icon, title);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SettingItem item = items.get(position);

            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                headerHolder.titleText.setText(item.getTitle());

                int headerColor;
                if (item.getId() == SettingId.HEADER_DANGER_ZONE) {
                    headerColor = Color.parseColor("#FF5252");
                } else if (item.getId() == SettingId.HEADER_EXP_ZONE) {
                    headerColor = Color.parseColor("#FFCA28");
                } else {
                    headerColor = Color.parseColor("#64B5F6");
                }

                headerHolder.titleText.setTextColor(headerColor);

            } else {
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                itemHolder.titleText.setText(item.getTitle());
                itemHolder.iconImage.setImageResource(item.getIconResId());

                int textColor, iconColor;
                if (item.getId() == SettingId.CLEAR_PLUGINS || item.getId() == SettingId.RESET_APP_DATA) {
                    textColor = Color.parseColor("#FF6B8B");
                    iconColor = Color.parseColor("#FF6B8B");
                } else {
                    textColor = Color.parseColor("#F5F5F5");
                    iconColor = Color.parseColor("#FFB7C5");
                }

                itemHolder.titleText.setTextColor(textColor);
                itemHolder.iconImage.setColorFilter(iconColor);

                itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(item));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }


        private int dpToPx(int dp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        }

        public static class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView iconImage;
            TextView titleText;

            public ItemViewHolder(View itemView, ImageView iconImage, TextView titleText) {
                super(itemView);
                this.iconImage = iconImage;
                this.titleText = titleText;
            }
        }

        public static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView titleText;

            public HeaderViewHolder(View itemView, TextView titleText) {
                super(itemView);
                this.titleText = titleText;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void showAboutDialog() {
        String version = BuildConfig.VERSION_NAME;

        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        int dp20 = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp20, dp16, dp20, dp16);

        TextView descText = new TextView(this);
        descText.setText("Customized Termux fork for IPTV, plugins and lightweight tools.");
        descText.setTextColor(Color.LTGRAY);
        descText.setTextSize(14f);
        descText.setPadding(0, 0, 0, dp16);
        container.addView(descText);

        TextView infoBox = new TextView(this);
        infoBox.setText("Version: " + version + "\nDeveloper: Siddharthsky");
        infoBox.setTextIsSelectable(true);
        infoBox.setPadding(dp16, dp16, dp16, dp16);
        infoBox.setTextColor(Color.parseColor("#FFD700"));
        infoBox.setBackgroundResource(R.drawable.deviceid_bg);

        infoBox.setLineSpacing(12f, 1f);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        infoBox.setLayoutParams(infoParams);

        container.addView(infoBox);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("About CustTermux Engine")
            .setIcon(R.drawable.ic_launcher_foreground)
            .setView(container)
            .setPositiveButton("GitHub", (d, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/siddharthsky/CustTermux"));
                startActivity(intent);
            })
            .setNegativeButton("Telegram", (d, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/CustTermux"));
                startActivity(intent);
            })
            .setNeutralButton("Close", null)
            .create();

        dialog.setOnShowListener(d -> {
            Button githubBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button telegramBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            Button closeBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

            Button[] buttons = {githubBtn, telegramBtn, closeBtn};
            for (Button b : buttons) {
                if (b != null) {
                    b.setBackgroundTintList(null);
                    b.setBackgroundResource(R.drawable.golden_focus_selector);
                    b.setTextColor(Color.WHITE);
                    b.setFocusable(true);
                }
            }

            if (githubBtn != null) {
                githubBtn.setTextColor(Color.parseColor("#FFD700"));
            }
        });

        dialog.show();
    }

    private void showConfirmDialog(String title,
                                   String message,
                                   Runnable action) {

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setIcon(R.drawable.tx_warning_amber)
            .setPositiveButton("Confirm", (dialog, which) -> {
                Toast.makeText(this,
                    "Processing...",
                    Toast.LENGTH_SHORT).show();

                action.run();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }


    @SuppressLint("SetTextI18n")
    private void showLicenseDialog() {

        boolean isPremium = TxVerify.isPremium(this);
        String deviceId = TxVerify.getDeviceId(this);

        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        int dp20 = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp20, dp16, dp20, dp16);

        TextView info = new TextView(this);
        info.setText("Enter your premium license key.\nRemove ads and unlock premium.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(14f);
        info.setPadding(0, 0, 0, dp16);
        container.addView(info);

        Button selectFileBtn = new Button(this);
        selectFileBtn.setText("Select Licence File");
        selectFileBtn.setBackgroundResource(R.drawable.golden_focus_selector);
        selectFileBtn.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams selectFileParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        selectFileParams.bottomMargin = dp16;
        selectFileBtn.setLayoutParams(selectFileParams);

        selectFileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.EXTRA_FILTERS, new String[]{".lic"});
            startActivityForResult(intent, REQUEST_CODE_LICENSE_FILE);
        });

        container.addView(selectFileBtn);

        mLicenseInput = new EditText(this);
        mLicenseInput.setHint(isPremium ? "Premium Active" : "Enter License Key");
        mLicenseInput.setTextSize(16f);
        mLicenseInput.setMinLines(2);
        mLicenseInput.setMaxLines(4);
        mLicenseInput.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        mLicenseInput.setFocusable(true);
        mLicenseInput.setFocusableInTouchMode(true);
        mLicenseInput.setBackgroundResource(R.drawable.edittext_bg);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.bottomMargin = dp16;
        mLicenseInput.setLayoutParams(inputParams);

        container.addView(mLicenseInput);

        TextView label = new TextView(this);
        label.setText("Device ID");
        label.setTextColor(Color.WHITE);
        label.setTextSize(13f);
        container.addView(label);

        TextView deviceBox = new TextView(this);
        deviceBox.setText(deviceId);
        deviceBox.setTextIsSelectable(true);
        deviceBox.setFocusable(true);
        deviceBox.setClickable(true);
        deviceBox.setPadding(dp16, dp16, dp16, dp16);
        deviceBox.setTextColor(Color.parseColor("#FFD700"));
        deviceBox.setBackgroundResource(R.drawable.deviceid_bg);

        LinearLayout.LayoutParams deviceParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        deviceParams.topMargin = 8;
        deviceBox.setLayoutParams(deviceParams);

        container.addView(deviceBox);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy Device ID");
        copyBtn.setBackgroundResource(R.drawable.golden_focus_selector);
        copyBtn.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams copyParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        copyParams.topMargin = dp16;
        copyBtn.setLayoutParams(copyParams);

        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                ClipData.newPlainText("Device ID", deviceId)
            );
            Toast.makeText(this, "Device ID copied", Toast.LENGTH_SHORT).show();
        });

        container.addView(copyBtn);

        CharSequence title = "Activate Premium";

        if (isPremium) {
            SpannableString s = new SpannableString("⭐ Premium Active");
            s.setSpan(
                new ForegroundColorSpan(Color.parseColor("#FFD700")),
                0,
                s.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            title = s;
        }

        AlertDialog dialog = new AlertDialog.Builder(
            this,
            R.style.GoldenFocusDialogTheme
        )
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Activate", null)
            .setNeutralButton("Clear License", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(d -> {

            Button activate =
                dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button clear =
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            Button cancel =
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            Button[] buttons = {activate, clear, cancel};

            for (Button b : buttons) {
                if (b != null) {
                    b.setBackgroundTintList(null);
                    b.setBackgroundResource(
                        R.drawable.golden_focus_selector
                    );
                    b.setTextColor(Color.WHITE);
                    b.setFocusable(true);
                }
            }

            if (activate != null) {
                activate.setTextColor(
                    Color.parseColor("#FFD700")
                );

                activate.setOnClickListener(v -> {

                    String license =
                        mLicenseInput.getText().toString().trim();

                    if (license.isEmpty()) {
                        mLicenseInput.setError("Enter license key");
                        return;
                    }

                    if (TxVerify.activateLicense(
                        this,
                        license
                    )) {

                        Toast.makeText(
                            this,
                            "Premium Activated ✅",
                            Toast.LENGTH_LONG
                        ).show();

                        dialog.dismiss();

                    } else {

                        mLicenseInput.setError("Invalid license");

                        Toast.makeText(
                            this,
                            "Invalid License",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }

            if (clear != null) {
                clear.setOnClickListener(v -> {

                    TxVerify.clearLicense(this);

                    Toast.makeText(
                        this,
                        "License cleared. Ads restored.",
                        Toast.LENGTH_LONG
                    ).show();

                    dialog.dismiss();
                });
            }
        });

        dialog.show();

        mLicenseInput.requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LICENSE_FILE && resultCode == RESULT_OK && data != null) {
            try {
                Uri fileUri = data.getData();

                if (fileUri != null) {
                    java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    inputStream.close();
                    String licenseText = stringBuilder.toString().trim();

                    if (mLicenseInput != null) {
                        mLicenseInput.setText(licenseText);
                        Toast.makeText(this, "License file loaded", Toast.LENGTH_SHORT).show();
                    } else {
                        if (TxVerify.activateLicense(this, licenseText)) {
                            Toast.makeText(this, "Premium Activated ✓", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Invalid License File", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to read .lic file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
