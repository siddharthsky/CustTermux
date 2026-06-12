package com.termux.sky;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class TxVerify {

    private static final String PREFS = "premium_pref";
    private static final String KEY_PREMIUM = "ads_removed";
    private static final String KEY_LICENSE = "license_key";
    private static final String KEY_EXPIRY = "expiry_date";

    public static final String PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc6zycJFGDWtBxfbVUzAley/zpVq1ZIJ5xtskAMjg9ln/ennTwh+K3eK4x6s6XWPenIKILIBq6hD9aXGNW9H8bw==";

    public static boolean verifyLicense(
        Context context,
        String license) {

        try {

            String[] parts =
                license.split("\\|");

            if (parts.length != 4)
                return false;

            String tier = parts[0];
            String expiryStr = parts[1];
            String licenseDeviceId = parts[2];

            // 1. EXPIRY DATE CHECK
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                java.util.Date expiryDate = sdf.parse(expiryStr);
                java.util.Date currentDate = new java.util.Date();

                if (expiryDate != null && currentDate.after(expiryDate)) {
                    return false;
                }
            } catch (java.text.ParseException e) {
                e.printStackTrace();
                return false;
            }

            // 2. CONDITIONAL DEVICE ID MATCH CHECK
            if (!tier.contains("sky7")) {
                String currentDeviceId = getDeviceId(context);
                if (!currentDeviceId.equals(licenseDeviceId)) {
                    return false;
                }
            }

            // 3. CRYPTOGRAPHIC SIGNATURE CHECK
            String data =
                parts[0] + "|" +
                    parts[1] + "|" +
                    parts[2];

            String sigStr = parts[3];

            while (sigStr.length() % 4 != 0)
                sigStr += "=";

            byte[] sig =
                Base64.decode(
                    sigStr,
                    Base64.URL_SAFE);

            byte[] pubBytes =
                Base64.decode(
                    PUBLIC_KEY,
                    Base64.DEFAULT);

            PublicKey publicKey =
                KeyFactory.getInstance("EC")
                    .generatePublic(
                        new X509EncodedKeySpec(
                            pubBytes));

            Signature verifier =
                Signature.getInstance(
                    "SHA256withECDSA");

            verifier.initVerify(publicKey);

            verifier.update(
                data.getBytes(
                    StandardCharsets.UTF_8));

            return verifier.verify(sig);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean activateLicense(
        Context context,
        String license) {

        boolean valid = verifyLicense(context, license);

        SharedPreferences prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        if (valid) {
            try {
                String[] parts = license.split("\\|");
                String expiryStr = parts[1];

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                java.util.Date expiryDate = sdf.parse(expiryStr);

                long expiryMillis = (expiryDate != null) ? expiryDate.getTime() : 0;

                prefs.edit()
                    .putBoolean(KEY_PREMIUM, true)
                    .putString(KEY_LICENSE, license)
                    .putLong(KEY_EXPIRY, expiryMillis)
                    .apply();

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    public static boolean isPremium(Context context) {
        SharedPreferences prefs =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String storedLicense = prefs.getString(KEY_LICENSE, null);

        if (storedLicense == null || storedLicense.isEmpty()) {
            return false;
        }

        boolean isValid = verifyLicense(context, storedLicense);

        if (!isValid) {
            clearLicense(context);
            return false;
        }

        return true;
    }

    public static void clearLicense(Context context) {

        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREMIUM)
            .remove(KEY_LICENSE)
            .remove(KEY_EXPIRY)
            .apply();
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        if (context == null) {
            return "UNKNOWN_DEVICE";
        }

        try {
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );

            if (androidId == null || "9774d56d682e549c".equals(androidId)) {
                return "UNKNOWN_DEVICE";
            }

            return androidId;
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN_DEVICE";
        }
    }
}
