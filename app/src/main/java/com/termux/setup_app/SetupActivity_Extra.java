package com.termux.setup_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;
import com.termux.WebViewDL;

import java.util.Base64;
import java.util.Random;

public class SetupActivity_Extra extends AppCompatActivity {

    private SwitchCompat switchEXTRA;
    private Button ZeeOption;
    private Button SonyOption;
    private Button AllOption;
    private Button PORTbtn;
    private Button ZeeReset;
    private Button AllReset;
    private SwitchCompat switchTATA;
    private SwitchCompat switchTATA2;


    private static final int PERMISSION_REQUEST_CODE = 1;


    private SkySharedPref preferenceManager;
    private LottieAnimationView lottieAnimationView;
    private int[] animationResources = {
            R.raw.batmanx,
            R.raw.bird,
    };
    private int[] weights = {
            3  // Weight for batman
            , 5  // Weight for bird
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

        switchTATA = findViewById(R.id.switchTATA);
        switchTATA2 = findViewById(R.id.switchTATA2);

        ZeeReset = findViewById(R.id.ZeeReset);
        AllReset = findViewById(R.id.AllReset);



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

        String serverSetupIsTATA = preferenceManager.getKey("server_setup_isTATA");
        switchTATA.setChecked("Yes".equals(serverSetupIsTATA));

        String serverSetupIsTATA2 = preferenceManager.getKey("server_setup_isTATA2");
        switchTATA2.setChecked("Yes".equals(serverSetupIsTATA2));

    }


