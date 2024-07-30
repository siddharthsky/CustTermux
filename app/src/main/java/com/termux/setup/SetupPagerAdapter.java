package com.termux.setup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

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
                return new StepZeroFragment();
            case 1:
                //Log.d("dx","1");
                return new StepOneFragment();
            case 2:
                //Log.d("dx","2");
                return new StepTwoFragment();
            case 3:
                //Log.d("dx","3");
                return new StepThreeFragment();
            // Add more cases for additional steps
            default:
                return new StepZeroFragment();
        }
    }

    @Override
    public int getCount() {
        return 4; // Number of steps or fragments
    }
}
