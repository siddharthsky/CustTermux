package com.termux.sky.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

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
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginSetupManager {

    private final Activity activity;
    private final InstallCallback callback;

    public interface InstallCallback {
        void onShowProgress(String title);
        void onUpdateProgress(int progress);
        void onHideProgress();
        void onError(String message, Exception e);
        void onRestartRequired();
    }

    public PluginSetupManager(Activity activity, InstallCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void initPlugin(int port, String repoUrl, String repoBranch, String startComm,
                           String binDownloadURL, String postInstallScript) {

        String folderName = String.valueOf(port);

        File baseDirInit = new File(activity.getFilesDir(), "home");
        File baseDir = new File(baseDirInit, "plugins");
        File pluginDir = new File(baseDir, folderName);

        if (!baseDir.exists()) baseDir.mkdirs();
        if (!pluginDir.exists()) pluginDir.mkdirs();

        File zipFile = new File(activity.getCacheDir(), folderName + ".zip");
        new Thread(() -> {
            try {
                boolean hasBin = binDownloadURL != null && !binDownloadURL.trim().isEmpty();
                boolean hasRepo = repoUrl != null && !repoUrl.trim().isEmpty();

                if (!hasBin && !hasRepo) {
                    Log.d("PluginInit", "No download URLs provided. Skipping download/extraction.");
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

                restart_request();

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
                run_post_script(pluginDir, port);
                restart_request();

            } catch (Exception e) {
                handleError("Post-install setup failed", e);
            }
        }).start();
    }

    public void executeUninstallScriptFromCache(String scriptContent, int port) {
        new Thread(() -> {
            try {
                File cacheDir = activity.getCacheDir();
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
                run_cached_uninstall_runner(tempUninstallFile);

            } catch (Exception e) {
                handleError("Uninstall script tracking failed", e);
            }
        }).start();
    }

    private void run_cached_uninstall_runner(File scriptFile) {
        File homeDir = new File(activity.getFilesDir(), "home");
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

        File fallbackWorkDir = new File(activity.getFilesDir(), "home");
        String SCRIPT_PATH = scriptFile.getAbsolutePath();

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

    public void restart_request() {
        SharedPreferences settings = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        settings.edit().putBoolean("plugin_restart", true).apply();
        if (callback != null) {
            callback.onRestartRequired();
        }
    }

    public void run_post_script(File pluginDir, int port) {
        String TERMUX_PACKAGE = "com.termux";
        String TERMUX_SERVICE = "com.termux.app.RunCommandService";
        String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

        File homeDir = new File(activity.getFilesDir(), "home");
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

        activity.startService(intent);
        Log.d("SkyLog", "skyUpdate Demo");
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
        conn.disconnect();
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

    private void createRunScript(File pluginDir, int port, String startComm) {
        File scriptFile = new File(pluginDir, port + ".sh");
        String scriptContent = "#!/bin/bash\n\n" + startComm + "\n\n# END\n";
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
        if (callback != null) {
            callback.onShowProgress(title);
        }
    }

    private void updateProgress(int progress) {
        if (callback != null) {
            callback.onUpdateProgress(progress);
        }
    }

    private void hideProgress() {
        if (callback != null) {
            callback.onHideProgress();
        }
    }

    private void handleError(String message, Exception e) {
        if (callback != null) {
            callback.onError(message, e);
        }
    }
}
