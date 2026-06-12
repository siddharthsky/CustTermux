package com.termux.sky;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.activities.SettingsActivity;
import com.termux.sky.iptv.AppPickerActivity;

public class MoreOptions {

    public static void setButtonClickListener(Context context, Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder =
                    new AlertDialog.Builder(context, R.style.GoldenFocusDialogTheme);

                LinearLayout titleLayout = new LinearLayout(context);
                titleLayout.setOrientation(LinearLayout.HORIZONTAL);
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics());
                titleLayout.setPadding(padding, padding, padding, padding / 2);
                titleLayout.setGravity(Gravity.CENTER_VERTICAL);


                TextView titleText = new TextView(context);
                titleText.setText("Choose an option");
                titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                titleText.setTypeface(null, Typeface.BOLD);
                titleText.setTextColor(Color.WHITE);

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                titleLayout.addView(titleText, textParams);

                ImageView settingsIcon = new ImageView(context);
                settingsIcon.setImageResource(R.drawable.tx_settings);
                settingsIcon.setColorFilter(Color.WHITE);

                settingsIcon.setFocusable(true);
                settingsIcon.setBackgroundResource(R.drawable.golden_focus_selector);

                int iconPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                settingsIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

                titleLayout.addView(settingsIcon);

                builder.setCustomTitle(titleLayout);

                String[] options = {
                    "IPTV Manager",
                    "Auto-start on boot",
                    "Backup & Restore",
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
                                TxUtils.showAutoStartDialog(context);
                                break;

                            case 2:
                                TxUtils.showBackupRestoreDialog(context);
                                break;

                            case 3:
                                TxUtils.terminal_switch_dialog(context);
                                break;

                            default:
                                break;
                        }
                    }
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();

                settingsIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, TxSettingsActivity.class);
                        context.startActivity(intent);
                        dialog.dismiss();
                    }
                });

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
        });
    }
}
