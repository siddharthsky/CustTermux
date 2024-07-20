package com.termux;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;



public class LoginReceiverActivity extends AppCompatActivity {

    private Handler handler;

    private Runnable runnable;

    private String phoneNumber;

    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_receiver);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.termux.ACTION_RECEIVE_LOGIN")) {
                // Extract the phone number from the Intent
                //String phoneNumber = intent.getStringExtra("phone_number");

                // Handle the phone number
                sky_login();
            }
        }
    }



    private void sky_login() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Number To Login");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Retrieve the phone number from the input field
                phoneNumber = input.getText().toString();

                // Log the phone number
                //Log.d("SkyLogin", "Phone Number: " + phoneNumber);

                // Call the nested function and pass the phone number
                handleSendOTP(phoneNumber);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                login_finish_file();
                finish();
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sky_otp() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)}); // OTP is 6 digits
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Retrieve the OTP from the input field
                otp = input.getText().toString();

                // Log the OTP
                //Log.d("SkyOTP", "OTP: " + otp);

                // Call the nested function and pass the OTP
                handleVerifyOTP(otp);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                login_finish_file();
                dialog.dismiss();

            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
        finish();
    }

    // Nested function to handle the phone number
    private void handleSendOTP(String phoneNumber) {
        // Your code to handle the phone number
        //Log.d("HandlePhoneNumber", "Phone Number in handleSendOTP: " + phoneNumber);

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"sendotp", phoneNumber});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);

        wait_tx(1000);

        sky_otp();
    }


    public void wait_tx(int timex) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, timex);
    }

    private void handleVerifyOTP(String otp) {
        // Your code to handle the OTP
        //Log.d("HandleOTP", "OTP in handleVerifyOTP: " + otp);
        Intent intentO = new Intent();
        intentO.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentO.setAction("com.termux.RUN_COMMAND");
        intentO.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentO.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"verifyotp", phoneNumber, otp});
        intentO.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentO.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentO.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentO);
        wait_tx(1000);
        login_finish_file();
    }

    private void login_finish_file() {
        wait_tx(1000);
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"touch","$HOME/.jiotv_go/bin/login_check.dummy"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }
}
