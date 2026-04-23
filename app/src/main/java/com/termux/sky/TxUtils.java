package com.termux.sky;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.termux.R;
import com.termux.view.TerminalView;

public class TxUtils {

    // Termux constants
    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_SERVICE = "com.termux.app.RunCommandService";
    private static final String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";

    private static final String HOME_PATH = "/data/data/com.termux/files/home";
    private static final String SCRIPT_PATH = HOME_PATH + "/.skyutils.sh";


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
            new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

        builder.setTitle("Confirmation");
        builder.setMessage("Do you want to proceed?\n[Note: To exit press back button, reopen]");

        builder.setPositiveButton("OK", (dialog, id) -> sky_terminal(context));
        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Fix invisible buttons
        int activeColor = Color.parseColor("#007BFF");

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setTextColor(activeColor);
        }
        if (negButton != null) {
            negButton.setTextColor(activeColor);
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
            R.style.CustomAlertDialogTheme
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

        // Fix invisible buttons
        int activeColor = Color.parseColor("#007BFF");

        Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (posButton != null) {
            posButton.setTextColor(activeColor);
        }
        if (negButton != null) {
            negButton.setTextColor(activeColor);
        }
    }
}
