package com.termux;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AppSelectorActivity extends AppCompatActivity {

    private ListView listView;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selector); // Ensure this matches your layout file

        getSupportActionBar().setTitle("Select IPTV app to launch");

        listView = findViewById(R.id.app_list_view); // This ID should match the ListView ID in your layout file
        packageManager = getPackageManager();

        List<AppInfo> appInfoList = new ArrayList<>();
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

                SharedPreferences sharedPreferences = getSharedPreferences("SkySharedPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("app_name", selectedPackageName);
                editor.putString("app_launchactivity", selectedLaunchActivity);
                editor.apply();

                finish(); // Close the activity
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

            return convertView;
        }
    }
}
