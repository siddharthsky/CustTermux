package com.termux.sky;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.termux.R;
import com.termux.sky.wizard.SetupWizardActivity;

public class MoreOptions {

    public static void setButtonClickListener(Context context, Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder =
                    new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

                builder.setTitle("Choose an option");

                String[] options = {
                    "Switch to Terminal",
                    "Auto start on boot"
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
//                                TermuxUtilz.terminal_switch_dialog(context);
                                context.startActivity(new Intent(context, SetupWizardActivity.class));


                                break;

                            case 1:
                                TermuxUtilz.showAutoStartDialog(context);
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
