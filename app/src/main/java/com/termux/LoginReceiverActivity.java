package com.termux;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;



public class PhoneNumberReceiverActivity extends AppCompatActivity {

    private static String phoneNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_receiver);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.yourapp.ACTION_RECEIVE_PHONE_NUMBER")) {
                // Extract the phone number from the Intent
                String phoneNumber = intent.getStringExtra("phone_number");

                // Handle the phone number
                handlePhoneNumber(phoneNumber);
            }
        }
    }

    private void handlePhoneNumber(String phoneNumber) {
        // Your existing method for handling the phone number
        Log.d("PhoneNumberReceiver", "Received Phone Number: " + phoneNumber);
    }
}
