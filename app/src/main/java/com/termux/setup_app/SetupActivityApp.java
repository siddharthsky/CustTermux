package com.termux.setup_app;

import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.termux.AppSelectorActivity;
import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;
import com.termux.Utils2;
import com.termux.app.TermuxActivity;
import com.termux.setup.SetupActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.crash.TermuxCrashUtils;

import java.util.Random;

public class SetupActivityApp extends AppCompatActivity {

    private SwitchCompat switchisLocal;
    private SwitchCompat switchAutostart;
    private SwitchCompat switchAutoboot;
    private SwitchCompat switchAutobootBG;
    private SwitchCompat switchLoginCheck;
    private SwitchCompat switchEPG;
    private SwitchCompat switchDRM;
    private SwitchCompat switchBANNER;
    private SwitchCompat switchSSH;
    private SwitchCompat switchZEE;
    private TextView textSSH;
    private TextView textselectedAPP;
    private Button IPTVbtn;
    private Button IPTVtim;
    private Button PORTbtn;
    private Button WEBbtn;
    private Button restartButton;
    private View IPTVbtnlay;
    private View IPTVtimlay;
    private View AUTOBGlay;
    private LinearLayout rbl;
    private LinearLayout sid;

    private SwitchCompat switchMINI;
    private View IPTVminilay;

    private ImageView sid2;
    private ImageView extraIconSKY;

    private ImageView rbx;
    private ImageView extraIconRBL;




