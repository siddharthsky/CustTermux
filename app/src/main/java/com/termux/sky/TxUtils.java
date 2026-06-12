package com.termux.sky;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.termux.R;
import com.termux.sky.filehandlers.FilePickerActivity;
import com.termux.view.TerminalView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TxUtils {


    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_SERVICE = "com.termux.app.RunCommandService";
    private static final String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

    private static final String HOME_PATH = "/data/data/com.termux/files/home";
    private static final String SCRIPT_PATH = HOME_PATH + "/.skyutils.sh";

    public static final int REQUEST_CODE_RESTORE = 8008;
    private static final int XOR_KEY = 42;

    public static void showCustomToast(Context context, String message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public static void sky_update(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction(ACTION_RUN_COMMAND);

        intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        context.startService(intent);

        Log.d("SkyLog","skyUpdate Demo");
    }

    public static void terminal_switch_dialog(Context context) {
        AlertDialog.Builder builder =
            new AlertDialog.Builder(context, R.style.GoldenFocusDialogTheme);

        builder.setTitle("Confirmation");
        builder.setMessage("Do you want to proceed?\n[Note: To exit press back button, reopen]");

        builder.setPositiveButton("OK", (dialog, id) -> sky_terminal(context));
        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(Color.WHITE);
            posButton.setFocusable(true);
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(Color.WHITE);
            negButton.setFocusable(true);
        }
    }

    private static void sky_terminal(Context context) {
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        TerminalView terminalView = activity.findViewById(R.id.terminal_view);

        if (terminalView != null) {
            terminalView.setFocusableInTouchMode(true);
            terminalView.setFocusable(true);
            terminalView.requestFocus();
        }
    }

    public static void run_script(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction(ACTION_RUN_COMMAND);

        intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH);
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        context.startService(intent);

        Log.d("SkyLog","skyUpdate Demo");
    }

    static void showAutoStartDialog(Context context) {
        String[] choices = {
            "Run on boot (in background)",
            "Run on boot (open app)",
            "Disable auto start"
        };

        String mode = SkySharedPref.getAutoStartMode(context);
        int defaultIndex;

        switch (mode) {
            case "background":
                defaultIndex = 0;
                break;
            case "foreground":
                defaultIndex = 1;
                break;
            case "disabled":
            default:
                defaultIndex = 2;
                break;
        }

        final int[] selected = {defaultIndex};

        AlertDialog.Builder builder = new AlertDialog.Builder(
            context,
            R.style.GoldenFocusDialogTheme
        );

        builder.setTitle("Auto start on boot?");

        builder.setSingleChoiceItems(choices, selected[0], (dialog, which) -> {
            selected[0] = which;
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            if (selected[0] == 0) {
                SkySharedPref.setAutoStart(context, true);
                SkySharedPref.setAutoStartMode(context, "background");
                Toast.makeText(context, "Enabled on boot (Background)", Toast.LENGTH_LONG).show();

            } else if (selected[0] == 1) {
                SkySharedPref.setAutoStart(context, true);
                SkySharedPref.setAutoStartMode(context, "foreground");
                Toast.makeText(context, "Enabled on boot (Foreground)", Toast.LENGTH_LONG).show();

            } else {
                SkySharedPref.setAutoStart(context, false);
                SkySharedPref.setAutoStartMode(context, "disabled");
                Toast.makeText(context, "Disabled auto start", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(Color.WHITE);
            posButton.setFocusable(true);
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(Color.WHITE);
            negButton.setFocusable(true);
        }
    }

    public static void showBackupRestoreDialog(Context context) {
        String[] choices = {
            "Backup Data",
            "Restore Data",
            "Clear Data"
        };

        final int[] selected = {0};

        AlertDialog.Builder builder = new AlertDialog.Builder(
            context,
            R.style.GoldenFocusDialogTheme
        );

        builder.setTitle("Manage App Data");

        builder.setSingleChoiceItems(choices, selected[0], (dialog, which) -> {
            selected[0] = which;
        });

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            if (selected[0] == 0) {
                doBackup(context);
            } else if (selected[0] == 1) {
                doRestore(context);
            } else if (selected[0] == 2) {
                doClearData(context);
            }
        });

        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(Color.WHITE);
            posButton.setFocusable(true);
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(Color.WHITE);
            negButton.setFocusable(true);
        }
    }

    private static AlertDialog createProgressDialog(Context context, String message) {
        int padding = 50;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(padding, padding, padding, padding);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#1A1A1B"));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.setLayoutParams(layoutParams);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);

        progressBar.getIndeterminateDrawable().setTint(
            Color.parseColor("#FFD700")
        );

        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
            80,
            80
        );
        progressBar.setLayoutParams(pbParams);

        layout.addView(progressBar);

        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(padding, 0, 0, 0);
        textView.setLayoutParams(textParams);

        layout.addView(textView);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.GoldenFocusDialogTheme);
        builder.setCancelable(false);
        builder.setView(layout);

        return builder.create();
    }

    private static void doBackup(Context context) {
        if (!(context instanceof Activity)) return;

        AlertDialog progressDialog = createProgressDialog(context, "Backing up data...\nThis may take a moment.");
        progressDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new Thread(() -> {
                try {
                    File dataDir = context.getFilesDir().getParentFile();
                    File homeDir = new File(dataDir, "files/home");
                    File prefsDir = new File(dataDir, "shared_prefs");

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) downloadsDir.mkdirs();

                    String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date());
                    String fileName = "CTxEngine-backup-" + timeStamp + ".bak";
                    File finalBackupFile = new File(downloadsDir, fileName);


                    File tempZip = new File(context.getCacheDir(), "temp_backup.zip");

                    List<File> dirsToZip = new ArrayList<>();
                    if (homeDir.exists()) dirsToZip.add(homeDir);
                    if (prefsDir.exists()) dirsToZip.add(prefsDir);


                    zipDirectories(dataDir, dirsToZip.toArray(new File[0]), tempZip);


                    applyXorToFile(tempZip, finalBackupFile);


                    tempZip.delete();

                    ((Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Backup saved to Downloads:\n" + finalBackupFile.getName(), Toast.LENGTH_LONG).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Backup Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }, 100);
    }

    private static void doRestore(Context context) {
        if (!(context instanceof Activity)) {
            Toast.makeText(context, "Error: Context is not an activity.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, FilePickerActivity.class);

        intent.putExtra(FilePickerActivity.EXTRA_FILTERS, new String[]{".bak"});
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_RESTORE);
    }

    public static void doClearData(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.GoldenFocusDialogTheme);
        builder.setTitle("Warning: Clear All Data");
        builder.setMessage("This will permanently delete ALL app data, settings, and files. The app will close immediately.\n\nAre you sure you want to continue?");

        builder.setPositiveButton("Clear Data", (dialog, id) -> {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    if (am != null) {
                        am.clearApplicationUserData();
                    }
                } else {
                    Runtime.getRuntime().exec("pm clear " + context.getPackageName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to clear data.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(Color.parseColor("#FF5252"));
            posButton.setFocusable(true);
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(Color.WHITE);
            negButton.setFocusable(true);
        }
    }


    private static void zipDirectories(File rootDir, File[] targetDirs, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File dir : targetDirs) {
                if (dir.exists()) {
                    zipFileOrDirectory(dir, rootDir, zos);
                }
            }
        }
    }

    private static void zipFileOrDirectory(File fileToZip, File rootDir, ZipOutputStream zos) throws IOException {
        String rootPath = rootDir.getAbsolutePath();
        if (!rootPath.endsWith(File.separator)) {
            rootPath += File.separator;
        }

        String absolutePath = fileToZip.getAbsolutePath();
        String relativePath = absolutePath;

        if (absolutePath.startsWith(rootPath)) {
            relativePath = absolutePath.substring(rootPath.length());
        }

        if (fileToZip.isDirectory()) {
            if (!relativePath.isEmpty() && !relativePath.endsWith("/")) {
                relativePath += "/";
            }
            if (!relativePath.isEmpty()) {
                zos.putNextEntry(new ZipEntry(relativePath));
                zos.closeEntry();
            }

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFileOrDirectory(childFile, rootDir, zos);
                }
            }
        } else {
            zos.putNextEntry(new ZipEntry(relativePath));
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }
    }

    public static void handleRestoreResult(Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RESTORE && resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                if (!(context instanceof Activity)) return;

                File selectedFile = new File(fileUri.getPath());
                String fileName = selectedFile.getName().toLowerCase();


                if (!fileName.startsWith("ctx") || !fileName.endsWith(".bak")) {
                    Toast.makeText(context, "Invalid backup file. Only .bak files are supported.", Toast.LENGTH_LONG).show();
                    return;
                }

                AlertDialog progressDialog = createProgressDialog(context, "Restoring data...\nPlease do not close the app.");
                progressDialog.show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    new Thread(() -> {
                        try {
                            File targetDir = context.getFilesDir().getParentFile();


                            File tempZip = new File(context.getCacheDir(), "temp_restore.zip");


                            applyXorToFile(selectedFile, tempZip);


                            unzip(tempZip, targetDir);


                            tempZip.delete();

                            ((Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(context, "Restore complete! Restarting app...", Toast.LENGTH_LONG).show();
                            });

                            Thread.sleep(1500);
                            restartApp(context);

                        } catch (Exception e) {
                            e.printStackTrace();
                            ((Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(context, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                }, 100);
            }
        }
    }

    public static void restartApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            if (context instanceof Activity) {
                ((Activity) context).finish();
            }

            Log.d("SkyLog", "Out Of The App - Restarting");
            System.exit(0);
        }
    }

    private static void unzip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }

                    if ("run.bin".equals(newFile.getName())) {
                        newFile.setExecutable(true);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static void doPluginClearData(Context context) {

        AlertDialog.Builder builder =
            new AlertDialog.Builder(context, R.style.GoldenFocusDialogTheme);

        builder.setTitle("Clear Plugins");
        builder.setMessage(
            "This will remove plugin configuration, port settings, and all plugin files.\n\n" +
                "Continue?"
        );

        builder.setPositiveButton("Clear", (dialog, id) -> {
            try {
                int deletedPrefs = 0;


                File prefsDir = new File(
                    context.getApplicationInfo().dataDir,
                    "shared_prefs"
                );

                if (prefsDir.exists() && prefsDir.isDirectory()) {
                    File[] files = prefsDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String name = file.getName();


                            if ("plugins_pref.xml".equals(name)) {
                                if (file.delete()) deletedPrefs++;
                            }


                            if (name.startsWith("port_") && name.endsWith(".xml")) {
                                if (file.delete()) deletedPrefs++;
                            }
                        }
                    }
                }

                File pluginsDir = new File(context.getFilesDir(), "home/plugins");
                boolean filesCleared = false;

                if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                    File[] pluginFiles = pluginsDir.listFiles();
                    if (pluginFiles != null) {
                        for (File child : pluginFiles) {
                            deleteRecursive(child);
                        }
                        filesCleared = true;
                    }
                }

                Toast.makeText(
                    context,
                    "Removed " + deletedPrefs + " config files & cleared plugin directory.",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(
                    context,
                    "Failed to clear plugins.",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        builder.setNegativeButton(
            "Cancel",
            (dialog, id) -> dialog.dismiss()
        );

        AlertDialog dialog = builder.create();
        dialog.show();

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setBackgroundTintList(null);
            posButton.setBackgroundResource(R.drawable.golden_focus_selector);
            posButton.setTextColor(Color.parseColor("#FFB300"));
            posButton.setFocusable(true);
        }

        if (negButton != null) {
            negButton.setBackgroundTintList(null);
            negButton.setBackgroundResource(R.drawable.golden_focus_selector);
            negButton.setTextColor(Color.WHITE);
            negButton.setFocusable(true);
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        try {
            if (fileOrDirectory.isDirectory()) {
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            fileOrDirectory.delete();
        } catch(Exception e) {
            Log.d("TxUtils", String.valueOf(e));

        }
    }


    private static void applyXorToFile(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    buffer[i] = (byte) (buffer[i] ^ XOR_KEY);
                }
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}
