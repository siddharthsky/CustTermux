package com.termux;


import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.setup_login.LoginActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class SkyActionActivity extends AppCompatActivity {

    private Handler handler;

    private Runnable runnable;

    private String phoneNumber;

    private String url;

    private String loginChecker;

    private String urlString;

    private String urlchannel;

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
                        case "loginstatus2":
                            loginstatus2();
                            finish();
                            break;
                        case "iptvrunner2":
//                            iptvrunner2();
                            finish();
                            break;
                        case "setup_finisher":
                            setup_finisher();
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

    private void setup_finisher() {
        Intent intentC = new Intent();
        intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentC.setAction("com.termux.RUN_COMMAND");
        intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"exitpath"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);

        SkySharedPref preferenceManager = new SkySharedPref(this);
        String isExit = preferenceManager.getKey("isExit");

        while (!isExit.equals("yesExit")) {
            isExit = preferenceManager.getKey("isExit");
        }

        // Create an intent to restart the app
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            // Finish current activity
            finish();

            // Restart the app
            startActivity(intent);

            // Exit the app
            System.exit(0);
        }

    }

    private void iptvrunner2() {
        SkySharedPref preferenceManager = new SkySharedPref(SkyActionActivity.this);
        String apppkg = preferenceManager.getKey("app_name");
        String appclass = preferenceManager.getKey("app_launchactivity");

        if (apppkg == null || "null".equals(apppkg)) {
            Log.d("d", "no iptv");
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(apppkg, appclass));
            startActivity(intent);
        }

    }

    private void runner2x() {
        SkySharedPref preferenceManager = new SkySharedPref(SkyActionActivity.this);
        String iptvChecker = preferenceManager.getKey("app_name");
        String appclass = preferenceManager.getKey("app_launchactivity");

        if (iptvChecker != null && !iptvChecker.isEmpty()) {
            if (iptvChecker.equals("null")) {
                System.out.println("IPTV, null!");
            } else if (iptvChecker.equals("sky_web_tv")) {
                System.out.println("IPTV, webTV!");
                try {
                    Intent intent = new Intent(SkyActionActivity.this, WebPlayerActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    // Log or handle the exception if needed
                    System.out.println("Unable to start WebPlayerActivity.");
                }
            } else {
                System.out.println("IPTV, found!");
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(iptvChecker, appclass));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Log or handle the exception if needed
                    System.out.println("Unable to open the specified app.");
                    Toast.makeText(SkyActionActivity.this, "Unable to open the specified app.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    // Handle any other exceptions
                    System.out.println("Error occurred while starting the activity.");
                }
            }
        } else {
            System.out.println("IPTV, null!");
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
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"run", "-P"});
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
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"bg", "run"});
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
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f", "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
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
        intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"run", "-P"});
        intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentC);


        wait_X();

        SkySharedPref preferenceManager = new SkySharedPref(this);
        urlString = preferenceManager.getKey("isLocalPORT");
        urlchannel = preferenceManager.getKey("isLocalPORTchannel");

        // URL to check
        String url = urlString+urlchannel;

        // Execute AsyncTask to check status code
        new SkyActionActivity.CheckStatusTask().execute(url);

        wait_X();

        Intent intentCD = new Intent();
        intentCD.setClassName("com.termux", "com.termux.app.RunCommandService");
        intentCD.setAction("com.termux.RUN_COMMAND");
        intentCD.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/pkill");
        intentCD.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-f", "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go"});
        intentCD.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intentCD.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        intentCD.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(intentCD);
        wait_X();
    }


    public void loginstatus2() {
        SkySharedPref preferenceManager = new SkySharedPref(SkyActionActivity.this);
        loginChecker = preferenceManager.getKey("server_setup_isLoginCheck");
        urlString = preferenceManager.getKey("isLocalPORT");
        urlchannel = preferenceManager.getKey("isLocalPORTchannel");

        url = urlString+urlchannel;
        Log.d("StyleP","dw "+url);


        if (loginChecker != null && !loginChecker.isEmpty()) {
            if (loginChecker.equals("No")) {
                System.out.println("loginChecker off");
                runner2x();
            } else {
                System.out.println("loginChecker on");
                new SkyActionActivity.CheckStatusTask().execute(url);
            }
        } else {
            System.out.println("loginChecker Null");
            new SkyActionActivity.CheckStatusTask().execute(url);

        }
    }


    public class CheckStatusTask extends AsyncTask<String, Void, Integer> {

        private static final int RETRY_DELAY = 3000; // 3 seconds delay for retrying

        @Override
        protected Integer doInBackground(String... urls) {
            String urlString = urls[0];
            Integer responseCode = checkStatus(urlString);
            if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                // Retry after a delay
                try {
                    Thread.sleep(RETRY_DELAY); // Wait before retrying
                    responseCode = checkStatus(urlString); // Retry the check
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return responseCode;
        }

        private Integer checkStatus(String urlString) {
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
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println("SkyActivity: The webpage is accessible.");
                        runner2x();
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        System.out.println("SkyActivity: The webpage was not found.");
                        Toast.makeText(SkyActionActivity.this, "Login Service Error.", Toast.LENGTH_SHORT).show();
                        break;
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        System.out.println("SkyActivity: Internal server error after retry.");
                        Intent intent = new Intent(SkyActionActivity.this, LoginErrorActivity.class);
                        startActivity(intent);
                        break;
                    default:
                        System.out.println("SkyActivity: Response code: " + responseCode);
                        break;
                }
            } else {
                System.out.println("Error occurred while checking status code.");
                Toast.makeText(SkyActionActivity.this, "Login Service Error.", Toast.LENGTH_SHORT).show();
            }
        }


        private void showAlert() {
            new AlertDialog.Builder(SkyActionActivity.this)
                .setTitle("Server Error")
                .setMessage("An error occurred on the server. Do you want to log in again?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sky_login();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        }


        private void sky_login() {
            Intent intent = new Intent(SkyActionActivity.this, LoginActivity.class);
            startActivity(intent);
            iptvrunner2();
        }

        private void lake_alert_confirmation(Context context) {
            // Create an AlertDialog Builder
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);

            // Set the message and the title
            builder.setMessage("Do you want to Login?")
                .setTitle("Login Expired!");

            // Add the buttons
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    sky_login();
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
            AlertDialog dialog = builder.create();

            // Show the AlertDialog
            dialog.show();
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


    }
}
