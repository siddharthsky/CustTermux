package com.termux.setup;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.R;
import com.termux.SkySharedPref;

public class StepThreeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_3, container, false);

        RadioGroup serverBootGroup = view.findViewById(R.id.server_boot_group);
        RadioButton serverYesOption = view.findViewById(R.id.server_yes_option);
        RadioButton serverNoOption = view.findViewById(R.id.server_no_option);

        SkySharedPref preferenceManager = new SkySharedPref(getActivity());

        // Set default selection to "NO"
        //serverBootGroup.check(R.id.server_no_option);

        // Set up listener for RadioGroup
        serverBootGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String isAutoboot = preferenceManager.getKey("server_setup_isAutoboot");
            preferenceManager.setKey("server_setup_isAutoboot", isAutoboot);

            if (checkedId == R.id.server_yes_option) {
                preferenceManager.setKey("server_setup_isAutoboot", "Yes");
            } else if (checkedId == R.id.server_no_option) {
                preferenceManager.setKey("server_setup_isAutoboot", null);
            }
        });

        // Set up focus change listeners
        setupFocusListeners(serverYesOption, serverNoOption);

        return view;
    }

    private void setupFocusListeners(RadioButton... radioButtons) {
        View.OnFocusChangeListener focusChangeListener = (view, hasFocus) -> {
            if (hasFocus) {
                view.setBackgroundColor(Color.YELLOW); // Change background color when focused
            } else {
                view.setBackgroundColor(Color.TRANSPARENT); // Reset background color when not focused
            }
        };

        for (RadioButton radioButton : radioButtons) {
            final int defaultTextColor = radioButton.getCurrentTextColor();

            radioButton.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    view.setBackgroundColor(Color.YELLOW);
                    ((RadioButton) view).setTextColor(Color.BLACK); // Change text color to black when focused
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    ((RadioButton) view).setTextColor(defaultTextColor); // Reset text color to default
                }
            });
        }
    }
}
