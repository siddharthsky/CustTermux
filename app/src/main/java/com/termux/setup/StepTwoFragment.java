package com.termux.setup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.AppSelectorActivity;
import com.termux.R;
import com.termux.SkySharedPref;

import java.util.List;

public class StepTwoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_2, container, false);

        RadioGroup autoOpenIptvGroup = view.findViewById(R.id.auto_open_iptv_group);
        Button buttonIptvName = view.findViewById(R.id.button_iptv_name);
        LinearLayout appInfoLayout = view.findViewById(R.id.app_info_layout);
        ImageView appIcon = view.findViewById(R.id.app_icon);
        TextView textAppName = view.findViewById(R.id.text_app_name);

        // Set up listener for RadioGroup
        autoOpenIptvGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.iptv_yes_option) {
                    buttonIptvName.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.iptv_no_option) {
                    buttonIptvName.setVisibility(View.GONE);
                }
            }
        });

        buttonIptvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "IPTV Selected", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), AppSelectorActivity.class);
                startActivity(intent);

                // Retrieve and display app_launchactivity and app_name
                SkySharedPref preferenceManager = new SkySharedPref(getActivity());
                String packageName = preferenceManager.getKey("app_name"); // Assuming app_name contains the package name

                if (packageName != null && !packageName.isEmpty()) {
                    try {
                        PackageManager packageManager = getActivity().getPackageManager();
                        Drawable icon = packageManager.getApplicationIcon(packageName);
                        String appLabel = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();

                        // Set the app name and icon
                        textAppName.setText(appLabel);
                        appIcon.setImageDrawable(icon);

                        appInfoLayout.setVisibility(View.VISIBLE);

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        // Handle error: app package not found
                        Toast.makeText(getActivity(), "App not found", Toast.LENGTH_SHORT).show();
                    }
                }



            }
        });



        return view;
    }
}
