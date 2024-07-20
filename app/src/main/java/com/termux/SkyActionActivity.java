package com.termux;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.app.TermuxActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class SkyActionActivity extends AppCompatActivity {

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
            if (intent.getAction().equals("com.termux.SKY_ACTION")) {
                // Extract the phone number from the Intent
                String int_var = intent.getStringExtra("mode");
                if (int_var != null && !int_var.isEmpty()) {
                    switch (int_var) {
                        case "start_server":
                            start_server();
                            finish();
                            break;
                        case "start_server_bg":
                            start_server_bg();
                            finish();
                            break;
                        case "end_server":
                            end_server();
                            finish();
                            break;
                        case "iptvrunner":
                            iptvrunner();
                            finish();
                            break;
                        case "loginstatus":
                            loginstatus();
                            finish();
                            break;
                        case "loginotp":
                            Intent intentv = new Intent();
                            intentv.setAction("com.termux.ACTION_RECEIVE_LOGIN");
                            intentv.setPackage("com.termux");
                            startActivity(intentv);
                            finish();
                            break;
                        default:
                            end_server();
                            //dw
                            finish();
                            //finish();
                            break;
                    }
                }
            }
        }

    }

    public void wait_X() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, 1000);
    }


    private void start_server() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"run","-P"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void start_server_bg() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"bg","run"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    private void end_server() {
        wait_X();
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f","/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
        wait_X();
    }

    private void iptvrunner() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"iptvrunner"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);
    }

    public void loginstatus() {

        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"run","-P"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);


        wait_X();

        // URL to check
        String url = "http://localhost:5001/live/144.m3u8";

        // Execute AsyncTask to check status code
        new SkyActionActivity.CheckStatusTask().execute(url);

        wait_X();

        Intent intentCD = new Intent();
        intentCD.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentCD.setAction("com.termux.RUN_COMMAND");
        intentCD.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        intentCD.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f","/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
        intentCD.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentCD.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentCD.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentCD);
        wait_X();
    }

    private class CheckStatusTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                return connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode != null) {
                // Handle the response code
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println("The webpage is accessible.");
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        System.out.println("The webpage was not found.");
                        Toast.makeText(SkyActionActivity.this, "Login Service Error.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        System.out.println("Response code: " + responseCode);
                        if (responseCode == 500) {
                            sky_login();
                        }
                        break;
                }
            } else {
                System.out.println("Error occurred while checking status code.");
                Toast.makeText(SkyActionActivity.this, "Login Service Error.", Toast.LENGTH_SHORT).show();
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
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"sendotpx", phoneNumber});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);

        wait_special();

        sky_otp();
    }

    public void wait_special() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //EMPTY
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private void handleVerifyOTP(String otp) {
        // Your code to handle the OTP
        //Log.d("HandleOTP", "OTP in handleVerifyOTP: " + otp);
        Intent intentO = new Intent();
        intentO.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentO.setAction("com.termux.RUN_COMMAND");
        intentO.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentO.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"verifyotpx", phoneNumber, otp});
        intentO.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentO.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentO.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentO);
    }


}

