package com.termux.setup_app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;
import com.termux.WebViewDL;
import java.util.Random;

public class SetupActivityExtra extends AppCompatActivity {

    private SwitchCompat switchEXTRA;
    private Button ZeeOption;
    private Button SonyOption;
    private Button AllOption;
    private Button PORTbtn;
    private Button ZeeReset;






    private SkySharedPref preferenceManager;
    private LottieAnimationView lottieAnimationView;
    private int[] animationResources = {
        R.raw.batmanx,
        R.raw.bird,
    };
    private int[] weights = {
         3  // Weight for batman
        ,5  // Weight for bird
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.CustomDarkBlueTheme);

        setContentView(R.layout.activity_setup_extra);

        // Enable the home button as an up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        switchEXTRA = findViewById(R.id.switchEXTRA);
        ZeeOption = findViewById(R.id.ZeeOption);
        SonyOption = findViewById(R.id.SonyOption);
        AllOption = findViewById(R.id.AllOption);
        ZeeReset = findViewById(R.id.ZeeReset);

        PORTbtn = findViewById(R.id.PORTbtn);



        lottieAnimationView = findViewById(R.id.settingsani);

        setRandomAnimationBasedOnWeight();

        preferenceManager = new SkySharedPref(this);



        loadPreferences();
        setupSwitchListeners();
        setupFocusListeners();
    }

    private void setRandomAnimationBasedOnWeight() {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }

        Random random = new Random();
        int randomValue = random.nextInt(totalWeight);

        int cumulativeWeight = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue < cumulativeWeight) {
                lottieAnimationView.setAnimation(animationResources[i]);

                if (animationResources[i] == R.raw.bird) {
                    // Apply mirroring transformation
                    lottieAnimationView.setScaleX(-1); // Mirror horizontally
                } else {
                    // Reset transformation for other animations
                    lottieAnimationView.setScaleX(1);
                }

                lottieAnimationView.playAnimation();
                break;
            }
        }
    }
    private void loadPreferences() {

        String serverSetupIsEXTRA = preferenceManager.getKey("server_setup_isEXTRA");
        switchEXTRA.setChecked("Yes".equals(serverSetupIsEXTRA));


    }


    private void setupSwitchListeners() {

        switchEXTRA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Utils.sky_extra_on(SetupActivityExtra.this,preferenceManager);
                } else {
                    Utils.showCustomToast(SetupActivityExtra.this, ("Disabling support for extra channels"));
                    preferenceManager.setKey("server_setup_isEXTRA", "No");
                    Utils.sky_extra_off(SetupActivityExtra.this);
                }
            }
        });

        ZeeOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChromeInstalled()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shrinkme.ink/kZ4ts"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    startActivity(intent);
                } else {
                    Utils.showCustomToast(SetupActivityExtra.this, "Downloading ZEE playlist");
                    String fileUrl = "https://raw.githubusercontent.com/siddharthsky/Extrix/refs/heads/main/golang/data.json";
                    downloadFile(fileUrl);
                }
            }
        });


        SonyOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChromeInstalled()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shrinkme.ink/kZ4ts"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    startActivity(intent);
                } else {
                    Utils.showCustomToast(SetupActivityExtra.this, "Downloading SONY playlist");
                    String fileUrl = "https://sky.fol.com/do.m3u";
                    downloadFile(fileUrl);
                }
            }
        });

        AllOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChromeInstalled()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shrinkme.ink/kZ4ts"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    startActivity(intent);
                } else {
                    Utils.showCustomToast(SetupActivityExtra.this, "Downloading Combined playlist");
//                    downloadFileWEB();

                    // Create an explicit intent for WebViewDL
                    Intent intent = new Intent(SetupActivityExtra.this, WebViewDL.class);

                    // Add the URL as an extra to the Intent
                    String url = "https://sky7t.github.io/1/?ip=192.168.1.10:5350"; // Replace with your desired URL
                    intent.putExtra("url", url);

                    // Start the WebViewDL activity
                    startActivity(intent);

                }
            }
        });

        ZeeReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivityExtra.this, "Reinstalling ZEE Script");

                Intent intentC = new Intent();
                intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                intentC.setAction("com.termux.RUN_COMMAND");
                intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"extra_on"});
                intentC.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
                intentC.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
                intentC.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
                startService(intentC);
            }

        });



        // HIDDEN
        PORTbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.sky_changeportzee(SetupActivityExtra.this, new OnPortChangeListener() {
                    @Override
                    public void onPortChanged(String newPort) {
                        // Update the button text with the new port number
                        PORTbtn.setText(newPort);
                        // Optionally, show a toast message
                        // Utils.showCustomToast(SetupActivityApp.this, "Changed port to " + newPort);
                        Utils.showCustomToast(SetupActivityExtra.this, "Restarting CustTermux to apply changes");
                    }
                });
            }
        });


    }

    public interface OnPortChangeListener {
        void onPortChanged(String newPort);
    }

    public interface OnTimeChangeListener {
        void OnTimeChanged(String newTime);
    }



    private void setupFocusListeners() {
        View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    view.setBackgroundColor(Color.YELLOW); // Highlight the focused switch
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT); // Reset to default when focus is lost
                }
            }
        };


        switchEXTRA.setOnFocusChangeListener(focusChangeListener);
        ZeeOption.setOnFocusChangeListener(focusChangeListener);
        SonyOption.setOnFocusChangeListener(focusChangeListener);
        AllOption.setOnFocusChangeListener(focusChangeListener);

        PORTbtn.setOnFocusChangeListener(focusChangeListener);
        ZeeReset.setOnFocusChangeListener(focusChangeListener);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        SkySharedPref preferenceManager = new SkySharedPref(this);
        String portid = preferenceManager.getKey("isZEEPORTonly");
        PORTbtn.setText(portid);
    }

    private boolean isChromeInstalled() {
        String chromePackage = "com.android.chrome";
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(chromePackage, PackageManager.GET_ACTIVITIES);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void downloadFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle("File Download");
        request.setDescription("Downloading " + fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }


}
