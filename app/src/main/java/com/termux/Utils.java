package com.termux;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.termux.app.TermuxActivity;
import com.termux.setup_app.SetupActivityApp;
import com.termux.view.TerminalView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


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

        // Change focusable properties
        terminalView.setFocusableInTouchMode(true);
        terminalView.setFocusable(true);
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


    public static void updateCustTermux(Context context) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/siddharthsky/CustTermux-JioTVGo/releases"));
        context.startActivity(browserIntent);
    }

    public static void sky_changeport(final Context context, final SetupActivityApp.OnPortChangeListener listener) {

        // Create an EditText for input
        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // Set the text color
        input.setTextColor(ContextCompat.getColor(context, R.color.text_color_white));
        input.setBackgroundColor(ContextCompat.getColor(context, R.color.text_color_black));

        // Restrict input to 4 digits
        input.setFilters(new InputFilter[] {
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
                        String c_port = "http://localhost:"+port+"/";
                        preferenceManager.setKey("isLocalPORT", c_port);

                        Intent intentC = new Intent();
                        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                        intentC.setAction("com.termux.RUN_COMMAND");
                        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"write_port",String.valueOf(port)});
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
                // Optionally, show the keyboard automatically
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        dialog.show();
    }

}


