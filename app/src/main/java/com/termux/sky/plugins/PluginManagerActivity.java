package com.termux.sky.plugins;

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
import androidx.recyclerview.widget.*;

import com.termux.R;
import com.termux.sky.txplayer.PlugDRM;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    // Replace your existing filePicker with this
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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.plugin_manager_activity);

        listView = findViewById(R.id.pluginList);
        btnAdd = findViewById(R.id.btnAdd);
        restartBanner = findViewById(R.id.restartBanner);

        list = PluginStorage.load(this);

        adapter = new PluginAdapter(this, list, this::loginPlugin, this::updatePlugin, this::watchPlugin, this::deletePlugin );

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showPortInputDialog());

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
                filePicker.launch("application/json");
                Toast.makeText(this, "Add JSON...", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }


    private void handleFile(Uri uri) {
        if (uri == null) return;

        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getContentResolver().openInputStream(uri)))
            );

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();

            // JSON parse
            Plugin p = PluginUtils.parse(sb.toString());

            if (p != null) {
                // Check if plugin already exists to avoid duplicates
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

        AlertDialog dialog = new AlertDialog.Builder(this)
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

                fetchPluginFromServer(port);
                dialog.dismiss();
            });

            input.requestFocus();
        });

        dialog.show();
    }
    private void fetchPluginFromServer(String port) {

        new Thread(() -> {
            try {
                String urlStr = "https://raw.githubusercontent.com/siddharthsky/Extrix/refs/heads/main/Misc/" + port + ".json";

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

                showProgress("Downloading script...");

                downloadFile(postInstallScript, postFile);

                postFile.setExecutable(true);

                hideProgress();

                run_post_script(this, pluginDir, port);

                restart_request(this);

            } catch (Exception e) {
                handleError("Post-install script failed", e);
            }
        }).start();
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
        new AlertDialog.Builder(this)
            .setTitle("Delete Plugin")
            .setMessage("Are you sure you want to remove the plugin: " + plugin.title + "?")
            .setPositiveButton("Delete", (d, w) -> {

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
                    if (!zip.delete()) {
                        Log.w("PluginDelete", "Failed to delete zip: " + zip.getAbsolutePath());
                    }
                }

                Toast.makeText(this, "Plugin " + plugin.port + " deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loginPlugin(Plugin plugin, int position) {

        new AlertDialog.Builder(this)
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

        new AlertDialog.Builder(this)
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

        Log.d("GIO", plugin.port + " "+plugin.playlist);

        Intent intent = new Intent(this, PlugDRM.class);
        intent.putExtra("playlist_url", plugin.playlist);
        intent.putExtra("port", plugin.port);
        startActivity(intent);

//        Intent intent = new Intent(this, ExoPlayerActivityDRM.class);
//        intent.putExtra("url", plugin.watch_url);
//        startActivity(intent);

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

            progressDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .show();
        });
    }

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

        new AlertDialog.Builder(this)
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
                filePicker.launch("*/*");
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

        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException e) {
            Log.e("PluginManager", "Failed to persist URI permission", e);
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
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        hasData = true;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e("PluginManager", "File read error", e);
            }

            boolean finalHasData = hasData;
            handler.post(() -> {
                if (finalHasData) {
                    processUrlAsPlugin(uri.toString());
                } else {
                    Toast.makeText(this, "File is empty or contains no data", Toast.LENGTH_SHORT).show();
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
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

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


