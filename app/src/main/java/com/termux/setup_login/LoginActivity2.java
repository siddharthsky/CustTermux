package com.termux.setup_login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;
import com.termux.setup_app.SetupActivityApp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoginActivity2 extends AppCompatActivity {

    private SkySharedPref preferenceManager;
    private String BASE_URL;
    private static final String TAG = "LoginActivity2";
    private SwitchCompat switchLOGIN;
    private View GRID1;
    private View GRID2;
    private TextView subtextLOGIN;
    private Button button_login_password;

    // ExecutorService for running tasks asynchronously
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.DarkActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setup);

        // Enable the home button as an up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Initialize SkySharedPref and BASE_URL
        preferenceManager = new SkySharedPref(this);
        BASE_URL = preferenceManager.getKey("isLocalPORT");

        EditText inputNumber = findViewById(R.id.input_number);
        EditText inputOtp = findViewById(R.id.input_otp);
        EditText input_numberpass = findViewById(R.id.input_numberpass);
        EditText input_otppass = findViewById(R.id.input_otppass);
        Button sendOtpButton = findViewById(R.id.button_send_otp);
        Button verifyOtpButton = findViewById(R.id.button_verify_otp);
        TextView server_status = findViewById(R.id.server_status);
        switchLOGIN = findViewById(R.id.switchLOGIN);
        subtextLOGIN = findViewById(R.id.subtextLOGIN);
        GRID1 = findViewById(R.id.GRID1);
        GRID2 = findViewById(R.id.GRID2);
        button_login_password = findViewById(R.id.button_login_password);

       

        String Server_chalu_hai_kay = preferenceManager.getKey("isServerRunning");
        String AIO = "Current server status: " + Server_chalu_hai_kay;
        if ("Running".equals(Server_chalu_hai_kay)) {
            server_status.setText(AIO);
            server_status.setTextColor(Color.GREEN);
        } else if ("Stopped".equals(Server_chalu_hai_kay)) {
            server_status.setText(AIO);
            server_status.setTextColor(Color.RED);
        } else {
            String AI2O = "Current server status: Error";
            server_status.setText(AI2O);
            server_status.setTextColor(Color.RED);
        }

        // Request focus and show keyboard for inputNumber
        inputNumber.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(inputNumber, InputMethodManager.SHOW_IMPLICIT);

        sendOtpButton.setOnClickListener(v -> {
            String phoneNumber = inputNumber.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                sendOtp(phoneNumber);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter a phone number");
            }
        });

        verifyOtpButton.setOnClickListener(v -> {
            String otp = inputOtp.getText().toString().trim();
            String phoneNumber = inputNumber.getText().toString().trim();
            if (!otp.isEmpty() && !phoneNumber.isEmpty()) {
                verifyOtp(phoneNumber, otp);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter phone number and OTP");
            }
        });

        button_login_password.setOnClickListener(v -> {
            String PasswordX = input_otppass.getText().toString().trim();
            String phoneNumber = input_numberpass.getText().toString().trim();
            if (!phoneNumber.isEmpty() && !PasswordX.isEmpty()) {
                verifyPassword(phoneNumber, PasswordX);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter a phone number and password");
            }
        });


        setupFocusListeners(sendOtpButton, verifyOtpButton, switchLOGIN, button_login_password );

       // loadPreferences();
        setupSwitchListeners();
        setupFocusListeners();
    }


    private void setupSwitchListeners() {
        switchLOGIN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                preferenceManager.setKey("server_setup_isGenericBanner", isChecked ? "Yes" : "No");
                if (isChecked) {
                    Utils.showCustomToast(LoginActivity2.this, ("Switched to Login via OTP"));
                    preferenceManager.setKey("server_setup_isLoginviaOTP", "Yes");
//                    Utils.changeIconToSecond(LoginActivity2.this);
                    subtextLOGIN.setText("Currently logging in via OTP");
                    GRID1.setVisibility(View.VISIBLE);
                    GRID2.setVisibility(View.GONE);
                    button_login_password.setVisibility(View.GONE);
                } else {
                    Utils.showCustomToast(LoginActivity2.this, ("Switched to Login via Password"));
                    preferenceManager.setKey("server_setup_isLoginviaOTP", "No");
//                    Utils.changeIconTOFirst(LoginActivity2.this);
                    subtextLOGIN.setText("Currently logging in via Password");
                    GRID1.setVisibility(View.GONE);
                    GRID2.setVisibility(View.VISIBLE);
                    button_login_password.setVisibility(View.VISIBLE);
                }
            }
        });
    }

