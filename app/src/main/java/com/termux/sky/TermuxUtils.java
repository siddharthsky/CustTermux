package com.termux.sky;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.termux.R;
import com.termux.view.TerminalView;

public class TermuxUtils {

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
//        Intent intent = new Intent();
//        intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
//        intent.setAction(ACTION_RUN_COMMAND);
//
//        intent.putExtra("com.termux.RUN_COMMAND_PATH", SCRIPT_PATH);
//        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"update"});
//        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH);
//        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
//        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
//
//        context.startService(intent);

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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            context.getResources().getColor(android.R.color.holo_green_light)
        );
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
            context.getResources().getColor(android.R.color.holo_red_light)
        );
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
}
