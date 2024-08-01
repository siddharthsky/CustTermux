package com.termux;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.termux.setup.SetupActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity2 extends AppCompatActivity {

    private static final String TAG = "LoginActivity2";
    private static final String BASE_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.DarkActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setup);

        EditText inputNumber = findViewById(R.id.input_number);
        EditText inputOtp = findViewById(R.id.input_otp);
        Button sendOtpButton = findViewById(R.id.button_send_otp);
        Button verifyOtpButton = findViewById(R.id.button_verify_otp);

        TextView server_status = findViewById(R.id.server_status);


        SkySharedPref preferenceManager = new SkySharedPref(LoginActivity2.this);
        String Server_chalu_hai_kay = preferenceManager.getKey("isServerRunning");
        String AIO = "Current server status: "+ Server_chalu_hai_kay;
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
                new SendOtpTask().execute(phoneNumber);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter a phone number");
            }
        });

        verifyOtpButton.setOnClickListener(v -> {
            String otp = inputOtp.getText().toString().trim();
            String phoneNumber = inputNumber.getText().toString().trim();
            if (!otp.isEmpty() && !phoneNumber.isEmpty()) {
                new VerifyOtpTask().execute(phoneNumber, otp);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter phone number and OTP");
            }
        });

        setupFocusListeners(sendOtpButton, verifyOtpButton);
    }


    private void setupFocusListeners(Button... buttons) {
        // Create a map to store default text colors for each button
        final Map<Button, Integer> buttonDefaultColors = new HashMap<>();

        // Retrieve and store default text colors for each button
        for (Button button : buttons) {
            buttonDefaultColors.put(button, button.getCurrentTextColor());
        }

        View.OnFocusChangeListener focusChangeListener = (view, hasFocus) -> {
            if (hasFocus) {
                view.setBackgroundColor(Color.YELLOW);
                if (view instanceof Button) {
                    ((Button) view).setTextColor(Color.BLACK); // Change text color to black when focused
                }
            } else {
                view.setBackgroundColor(Color.TRANSPARENT); // Reset to default background color
                if (view instanceof Button) {
                    Button button = (Button) view;
                    // Retrieve the default text color for this button
                    Integer defaultTextColor = buttonDefaultColors.get(button);
                    if (defaultTextColor != null) {
                        button.setTextColor(defaultTextColor); // Reset text color to default
                    }
                }
            }
        };

        // Apply the focus change listener to each Button
        for (Button button : buttons) {
            button.setOnFocusChangeListener(focusChangeListener);
        }
    }


    private class SendOtpTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String phoneNumber = params[0];
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

                return response.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error sending OTP", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Utils.showCustomToast(LoginActivity2.this, "OTP Sent: " + result);
                EditText inputOtp = findViewById(R.id.input_otp);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputOtp.requestFocus();
                imm.showSoftInput(inputOtp, InputMethodManager.SHOW_IMPLICIT);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Failed to send OTP");
            }
        }
    }

    private class VerifyOtpTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String phoneNumber = params[0];
            String otp = params[1];
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

                return response.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error verifying OTP", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
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
    }
}
