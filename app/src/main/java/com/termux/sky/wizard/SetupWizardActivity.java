package com.termux.sky.wizard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.termux.R;

public class SetupWizardActivity extends AppCompatActivity {

    ViewFlipper flipper;
    Button btnNext, btnBack;
    LinearLayout dots;

    int step = 0;
    int totalSteps = 5;

    GestureDetectorCompat gestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);

        flipper = findViewById(R.id.flipper);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        dots = findViewById(R.id.dots);

        setupDots();
        updateUI();

        // TOUCH SWIPE DETECTOR
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                if (e1.getX() - e2.getX() > 100) {
                    next();
                    return true;
                }

                if (e2.getX() - e1.getX() > 100) {
                    back();
                    return true;
                }

                return false;
            }
        });

        flipper.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        btnNext.setOnClickListener(v -> next());
        btnBack.setOnClickListener(v -> back());
    }

    private void next() {
        if (step < totalSteps - 1) {
            step++;
            animateNext();
            flipper.showNext();
            updateUI();
        } else {
            finishSetup();
        }
    }

    private void back() {
        if (step > 0) {
            step--;
            animateBack();
            flipper.showPrevious();
            updateUI();
        }
    }

    private void animateNext() {
        flipper.setInAnimation(this, R.anim.slide_in_right);
        flipper.setOutAnimation(this, R.anim.slide_out_left);
    }

    private void animateBack() {
        flipper.setInAnimation(this, R.anim.slide_in_left);
        flipper.setOutAnimation(this, R.anim.slide_out_right);
    }

    private void updateUI() {
        btnBack.setVisibility(step == 0 ? View.GONE : View.VISIBLE);
        btnNext.setText(step == totalSteps - 1 ? "Finish" : "Next");
        updateDots();
    }

    private void setupDots() {
        dots.removeAllViews();

        for (int i = 0; i < totalSteps; i++) {
            TextView dot = new TextView(this);
            dot.setText("•");
            dot.setTextSize(22);
            dot.setPadding(6, 0, 6, 0);
            dot.setTextColor(i == 0 ? 0xFFFFFFFF : 0x55FFFFFF);
            dots.addView(dot);
        }
    }

    private void updateDots() {
        int count = dots.getChildCount();

        for (int i = 0; i < count; i++) {
            TextView dot = (TextView) dots.getChildAt(i);
            dot.setTextColor(i == step ? 0xFFFFFFFF : 0x55FFFFFF);
        }
    }

    private void finishSetup() {
        getSharedPreferences("app", MODE_PRIVATE)
            .edit()
            .putBoolean("setup_done", true)
            .apply();

        finish();
    }
}
