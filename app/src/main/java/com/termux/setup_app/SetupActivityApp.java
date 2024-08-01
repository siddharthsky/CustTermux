package com.termux.setup_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.termux.AppSelectorActivity;
import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;
import com.termux.app.TermuxActivity;
import com.termux.setup.SetupActivity;

public class SetupActivityApp extends AppCompatActivity {

    private SwitchCompat switchisLocal;
    private SwitchCompat switchAutostart;
    private SwitchCompat switchAutoboot;
    private SwitchCompat switchLoginCheck;
    private Button IPTVbtn;
    private Button restartButton;
    private View IPTVbtnlay;
    private SkySharedPref preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.DarkActivityTheme);
        setContentView(R.layout.activity_setup_app);

        switchisLocal = findViewById(R.id.switchisLocal);
        switchAutostart = findViewById(R.id.switchAutostart);
        switchAutoboot = findViewById(R.id.switchAutoboot);
        switchLoginCheck = findViewById(R.id.switchLoginCheck);
        restartButton = findViewById(R.id.restartButton);
        IPTVbtn = findViewById(R.id.IPTVbtn);
        IPTVbtnlay = findViewById(R.id.IPTVbtnlay);


        preferenceManager = new SkySharedPref(this);

        loadPreferences();
        setupSwitchListeners();
        setupFocusListeners();
    }

    private void loadPreferences() {
        String serverSetupIsLocal = preferenceManager.getKey("server_setup_isLocal");
        switchisLocal.setChecked("No".equals(serverSetupIsLocal));

        String autostartAppName = preferenceManager.getKey("app_name");
        switchAutostart.setChecked(autostartAppName != null && !"null".equals(autostartAppName));
        if (!switchAutostart.isChecked()) {
            IPTVbtnlay.setVisibility(View.GONE);
        }

        String serverSetupIsAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
        switchAutoboot.setChecked("Yes".equals(serverSetupIsAutoboot));

        String serverSetupIsLoginCheck = preferenceManager.getKey("server_setup_isLoginCheck");
        switchLoginCheck.setChecked("Yes".equals(serverSetupIsLoginCheck));
    }

    private void savePreferences() {
        preferenceManager.setKey("server_setup_isLocal", switchisLocal.isChecked() ? "No" : "Yes");
        preferenceManager.setKey("server_setup_isAutoboot", switchAutoboot.isChecked() ? "Yes" : "No");
        preferenceManager.setKey("server_setup_isLoginCheck", switchAutoboot.isChecked() ? "Yes" : "No");
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
                } else {
                    IPTVbtnlay.setVisibility(View.GONE);
                    preferenceManager.setKey("app_name", "null");
                }
            }
        });

        switchAutoboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.showCustomToast(SetupActivityApp.this, (isChecked ? "CustTermux will autostart on boot." : "CustTermux will not autostart"));
                preferenceManager.setKey("server_setup_isAutoboot", isChecked ? "Yes" : "No");
            }
        });

        switchLoginCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferenceManager.setKey("server_setup_isLoginCheck", isChecked ? "Yes" : "No");
            }
        });

        IPTVbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showCustomToast(SetupActivityApp.this, "Select app to open");
                Intent intent = new Intent(SetupActivityApp.this, AppSelectorActivity.class);
                startActivityForResult(intent, 1); // Use startActivityForResult to get the result back
            }
        });
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
        switchLoginCheck.setOnFocusChangeListener(focusChangeListener);
        IPTVbtn.setOnFocusChangeListener(focusChangeListener);
        restartButton.setOnFocusChangeListener(focusChangeListener);
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
}
