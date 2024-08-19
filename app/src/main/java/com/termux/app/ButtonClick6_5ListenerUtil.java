package com.termux.app;

import android.content.Context;
import android.view.View;
import android.widget.Button;


public class ButtonClick6_5ListenerUtil {

    public static void setButtonClickListener(Context context, Button button) {

        button.setVisibility(View.GONE);

        // Check for updates
        new CheckForUpdateTask(context).execute();

    }
}