    private SkySharedPref preferenceManager;
    private LottieAnimationView lottieAnimationView;
    private int[] animationResources = {
        R.raw.batmanx,  // Example raw resource ID for animation
        R.raw.bird,
    };
    private int[] weights = {
         3  // Weight for batman
        ,5  // Weight for bird
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setTheme(R.style.DarkActivityTheme);
        setTheme(R.style.CustomDarkBlueTheme);

        setContentView(R.layout.activity_setup_app);

        // Enable the home button as an up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        switchisLocal = findViewById(R.id.switchisLocal);
        switchAutostart = findViewById(R.id.switchAutostart);
        switchAutoboot = findViewById(R.id.switchAutoboot);
        switchAutobootBG = findViewById(R.id.switchAutobootBG);
        switchLoginCheck = findViewById(R.id.switchLoginCheck);
        switchEPG = findViewById(R.id.switchEPG);
        switchDRM = findViewById(R.id.switchDRM);
        switchBANNER = findViewById(R.id.switchBANNER);
        switchSSH = findViewById(R.id.switchSSH);
        switchZEE = findViewById(R.id.switchZEE);
        textSSH = findViewById(R.id.textSSH);
        textselectedAPP = findViewById(R.id.textselectedAPP);
        restartButton = findViewById(R.id.restartButton);
        PORTbtn = findViewById(R.id.PORTbtn);
        WEBbtn = findViewById(R.id.WEBbtn);
        IPTVbtn = findViewById(R.id.IPTVbtn);
        IPTVbtnlay = findViewById(R.id.IPTVbtnlay);
        IPTVtim = findViewById(R.id.IPTVtim);
        IPTVtimlay = findViewById(R.id.IPTVtimlay);
        switchMINI = findViewById(R.id.switchMINI);
        IPTVminilay = findViewById(R.id.IPTVminilay);
        AUTOBGlay = findViewById(R.id.AUTOBGlay);



        sid2 = findViewById(R.id.sid2);
        sid2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/siddharthsky"));
                startActivity(browserIntent);
            }
        });
        extraIconSKY = findViewById(R.id.extraIconSKY);
        extraIconSKY.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/siddharthsky"));
                startActivity(browserIntent);
            }
        });
        rbx = findViewById(R.id.rbx);
        rbx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rabilrbl"));
                startActivity(browserIntent);
            }
        });
        extraIconRBL = findViewById(R.id.extraIconRBL);
        extraIconRBL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/rabilrbl"));
                startActivity(browserIntent);
            }
        });


        lottieAnimationView = findViewById(R.id.settingsani);

        setRandomAnimationBasedOnWeight();

        preferenceManager = new SkySharedPref(this);

        rbl = findViewById(R.id.rbl);
        rbl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rabilrbl"));
                startActivity(browserIntent);
            }
        });

        sid = findViewById(R.id.sid);
        sid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/siddharthsky"));
                startActivity(browserIntent);
            }
        });



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
        String serverSetupIsLocal = preferenceManager.getKey("server_setup_isLocal");
        switchisLocal.setChecked("No".equals(serverSetupIsLocal));



        String autostartAppName = preferenceManager.getKey("app_name");
        switchAutostart.setChecked(autostartAppName != null && !"null".equals(autostartAppName));
        if (!switchAutostart.isChecked()) {
            IPTVbtnlay.setVisibility(View.GONE);
            IPTVtimlay.setVisibility(View.GONE);
            IPTVminilay.setVisibility(View.GONE);
        }else {
            String app_namex = preferenceManager.getKey("app_name_x");
            String isDelayTime = preferenceManager.getKey("isDelayTime");
            if (app_namex == null || app_namex.isEmpty()) {
                textselectedAPP.setText("Selected App: empty");
                IPTVtim.setText("5 Sec");
                String serverSetupIsMINI = preferenceManager.getKey("isFlagSetForMinimize");
                switchMINI.setChecked("Yes".equals(serverSetupIsMINI));
            } else {
                String c_app_namex = "Selected App: " + app_namex;
                String isDelayTimestr = isDelayTime+" Sec";
                IPTVtim.setText(isDelayTimestr);
                textselectedAPP.setText(c_app_namex);
                String serverSetupIsMINI = preferenceManager.getKey("isFlagSetForMinimize");
                switchMINI.setChecked("Yes".equals(serverSetupIsMINI));
            }
        }

        String serverSetupIsAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
        switchAutoboot.setChecked("Yes".equals(serverSetupIsAutoboot));
        if (!switchAutoboot.isChecked()) {
            AUTOBGlay.setVisibility(View.GONE);
            String switchAutobootBGx = preferenceManager.getKey("server_setup_isAutobootBG");
            switchAutobootBG.setChecked("Yes".equals(switchAutobootBGx));
        }else {
            String server_setup_isAutobootBG = preferenceManager.getKey("server_setup_isAutobootBG");
            switchAutobootBG.setChecked("Yes".equals(server_setup_isAutobootBG));
            AUTOBGlay.setVisibility(View.VISIBLE);
            String switchAutobootBGx = preferenceManager.getKey("server_setup_isAutobootBG");
            switchAutobootBG.setChecked("Yes".equals(switchAutobootBGx));
        }

        String serverSetupIsLoginCheck = preferenceManager.getKey("server_setup_isLoginCheck");
        switchLoginCheck.setChecked("Yes".equals(serverSetupIsLoginCheck));

        String serverSetupIsEPG = preferenceManager.getKey("server_setup_isEPG");
        switchEPG.setChecked("Yes".equals(serverSetupIsEPG));

        String serverSetupIsDRM = preferenceManager.getKey("server_setup_isDRM");
        switchDRM.setChecked("Yes".equals(serverSetupIsDRM));

        String serverSetupIsGenericBanner = preferenceManager.getKey("server_setup_isGenericBanner");
        switchBANNER.setChecked("Yes".equals(serverSetupIsGenericBanner));

        String serverSetupIsSSH = preferenceManager.getKey("server_setup_isSSH");
        switchSSH.setChecked("Yes".equals(serverSetupIsSSH));

        String serverSetupIsZEE = preferenceManager.getKey("server_setup_isZEE");
        switchZEE.setChecked("Yes".equals(serverSetupIsZEE));

        String b_username = preferenceManager.getKey("server_setup_username");
        String b_ip = preferenceManager.getKey("server_setup_wifiip");
        String c_usage = "Username: "+ b_username +" \nPassword: letmein \nUsage: ssh "+b_username+"@"+b_ip+" -p 8022";

        if ("Yes".equals(serverSetupIsSSH)){
            textSSH.setText(c_usage);
        } else {
            textSSH.setText("Remote access terminal from other device");
        }

    }

    private void savePreferences() {
        preferenceManager.setKey("server_setup_isLocal", switchisLocal.isChecked() ? "No" : "Yes");
        preferenceManager.setKey("server_setup_isAutoboot", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isLoginCheck", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isEPG", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isDRM", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isGenericBanner", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isSSH", switchSSH.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isZEE", switchZEE.isChecked() ? "Yes" : "No");
    }

    private void setupSwitchListeners() {
        switchisLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.showCustomToast(SetupActivityApp.this, "Server: " + (isChecked ? "Public" : "Local"));
                preferenceManager.setKey("server_setup_isLocal", isChecked ? "No" : "Yes");
            }
        });

        switchAutostart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.showCustomToast(SetupActivityApp.this, "Autostart IPTV: " + (isChecked ? "On" : "No"));
                if (isChecked) {
                    IPTVbtnlay.setVisibility(View.VISIBLE);
                    IPTVtimlay.setVisibility(View.VISIBLE);
                    IPTVminilay.setVisibility(View.VISIBLE);
                    String serverSetupIsMINI = preferenceManager.getKey("isFlagSetForMinimize");
                    switchMINI.setChecked("Yes".equals(serverSetupIsMINI));
                } else {
                    IPTVbtnlay.setVisibility(View.GONE);
                    IPTVtimlay.setVisibility(View.GONE);
                    IPTVminilay.setVisibility(View.GONE);
                    preferenceManager.setKey("app_name", "null");
                    preferenceManager.setKey("app_name_x", "null");
                    preferenceManager.setKey("isDelayTime", "5");
                    IPTVtim.setText("5 Sec");
                    String serverSetupIsMINI = preferenceManager.getKey("isFlagSetForMinimize");
                    switchMINI.setChecked("Yes".equals(serverSetupIsMINI));
                }
            }
        });

        switchAutoboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.showCustomToast(SetupActivityApp.this, (isChecked ? "CustTermux will autostart on boot." : "CustTermux will not autostart"));
                //preferenceManager.setKey("server_setup_isAutoboot", isChecked ? "Yes" : "No");
                if (isChecked) {
                    AUTOBGlay.setVisibility(View.VISIBLE);
                    //Utils.showCustomToast(SetupActivityApp.this, ("2xCustTermux will autostart on boot."));
                    preferenceManager.setKey("server_setup_isAutoboot", "Yes");
                    String switchAutobootBGx = preferenceManager.getKey("server_setup_isAutobootBG");
                    switchAutobootBG.setChecked("Yes".equals(switchAutobootBGx));

                } else {
                    AUTOBGlay.setVisibility(View.GONE);
                    //Utils.showCustomToast(SetupActivityApp.this, ("2xCustTermux will not autostart"));
                    preferenceManager.setKey("server_setup_isAutoboot", "No");
                    String switchAutobootBGx = preferenceManager.getKey("server_setup_isAutobootBG");
                    switchAutobootBG.setChecked("Yes".equals(switchAutobootBGx));
                }
            }
        });

        switchAutobootBG.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isAutobootBG", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("[CTx] Server will run in Background"));
                    preferenceManager.setKey("server_setup_isAutobootBG", "Yes");
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("[CTx] Server will run in Foreground"));
                    preferenceManager.setKey("server_setup_isAutobootBG", "No");
                }
            }
        });

        switchLoginCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isLoginCheck", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("Login check is on"));
                    preferenceManager.setKey("server_setup_isLoginCheck", "Yes");
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("Login check is off"));
                    preferenceManager.setKey("server_setup_isLoginCheck", "No");
                }
            }
        });

        switchEPG.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isEPG", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("CustTermux will generate EPG at start"));
                    preferenceManager.setKey("server_setup_isEPG", "Yes");
                    Utils.sky_epg_on(SetupActivityApp.this);
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("EPG will not be generated"));
                    preferenceManager.setKey("server_setup_isEPG", "No");
                    Utils.sky_epg_off(SetupActivityApp.this);
                }
            }
        });

        switchDRM.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("DRM is enabled [works only on chrome/firefox]"));
                    preferenceManager.setKey("server_setup_isDRM", "Yes");
                    Utils.sky_drm_on(SetupActivityApp.this);
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("DRM is disabled"));
                    preferenceManager.setKey("server_setup_isDRM", "No");
                    Utils.sky_drm_off(SetupActivityApp.this);
                }
            }
        });

        switchBANNER.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isGenericBanner", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("Set Generic TV banner successfully"));
                    preferenceManager.setKey("server_setup_isGenericBanner", "Yes");
                    Utils.changeIconToSecond(SetupActivityApp.this);
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("Set JioTV Go banner successfully"));
                    preferenceManager.setKey("server_setup_isGenericBanner", "No");
                    Utils.changeIconTOFirst(SetupActivityApp.this);
                }
            }
        });

        switchSSH.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isGenericBanner", isChecked ? "Yes" : "No");
                if (isChecked) {
                    preferenceManager.setKey("server_setup_isSSH", "Yes");
                    Utils.sky_ssh_on(SetupActivityApp.this);
                    Utils.showCustomToast(SetupActivityApp.this, ("Enabled SSH"));
//                    finish();

                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("Disabled SSH"));
                    preferenceManager.setKey("server_setup_isSSH", "No");
                    Utils.sky_ssh_off(SetupActivityApp.this);
