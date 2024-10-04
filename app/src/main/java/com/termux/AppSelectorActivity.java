package com.termux;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.Toast;

public class AppSelectorActivity extends AppCompatActivity {

    private ListView listView;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.DarkActivityTheme);
        setContentView(R.layout.activity_app_selector);

        // Enable the home button as an up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setTitle("Select IPTV app to launch");

        listView = findViewById(R.id.app_list_view);
        packageManager = getPackageManager();

        List<AppInfo> appInfoList = new ArrayList<>();

        // Add the "WEB TV" option as the first entry
        Drawable webTvIcon = getResources().getDrawable(R.mipmap.ic_launcher_neo);
        appInfoList.add(new AppInfo("WEB TV - for standalone use (No IPTV player needed)", "sky_web_tv", webTvIcon));

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.packageName;
            String appName = packageManager.getApplicationLabel(app.activityInfo.applicationInfo).toString();
            Drawable appIcon = null;
            try {
                appIcon = packageManager.getApplicationIcon(app.activityInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }

            appInfoList.add(new AppInfo(appName, packageName, appIcon));
        }

        AppAdapter adapter = new AppAdapter(this, appInfoList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo selectedApp = appInfoList.get(position);
                String selectedPackageName = selectedApp.packageName;
                String selectedLaunchActivity = getLaunchActivityForPackage(selectedPackageName);

                if ("sky_web_tv".equals(selectedPackageName)) {
                    selectedLaunchActivity = "WEB TV"; // or any default activity for WEB TV
                }

                // Convert Drawable to Bitmap and then to Base64
                Bitmap appIconBitmap = drawableToBitmap(selectedApp.appIcon);
                String appIconBase64 = bitmapToBase64(appIconBitmap);

                SharedPreferences sharedPreferences = getSharedPreferences("SkySharedPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("app_name", selectedPackageName);
                editor.putString("app_launchactivity", selectedLaunchActivity);
                editor.putString("app_icon", appIconBase64);
                editor.putString("app_name_x", selectedApp.appName); // Save app name in app_name_x
                editor.apply();

                LayoutInflater inflater = getLayoutInflater();
                View toastLayout = inflater.inflate(R.layout.custom_toast, null);

                ImageView toastIcon = toastLayout.findViewById(R.id.toast_icon);
                TextView toastText = toastLayout.findViewById(R.id.toast_text);

                toastIcon.setImageDrawable(selectedApp.appIcon);
                toastText.setText(selectedApp.appName);

                Toast toast = new Toast(getApplicationContext());
                toast.setView(toastLayout);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1000); // 1 second delay
            }
        });

    }

    private String getLaunchActivityForPackage(String packageName) {
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            return intent.getComponent().getClassName();
        }
        return "";
    }

    // Convert Drawable to Bitmap
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Convert Bitmap to Base64
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Convert Base64 to Bitmap
    public static Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    // Convert Bitmap to Drawable
    public static Drawable bitmapToDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static class AppInfo {
        String appName;
        String packageName;
        Drawable appIcon;

        AppInfo(String appName, String packageName, Drawable appIcon) {
            this.appName = appName;
            this.packageName = packageName;
            this.appIcon = appIcon;
        }
    }

    private static class AppAdapter extends ArrayAdapter<AppInfo> {

        private final List<AppInfo> apps;
        private final PackageManager packageManager;

        AppAdapter(AppSelectorActivity context, List<AppInfo> apps) {
            super(context, R.layout.app_list_item, apps);
            this.apps = apps;
            this.packageManager = context.getPackageManager();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.app_list_item, parent, false);
            }

            AppInfo appInfo = apps.get(position);

            ImageView iconView = convertView.findViewById(R.id.app_icon);
            TextView nameView = convertView.findViewById(R.id.app_name);

            iconView.setImageDrawable(appInfo.appIcon);
            nameView.setText(appInfo.appName);

            nameView.setTextColor(Color.parseColor("#C0C0C0"));

            return convertView;
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
