package com.termux;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
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

import com.termux.setup_app.SetupActivityApp;
import com.termux.view.TerminalView;

import java.io.File;


public class Utils {
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
        preferenceManager.setKey("server_setup_isAutobootBG", "No");
        preferenceManager.setKey("server_setup_isLocal", "No");
        preferenceManager.setKey("app_name", "null");
        preferenceManager.setKey("app_launchactivity", "null");
        preferenceManager.setKey("isExit", "noExit");
        preferenceManager.setKey("server_setup_isEPG", "Yes");
        preferenceManager.setKey("server_setup_isDRM", "No");
        preferenceManager.setKey("server_setup_isGenericBanner", "No");
        preferenceManager.setKey("server_setup_isSSH", "No");
        preferenceManager.setKey("isDelayTime", "5");
        preferenceManager.setKey("permissionRequestCount", "0");
        preferenceManager.setKey("isFlagSetForMinimize", "No");
        preferenceManager.setKey("isWEBTVconfig", " ");


        File downloadDir = Utils.getDownloadDirectory(context);
        File file = new File(downloadDir, "update.apk");
        boolean isDeleted = Utils.deleteFile(file);

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"reinstall2"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intentC);
        } else {
            context.startService(new Intent(intentC));
        }
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
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
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
        File downloadDir = Utils.getDownloadDirectory(context);
        File file = new File(downloadDir, "update.apk");
        boolean isDeleted = Utils.deleteFile(file);
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

                        Utils.showCustomToast(context, "Port number updated to: " + port);
                        // Notify the listener about the port change
                        if (listener != null) {
                            listener.onPortChanged(portStr);
                        }
                    } else {
                        Utils.showCustomToast(context, "Please enter a valid 4-digit port number.");
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
                            Utils.showCustomToast(context, "Time cannot be set below 2 seconds.");
                            Utils.showCustomToast(context, "Delay time updated to: 2 Sec");
                        } else {
                            preferenceManager.setKey("isDelayTime", String.valueOf(timeD));
                            Utils.showCustomToast(context, "Delay time updated to: " + timeD + " Sec");

                            // Notify the listener about the time change
                            if (listener != null) {
                                listener.OnTimeChanged(portStr);
                            }
                        }
                    } else {
                        Utils.showCustomToast(context, "Please enter a valid time.");
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
            Utils.sky_custom_TV(context, selectedOS, selectedArch);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Options");

        // Inflate the layout for the main dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        android.view.View customView = inflater.inflate(R.layout.dialog_main_buttons, null);
        builder.setView(customView);

        // Initialize buttons
        Button buttonQuality = customView.findViewById(R.id.button_quality);
        Button buttonCategory = customView.findViewById(R.id.button_category);
        Button buttonLanguage = customView.findViewById(R.id.button_language);

        // Set up button actions
        buttonQuality.setOnClickListener(v -> showQualityDialog(context));
        buttonCategory.setOnClickListener(v -> showCategoryDialog(context));
        buttonLanguage.setOnClickListener(v -> showLanguageDialog(context));

        builder.setPositiveButton("Save", (dialog, which) -> sky_saver(context));

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private static void showCategoryDialog(Context context) {
        String[] categoryOptions = {"All Categories", "Entertainment", "Movies", "Kids", "Sports", "Lifestyle", "Infotainment", "News", "Music", "Devotional", "Business", "Educational", "Shopping", "JioDarshan"};
        boolean[] selectedCategories = new boolean[categoryOptions.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Categories");

        builder.setMultiChoiceItems(categoryOptions, selectedCategories, (dialog, which, isChecked) -> {
            selectedCategories[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            StringBuilder selectedCategoriesStr = new StringBuilder();
            for (int i = 0; i < selectedCategories.length; i++) {
                if (selectedCategories[i]) {
                    selectedCategoriesStr.append(categoryOptions[i]).append(",");
                }
            }
            if (selectedCategoriesStr.length() > 0) {
                selectedCategoriesStr.setLength(selectedCategoriesStr.length() - 1); // Remove trailing comma
            }

            // Save selected categories to SharedPreferences
            SkySharedPref preferenceManager = new SkySharedPref(context);
            preferenceManager.setKey("isWEBTV_CAT", selectedCategoriesStr.toString());
            //sky_saver(context);

            Toast.makeText(context, "Selected Categories: " + selectedCategoriesStr.toString(), Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private static void showLanguageDialog(Context context) {
        String[] languageOptions = {"All Languages", "Hindi", "Marathi", "Punjabi", "Urdu", "Bengali", "English", "Malayalam", "Tamil", "Gujarati", "Odia", "Telugu", "Bhojpuri", "Kannada", "Assamese", "Nepali", "French", "Other"};
        boolean[] selectedLanguages = new boolean[languageOptions.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Languages");

        builder.setMultiChoiceItems(languageOptions, selectedLanguages, (dialog, which, isChecked) -> {
            selectedLanguages[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            StringBuilder selectedLanguagesStr = new StringBuilder();
            for (int i = 0; i < selectedLanguages.length; i++) {
                if (selectedLanguages[i]) {
                    selectedLanguagesStr.append(languageOptions[i]).append(",");
                }
            }
            if (selectedLanguagesStr.length() > 0) {
                selectedLanguagesStr.setLength(selectedLanguagesStr.length() - 1); // Remove trailing comma
            }

            // Save selected languages to SharedPreferences
            SkySharedPref preferenceManager = new SkySharedPref(context);
            preferenceManager.setKey("isWEBTV_LANG", selectedLanguagesStr.toString());
            //sky_saver(context);

            Toast.makeText(context, "Selected Languages: " + selectedLanguagesStr.toString(), Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private static void showQualityDialog(Context context) {
        String[] qualityOptions = {"High", "Medium", "Low"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Quality");

        builder.setSingleChoiceItems(qualityOptions, -1, (dialog, which) -> {
            // Save the selected quality option
            String selectedQuality = qualityOptions[which];
            String qualityParameter = getQualityParameter(selectedQuality);

            // Save selected quality to SharedPreferences
            SkySharedPref preferenceManager = new SkySharedPref(context);
            preferenceManager.setKey("isWEBTV_QUALITY", qualityParameter);
            //sky_saver(context);

            Toast.makeText(context, "Selected Quality: " + selectedQuality, Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            // Handle saving or applying the selected quality
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

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
    private static  void sky_saver(Context context){
        SkySharedPref preferenceManager = new SkySharedPref(context);

        // Retrieve saved preferences
        String categories = preferenceManager.getKey("isWEBTV_CAT");
        String languages = preferenceManager.getKey("isWEBTV_LANG");
        String qualityParameter = preferenceManager.getKey("isWEBTV_QUALITY");

        // Convert comma-separated category names to their corresponding numbers
        String[] categoryArray = categories.split(",");
        StringBuilder categoryNumbers = new StringBuilder();
        for (String category : categoryArray) {
            int number = getCategoryNumber(category.trim());
            if (number != 0) {
                categoryNumbers.append(number).append(",");
            }
        }
        if (categoryNumbers.length() > 0) {
            categoryNumbers.setLength(categoryNumbers.length() - 1); // Remove trailing comma
        }

        // Convert comma-separated language names to their corresponding numbers
        String[] languageArray = languages.split(",");
        StringBuilder languageNumbers = new StringBuilder();
        for (String language : languageArray) {
            int number = getLanguageNumber(language.trim());
            if (number != 0) {
                languageNumbers.append(number).append(",");
            }
        }
        if (languageNumbers.length() > 0) {
            languageNumbers.setLength(languageNumbers.length() - 1); // Remove trailing comma
        }

        // Construct the final query string
        StringBuilder queryBuilder = new StringBuilder("?");
        if (categoryNumbers.length() > 0) {
            queryBuilder.append("category=").append(categoryNumbers.toString());
        }
        if (languageNumbers.length() > 0) {
            if (queryBuilder.length() > 1) queryBuilder.append("&");
            queryBuilder.append("language=").append(languageNumbers.toString());
        }
        if (!qualityParameter.isEmpty()) {
            if (queryBuilder.length() > 1) queryBuilder.append("&");
            queryBuilder.append("q=").append(qualityParameter);
        }

        preferenceManager.setKey("isWEBTVconfig", queryBuilder.toString());

        String finalURL = "http://localhost:5001/" + queryBuilder.toString();

        // Use the final URL as needed
        Toast.makeText(context, "Generated URL: " + finalURL, Toast.LENGTH_LONG).show();
        //Toast.makeText(context, "Configured WEBTV", Toast.LENGTH_LONG).show();
    }

    private static void sky_webtv_adder(Context context, String categories, String languages, String qualityParameter) {
        SkySharedPref preferenceManager = new SkySharedPref(context);

        // Convert comma-separated category names to their corresponding numbers
        String[] categoryArray = categories.split(",");
        StringBuilder categoryNumbers = new StringBuilder();
        for (String category : categoryArray) {
            int number = getCategoryNumber(category.trim());
            if (number != 0) {
                categoryNumbers.append(number).append(",");
            }
        }
        if (categoryNumbers.length() > 0) {
            categoryNumbers.setLength(categoryNumbers.length() - 1); // Remove trailing comma
        }

        // Convert comma-separated language names to their corresponding numbers
        String[] languageArray = languages.split(",");
        StringBuilder languageNumbers = new StringBuilder();
        for (String language : languageArray) {
            int number = getLanguageNumber(language.trim());
            if (number != 0) {
                languageNumbers.append(number).append(",");
            }
        }
        if (languageNumbers.length() > 0) {
            languageNumbers.setLength(languageNumbers.length() - 1); // Remove trailing comma
        }

        // Construct the final query string
        StringBuilder queryBuilder = new StringBuilder("?");
        if (categoryNumbers.length() > 0) {
            queryBuilder.append("category=").append(categoryNumbers.toString());
        }
        if (languageNumbers.length() > 0) {
            if (queryBuilder.length() > 1) queryBuilder.append("&");
            queryBuilder.append("language=").append(languageNumbers.toString());
        }
        if (!qualityParameter.isEmpty()) {
            if (queryBuilder.length() > 1) queryBuilder.append("&");
            queryBuilder.append("q=").append(qualityParameter);
        }

        String SAVE = queryBuilder.toString();

        // Example URL: http://localhost:5001/?category=5,6,7&language=1,6&q=high

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

    public static void sky_customupdate(Context context) {

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"custominstall2"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_drm_on(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"drm_on"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_drm_off(Context context) {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"drm_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }

    public static void sky_extra_on(Context context, SkySharedPref preferenceManager) {
        Log.d("Utils","EXTRA ON");

        Utils.showAlertbox_extra(context, preferenceManager);

    }

    public static void sky_extra_off(Context context) {
        Log.d("Utils","EXTRA OFF");
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"extra_off"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        context.startService(intentC);
    }


    public static void showAlertbox_extra(Context context, SkySharedPref preferenceManager) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);

        builder.setMessage("These channels are sourced from official providers. However, we cannot guarantee their functionality at all times. Proceed at your own discretion, and please do not report if they stop working in the future.")
            .setTitle("Important Notice");

        builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                Utils.showCustomToast(context, ("Enabling support for extra channels"));
                preferenceManager.setKey("server_setup_isEXTRA", "Yes");

                Intent intentC = new Intent();
                intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                intentC.setAction("com.termux.RUN_COMMAND");
                intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"extra_on"});
                intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
                intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
                context.startService(intentC);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        android.app.AlertDialog dialog = builder.create();

        dialog.show();
    }

    public static void sky_changeportzee(final Context context, final SetupActivity_Extra.OnPortChangeListener listener) {

        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        input.setTextColor(ContextCompat.getColor(context, R.color.text_color_white));
        input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_underline_green));

        // Restrict input to 4 digits
        input.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(4),
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    // Allow only digits
                    if (TextUtils.isEmpty(source)) {
                        return null;
                    }
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            return "";
                        }
                    }
                    return null;
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

                        preferenceManager.setKey("isZEEPORTonly", String.valueOf(port));

                        Utils.showCustomToast(context, "Port number updated to: " + port);
                        // Notify the listener about the port change
                        if (listener != null) {
                            listener.onPortChanged(portStr);
                        }
                    } else {
                        Utils.showCustomToast(context, "Please enter a valid 4-digit port number.");
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
}

