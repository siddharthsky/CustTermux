package com.termux.sky.plugins;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;

import com.termux.R;
import com.termux.sky.txplayer.PlugDRM;
import com.termux.sky.filehandlers.FilePickerActivity;
import com.termux.sky.txplayer.GenericWebActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginManagerActivity extends AppCompatActivity {

    RecyclerView listView;
    Button btnAdd;

    List<Plugin> list;
    PluginAdapter adapter;

    private ImageButton btnMenu;

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView restartBanner;

    ActivityResultLauncher<String> filePicker =
        registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String fileName = getFileName(uri);
                    if (fileName != null && fileName.toLowerCase().endsWith(".json")) {
                        handleFile(uri);
                    } else {
                        handlePlaylistFile(uri);
                    }
                }
            }
        );

    ActivityResultLauncher<Intent> customFilePickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        String path = uri.getPath();
                        if (path != null && path.toLowerCase().endsWith(".json")) {
                            handleFile(uri);
                        } else {
                            handlePlaylistFile(uri);
                        }
                    }
                }
            }
        );


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.plugin_manager_activity);

        listView = findViewById(R.id.pluginList);
        btnAdd = findViewById(R.id.btnAdd);
        restartBanner = findViewById(R.id.restartBanner);

        list = PluginStorage.load(this);

        adapter = new PluginAdapter(this, list, this::loginPlugin, this::updatePlugin, this::watchPlugin, this::supportPlugin, this::deletePlugin );

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showPortInputDialog());

        btnAdd.setOnLongClickListener(v -> {
            showPortInputDialogLegacy();
            return true;
        });

        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(this::showPopupMenu);


    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRestartRequired();
    }

    private void checkRestartRequired() {
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean needsRestart = settings.getBoolean("plugin_restart", false);

        if (needsRestart) {
            restartBanner.setBackground(ContextCompat.getDrawable(this, R.drawable.tv_warning_bg));
            restartBanner.setText("Action Required: Please restart the server for changes to take effect.");
            restartBanner.setVisibility(View.VISIBLE);
        } else {
            restartBanner.setVisibility(View.GONE);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.plugin_menu, popup.getMenu());

//        popup.getMenu().findItem(R.id.menu_layout_toggle)
//            .setTitle("Clear Favorite");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_add_url) {
                showUrlInputDialog();
                Toast.makeText(this, "Add M3U URL...", Toast.LENGTH_SHORT).show();
                Log.d("PlugDRM","Cleared fav.");
                return true;
            } else if (id == R.id.menu_add_json) {

                new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
                    .setTitle("⚠ Security Warning")
                    .setMessage(
                        "JSON plugins can execute scripts and run commands on your device.\n\n" +
                            "Installing unknown plugins may be dangerous and can damage your data, " +
                            "device environment, or compromise security.\n\n" +
                            "Only install plugins from trusted or official sources.\n\n" +
                            "The developer is not responsible for any damage caused by third-party plugins."
                    )
                    .setPositiveButton("I Understand", (dialog, which) -> {
                        Intent intent = new Intent(this, FilePickerActivity.class);

                        intent.putExtra(FilePickerActivity.EXTRA_FILTERS, new String[]{".json"});
                        customFilePickerLauncher.launch(intent);

                    })
                    .setNegativeButton("Cancel", null)
                    .show();

                return true;
            } else if  (id == R.id.menu_repo_url) {
                Intent intent = new Intent(this, GenericWebActivity.class);
                intent.putExtra("url", "https://siddharthsky.github.io/ctx-plugins/");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
            }
            return false;
        });
        popup.show();
    }

    private void handleFile(Uri uri) {
        if (uri == null) return;

        try {
            InputStream is;
            if ("file".equals(uri.getScheme())) {
                is = new FileInputStream(new File(uri.getPath()));
            } else {
                is = getContentResolver().openInputStream(uri);
            }

            if (is == null) {
                Toast.makeText(this, "Could not open file", Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            // JSON parse
            Plugin p = PluginUtils.parse(sb.toString());

            if (p != null) {
                for (Plugin existing : list) {
                    if (existing.port == p.port) {
                        Toast.makeText(this, "Plugin on port " + p.port + " already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                initPlugin(
                    p.port,
                    p.repo,
                    p.repo_branch,
                    p.start,
                    p.bin_download,
                    p.post_install_script,
                    p.pkg != null ? Arrays.toString(p.pkg) : ""
                );

                list.add(p);
                PluginStorage.save(this, list);
                adapter.notifyDataSetChanged();

                Toast.makeText(this, "Plugin added and initializing...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid JSON format", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("PluginManager", "Error reading file:", e);
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPortInputDialogLegacy() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(60, 20, 60, 0);

        // Placeholder text that reacts to double taps
        TextView tapPrompt = new TextView(this);
        tapPrompt.setLayoutParams(params);
        tapPrompt.setText("Double tap to enter code...");
        tapPrompt.setPadding(0, 20, 0, 20);

        // The actual input box (hidden initially)
        EditText input = new EditText(this);
        input.setLayoutParams(params);
        input.setHint("Experimental Plugin code");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setVisibility(View.GONE);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);

        container.addView(tapPrompt);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("⚠ Experimental Plugins")
            .setMessage("This feature is for advanced users. If you don't know what this is, please cancel.")
            .setView(container)
            .setPositiveButton("Load", null)
            .setNegativeButton("Cancel", null)
            .create();

        tapPrompt.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;
            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) {
                    tapPrompt.setVisibility(View.GONE);
                    input.setVisibility(View.VISIBLE);
                    input.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
                lastClickTime = clickTime;
            }
        });

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        dialog.setOnShowListener(d -> {
            Button posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posButton.setOnClickListener(v -> {

                if (input.getVisibility() != View.VISIBLE) {
                    Toast.makeText(this, "Please unlock the input first", Toast.LENGTH_SHORT).show();
                    return;
                }

                String port = input.getText().toString().trim();
                if (port.isEmpty()) {
                    Toast.makeText(this, "Please enter a valid plugin code", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchPluginFromServer(port, true);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showPortInputDialog() {

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(60, 20, 60, 0);

        EditText input = new EditText(this);
        input.setLayoutParams(params);
        input.setHint("Enter plugin code (4-digits)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setMaxLines(1);
        input.setSingleLine(true);


        input.setImeOptions(EditorInfo.IME_ACTION_GO);

        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Add Plugin")
            .setView(container)
            .setPositiveButton("Load", null)
            .setNegativeButton("Cancel", null)
            .create();

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        dialog.setOnShowListener(d -> {
            Button posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posButton.setOnClickListener(v -> {
                String port = input.getText().toString().trim();

                if (port.isEmpty()) {
                    Toast.makeText(this, "Enter plugin code", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchPluginFromServer(port,false);
                dialog.dismiss();
            });

            input.requestFocus();
        });

        dialog.show();
    }


    private void fetchPluginFromServer(String port, Boolean debug) {

        new Thread(() -> {
            try {

                String urlStr =
                    (debug
                        ? "https://raw.githubusercontent.com/siddharthsky/Extrix/refs/heads/main/Misc/" // true
                        : "https://raw.githubusercontent.com/siddharthsky/ctx-plugins/refs/heads/main/plugins/" ) // false
                        + port.trim() + ".json";


                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);

                int code = conn.getResponseCode();

                if (code != 200) {
                    runOnUiThread(() ->
                        Toast.makeText(this, "Plugin not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                Plugin p = PluginUtils.parse(sb.toString());

                runOnUiThread(() -> {
                    if (p != null) {

                        for (Plugin existing : list) {
                            if (existing.port == Integer.parseInt(port)) {
                                Toast.makeText(this, "Plugin already added", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        initPlugin(p.port, p.repo, p.repo_branch, p.start, p.bin_download, p.post_install_script, Arrays.toString(p.pkg));

                        list.add(p);
                        PluginStorage.save(this, list);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(this, "Plugin loaded", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e("PluginManager", "Error loading plugin for port " + port, e);

                runOnUiThread(() ->
                    Toast.makeText(this, "Error loading plugin", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void initPlugin(int port, String repoUrl, String repoBranch, String startComm,
                           String binDownloadURL, String postInstallScript, String pkgList ) {

        String folderName = String.valueOf(port);

        File baseDirInit = new File(getFilesDir(), "home");
        File baseDir = new File(baseDirInit, "plugins");
        File pluginDir = new File(baseDir, folderName);

        if (!baseDir.exists()) baseDir.mkdirs();
        if (!pluginDir.exists()) pluginDir.mkdirs();

        File zipFile = new File(getCacheDir(), folderName + ".zip");
        new Thread(() -> {
            try {
                // 1. Check if both URLs are empty
                boolean hasBin = binDownloadURL != null && !binDownloadURL.trim().isEmpty();
                boolean hasRepo = repoUrl != null && !repoUrl.trim().isEmpty();

                if (!hasBin && !hasRepo) {
                    Log.d("PluginInit", "No download URLs provided. Skipping download/extraction.");

//                    createRunScript(pluginDir, port, startComm);

                    if (postInstallScript != null && !postInstallScript.trim().isEmpty()) {
                        postInstall(postInstallScript, pluginDir, port);
                    }
                    return;
                }

                if (hasBin) {
                    File binFile = new File(pluginDir, "run.bin");
                    showProgress("Downloading binary...");
                    try {
                        downloadFile(binDownloadURL, binFile);
                    } catch (Exception e) {
                        handleError("Binary download failed", e);
                        return;
                    }
                    binFile.setExecutable(true);
                    createRunScript(pluginDir, port, startComm);
                    hideProgress();

                    if (postInstallScript != null && !postInstallScript.trim().isEmpty()) {
                        postInstall(postInstallScript, pluginDir, port);
                    }
                    return;
                }

                String zipUrl;
                if (repoUrl.endsWith(".zip")) {
                    zipUrl = repoUrl;
                } else {
                    zipUrl = getRepoZipUrl(Objects.requireNonNull(repoUrl), repoBranch);
                }

                showProgress("Downloading repository...");
                try {
                    downloadZip(zipUrl, zipFile);
                } catch (Exception e) {
                    handleError("Repository download failed", e);
                    return;
                }
                hideProgress();

                showProgress("Extracting...");
                try {
                    boolean isDirectZip = repoUrl.toLowerCase().endsWith(".zip");
                    fullUnzip(zipFile, pluginDir, !isDirectZip);
                } catch (Exception e) {
                    handleError("Extraction failed", e);
                    return;
                }

                createRunScript(pluginDir, port, startComm);
                hideProgress();

                if (postInstallScript != null && !postInstallScript.trim().isEmpty()) {
                    postInstall(postInstallScript, pluginDir, port);
                }

                restart_request(this);

            } catch (Exception e) {
                handleError("Plugin install failed", e);
            }
        }).start();
    }

    public String getRepoZipUrl(String repoUrl, String repoBranch) throws Exception {

        String repo = repoUrl.replace(".git", "").trim();

        if (repoBranch != null && !repoBranch.trim().isEmpty()) {
            return repo + "/archive/refs/heads/" + repoBranch.trim() + ".zip";
        }

        String apiUrl = repo.replace("https://github.com/", "https://api.github.com/repos/");

        HttpURLConnection conn = null;
        BufferedReader br = null;

        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            int code = conn.getResponseCode();

            if (code == 200) {

                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                String json = sb.toString();

                String branch = "main";

                int index = json.indexOf("\"default_branch\":\"");
                if (index != -1) {
                    int start = index + 18;
                    int end = json.indexOf("\"", start);
                    branch = json.substring(start, end);
                }

                return repo + "/archive/refs/heads/" + branch + ".zip";
            }

        } catch (Exception e) {
            // ignore → fallback below
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }

        return repo + "/archive/refs/heads/main.zip";
    }

    private void postInstall(String postInstallScript, File pluginDir, int port) {
        new Thread(() -> {
            try {
                File postFile = new File(pluginDir, ".post_install_script.sh");
                String scriptContent = postInstallScript.trim();

                if (scriptContent.toLowerCase().startsWith("http://") ||
                    scriptContent.toLowerCase().startsWith("https://")) {

                    showProgress("Downloading script...");
                    downloadFile(scriptContent, postFile);
                    hideProgress();
                } else {
                    showProgress("Configuring custom script...");

                    if (!scriptContent.startsWith("#!")) {
                        scriptContent = "#!/bin/bash\n" + scriptContent;
                    }

                    FileOutputStream fos = new FileOutputStream(postFile);
                    fos.write(scriptContent.getBytes());
                    fos.close();

                    hideProgress();
                }

                postFile.setExecutable(true);
                run_post_script(this, pluginDir, port);
                restart_request(this);

            } catch (Exception e) {
                handleError("Post-install setup failed", e);
            }
        }).start();
    }

    private void executeUninstallScriptFromCache(String scriptContent, int port) {
        new Thread(() -> {
            try {
                File cacheDir = getCacheDir();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                File tempUninstallFile = new File(cacheDir, "uninstall_" + port + ".sh");

                if (scriptContent.toLowerCase().startsWith("http://") ||
                    scriptContent.toLowerCase().startsWith("https://")) {

                    showProgress("Downloading uninstall script...");
                    downloadFile(scriptContent, tempUninstallFile);
                    hideProgress();
                } else {
                    showProgress("Configuring uninstall script...");

                    String completeScript = scriptContent;
                    if (!completeScript.startsWith("#!")) {
                        completeScript = "#!/bin/bash\n" + completeScript;
                    }

                    FileOutputStream fos = new FileOutputStream(tempUninstallFile);
                    fos.write(completeScript.getBytes());
                    fos.close();

                    hideProgress();
                }

                tempUninstallFile.setExecutable(true);

                run_cached_uninstall_runner(this, tempUninstallFile, port);

            } catch (Exception e) {
                handleError("Uninstall script tracking failed", e);
            }
        }).start();
    }

    private void run_cached_uninstall_runner(PluginManagerActivity activity, File scriptFile, int port) {

        // Required for stopping auto-redirect
        File homeDir = new File(getFilesDir(), "home");
        File launchFile = new File(homeDir, ".launch");

        if (launchFile.exists()) {
            if (launchFile.delete()) {
                Log.d("FILE", ".launch deleted successfully");
            } else {
                Log.d("FILE", "Failed to delete .launch");
            }
        }

        String TERMUX_PACKAGE = "com.termux";
        String TERMUX_SERVICE = "com.termux.app.RunCommandService";
        String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

        File fallbackWorkDir = new File(getFilesDir(), "home");
        String SCRIPT_PATH = scriptFile.getAbsolutePath();
        String port_no = String.valueOf(port);

        Intent intent = new Intent();
        intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
        intent.setAction(ACTION_RUN_COMMAND);

        intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", fallbackWorkDir.getAbsolutePath());
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        activity.startService(intent);
        Log.d("SkyLog", "Termux isolated script triggered out of cache: " + SCRIPT_PATH);
    }

    private void restart_request(PluginManagerActivity activity) {
        SharedPreferences settings = activity.getSharedPreferences("settings", MODE_PRIVATE);
        settings.edit().putBoolean("plugin_restart", true).apply();

        runOnUiThread(this::checkRestartRequired);
    }

    private void run_post_script(PluginManagerActivity pluginManagerActivity, File pluginDir, int port) {
        String TERMUX_PACKAGE = "com.termux";
        String TERMUX_SERVICE = "com.termux.app.RunCommandService";
        String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

        // Required for stopping auto-redirect
        File homeDir = new File(getFilesDir(), "home");
        File launchFile = new File(homeDir, ".launch");

        if (launchFile.exists()) {
            if (launchFile.delete()) {
                Log.d("FILE", ".launch deleted successfully");
            } else {
                Log.d("FILE", "Failed to delete .launch");
            }
        }


        String HOME_PATH = pluginDir.getAbsolutePath();

        File scriptFile = new File(pluginDir, ".post_install_script.sh");
        String SCRIPT_PATH = scriptFile.getAbsolutePath();
        String port_no = String.valueOf(port);

        Intent intent = new Intent();
        intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
        intent.setAction(ACTION_RUN_COMMAND);

        intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"--port", port_no});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        pluginManagerActivity.startService(intent);

        Log.d("SkyLog","skyUpdate Demo");

    }


    private void downloadZip(String urlStr, File output) throws Exception {

        if (urlStr == null || urlStr.trim().isEmpty() || urlStr.equalsIgnoreCase("empty_url")) {
            Log.d("Download", "Skipping download (empty_url)");
            return;
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        int fileLength = conn.getContentLength();

        InputStream in = conn.getInputStream();
        FileOutputStream out = new FileOutputStream(output);

        byte[] buffer = new byte[8192];
        int len;
        int total = 0;

        while ((len = in.read(buffer)) != -1) {
            total += len;

            if (fileLength > 0) {
                int progress = (int) ((total * 100L) / fileLength);
                updateProgress(progress);
            }

            out.write(buffer, 0, len);
        }

        out.close();
        in.close();
    }

    public void fullUnzip(File zipFile, File targetDir, boolean stripTopLevel) throws Exception {

        if (!targetDir.exists()) targetDir.mkdirs();

        ZipInputStream zis = new ZipInputStream(
            new BufferedInputStream(new FileInputStream(zipFile))
        );

        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {

            String entryName = entry.getName();

            if (stripTopLevel) {
                int firstSlash = entryName.indexOf("/");
                if (firstSlash != -1) {
                    entryName = entryName.substring(firstSlash + 1);
                }
            }

            if (entryName.isEmpty()) {
                zis.closeEntry();
                continue;
            }

            File newFile = new File(targetDir, entryName);

            String targetPath = targetDir.getCanonicalPath();
            String newFilePath = newFile.getCanonicalPath();

            if (!newFilePath.startsWith(targetPath + File.separator)) {
                throw new SecurityException("Zip Slip detected: " + entryName);
            }

            if (entry.isDirectory()) {
                newFile.mkdirs();
            } else {
                File parent = newFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(newFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                byte[] buffer = new byte[8192];
                int len;

                while ((len = zis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }

                bos.close();
                fos.close();
            }

            zis.closeEntry();
        }

        zis.close();
    }

    private void deletePlugin(Plugin plugin, int position) {
        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Delete Plugin")
            .setMessage("Are you sure you want to remove the plugin: " + plugin.title + "?")
            .setPositiveButton("Delete", (d, w) -> {

                if (plugin.uninstall_script != null && !plugin.uninstall_script.trim().isEmpty()) {
                    executeUninstallScriptFromCache(plugin.uninstall_script.trim(), plugin.port);
                }

                list.remove(position);
                PluginStorage.save(this, list);
                adapter.notifyItemRemoved(position);

                String prefName = "port_" + plugin.port;
                getSharedPreferences(prefName, MODE_PRIVATE).edit().clear().commit();

                boolean apiDeleted = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    apiDeleted = deleteSharedPreferences(prefName);
                }

                if (!apiDeleted) {
                    File dataDir = new File(getApplicationInfo().dataDir, "shared_prefs");
                    File prefFile = new File(dataDir, prefName + ".xml");
                    File backupFile = new File(dataDir, prefName + ".bak");

                    if (prefFile.exists()) prefFile.delete();
                    if (backupFile.exists()) backupFile.delete();
                }

                File baseDir = new File(getFilesDir(), "home/plugins");
                File pluginDir = new File(baseDir, String.valueOf(plugin.port));
                if (pluginDir.exists()) {
                    deleteRecursive(pluginDir);
                }

                File zip = new File(getCacheDir(), plugin.port + ".zip");
                if (zip.exists()) {
                    zip.delete();
                }

                restart_request(this);
                Toast.makeText(this, "Plugin " + plugin.port + " deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loginPlugin(Plugin plugin, int position) {

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Open Login Web Page?")
            .setMessage("URL: " + plugin.login_url)
            .setPositiveButton("Open", (d, w) -> {

                Intent intent = new Intent(this, WebViewLoginActivity.class);
                intent.putExtra("url", plugin.login_url);
                startActivity(intent);

            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updatePlugin(Plugin plugin, int position) {

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Update Plugin?")
            .setMessage(plugin.title + " on port " + plugin.port)
            .setPositiveButton("Update", (d, w) -> {

                String TERMUX_PACKAGE = "com.termux";
                String TERMUX_SERVICE = "com.termux.app.RunCommandService";
                String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

                String folderName = String.valueOf(plugin.port);
                File baseDirInit = new File(getFilesDir(), "home");
                File baseDir = new File(baseDirInit, "plugins");
                File pluginDir = new File(baseDir, folderName);

                String HOME_PATH = pluginDir.getAbsolutePath();

                File scriptFile = new File(pluginDir, ".post_install_script.sh");
                String SCRIPT_PATH = scriptFile.getAbsolutePath();
                String port_no = String.valueOf(plugin.port);

                // Required for stopping auto-redirect
                File homeDir = new File(getFilesDir(), "home");
                File launchFile = new File(homeDir, ".launch");

                if (launchFile.exists()) {
                    if (launchFile.delete()) {
                        Log.d("FILE", ".launch deleted successfully");
                    } else {
                        Log.d("FILE", "Failed to delete .launch");
                    }
                }

                restart_request(this);

                Intent intent = new Intent();
                intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
                intent.setAction(ACTION_RUN_COMMAND);

                intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
                intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"--port", port_no, "--update"});
                intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH);
                intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
                intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

                this.startService(intent);

                Log.d("SkyLog","skyUpdate Runner");

            })
            .setNegativeButton("Cancel", null)
            .show();

    }

    private void watchPlugin(Plugin plugin, int position) {

        boolean isTool = (plugin.tool != null) ? plugin.tool : false;

        if (!isTool) {
            Log.d("GIO", plugin.port + " "+plugin.playlist);

            Intent intent = new Intent(this, PlugDRM.class);
            intent.putExtra("playlist_url", plugin.playlist);
            intent.putExtra("port", plugin.port);

            intent.putExtra("login_url", plugin.login_url);
            intent.putExtra("watch_url", plugin.watch_url);

            startActivity(intent);
        } else {

            Log.d("GIO", plugin.port + " "+plugin.playlist);

            Intent intent = new Intent(this, GenericWebActivity.class);
            intent.putExtra("url", plugin.playlist);
            intent.putExtra("port", plugin.port);
            startActivity(intent);

        }




    }

    private void supportPlugin(Plugin plugin, int position) {
        String openUrl = (plugin.support_url != null && !plugin.support_url.trim().isEmpty())
            ? plugin.support_url.trim()
            : (plugin.repo != null ? plugin.repo.trim() : "");

        if (openUrl.isEmpty()) {
            Toast.makeText(this, "Support URL not available", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isGithub = openUrl.toLowerCase().contains("github");
        String title = isGithub ? "Open GitHub Page?" : "Open Support Page?";
        String msg = isGithub
            ? "Get support for " + plugin.title + " on GitHub."
            : "Open support page for " + plugin.title + "?";

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Open", (d, w) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)));
                } catch (Exception e) {
                    Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }


    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }

        if (!file.delete()) {
            Log.w("PluginDelete", "Failed to delete: " + file.getAbsolutePath());
        }
    }

    private void createRunScript(File pluginDir, int port, String startComm) {
        File scriptFile = new File(pluginDir, port + ".sh");

        String scriptContent =
            "#!/bin/bash\n\n" +

                startComm + "\n\n"+

        "# END\n";

        try {
            FileOutputStream fos = new FileOutputStream(scriptFile);
            fos.write(scriptContent.getBytes());
            fos.close();

            scriptFile.setExecutable(true);

            Log.d("PluginScript", "Script created: " + scriptFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("PluginScript", "Error creating script", e);
        }
    }

    private void downloadFile(String urlStr, File output) throws Exception {

        if (urlStr == null || urlStr.trim().isEmpty() || urlStr.equalsIgnoreCase("empty_url")) {
            Log.d("Download", "Skipping download (empty_url)");
            return;
        }


        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream out = null;

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server returned: " + responseCode);
            }

            int fileLength = conn.getContentLength();

            in = conn.getInputStream();
            out = new FileOutputStream(output);

            byte[] buffer = new byte[8192];
            int len;
            int total = 0;

            while ((len = in.read(buffer)) != -1) {
                total += len;

                if (fileLength > 0) {
                    int progress = (int) ((total * 100L) / fileLength);
                    updateProgress(progress);
                }

                out.write(buffer, 0, len);
            }

        } catch (Exception e) {

            if (output.exists()) {
                output.delete();
            }

            throw e;

        } finally {

            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}

            if (conn != null) conn.disconnect();
        }
    }

    private void showProgress(String title) {

        runOnUiThread(() -> {

            progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            );
            progressBar.setMax(100);
            progressBar.setProgress(0);

            progressText = new TextView(this);
            progressText.setText("0%");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);
            layout.addView(progressBar);
            layout.addView(progressText);

            progressDialog = new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
                .setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .show();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateProgress(int progress) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
                progressText.setText(progress + "%");
            }
        });
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception ignored) {}
        });
    }

    private void handleError(String message, Exception e) {
        Log.e("PluginManager", message, e);

        runOnUiThread(() -> {
            hideProgress();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showUrlInputDialog() {
        EditText input = new EditText(this);
        input.setHint("https://example.com/playlist.m3u");
        input.setPadding(50, 20, 50, 20);

        new AlertDialog.Builder(this, R.style.GoldenFocusDialogTheme)
            .setTitle("Add M3U URL")
            .setMessage("Enter the playlist URL or select a file:")
            .setView(input)
            .setPositiveButton("Add URL", (dialog, which) -> {
                String urlString = input.getText().toString().trim();
                if (urlString.isEmpty()) {
                    Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean isValid = Patterns.WEB_URL.matcher(urlString).matches() ||
                    urlString.toLowerCase().contains("://localhost") ||
                    urlString.toLowerCase().contains("://127.0.0.1");

                if (!isValid) {
                    Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    return;
                }
                verifyUrlHasData(urlString);
            })
            .setNeutralButton("Choose File", (dialog, which) -> {
                Intent intent = new Intent(this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.EXTRA_FILTERS, new String[]{".m3u", ".m3u8", ".txt"});
                customFilePickerLauncher.launch(intent);

            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void verifyUrlHasData(String urlString) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean hasData = false;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    hasData = true;
                }
                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                hasData = false;
            }

            final boolean finalHasData = hasData;
            handler.post(() -> {
                if (finalHasData) {
                    processUrlAsPlugin(urlString);
                } else {
                    Toast.makeText(this, "URL is unreachable or contains no data", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void handlePlaylistFile(Uri uri) {

        if ("content".equals(uri.getScheme())) {
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException e) {
                Log.e("PluginManager", "Failed to persist URI permission", e);
            }
        }

        String fileName = getFileName(uri);

        if (fileName == null || !isValidExtension(fileName)) {
            Toast.makeText(this, "Invalid file type. Use a valid playlist.", Toast.LENGTH_LONG).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean hasData = false;
            String errorMsg = null;

            try {
                InputStream is = null;

                if ("file".equals(uri.getScheme())) {
                    File file = new File(uri.getPath());
                    if (file.exists() && file.canRead()) {
                        is = new FileInputStream(file);
                    } else {
                        try {
                            is = getContentResolver().openInputStream(uri);
                        } catch (Exception ex) {
                            errorMsg = "Permission Denied: Android blocked read access.";
                        }
                    }
                } else {
                    is = getContentResolver().openInputStream(uri);
                }

                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                hasData = true;
                                break; // Data found, exit loop!
                            }
                        }
                    }
                    is.close();
                } else if (errorMsg == null) {
                    errorMsg = "Failed to open file stream.";
                }

            } catch (Exception e) {
                Log.e("PluginManager", "File read error", e);
                errorMsg = e.getMessage();
            }

            final boolean finalHasData = hasData;
            final String finalErrorMsg = errorMsg;

            handler.post(() -> {
                if (finalHasData) {
                    Log.d("FOR", uri.toString());
                    processUrlAsPlugin(uri.toString());
                } else {
                    if (finalErrorMsg != null) {
                        Toast.makeText(this, "Error: " + finalErrorMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "File is empty or contains no data", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private boolean isValidExtension(String name) {
        String low = name.toLowerCase();
        return low.endsWith(".m3u") || low.endsWith(".m3u8") ||
            low.endsWith(".php") || low.endsWith(".txt");
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processUrlAsPlugin(String url) {
        int uniquePort = findAvailablePort(6001, 6099);

        if (uniquePort == -1) {
            Toast.makeText(this, "No available ports left", Toast.LENGTH_SHORT).show();
            return;
        }

        String nextTitle = generateNextPlaylistTitle();

        String json = "{\n" +
            "  \"title\": \"" + nextTitle + "\",\n" +
            "  \"port\": " + uniquePort + ",\n" +
            "  \"playlist\": \"" + url + "\"\n" +
            "}";

        Plugin p = PluginUtils.parse(json);

        if (p != null) {
            list.add(p);
            PluginStorage.save(this, list);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, nextTitle + " added", Toast.LENGTH_SHORT).show();
        }
    }

    private int findAvailablePort(int min, int max) {
        Set<Integer> usedPorts = new HashSet<>();
        for (Plugin p : list) {
            usedPorts.add(p.port);
        }

        List<Integer> available = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            if (!usedPorts.contains(i)) {
                available.add(i);
            }
        }

        if (available.isEmpty()) return -1;

        return available.get(new Random().nextInt(available.size()));
    }

    private String generateNextPlaylistTitle() {
        int maxNumber = 0;

        for (Plugin p : list) {
            if (p.title != null && p.title.startsWith("Playlist ")) {
                try {
                    String numPart = p.title.substring(9).trim();
                    int currentNum = Integer.parseInt(numPart);
                    if (currentNum > maxNumber) {
                        maxNumber = currentNum;
                    }
                } catch (Exception ignored) {
                    // If title isn't formatted with a number, just skip it
                }
            }
        }

        int nextNumber = maxNumber + 1;
        return String.format(Locale.US, "Playlist %03d", nextNumber);
    }


}


