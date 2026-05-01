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

        // --- Create Programmatic Card Layout ---
        CardView card = new CardView(ctx);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);
        card.setRadius(24f);
        card.setCardElevation(8f);

        Drawable selector = ContextCompat.getDrawable(card.getContext(), R.drawable.tv_plugin_card_bg);
        card.setBackground(selector);

        // Enable Focus and Long Click for TV Remotes
        card.setFocusable(true);
        card.setClickable(true);
        card.setLongClickable(true);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setGravity(Gravity.CENTER);

        // FrameLayout to hold Logo and optional Favorite indicator
        FrameLayout imageContainer = new FrameLayout(ctx);
        int sizePx = (int) (100 * ctx.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            sizePx
        );
        imageContainer.setLayoutParams(containerParams);

        ImageView logo = new ImageView(ctx);
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        logo.setLayoutParams(logoParams);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(8, 8, 8, 8);

        ImageView favIcon = new ImageView(ctx);
        FrameLayout.LayoutParams favParams = new FrameLayout.LayoutParams(48, 48);
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
        name.setPadding(0, 10, 0, 0);
        name.setTextSize(14);
        name.setTextColor(Color.WHITE);
//        name.setTypeface(null, Typeface.BOLD);
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

        // Show/hide favorite star overlay
        holder.favIcon.setVisibility(channel.isFavorite ? View.VISIBLE : View.GONE);

        Glide.with(holder.itemView.getContext())
            .load(channel.logo)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .into(holder.logo);

        holder.itemView.setOnClickListener(v -> clickListener.onClick(channel));

        // Standard touch/click long listener
        holder.itemView.setOnClickListener(v -> clickListener.onClick(channel));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(channel);
            return true;
        });

        // TV Remote Key Listener for DPAD_CENTER / ENTER long press
        holder.itemView.setOnKeyListener(new View.OnKeyListener() {
            boolean isLongPressFired = false;

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getRepeatCount() == 0) {
                            isLongPressFired = false;
                            v.setPressed(true);
                            event.startTracking();
                            return true;
                        } else if (event.isLongPress() || event.getRepeatCount() >= 2) {
                            if (!isLongPressFired) {
                                longClickListener.onLongClick(channel);
                                isLongPressFired = true;
                            }
                            return true;
                        }
                        return true;
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {

                        v.setPressed(false);

                        if (!isLongPressFired) {
                            clickListener.onClick(channel);
                        }
                        isLongPressFired = false;
                        return true;
                    }
                }

                return false;
            }
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
