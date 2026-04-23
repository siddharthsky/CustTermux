package com.termux.sky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.termux.R;
import com.termux.sky.iptv.AppPickerActivity;

public class MoreOptions {

    public static void setButtonClickListener(Context context, Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder =
                    new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

                builder.setTitle("Choose an option");

                String[] options = {
                    "IPTV Manager",
                    "Auto-start on boot",
                    "Switch to Terminal"
                };

                final int[] selectedOption = {-1};

                builder.setSingleChoiceItems(options, selectedOption[0],
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedOption[0] = which;
                        }
                    });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (selectedOption[0]) {

                            case 0:
                                Intent intent = new Intent(context, AppPickerActivity.class);
                                context.startActivity(intent);
                                break;

                            case 1:
                                TxUtils.showAutoStartDialog(context);;
                                break;

                            case 2:
                                TxUtils.terminal_switch_dialog(context);
                                break;

                            default:
                                break;
                        }
                    }
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}
