package com.termux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;

import com.termux.R;
import com.termux.Utils;

public class ButtonClick6ListenerUtil {

    public static void setButtonClickListener(Context context, Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an AlertDialog Builder with the custom style
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

                // Set the dialog title
                builder.setTitle("Choose an option");

                // Add a radio button list
//                String[] options = {"Update JioTV Go", "Try Different JioTV Go binary" ,"Fix CustTermux Update","Show System Info","Reinstall","Switch to Terminal","Add Channels [Cust. Binary]"};
                String[] options = {"Update JioTV Go", "Try Different JioTV Go binary" ,"Fix CustTermux Update","Show System Info","Reinstall","Switch to Terminal"};
                final int[] selectedOption = {-1}; // Store the selected option

                builder.setSingleChoiceItems(options, selectedOption[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Store the selected option
                        selectedOption[0] = which;
                    }
                });

                // Add OK and Cancel buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked OK button
                        // Handle the selected option
                        switch (selectedOption[0]) {
                            case 0:
                                // Option 1 selected
                                Utils.sky_update(context);
                                break;
                            case 1:
                                // Option 2 selected
                                Utils.lake_alert_DiffARCH(context);
                                break;
                            case 2:
                                // Option 3 selected
                                Utils.updateCustTermux(context);
                                break;
                            case 3:
                                // Option 4 selected
                                Utils.runTERMUXinfo(context);
                                break;
                            case 4:
                                // Option 5 selected
                                Utils.sky_reinstall(context);
                                break;
                            case 5:
                                // Option 6 selected
                                Utils.lake_alert_confirmation(context);
                                break;
//                            case 6:
//                                // Option 6 selected
//                                Utils.sky_customupdate(context);
//                                break;


                            default:
                                // No option selected or invalid
                                break;
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });

                // Create and show the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

}
