package com.termux.sky.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.termux.R;

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
import java.nio.file.Files;
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
        setContentView(R.layout.activity_plugin_manager);

        listView = findViewById(R.id.pluginList);
        btnAdd = findViewById(R.id.btnAdd);

        list = PluginStorage.load(this);

        adapter = new PluginAdapter(this, list, this::deletePlugin);

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

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter port (e.g. 8383)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new androidx.appcompat.app.AlertDialog.Builder(this)
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
            .show();
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

                        initPlugin(p.port, p.repo, p.start, p.bin_download, Arrays.toString(p.pkg));

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

    public void initPlugin(int port, String repoUrl, String startComm,
                           String binDownloadURL, String pkgList) {

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
                    downloadFile(binDownloadURL, binFile);

                    binFile.setExecutable(true);

                    createRunScript(pluginDir, port, startComm);

                    hideProgress();


                    Log.d("Plugin", "Binary plugin installed: " + pluginDir.getAbsolutePath());
                    return;
                }



                String zipUrl = repoUrl.replace(".git", "") + "/archive/refs/heads/main.zip";

                showProgress("Downloading repository...");

                downloadZip(zipUrl, zipFile);

                hideProgress();

                showProgress("Extracting...");

                fullUnzip(zipFile, pluginDir);

                createRunScript(pluginDir, port, startComm);

                hideProgress();


                Log.d("Plugin", "Repo plugin installed: " + pluginDir.getAbsolutePath());

            } catch (Exception e) {
                Log.e("PluginManager", "Plugin install error:", e);
            }
        }).start();
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

    public void fullUnzip(File zipFile, File targetDir) throws Exception {

        if (!targetDir.exists()) targetDir.mkdirs();

        ZipInputStream zis = new ZipInputStream(
            new BufferedInputStream(new FileInputStream(zipFile))
        );

        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {

            String entryName = entry.getName();

            int firstSlash = entryName.indexOf("/");
            if (firstSlash != -1) {
                entryName = entryName.substring(firstSlash + 1);
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
            .setMessage("Delete plugin on port " + plugin.port + "?")
            .setPositiveButton("Delete", (d, w) -> {

                list.remove(position);
                PluginStorage.save(this, list);
                adapter.notifyItemRemoved(position);

                File baseDir = new File(getFilesDir(), "home/plugins");
                File pluginDir = new File(baseDir, String.valueOf(plugin.port));

                deleteRecursive(pluginDir);

                File zip = new File(getCacheDir(), plugin.port + ".zip");
                if (zip.exists() && !zip.delete()) {
                    Log.w("PluginDelete", "Failed to delete zip: " + zip.getAbsolutePath());
                }

                Toast.makeText(this, "Plugin deleted", Toast.LENGTH_SHORT).show();
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
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }
}


