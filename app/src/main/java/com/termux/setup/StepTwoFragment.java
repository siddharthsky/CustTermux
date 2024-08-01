package com.termux.setup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.AppSelectorActivity;
import com.termux.R;
import com.termux.SkySharedPref;

public class StepTwoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_2, container, false);

        RadioGroup autoOpenIptvGroup = view.findViewById(R.id.auto_open_iptv_group);
        RadioButton iptvYesOption = view.findViewById(R.id.iptv_yes_option);
        RadioButton iptvNoOption = view.findViewById(R.id.iptv_no_option);
        Button buttonIptvName = view.findViewById(R.id.button_iptv_name);
        LinearLayout appInfoLayout = view.findViewById(R.id.app_info_layout);
        ImageView appIcon = view.findViewById(R.id.app_icon);
        TextView textAppName = view.findViewById(R.id.text_app_name);

        SkySharedPref preferenceManager = new SkySharedPref(getActivity());

        // Set up listener for RadioGroup
        autoOpenIptvGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.iptv_yes_option) {
                buttonIptvName.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.iptv_no_option) {
                buttonIptvName.setVisibility(View.GONE);
                preferenceManager.setKey("app_name", "null");
                preferenceManager.setKey("app_launchactivity", "null");
            }
        });

        buttonIptvName.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AppSelectorActivity.class);
            startActivity(intent);
        });

        // Set up focus change listeners
        setupFocusListeners(buttonIptvName, iptvYesOption, iptvNoOption);

        return view;
    }

    private void setupFocusListeners(Button button, RadioButton... radioButtons) {
        View.OnFocusChangeListener focusChangeListener = (view, hasFocus) -> {
            if (hasFocus) {
                if (view instanceof Button) {
                    view.setBackgroundColor(Color.YELLOW);
                    ((Button) view).setTextColor(Color.BLACK); // Change text color to black when focused
                } else if (view instanceof RadioButton) {
                    view.setBackgroundColor(Color.YELLOW);
                    ((RadioButton) view).setTextColor(Color.BLACK); // Change text color to black when focused
                }
            } else {
                view.setBackgroundColor(Color.TRANSPARENT); // Reset to default background when focus is lost
                if (view instanceof Button) {
                    ((Button) view).setTextColor(Color.WHITE); // Reset button text color to default
                } else if (view instanceof RadioButton) {
                    ((RadioButton) view).setTextColor(Color.WHITE); // Reset RadioButton text color to default
                }
            }
        };

        button.setOnFocusChangeListener(focusChangeListener);

        for (RadioButton radioButton : radioButtons) {
            radioButton.setOnFocusChangeListener(focusChangeListener);
        }
    }
}