//                    String c_usage = "Remote access terminal from other device";
//                    textSSH.setText(c_usage);
                }
            }
        });

        switchZEE.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    preferenceManager.setKey("server_setup_isZEE", "Yes");
                    Utils2.sky_zee_on(SetupActivityApp.this);
                    Utils.showCustomToast(SetupActivityApp.this, ("Enabled ZEE"));

                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("Disabled ZEE"));
                    preferenceManager.setKey("server_setup_isZEE", "No");
                    Utils2.sky_zee_off(SetupActivityApp.this);
                }
            }
        });

        switchMINI.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isLoginCheck", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(SetupActivityApp.this, ("isFlagSetForMinimize on"));
                    preferenceManager.setKey("isFlagSetForMinimize", "Yes");
                } else {
                    Utils.showCustomToast(SetupActivityApp.this, ("isFlagSetForMinimize off"));
                    preferenceManager.setKey("isFlagSetForMinimize", "No");
                }
            }
        });

        IPTVbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivityApp.this, "Select app to open");
                Intent intent = new Intent(SetupActivityApp.this, AppSelectorActivity.class);
                startActivityForResult(intent, 1); // Use startActivityForResult to get the result back
                String app_namex = preferenceManager.getKey("app_name_x");
                String c_app_namex = "Selected App: " + app_namex;
            }
        });

        IPTVtim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.sky_changedelay(SetupActivityApp.this, new OnTimeChangeListener() {
                    @Override
                    public void OnTimeChanged(String newTime) {
                        // Update the button text with the new port number
                        IPTVtim.setText(newTime+" Sec");
                        // Optionally, show a toast message
                        // Utils.showCustomToast(SetupActivityApp.this, "Changed port to " + newPort);
                        Utils.showCustomToast(SetupActivityApp.this, "Restarting CustTermux to apply changes");
                    }
                });
            }
        });

        PORTbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.sky_changeport(SetupActivityApp.this, new OnPortChangeListener() {
                    @Override
                    public void onPortChanged(String newPort) {
                        // Update the button text with the new port number
                        PORTbtn.setText(newPort);
                        // Optionally, show a toast message
                        // Utils.showCustomToast(SetupActivityApp.this, "Changed port to " + newPort);
                        Utils.showCustomToast(SetupActivityApp.this, "Restarting CustTermux to apply changes");
                    }
                });
            }
        });

        WEBbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Utils.lake_alert_WEBTV(TermuxActivity.this);
                Utils2.lake_alert_WEBTV(SetupActivityApp.this);

            }
        });


        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivityApp.this, "Restarting CustTermux");
                Utils.sky_rerun(SetupActivityApp.this);
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

        switchisLocal.setOnFocusChangeListener(focusChangeListener);
        switchAutostart.setOnFocusChangeListener(focusChangeListener);
        switchAutoboot.setOnFocusChangeListener(focusChangeListener);
        switchAutobootBG.setOnFocusChangeListener(focusChangeListener);
        switchLoginCheck.setOnFocusChangeListener(focusChangeListener);
        switchEPG.setOnFocusChangeListener(focusChangeListener);
        switchBANNER.setOnFocusChangeListener(focusChangeListener);
        switchSSH.setOnFocusChangeListener(focusChangeListener);
        IPTVbtn.setOnFocusChangeListener(focusChangeListener);
        IPTVtim.setOnFocusChangeListener(focusChangeListener);
        PORTbtn.setOnFocusChangeListener(focusChangeListener);
        WEBbtn.setOnFocusChangeListener(focusChangeListener);
        restartButton.setOnFocusChangeListener(focusChangeListener);
        switchMINI.setOnFocusChangeListener(focusChangeListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String selectedAppName = data.getStringExtra("selectedAppName");
            preferenceManager.setKey("app_name", selectedAppName);
            switchAutostart.setChecked(true);
            Utils.showCustomToast(SetupActivityApp.this, "Selected app: " + selectedAppName);
        }
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
        String app_namex = preferenceManager.getKey("app_name_x");
        String c_app_namex = "Selected App: " + app_namex;
        textselectedAPP.setText(c_app_namex);
        String portid = preferenceManager.getKey("isLocalPORTonly");
        PORTbtn.setText(portid);
    }

}
