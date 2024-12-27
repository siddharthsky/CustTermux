package com.termux;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;
import com.termux.app.TermuxActivity;


import org.json.JSONException;
import org.json.JSONObject;

public class KickStart extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JSONObject consentObject = new JSONObject();
        try {
            // Provide correct consent value to sdk which is obtained by User
            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, false);
            // Provide 0 if GDPR is not applicable and 1 if applicable
            consentObject.put("gdpr", "0");
            // Provide user consent in IAB format
            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_IAB, " << consent in IAB format >> ");
        } catch (JSONException e) {
            Log.e("XX#-gms", "InMobi Init failed -" + e);
        }
        InMobiSdk.init(this, "5bdfd8c077e34cdda2cd4c34aa65af1c", consentObject, new SdkInitializationListener() {
            @Override
            public void onInitializationComplete(@Nullable Error error) {
                if (null != error) {
                    Log.e("XX#-gms", "InMobi Init failed -" + error.getMessage());
                } else {
                    Log.d("XX#-gms", "InMobi Init Successful");
                }
            }
        });



        // Start the TermuxActivity and finish KickStart to remove it from the back stack
        Intent intent = new Intent(KickStart.this, TermuxActivity.class);
        startActivity(intent);
        finish();
    }
}
