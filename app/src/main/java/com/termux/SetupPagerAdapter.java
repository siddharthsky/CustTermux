package com.termux;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.termux.setup.StepOneFragment;
import com.termux.setup.StepThreeFragment;
import com.termux.setup.StepTwoFragment;

// SetupPagerAdapter.java
public class SetupPagerAdapter extends FragmentStatePagerAdapter {

    public SetupPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                //Log.d("dx","0");
                return new StepOneFragment();
            case 1:
                //Log.d("dx","1");
                return new StepTwoFragment();
            case 2:
                //Log.d("dx","2");
                return new StepThreeFragment();
            // Add more cases for additional steps
            default:
                return new StepOneFragment();
        }
    }

    @Override
    public int getCount() {
        return 3; // Number of steps or fragments
    }
}
