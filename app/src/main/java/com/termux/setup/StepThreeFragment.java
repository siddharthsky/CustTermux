package com.termux.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

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

        SkySharedPref preferenceManager = new SkySharedPref(getActivity());

        // Set default selection to "NO"
        //serverBootGroup.check(R.id.server_no_option);

        // Set up listener for RadioGroup
        serverBootGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SkySharedPref preferenceManager = new SkySharedPref(getActivity());
                String isLocal = preferenceManager.getKey("server_setup_isAutoboot");
                preferenceManager.setKey("server_setup_isAutoboot", isLocal);

                if (checkedId == R.id.server_yes_option) {
                    preferenceManager.setKey("server_setup_isAutoboot", "Yes");
                } else if (checkedId == R.id.server_no_option) {
                    preferenceManager.setKey("server_setup_isAutoboot", null);
                }
            }
        });

        return view;
    }
}