//    private void loadPreferences() {
//        String server_setup_isLoginviaOTP = preferenceManager.getKey("server_setup_isLoginviaOTP");
//        switchLOGIN.setChecked("Yes".equals(server_setup_isLoginviaOTP));
//        subtextLOGIN.setText("Currently logging in via OTP");
//        GRID1.setVisibility(View.VISIBLE);
//        GRID2.setVisibility(View.GONE);
//        button_login_password.setVisibility(View.GONE);
//    }


    private void verifyPassword(String phoneNumber, String passwordX) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "login");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"username\": \"" + phoneNumber + "\", \"password\": \"" + passwordX + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Utils.showCustomToast(LoginActivity2.this, ("Login success. Enjoy!"));
                        finish();
//                        startActivity(getIntent());
//                        Utils.sky_rerun(this);
                    });
                } else {
                    runOnUiThread(() -> Utils.showCustomToast(LoginActivity2.this, ("Login failed!")));
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> Utils.showCustomToast(LoginActivity2.this, ("Login failed!")));
            }
        }).start();
    }




    private void sendOtp(String phoneNumber) {
        executorService.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "login/sendOTP");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = "{\"number\": \"+91" + phoneNumber + "\"}";
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                runOnUiThread(() -> handleSendOtpResponse(response.toString()));

            } catch (Exception e) {
                Log.e(TAG, "Error sending OTP", e);
                runOnUiThread(() -> Utils.showCustomToast(LoginActivity2.this, "Failed to send OTP"));
            }
        });
    }

    private void handleSendOtpResponse(String result) {
        if (result != null) {
            Utils.showCustomToast(LoginActivity2.this, "OTP Sent: " + result);
            EditText inputOtp = findViewById(R.id.input_otp);
            inputOtp.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(inputOtp, InputMethodManager.SHOW_IMPLICIT);
        } else {
            Utils.showCustomToast(LoginActivity2.this, "Failed to send OTP");
        }
    }

    private void verifyOtp(String phoneNumber, String otp) {
        executorService.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "login/verifyOTP");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = "{\"number\": \"+91" + phoneNumber + "\", \"otp\": \"" + otp + "\"}";
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                runOnUiThread(() -> handleVerifyOtpResponse(response.toString()));

            } catch (Exception e) {
                Log.e(TAG, "Error verifying OTP", e);
                runOnUiThread(() -> Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP"));
            }
        });
    }

    private void handleVerifyOtpResponse(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = jsonObject.optString("status");
                if ("success".equals(status)) {
                    Utils.showCustomToast(LoginActivity2.this, "OTP Verified Successfully");
                    finish();
                } else {
                    Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
                }
            } catch (Exception e) {
                Utils.showCustomToast(LoginActivity2.this, "Error parsing response");
            }
        } else {
            Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
        }
//        finish();
    }

    private void setupFocusListeners(Button... buttons) {
        final Map<Button, Integer> buttonDefaultColors = new HashMap<>();

        for (Button button : buttons) {
            buttonDefaultColors.put(button, button.getCurrentTextColor());
        }

        View.OnFocusChangeListener focusChangeListener = (view, hasFocus) -> {
            if (hasFocus) {
                view.setBackgroundColor(Color.YELLOW);
                if (view instanceof Button) {
                    ((Button) view).setTextColor(Color.BLACK);
                }
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
                if (view instanceof Button) {
                    Button button = (Button) view;
                    Integer defaultTextColor = buttonDefaultColors.get(button);
                    if (defaultTextColor != null) {
                        button.setTextColor(defaultTextColor);
                    }
                }
            }
        };

        for (Button button : buttons) {
            button.setOnFocusChangeListener(focusChangeListener);
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
}
