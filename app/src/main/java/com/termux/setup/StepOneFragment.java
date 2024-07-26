package com.termux.setup;

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

public class StepOneFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_1, container, false);

        // Find the RadioGroup and RadioButtons
        RadioGroup deviceSelectionGroup = view.findViewById(R.id.device_selection_group);
        RadioButton deviceOnlyOption = view.findViewById(R.id.device_only_option);
        RadioButton networkDeviceOption = view.findViewById(R.id.network_device_option);

        // Set up listener for RadioGroup
        deviceSelectionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SkySharedPref preferenceManager = new SkySharedPref(getActivity());

                if (checkedId == R.id.device_only_option) {
                    preferenceManager.setKey("server_setup_isLocal", "Yes");
                } else if (checkedId == R.id.network_device_option) {
                    preferenceManager.setKey("server_setup_isLocal", "No");
                }
            }
        });

        return view;
    }
}
