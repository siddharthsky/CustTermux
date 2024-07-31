package com.termux;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.setup.SetupActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity2 extends AppCompatActivity {

    private static final String TAG = "LoginActivity2";
    private static final String BASE_URL = "http://localhost:5001/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.DarkActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setup);

        // Initialize Views
        EditText inputNumber = findViewById(R.id.input_number);
        EditText inputOtp = findViewById(R.id.input_otp);
        Button sendOtpButton = findViewById(R.id.button_send_otp);
        Button verifyOtpButton = findViewById(R.id.button_verify_otp);

        // Set up Button Click Listeners
        sendOtpButton.setOnClickListener(v -> {
            String phoneNumber = inputNumber.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                new SendOtpTask().execute(phoneNumber);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter a phone number");
                //Toast.makeText(LoginActivity2.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            }
        });

        verifyOtpButton.setOnClickListener(v -> {
            String otp = inputOtp.getText().toString().trim();
            String phoneNumber = inputNumber.getText().toString().trim();
            if (!otp.isEmpty() && !phoneNumber.isEmpty()) {
                new VerifyOtpTask().execute(phoneNumber, otp);
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Please enter phone number and OTP");
                //Toast.makeText(LoginActivity2.this, "Please enter phone number and OTP", Toast.LENGTH_SHORT).show();
            }
        });
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
                //Toast.makeText(LoginActivity2.this, "OTP Sent: " + result, Toast.LENGTH_SHORT).show();
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Failed to send OTP");
                //Toast.makeText(LoginActivity2.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
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
                        //Toast.makeText(LoginActivity2.this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
                        //Toast.makeText(LoginActivity2.this, "Failed to verify OTP", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Utils.showCustomToast(LoginActivity2.this, "Error parsing response");
                    //Toast.makeText(LoginActivity2.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            } else {
                Utils.showCustomToast(LoginActivity2.this, "Failed to verify OTP");
                //Toast.makeText(LoginActivity2.this, "Failed to verify OTP", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }
}
