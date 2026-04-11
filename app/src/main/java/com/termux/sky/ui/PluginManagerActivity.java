package com.termux.sky.ui;

import android.content.Intent;
import android.net.Uri;
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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginManagerActivity extends AppCompatActivity {

    RecyclerView listView;
    Button btnAdd;

    List<Plugin> list;
    PluginAdapter adapter;

    // 🔥 File picker launcher
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

        adapter = new PluginAdapter(this, list);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        // 📂 Open file picker
        btnAdd.setOnClickListener(v -> showPortInputDialog());
    }

    // 🔥 Handle selected file
    private void handleFile(Uri uri) {
        if (uri == null) return;

        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri))
            );

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();

            // 🔥 Parse JSON
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
            e.printStackTrace();
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

                // read response
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                // parse
                Plugin p = PluginUtils.parse(sb.toString());

                runOnUiThread(() -> {
                    if (p != null) {

                        // 🔥 duplicate check
                        for (Plugin existing : list) {
                            if (existing.port == p.port) {
                                Toast.makeText(this, "Plugin already added", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        initPlugin(p.port, p.repo);

                        list.add(p);
                        PluginStorage.save(this, list);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(this, "Plugin loaded", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() ->
                    Toast.makeText(this, "Error loading plugin", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void initPlugin(int port, String repoUrl) {
        String folderName = String.valueOf(port);

        File baseDirInit = new File(getFilesDir(), "home");
        File baseDir = new File(baseDirInit, "plugins");
        File pluginDir = new File(baseDir, folderName);

        if (!baseDir.exists()) baseDir.mkdirs();

        // Convert repo → zip URL
        String zipUrl = repoUrl.replace(".git", "") + "/archive/refs/heads/main.zip";

        File zipFile = new File(getCacheDir(), folderName + ".zip");

        new Thread(() -> {
            try {
                downloadZip(zipUrl, zipFile);
                fullUnzip(zipFile, pluginDir);

                Log.d("Plugin", "Installed at: " + pluginDir.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void downloadZip(String urlStr, File output) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        InputStream in = conn.getInputStream();
        FileOutputStream out = new FileOutputStream(output);

        byte[] buffer = new byte[4096];
        int len;

        while ((len = in.read(buffer)) != -1) {
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

            // 🔥 Remove top-level folder (e.g., plugin-main/)
            int firstSlash = entryName.indexOf("/");
            if (firstSlash != -1) {
                entryName = entryName.substring(firstSlash + 1);
            }

            // Skip root folder
            if (entryName.isEmpty()) {
                zis.closeEntry();
                continue;
            }

            File newFile = new File(targetDir, entryName);

            // 🔒 Zip Slip protection
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
}


