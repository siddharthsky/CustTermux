package com.termux.sky.plugin_parser;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.termux.R;

public class ChannelBannerManager {

    private final Context context;
    private final LinearLayout bannerBar;
    private final ImageView logoView;
    private final TextView numberPill;
    private final TextView nameView;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable;
    private final float density;

    private static final long AUTO_HIDE_MS = 3500;

    public ChannelBannerManager(FrameLayout root) {
        this.context = root.getContext();
        this.density = context.getResources().getDisplayMetrics().density;

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        boolean isTv = uiModeManager != null
            && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;

        
        int padH = dp(isTv ? 40 : 18);
        int padTop = dp(isTv ? 36 : 26);
        int padBottom = dp(isTv ? 28 : 16);
        int logoSize = dp(isTv ? 72 : 52);

        
        bannerBar = new LinearLayout(context);
        bannerBar.setOrientation(LinearLayout.HORIZONTAL);
        bannerBar.setGravity(Gravity.CENTER_VERTICAL);
        bannerBar.setPadding(padH, padTop, padH, padBottom);
        bannerBar.setVisibility(View.GONE);

        GradientDrawable gradientBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{
                Color.TRANSPARENT,
                Color.parseColor("#CC0A0E17"),
                Color.parseColor("#F50A0E17")
            }
        );
        bannerBar.setBackground(gradientBg);

        
        FrameLayout logoBox = new FrameLayout(context);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setShape(GradientDrawable.RECTANGLE);
        logoBg.setCornerRadius(dp(12));
        logoBg.setColor(Color.parseColor("#1E2738"));
        logoBg.setStroke(dp(1), Color.parseColor("#33FFFFFF"));
        logoBox.setBackground(logoBg);
        logoBox.setPadding(dp(6), dp(6), dp(6), dp(6));

        logoView = new ImageView(context);
        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoBox.addView(logoView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.rightMargin = dp(isTv ? 20 : 14);
        bannerBar.addView(logoBox, logoParams);

        
        LinearLayout centerGroup = new LinearLayout(context);
        centerGroup.setOrientation(LinearLayout.HORIZONTAL);
        centerGroup.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        centerParams.rightMargin = dp(16);

        numberPill = new TextView(context);
        numberPill.setTextColor(Color.parseColor("#FFD700"));
        numberPill.setTextSize(isTv ? 20 : 15);
        numberPill.setTypeface(null, Typeface.BOLD);
        numberPill.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(dp(8));
        pillBg.setColor(Color.parseColor("#26FFD700"));
        pillBg.setStroke(dp(1), Color.parseColor("#66FFD700"));
        numberPill.setBackground(pillBg);
        numberPill.setVisibility(View.GONE);

        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillParams.rightMargin = dp(12);
        centerGroup.addView(numberPill, pillParams);

        nameView = new TextView(context);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(isTv ? 26 : 19);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setMaxLines(1);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setShadowLayer(4f, 0f, 2f, Color.BLACK);
        centerGroup.addView(nameView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bannerBar.addView(centerGroup, centerParams);

        
        LinearLayout timeCol = new LinearLayout(context);
        timeCol.setOrientation(LinearLayout.VERTICAL);
        timeCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextClock clockView = new TextClock(context);
        clockView.setFormat12Hour("hh:mm a");
        clockView.setFormat24Hour("HH:mm");
        clockView.setTextColor(Color.WHITE);
        clockView.setTextSize(isTv ? 22 : 16);
        clockView.setTypeface(null, Typeface.BOLD);
        timeCol.addView(clockView);

        TextClock dateView = new TextClock(context);
        dateView.setFormat12Hour("EEE, dd MMM");
        dateView.setFormat24Hour("EEE, dd MMM");
        dateView.setTextColor(Color.parseColor("#B3FFFFFF"));
        dateView.setTextSize(isTv ? 13 : 11);
        timeCol.addView(dateView);

        bannerBar.addView(timeCol);

        
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.gravity = Gravity.BOTTOM;
        root.addView(bannerBar, barParams);

        hideRunnable = this::hide;
    }

    public void show(String channelName, String logoUrl) {
        show(channelName, logoUrl, null);
    }

    public void show(String channelName, String logoUrl, String channelNumber) {
        hideHandler.removeCallbacks(hideRunnable);
        bannerBar.animate().cancel();

        nameView.setText(channelName != null ? channelName : "Unknown Channel");

        if (channelNumber != null && !channelNumber.trim().isEmpty()) {
            numberPill.setText(channelNumber.trim());
            numberPill.setVisibility(View.VISIBLE);
        } else {
            numberPill.setVisibility(View.GONE);
        }

        if (logoUrl != null && !logoUrl.trim().isEmpty()) {
            Glide.with(context)
                .load(logoUrl)
                .placeholder(R.drawable.tx_broken_image)
                .error(R.drawable.tx_broken_image)
                .into(logoView);
        } else {
            logoView.setImageResource(R.drawable.tx_broken_image);
        }

        if (bannerBar.getVisibility() != View.VISIBLE) {
            bannerBar.setVisibility(View.VISIBLE);
            bannerBar.setAlpha(0f);
            bannerBar.setTranslationY(dp(24));
            bannerBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
        } else {
            bannerBar.setAlpha(1f);
            bannerBar.setTranslationY(0f);
        }

        hideHandler.postDelayed(hideRunnable, AUTO_HIDE_MS);
    }

    public void hide() {
        hideHandler.removeCallbacks(hideRunnable);
        if (bannerBar.getVisibility() == View.VISIBLE) {
            bannerBar.animate()
                .alpha(0f)
                .translationY(dp(18))
                .setDuration(200)
                .withEndAction(() -> bannerBar.setVisibility(View.GONE))
                .start();
        }
    }

    private int dp(int value) {
        return (int) (value * density);
    }
}
