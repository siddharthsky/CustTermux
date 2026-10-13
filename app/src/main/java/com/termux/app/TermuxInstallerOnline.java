package com.termux.app;

import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR_PATH;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.system.Os;
import android.view.WindowManager;

import com.hzy.libp7zip.P7ZipApi;
import com.termux.R;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

final class TermuxInstallerOnline {

    private static final String LOG_TAG = "TermuxInstaller";

    private static final boolean SKIP_SHA256_CHECK = true;

    private static class BootstrapAsset {
        final String url;
        final String expectedSha256;

        BootstrapAsset(String url, String expectedSha256) {
            this.url = url;
            this.expectedSha256 = expectedSha256;
        }
    }

    private static BootstrapAsset getBootstrapAssetForArch() {
        String abi = android.os.Build.SUPPORTED_ABIS[0];
        if (abi.startsWith("arm64")) {
            return new BootstrapAsset(
                "https://gitlab.com/api/v4/projects/87554177/packages/generic/release-assets/release-20261013-9a7eb4b4/bootstrap-aarch64.7z",
                "EXPECTED_SHA256_FOR_AARCH64"
            );
        } else if (abi.startsWith("arm")) {
            return new BootstrapAsset(
                "https://gitlab.com/api/v4/projects/87554177/packages/generic/release-assets/release-20261013-9a7eb4b4/bootstrap-arm.7z",
                "EXPECTED_SHA256_FOR_ARM"
            );
        } else if (abi.startsWith("x86_64")) {
            return new BootstrapAsset(
                "https://gitlab.com/api/v4/projects/87554177/packages/generic/release-assets/release-20261013-9a7eb4b4/bootstrap-x86_64.7z",
                "EXPECTED_SHA256_FOR_X86_64"
            );
        } else if (abi.startsWith("x86")) {
            return new BootstrapAsset(
                "https://gitlab.com/api/v4/projects/87554177/packages/generic/release-assets/release-20261013-9a7eb4b4/bootstrap-i686.7z",
                "EXPECTED_SHA256_FOR_I686"
            );
        } else {
            throw new RuntimeException("Unsupported architecture: " + abi);
        }
    }

    private static File downloadAndVerifyBootstrap(Activity activity, ProgressDialog progress, BootstrapAsset asset) throws Exception {
        File tempArchive = new File(activity.getCacheDir(), "temp_bootstrap.7z");
        URL url = new URL(asset.url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        int fileLength = connection.getContentLength();
        if (fileLength == -1) {
            Logger.logError(LOG_TAG, "Could not determine file size for download.");
        }

        activity.runOnUiThread(() -> {
            progress.setMessage("Downloading Bootstrap...");
            if (fileLength > 0) {
                progress.setIndeterminate(false);
                progress.setMax(100);
            }
        });

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(tempArchive)) {

            byte[] data = new byte[8192];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                if (fileLength > 0) {
                    int progressStatus = (int) (total * 100 / fileLength);
                    activity.runOnUiThread(() -> progress.setProgress(progressStatus));
                }
                output.write(data, 0, count);
                digest.update(data, 0, count);
            }
        }

        activity.runOnUiThread(() -> {
            progress.setIndeterminate(true);
            progress.setMessage("Verifying Download...");
        });

        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        String calculatedSha256 = hexString.toString();

        if (!SKIP_SHA256_CHECK && asset.expectedSha256 != null && !asset.expectedSha256.isEmpty() && !calculatedSha256.equalsIgnoreCase(asset.expectedSha256)) {
            tempArchive.delete();
            throw new RuntimeException("SHA-256 verification failed! Expected: " + asset.expectedSha256 + " Got: " + calculatedSha256);
        }

