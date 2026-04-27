package com.termux.sky.txplayer;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.termux.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChannelBannerManager {
    private final View bannerView;
    private final TextView tvName, tvTime;
    private final ImageView imgLogo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable;

    public ChannelBannerManager(ViewGroup parent) {
        bannerView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.layout_channel_overlay, parent, false);
        parent.addView(bannerView);

        tvName = bannerView.findViewById(R.id.overlay_channel_name);
        tvTime = bannerView.findViewById(R.id.overlay_time);
        imgLogo = bannerView.findViewById(R.id.overlay_logo);

        bannerView.setVisibility(View.GONE);
        hideRunnable = () -> bannerView.setVisibility(View.GONE);
    }

    public void show(String channelName, String logoURL) {
        handler.removeCallbacks(hideRunnable);

        tvName.setText(channelName);
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvTime.setText(currentTime);

        if (logoURL != null && !logoURL.isEmpty()) {
            Glide.with(bannerView.getContext())
                .load(logoURL)
                .placeholder(R.drawable.tv_plugin_add_btn)
                .error(R.drawable.tv_plugin_add_btn)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgLogo);
        } else {
            imgLogo.setImageResource(R.drawable.tv_plugin_add_btn);
        }

        bannerView.setVisibility(View.VISIBLE);
        handler.postDelayed(hideRunnable, 2000);
    }
}
