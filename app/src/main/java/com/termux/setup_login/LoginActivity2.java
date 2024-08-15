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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.SkySharedPref;
import com.termux.Utils;

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

        // Initialize SkySharedPref and other member variables
        preferenceManager = new SkySharedPref(this);
        BASE_URL = preferenceManager.getKey("isLocalPORT");

        EditText inputNumber = findViewById(R.id.input_number);
        EditText inputOtp = findViewById(R.id.input_otp);
        Button sendOtpButton = findViewById(R.id.button_send_otp);
        Button verifyOtpButton = findViewById(R.id.button_verify_otp);
        TextView server_status = findViewById(R.id.server_status);

        String Server_chalu_hai_kay = preferenceManager.getKey("isServerRunning");
        String AIO = "Current server status: " + Server_chalu_hai_kay;
        if ("Running".equals(Server_chalu_hai_kay)) {
            server_status.setText(AIO);
        } else if ("Stopped".equals(Server_chalu_hai_kay)) {
            server_status.setText(AIO);
        } else {
            String AI2O = "Current server status: Error";
            server_status.setText(AI2O);
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

        setupFocusListeners(sendOtpButton, verifyOtpButton);
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
                } else {
                    Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
                }
            } catch (Exception e) {
                Utils.showCustomToast(LoginActivity2.this, "Error parsing response");
            }
        } else {
            Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
        }
        finish();
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