    private void setupSwitchListeners() {

        switchEXTRA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Utils.sky_extra_on(SetupActivity_Extra.this, preferenceManager);
                } else {
                    Utils.showCustomToast(SetupActivity_Extra.this, ("Disabling support for extra channels"));
                    preferenceManager.setKey("server_setup_isEXTRA", "No");
                    Utils.sky_extra_off(SetupActivity_Extra.this);
                }
            }
        });

        ZeeOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity_Extra.this);
                builder.setTitle("Choose Connection Type");
                builder.setMessage("Select localhost or IP for ZEE playlist download");

                builder.setPositiveButton("Localhost", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadZeePlaylistLocal();
                    }
                });

                builder.setNegativeButton("IP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("d","dwad");
                        downloadZeePlaylistIP();
                    }
                });

                builder.show();
            }
        });





        SonyOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isChromeInstalled()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shrinkme.ink/sony_playlist_"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    startActivity(intent);
                } else {
                    Utils.showCustomToast(SetupActivity_Extra.this, "Downloading SONY playlist");
                    String fileUrl = "https://bit.ly/sony-playlist";
                    downloadFile(fileUrl);
                }
            }
        });

        AllOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity_Extra.this);
                builder.setTitle("Choose Connection Type");
                builder.setMessage("Select localhost or IP for Combined playlist download");

                builder.setPositiveButton("Localhost", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadAIOPlaylistLocal();
                    }
                });

                builder.setNegativeButton("IP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("d","dwad");
                        downloadAIOPlaylistIP();
                    }
                });

                builder.show();
            }


        });

        switchTATA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Utils.sky_tata_on(SetupActivity_Extra.this, preferenceManager);
                } else {
                    Utils.showCustomToast(SetupActivity_Extra.this, "Disabling support for TATA Play");
                    preferenceManager.setKey("server_setup_isTATA", "No");
                    Utils.sky_tata_off(SetupActivity_Extra.this);
                }
            }
        });

        switchTATA2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Utils.sky_tata2_on(SetupActivity_Extra.this, preferenceManager);
                } else {
                    Utils.showCustomToast(SetupActivity_Extra.this, "Disabling support for TATA Play Alt");
                    preferenceManager.setKey("server_setup_isTATA2", "No");
                    Utils.sky_tata2_off(SetupActivity_Extra.this);
                }
            }
        });


        ZeeReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivity_Extra.this, "Reinstalling ZEE Script");

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

        AllReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivity_Extra.this, "Clearing all scripts");

                Intent intentC = new Intent();
                intentC.setClassName("com.termux", "com.termux.app.RunCommandService");
                intentC.setAction("com.termux.RUN_COMMAND");
                intentC.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.skyutils.sh");
                intentC.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"extra_reset"});
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
                Utils.sky_changeportzee(SetupActivity_Extra.this, new OnPortChangeListener() {
                    @Override
                    public void onPortChanged(String newPort) {
                        // Update the button text with the new port number
                        PORTbtn.setText(newPort);
                        // Optionally, show a toast message
                        // Utils.showCustomToast(SetupActivityApp.this, "Changed port to " + newPort);
                        Utils.showCustomToast(SetupActivity_Extra.this, "Restarting CustTermux to apply changes");
                    }
                });
            }
        });


    }

    private void downloadAIOPlaylistLocal() {
        String server_setup_wifiip = "localhost";
        String isLocalPORTonly = preferenceManager.getKey("isLocalPORTonly");
        String prefix = "aHR0cHM6Ly9zaHJpbmttZS5pby9zdD9hcGk9YTc1OTY2NmY3YWZlNTJlYzY2OTk1NjhjMTVkYTZhODk4MWMwNDkzOSZ1cmw9";
        @SuppressLint({"NewApi", "LocalSuppress"}) String decodedUrl = new String(Base64.getDecoder().decode(prefix));
        String url = "https://sky7t.github.io/1/?ip="+server_setup_wifiip+":"+isLocalPORTonly;
        String finalUrl = decodedUrl + url;

        if (isChromeInstalled()) {
            Utils.showCustomToast(SetupActivity_Extra.this, "Downloading combined playlist");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            startActivity(intent);
        } else {

            if (checkPermission()) {
                Utils.showCustomToast(SetupActivity_Extra.this, "Downloading combined playlist");
                Intent intent = new Intent(SetupActivity_Extra.this, WebViewDL.class);
                intent.putExtra("url", url);
                intent.putExtra("filen", "Combined_Playlist.m3u");
                startActivity(intent);
            }
            else{
                requestPermission();
            }


        }
    }

    private void downloadAIOPlaylistIP() {
        String server_setup_wifiip = preferenceManager.getKey("server_setup_wifiip");
        String isLocalPORTonly = preferenceManager.getKey("isLocalPORTonly");
        String prefix = "aHR0cHM6Ly9zaHJpbmttZS5pby9zdD9hcGk9YTc1OTY2NmY3YWZlNTJlYzY2OTk1NjhjMTVkYTZhODk4MWMwNDkzOSZ1cmw9";
        @SuppressLint({"NewApi", "LocalSuppress"}) String decodedUrl = new String(Base64.getDecoder().decode(prefix));
        String url = "https://sky7t.github.io/1/?ip="+server_setup_wifiip+":"+isLocalPORTonly;
        String finalUrl = decodedUrl + url;

        if (isChromeInstalled()) {
            Utils.showCustomToast(SetupActivity_Extra.this, "Downloading combined playlist");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            startActivity(intent);
        } else {

            if (checkPermission()) {
                Utils.showCustomToast(SetupActivity_Extra.this, "Downloading combined playlist");
                Intent intent = new Intent(SetupActivity_Extra.this, WebViewDL.class);
                intent.putExtra("url", url);
                intent.putExtra("filen", "Combined_Playlist.m3u");
                startActivity(intent);
            }
            else{
                requestPermission();
            }


        }
    }



    private void downloadZeePlaylistLocal() {
        if (isChromeInstalled()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shrinkme.ink/zee_playlist_"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            startActivity(intent);
        } else {
            Utils.showCustomToast(SetupActivity_Extra.this, "Downloading ZEE playlist");
            String fileUrl = "https://bit.ly/zee-playlist";
            downloadFile(fileUrl);
        }

    }

    private void  downloadZeePlaylistIP() {
        String server_setup_wifiip = preferenceManager.getKey("server_setup_wifiip");
        String isLocalPORTonly = preferenceManager.getKey("isLocalPORTonly");
        String prefix = "aHR0cHM6Ly9zaHJpbmttZS5pby9zdD9hcGk9YTc1OTY2NmY3YWZlNTJlYzY2OTk1NjhjMTVkYTZhODk4MWMwNDkzOSZ1cmw9";
        @SuppressLint({"NewApi", "LocalSuppress"}) String decodedUrl = new String(Base64.getDecoder().decode(prefix));
        String url = "https://sky7t.github.io/1/?ip="+server_setup_wifiip+":"+isLocalPORTonly+"&zee=true";
        String finalUrl = decodedUrl + url;

        if (isChromeInstalled()) {
            Utils.showCustomToast(SetupActivity_Extra.this, "Downloading ZEE playlist");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            startActivity(intent);
        } else {
            if (checkPermission()) {
                Utils.showCustomToast(SetupActivity_Extra.this, "Downloading ZEE playlist");
                Intent intent = new Intent(SetupActivity_Extra.this, WebViewDL.class);
                intent.putExtra("url", url);
                intent.putExtra("filen", "Zee_Playlist.m3u");
                startActivity(intent);
            }
            else{
                requestPermission();
            }
        }
        }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Storage permission is required. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
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
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void downloadFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName+" Download");
        request.setDescription("Downloading " + fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName+".m3u");

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }


}