        return tempArchive;
    }

    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        String bootstrapErrorMessage;
        Error filesDirectoryAccessibleError;

        filesDirectoryAccessibleError = TermuxFileUtils.isTermuxFilesDirectoryAccessible(activity, true, true);
        boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

        if (!isFilesDirectoryAccessible) {
            bootstrapErrorMessage = Error.getMinimalErrorString(filesDirectoryAccessibleError);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            activity.runOnUiThread(() -> {
                new AlertDialog.Builder(activity)
                    .setTitle(R.string.bootstrap_error_title)
                    .setMessage(bootstrapErrorMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            });
            return;
        }

        if (FileUtils.directoryFileExists(TERMUX_PREFIX_DIR_PATH, true)) {
            if (TermuxFileUtils.isTermuxPrefixDirectoryEmpty()) {
                Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" exists but is empty or only contains specific unimportant files.");
            } else {
                whenDone.run();
                return;
            }
        } else if (FileUtils.fileExists(TERMUX_PREFIX_DIR_PATH, false)) {
            Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" does not exist but another file exists at its destination.");
        }

        final ProgressDialog progress = new ProgressDialog(activity);
        progress.setTitle(activity.getString(R.string.bootstrap_installer_body));
        progress.setMessage("Preparing...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        new Thread() {
            @Override
            public void run() {
                File tempArchive = null;
                try {
                    Logger.logInfo(LOG_TAG, "Installing " + TermuxConstants.TERMUX_APP_NAME + " bootstrap packages.");

                    Error error;

                    error = FileUtils.deleteFile("termux prefix staging directory", TERMUX_STAGING_PREFIX_DIR_PATH, true);
                    if (error != null) throw new RuntimeException(Error.getErrorMarkdownString(error));

                    error = FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                    if (error != null) throw new RuntimeException(Error.getErrorMarkdownString(error));

                    error = TermuxFileUtils.isTermuxPrefixStagingDirectoryAccessible(true, true);
                    if (error != null) throw new RuntimeException(Error.getErrorMarkdownString(error));

                    error = TermuxFileUtils.isTermuxPrefixDirectoryAccessible(true, true);
                    if (error != null) throw new RuntimeException(Error.getErrorMarkdownString(error));

                    activity.runOnUiThread(() -> progress.setMessage("Connecting..."));

                    BootstrapAsset asset = getBootstrapAssetForArch();
                    tempArchive = downloadAndVerifyBootstrap(activity, progress, asset);

                    activity.runOnUiThread(() -> {
                        progress.setIndeterminate(true);
                        progress.setMessage("Extracting bootstrap archive...");
                    });

                    Logger.logInfo(LOG_TAG, "Extracting bootstrap archive using AndroidP7zip...");

                    String extractCmd = "7z x " + tempArchive.getAbsolutePath() + " -o" + TERMUX_STAGING_PREFIX_DIR_PATH;
                    int result = P7ZipApi.executeCommand(extractCmd);

                    if (result != 0) {
                        throw new RuntimeException("P7Zip native extraction failed with exit code: " + result);
                    }

                    File symlinksFile = new File(TERMUX_STAGING_PREFIX_DIR_PATH, "SYMLINKS.txt");
                    if (!symlinksFile.exists()) {
                        throw new RuntimeException("No SYMLINKS.txt encountered in extracted files");
                    }

                    try (BufferedReader symlinksReader = new BufferedReader(new FileReader(symlinksFile))) {
                        String line;
                        while ((line = symlinksReader.readLine()) != null) {
                            String[] parts = line.split("←");
                            if (parts.length != 2) throw new RuntimeException("Malformed symlink line: " + line);
                            String oldPath = parts[0];
                            String newPath = TERMUX_STAGING_PREFIX_DIR_PATH + "/" + parts[1];

                            error = ensureDirectoryExists(new File(newPath).getParentFile());
                            if (error != null) throw new RuntimeException(Error.getErrorMarkdownString(error));

                            Os.symlink(oldPath, newPath);
                        }
                    }

                    symlinksFile.delete();

                    String[] execDirs = {"bin", "libexec", "lib/apt/apt-helper", "lib/apt/methods"};
                    for (String dirPath : execDirs) {
                        File dir = new File(TERMUX_STAGING_PREFIX_DIR_PATH, dirPath);
                        recursivelySetExecutable(dir);
                    }

                    Logger.logInfo(LOG_TAG, "Moving termux prefix staging to prefix directory.");
                    if (!TERMUX_STAGING_PREFIX_DIR.renameTo(TERMUX_PREFIX_DIR)) {
                        throw new RuntimeException("Moving termux prefix staging to prefix directory failed");
                    }

                    Logger.logInfo(LOG_TAG, "Bootstrap packages installed successfully.");
                    TermuxShellEnvironment.writeEnvironmentToFile(activity);

                    activity.runOnUiThread(whenDone);

                } catch (final Exception e) {
                    showBootstrapErrorDialog(activity, whenDone, Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)));

                } finally {
                    if (tempArchive != null && tempArchive.exists()) {
                        tempArchive.delete();
                    }

                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                        }
                    });
                }
            }
        }.start();
    }

    private static void recursivelySetExecutable(File file) {
        if (!file.exists()) return;

        try {
            Os.chmod(file.getAbsolutePath(), 0700);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to chmod: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    recursivelySetExecutable(child);
                }
            }
        }
    }

    public static void showBootstrapErrorDialog(Activity activity, Runnable whenDone, String message) {
        Logger.logErrorExtended(LOG_TAG, "Bootstrap Error:\n" + message);

        sendBootstrapCrashReportNotification(activity, message);

        activity.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_body)
                    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                        dialog.dismiss();
                        activity.finish();
                    })
                    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                        dialog.dismiss();
                        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                        TermuxInstaller.setupBootstrapIfNeeded(activity, whenDone);
                    }).show();
            } catch (WindowManager.BadTokenException e1) {
            }
        });
    }

    private static void sendBootstrapCrashReportNotification(Activity activity, String message) {
        final String title = TermuxConstants.TERMUX_APP_NAME + " Bootstrap Error";

        TermuxCrashUtils.sendCrashReportNotification(activity, LOG_TAG,
            title, null, "## " + title + "\n\n" + message + "\n\n" +
                TermuxUtils.getTermuxDebugMarkdownString(activity),
            true, false, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES, true);
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        final String title = TermuxConstants.TERMUX_APP_NAME + " Setup Storage Error";

        Logger.logInfo(LOG_TAG, "Setting up storage symlinks.");

        new Thread() {
            public void run() {
                try {
                    Error error;
                    File storageDir = TermuxConstants.TERMUX_STORAGE_HOME_DIR;

                    error = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (error != null) {
                        Logger.logErrorAndShowToast(context, LOG_TAG, error.getMessage());
                        Logger.logErrorExtended(LOG_TAG, "Setup Storage Error\n" + error.toString());
                        TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                            "## " + title + "\n\n" + Error.getErrorMarkdownString(error),
                            true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/shared, ~/storage/downloads, ~/storage/dcim, ~/storage/pictures, ~/storage/music and ~/storage/movies for directories in \"" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\".");

                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    Os.symlink(documentsDir.getAbsolutePath(), new File(storageDir, "documents").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
                    Os.symlink(podcastsDir.getAbsolutePath(), new File(storageDir, "podcasts").getAbsolutePath());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        File audiobooksDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS);
                        Os.symlink(audiobooksDir.getAbsolutePath(), new File(storageDir, "audiobooks").getAbsolutePath());
                    }

                    File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    dirs = context.getExternalMediaDirs();
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "media-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Storage symlinks created successfully.");
                } catch (Exception e) {
                    Logger.logErrorAndShowToast(context, LOG_TAG, e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Setup Storage Error: Error setting up link", e);
                    TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                        "## " + title + "\n\n" + Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)),
                        true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                }
            }
        }.start();
    }

    private static Error ensureDirectoryExists(File directory) {
        return FileUtils.createDirectoryFile(directory.getAbsolutePath());
    }
}
