package com.termux;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.termux.app.TermuxActivity;
import com.termux.setup_app.SetupActivityApp;
import com.termux.view.TerminalView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class Utils2 {
    public static void showCustomToast(Context context, String message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast2, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
//    public static void changeIconToSecond(Context context) {
//        // disables the first icon
//        PackageManager packageManager = context.getPackageManager();
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivity"),
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP);
//
//        // enables the second icon
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivityAlias"),
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP);
//    }
//
//    public static void changeIconTOFirst(Context context) {
//        // disables the first icon
//        PackageManager packageManager = context.getPackageManager();
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivity"),
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP);
//
//        // enables the second icon
//        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.app.TermuxActivityAlias"),
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP);
//    }

    public static void changeIconToSecond(Context context) {
        // disables the first icon
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStart"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);

        // enables the second icon
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStartAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);
    }

    public static void changeIconTOFirst(Context context) {
        // disables the first icon
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStart"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);

        // enables the second icon
        packageManager.setComponentEnabledSetting(new ComponentName(context, "com.termux.KickStartAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }

    public static void sky_rerun(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            Log.d("SkyLog", "Out Of The App");
            context.startActivity(intent);
            System.exit(0);
        }
    }

    public static void sky_update(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_reinstall(Context context) {
        SkySharedPref preferenceManager = new SkySharedPref(context);
        preferenceManager.setKey("isServerSetupDone", null);

        preferenceManager.setKey("isLocalNOPORT", "http://localhost:");
        preferenceManager.setKey("isLocalPORT", "http://localhost:5001/");
        preferenceManager.setKey("isLocalPORTchannel", "live/144.m3u8");
        preferenceManager.setKey("isLocalPORTonly", "5001");
        preferenceManager.setKey("server_setup_isLoginCheck", "Yes");
        preferenceManager.setKey("server_setup_isAutoboot", "No");
        preferenceManager.setKey("server_setup_isLocal", "No");
        preferenceManager.setKey("app_name", "null");
        preferenceManager.setKey("app_launchactivity", "null");
        preferenceManager.setKey("isExit", "noExit");
        preferenceManager.setKey("server_setup_isEPG", "Yes");
        preferenceManager.setKey("server_setup_isGenericBanner", "No");
        preferenceManager.setKey("server_setup_isSSH", "No");
        preferenceManager.setKey("isDelayTime", "5");
        preferenceManager.setKey("permissionRequestCount", "0");
        preferenceManager.setKey("isFlagSetForMinimize", "No");
        preferenceManager.setKey("isWEBTVconfig", " ");

        File downloadDir = Utils2.getDownloadDirectory(context);
        File file = new File(downloadDir, "update.apk");
        boolean isDeleted = Utils2.deleteFile(file);

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"reinstall2"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void lake_alert_confirmation(Context context) {
        // Create an AlertDialog Builder
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        // Set the message and the title
        builder.setMessage("Do you want to proceed?\n[Note: To exit press back button, reopen]")
            .setTitle("Confirmation");

        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sky_terminal(context);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        android.app.AlertDialog dialog = builder.create();

        // Show the AlertDialog
        dialog.show();
    }

    private static void sky_terminal(Context context) {
        TerminalView terminalView = ((Activity) context).findViewById(R.id.terminal_view);
//        View terminalKeyView = ((Activity) context).findViewById(R.id.activity_termux_bottom_space_view);
//        ViewPager terminalViewPager = ((Activity) context).findViewById(R.id.terminal_toolbar_view_pager);

        // Change focusable properties
        terminalView.setFocusableInTouchMode(true);
        terminalView.setFocusable(true);

//        // Change focusable properties
//        terminalKeyView.setVisibility(View.VISIBLE);
//        terminalViewPager.setVisibility(View.VISIBLE);
    }

    public static void sky_wait(Context context, long delayMillis) {
        Log.d("DIX-sky_wait", "Waiting for " + delayMillis + " milliseconds");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("DIX-sky_wait", "Delay finished, executing code");

        }, delayMillis);
    }



    public static void sky_epg_on(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"epg_on"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_epg_off(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"epg_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void showAlertbox_playlist(Context context, DialogInterface.OnClickListener localListener,DialogInterface.OnClickListener publicListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle("Download playlist")
            .setMessage("Select whether this playlist is for local use only or for other device in network (e.g., TV). \nIf using for other devices, ensure this device has a fixed IP.")
            .setPositiveButton("This device only", localListener)
            .setNegativeButton("For other devices", publicListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showAlertbox(Context context,String title ,String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void sky_ssh_on(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"ssh_on"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);

//        Intent intentX = new Intent();
//        intentX.setClassName("com.termux", "com.termux.app.RunCommandService");
//        intentX.setAction("com.termux.RUN_COMMAND");
//        intentX.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.set_password.exp");
//        intentX.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{});
//        intentX.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
//        intentX.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
//        intentX.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
//        context.startService(intentX);
    }

    public static void sky_ssh_off(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"ssh_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }


//    public static void updateCustTermux(Context context) {
//        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/siddharthsky/CustTermux/releases"));
//        context.startActivity(browserIntent);
//    }

    public static void updateCustTermux(Context context) {
        File downloadDir = Utils2.getDownloadDirectory(context);
        File file = new File(downloadDir, "update.apk");
        boolean isDeleted = Utils2.deleteFile(file);
        SkySharedPref preferenceManager = new SkySharedPref(context);
        preferenceManager.setKey("permissionRequestCount", "0");

        Toast.makeText(context, "Applying Fix", Toast.LENGTH_SHORT).show();

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
            Log.d("SkyLog", "Out Of The App");
            context.startActivity(intent);
            System.exit(0);
        }
    }

    public static void sky_changeport(final Context context, final SetupActivityApp.OnPortChangeListener listener) {

        // Create an EditText for input
        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // Set the text color
        input.setTextColor(ContextCompat.getColor(context, R.color.text_color_white));
//        input.setBackgroundColor(ContextCompat.getColor(context, R.color.text_color_black));
        input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_underline_green));

        // Restrict input to 4 digits
        input.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(4), // Limit to 4 characters
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    // Allow only digits
                    if (TextUtils.isEmpty(source)) {
                        return null; // Let empty input pass through
                    }
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            return ""; // Reject non-digit characters
                        }
                    }
                    return null; // Accept digits
                }
            }
        });

        SkySharedPref preferenceManager = new SkySharedPref(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle("Enter Port Number")
            .setMessage("Please enter a 4-digit port number:")
            .setView(input)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String portStr = input.getText().toString();

                    if (portStr.matches("\\d{4}")) {
                        int port = Integer.parseInt(portStr);

                        preferenceManager.setKey("isLocalPORTonly", String.valueOf(port));
                        String c_port = "http://localhost:" + port + "/";
                        preferenceManager.setKey("isLocalPORT", c_port);

                        Intent intentC = new Intent();
                        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                        intentC.setAction("com.termux.RUN_COMMAND");
                        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"write_port", String.valueOf(port)});
                        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
                        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
                        context.startService(intentC);

                        Utils2.showCustomToast(context, "Port number updated to: " + port);
                        // Notify the listener about the port change
                        if (listener != null) {
                            listener.onPortChanged(portStr);
                        }
                    } else {
                        Utils2.showCustomToast(context, "Please enter a valid 4-digit port number.");
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // Request focus for the EditText
                input.requestFocus();
                // Show the keyboard automatically
                input.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200); // Delay to ensure the keyboard shows up
            }
        });

        dialog.show();
    }

    public static void sky_changedelay(final Context context, final SetupActivityApp.OnTimeChangeListener listener) {

        // Create an EditText for input
        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // Set the text color
        input.setTextColor(ContextCompat.getColor(context, R.color.text_color_white));
        input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_underline_green));

        // Restrict input to 1 digit
        input.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(1), // Limit to 1 character
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    // Allow only digits
                    if (TextUtils.isEmpty(source)) {
                        return null; // Let empty input pass through
                    }
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            return ""; // Reject non-digit characters
                        }
                    }
                    return null; // Accept digits
                }
            }
        });

        SkySharedPref preferenceManager = new SkySharedPref(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle("Enter Delay Time to Redirect to IPTV")
            .setMessage("Please enter Delay time:")
            .setView(input)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String portStr = input.getText().toString();

                    if (portStr.matches("\\d{1}")) {
                        int timeD = Integer.parseInt(portStr);

                        if (timeD < 2) {
                            preferenceManager.setKey("isDelayTime", String.valueOf(2));
                            Utils2.showCustomToast(context, "Time cannot be set below 2 seconds.");
                            Utils2.showCustomToast(context, "Delay time updated to: 2 Sec");
                        } else {
                            preferenceManager.setKey("isDelayTime", String.valueOf(timeD));
                            Utils2.showCustomToast(context, "Delay time updated to: " + timeD + " Sec");

                            // Notify the listener about the time change
                            if (listener != null) {
                                listener.OnTimeChanged(portStr);
                            }
                        }
                    } else {
                        Utils2.showCustomToast(context, "Please enter a valid time.");
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                input.requestFocus();
                // Show the keyboard automatically
                input.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200);
            }
        });

        dialog.show();
    }



    public static String getCurrentVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "0.0.0"; // Default version if not found
    }

    public static boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d("Update Cleaner", "File deleted successfully: " + file.getAbsolutePath());
            } else {
                Log.e("Update Cleaner", "Failed to delete file: " + file.getAbsolutePath());
            }
            return deleted;
        } else {
            Log.e("Update Cleaner", "File not found or null: " + (file != null ? file.getAbsolutePath() : "null"));
            return false;
        }
    }


    public static File getDownloadDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API level 30) and above, use the MediaStore API
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API level 29), use getExternalFilesDir
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        } else {
            // For Android 9 (API level 28) and below
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
    }

    public static void lake_alert_DiffARCH(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        android.view.View customView = inflater.inflate(R.layout.dialog_select_options, null);

        Spinner spinnerOS = customView.findViewById(R.id.spinner_os);
        Spinner spinnerArch = customView.findViewById(R.id.spinner_arch);

        String[] osOptions = {"Android", "Linux"};
        String[] archOptions = {"ARM", "ARM8", "x86", "x86_64"};

        ArrayAdapter<String> osAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, osOptions);
        osAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOS.setAdapter(osAdapter);

        ArrayAdapter<String> archAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, archOptions);
        archAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArch.setAdapter(archAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select OS and Architecture ");
        builder.setView(customView);


        builder.setPositiveButton("OK", (dialog, which) -> {
            String selectedOS = spinnerOS.getSelectedItem().toString();
            String selectedArch = spinnerArch.getSelectedItem().toString();

            Toast.makeText(context, "Selected OS: " + selectedOS + "\nSelected Arch: " + selectedArch, Toast.LENGTH_SHORT).show();
            Utils2.sky_custom_TV(context, selectedOS, selectedArch);
        });

        // Set cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }

    private static void sky_custom_TV(Context context, String selectedOS, String selectedArch) {
        // Map OS
        String os;
        if ("Android".equals(selectedOS)) {
            os = "android";
        } else if ("Linux".equals(selectedOS)) {
            os = "linux";
        } else {
            os = "android";
        }

        // Map Architecture
        String arch;
        switch (selectedArch) {
            case "x86_64":
                arch = "amd64";
                break;
            case "ARM8":
                arch = "arm64";
                break;
            case "ARM":
                arch = "arm";
                break;
            case "x86":
                arch = "386";
                break;
            default:
                arch = "arm";
                break;
        }

        if ("android".equals(os) && "386".equals(arch)) {
            os = "linux";
        }

        Log.d("DIX-OS",os+"--"+arch);

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"custominstall", String.valueOf(arch), String.valueOf(os)});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void lake_alert_WEBTV(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        android.view.View customView = inflater.inflate(R.layout.dialog_select_options_web, null);

        Spinner spinnerCategory = customView.findViewById(R.id.spinner_category);
        Spinner spinnerLanguage = customView.findViewById(R.id.spinner_language);
        Spinner spinnerQuality = customView.findViewById(R.id.spinner_quality);


        String[] categoryOptions = {"All Categories", "Entertainment", "Movies", "Kids", "Sports", "Lifestyle", "Infotainment", "News", "Music", "Devotional", "Business", "Educational", "Shopping", "JioDarshan"};
        String[] languageOptions = {"All Languages", "Hindi", "Marathi", "Punjabi", "Urdu", "Bengali", "English", "Malayalam", "Tamil", "Gujarati", "Odia", "Telugu", "Bhojpuri", "Kannada", "Assamese", "Nepali", "French", "Other"};
        String[] qualityOptions = {"High", "Medium", "Low"};

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, languageOptions);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);

        ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, qualityOptions);
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuality.setAdapter(qualityAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Category, Language, and Quality");
        builder.setView(customView);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String selectedCategory = spinnerCategory.getSelectedItem().toString();
            String selectedLanguage = spinnerLanguage.getSelectedItem().toString();
            String selectedQuality = spinnerQuality.getSelectedItem().toString();

            // Define the specific numbers for each option using the provided maps
            int categoryNumber = getCategoryNumber(selectedCategory);
            int languageNumber = getLanguageNumber(selectedLanguage);
            String qualityParameter = getQualityParameter(selectedQuality);

            Toast.makeText(context, "Selected Category: " + selectedCategory + "\n" +
                "Selected Language: " + selectedLanguage + "\n" +
                "Selected Quality: " + selectedQuality, Toast.LENGTH_SHORT).show();

            sky_webtv_adder(context, categoryNumber, languageNumber, qualityParameter);
        });

        // Set cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }

    private static int getCategoryNumber(String category) {
        switch (category) {
            case "Entertainment": return 5;
            case "Movies": return 6;
            case "Kids": return 7;
            case "Sports": return 8;
            case "Lifestyle": return 9;
            case "Infotainment": return 10;
            case "News": return 12;
            case "Music": return 13;
            case "Devotional": return 15;
            case "Business": return 16;
            case "Educational": return 17;
            case "Shopping": return 18;
            case "JioDarshan": return 19;
            default: return 0; // All Categories
        }
    }

    private static int getLanguageNumber(String language) {
        switch (language) {
            case "Hindi": return 1;
            case "Marathi": return 2;
            case "Punjabi": return 3;
            case "Urdu": return 4;
            case "Bengali": return 5;
            case "English": return 6;
            case "Malayalam": return 7;
            case "Tamil": return 8;
            case "Gujarati": return 9;
            case "Odia": return 10;
            case "Telugu": return 11;
            case "Bhojpuri": return 12;
            case "Kannada": return 13;
            case "Assamese": return 14;
            case "Nepali": return 15;
            case "French": return 16;
            case "Other": return 18;
            default: return 0; // All Languages
        }
    }

    private static String getQualityParameter(String quality) {
        switch (quality) {
            case "High": return "high";
            case "Medium": return "medium";
            case "Low": return "low";
            default: return "high";
        }
    }

    private static void sky_webtv_adder(Context context, int categoryNumber, int languageNumber, String qualityParameter) {
        SkySharedPref preferenceManager = new SkySharedPref(context);

//        String SAVE = "?category=" + categoryNumber + "&language=" + languageNumber;
//        if (!qualityParameter.isEmpty()) {
//            SAVE += "&q=" + qualityParameter;
//        }

        String SAVE = "?category=" + categoryNumber + "&language=" + languageNumber + "&q=" + qualityParameter;

        // Example URL: http://localhost:5001/?category=5&language=1&q=high

        preferenceManager.setKey("isWEBTVconfig", SAVE);
    }

    public static void runTERMUXinfo(Context context) {

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"termuxinfo"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }
}
