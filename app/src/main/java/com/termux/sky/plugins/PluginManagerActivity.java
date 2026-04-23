package com.termux.sky.plugins;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginManagerActivity extends AppCompatActivity {

    RecyclerView listView;
    Button btnAdd;

    List<Plugin> list;
    PluginAdapter adapter;

    private androidx.appcompat.app.AlertDialog progressDialog;
    private android.widget.ProgressBar progressBar;
    private android.widget.TextView progressText;

    ActivityResultLauncher<String> filePicker =
        registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleFile
        );

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.plugin_manager_activity);

        listView = findViewById(R.id.pluginList);
        btnAdd = findViewById(R.id.btnAdd);

        list = PluginStorage.load(this);

        adapter = new PluginAdapter(this, list, this::loginPlugin,this::watchPlugin, this::deletePlugin );

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showPortInputDialog());
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
                list.add(p);
                PluginStorage.save(this, list);
                adapter.notifyDataSetChanged();

                Toast.makeText(this, "Plugin added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("PluginManager", "Error reading file:", e);
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPortInputDialog() {

        EditText input = new EditText(this);
        input.setHint("Enter plugin code (4-digits)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.setShowSoftInputOnFocus(true);

        AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Plugin")
            .setView(input)
            .setPositiveButton("Load", (d, w) -> {

                String port = input.getText().toString().trim();

                if (port.isEmpty()) {
                    Toast.makeText(this, "Enter port", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchPluginFromServer(port);
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(d -> {

            input.requestFocus();

            input.postDelayed(() -> {

                InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);

            }, 150);
        });

        dialog.show();
    }

    private void fetchPluginFromServer(String port) {

        new Thread(() -> {
            try {
                String urlStr = "https://raw.githubusercontent.com/siddharthsky/Extrix/refs/heads/main/Misc/" + port + ".json";

                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);

                int code = conn.getResponseCode();

                if (code != 200) {
                    runOnUiThread(() ->
                        Toast.makeText(this, "Plugin not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
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

                if (binDownloadURL != null && !binDownloadURL.trim().isEmpty()) {

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

                if (repoUrl != null && repoUrl.endsWith(".zip")) {
                    zipUrl = repoUrl;
                } else {
                    assert repoUrl != null;
                    zipUrl = getRepoZipUrl(repoUrl, repoBranch);
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

            } catch (Exception e) {
                handleError("Post-install script failed", e);
            }
        }).start();
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Plugin")
            .setMessage("Are you sure you want to delete the plugin on port " + plugin.port + "?")
            .setPositiveButton("Delete", (d, w) -> {

                list.remove(position);
                PluginStorage.save(this, list);
                adapter.notifyItemRemoved(position);

                String prefName = "port_" + plugin.port;

                getSharedPreferences(prefName, MODE_PRIVATE).edit().clear().commit();

                boolean apiDeleted = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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

        if (urlStr == null || urlStr.trim().equals("empty_url")) {
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

            progressBar = new android.widget.ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            );
            progressBar.setMax(100);
            progressBar.setProgress(0);

            progressText = new android.widget.TextView(this);
            progressText.setText("0%");

            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);
            layout.addView(progressBar);
            layout.addView(progressText);

            progressDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
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
}


