package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.termux.R;
import com.termux.sky.txplayer.ChannelModel;

import java.util.List;

public class HanaChannelAdapter extends RecyclerView.Adapter<HanaChannelAdapter.VH> {

    private List<ChannelModel> list;
    private final OnChannelClickListener clickListener;
    private final OnChannelLongClickListener longClickListener;

    public interface OnChannelClickListener { void onClick(ChannelModel channel); }
    public interface OnChannelLongClickListener { void onLongClick(ChannelModel channel); }

    public HanaChannelAdapter(List<ChannelModel> list, OnChannelClickListener click, OnChannelLongClickListener longClick) {
        this.list = list;
        this.clickListener = click;
        this.longClickListener = longClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<ChannelModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();

        float density = ctx.getResources().getDisplayMetrics().density;

        int marginPx = (int) (8 * density);      // Outer card margin
        int paddingPx = (int) (12 * density);    // Inner card padding
        int imgHeightPx = (int) (80 * density);  // Image container height
        int logoPaddingPx = (int) (4 * density); // Padding around the channel logo
        int favIconSizePx = (int) (24 * density); // Favorite star icon size
        int textTopPaddingPx = (int) (8 * density); // Gap between logo and text

        // --- Create Programmatic Card Layout ---
        CardView card = new CardView(ctx);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(marginPx, marginPx, marginPx, marginPx);
        card.setLayoutParams(params);

        card.setRadius(16f * density);
        card.setCardElevation(4f * density);

        Drawable selector = ContextCompat.getDrawable(card.getContext(), R.drawable.tv_plugin_card_bg);
        card.setBackground(selector);

        card.setFocusable(true);
        card.setClickable(true);
        card.setLongClickable(true);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        layout.setGravity(Gravity.CENTER);

        FrameLayout imageContainer = new FrameLayout(ctx);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            imgHeightPx
        );
        imageContainer.setLayoutParams(containerParams);

        ImageView logo = new ImageView(ctx);
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        logo.setLayoutParams(logoParams);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(logoPaddingPx, logoPaddingPx, logoPaddingPx, logoPaddingPx);

        ImageView favIcon = new ImageView(ctx);
        FrameLayout.LayoutParams favParams = new FrameLayout.LayoutParams(favIconSizePx, favIconSizePx);
        favParams.gravity = Gravity.TOP | Gravity.END;
        favIcon.setLayoutParams(favParams);
        favIcon.setImageResource(R.drawable.tx_star);
        favIcon.setColorFilter(Color.parseColor("#FFD700"));
        favIcon.setAlpha(0.9f);
        favIcon.setVisibility(View.GONE);

        imageContainer.addView(logo);
        imageContainer.addView(favIcon);

        TextView name = new TextView(ctx);
        name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        name.setPadding(0, textTopPaddingPx, 0, 0);
        name.setTextSize(14);
        name.setTextColor(Color.WHITE);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setMaxLines(1);

        layout.addView(imageContainer);
        layout.addView(name);
        card.addView(layout);

        return new VH(card, logo, name, favIcon);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChannelModel channel = list.get(position);
        holder.name.setText(channel.name);

        holder.favIcon.setVisibility(channel.isFavorite ? View.VISIBLE : View.GONE);

        Glide.with(holder.itemView.getContext())
            .load(channel.logo)
            .placeholder(R.drawable.tx_broken_image)
            .into(holder.logo);

        // Standard Click Listener
        // Works for both touch and TV Remote "Center/Enter"
        holder.itemView.setOnClickListener(v -> clickListener.onClick(channel));

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(channel);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name;
        ImageView favIcon;
        VH(View v, ImageView logo, TextView name, ImageView favIcon) {
            super(v);
            this.logo = logo;
            this.name = name;
            this.favIcon = favIcon;
        }
    }
}
