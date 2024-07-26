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

import com.termux.AppSelectorActivity;
import com.termux.R;

public class StepTwoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_2, container, false);

        RadioGroup autoOpenIptvGroup = view.findViewById(R.id.auto_open_iptv_group);
        Button buttonIptvName = view.findViewById(R.id.button_iptv_name);

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
            }
        });

        return view;
    }
}
