package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.termux.R;
import com.termux.sky.plugin_parser.ChannelModel;

import java.util.List;

public class HanaChannelAdapter extends RecyclerView.Adapter<HanaChannelAdapter.VH> {

    private final OnChannelClickListener clickListener;
    private final OnChannelLongClickListener longClickListener;
    private List<ChannelModel> list;
    private int movingPosition = -1;

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

    public void setMovingPosition(int position) {
        int old = this.movingPosition;
        this.movingPosition = position;
        if (old != -1) notifyItemChanged(old);
        if (position != -1) notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;


        int marginPx = (int) (8 * density);
        int cardHeightPx = (int) (110 * density);
        int iconSizePx = (int) (24 * density);
        int paddingPx = (int) (10 * density);


        CardView card = new CardView(ctx);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            cardHeightPx
        );
        params.setMargins(marginPx, marginPx, marginPx, marginPx);
        card.setLayoutParams(params);
        card.setRadius(12f * density);
        card.setCardElevation(4f * density);
        card.setCardBackgroundColor(Color.parseColor("#1C2436"));
        card.setFocusable(true);
        card.setClickable(true);
        card.setLongClickable(true);


        FrameLayout rootFrame = new FrameLayout(ctx);
        rootFrame.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));


        ImageView logo = new ImageView(ctx);
        logo.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(paddingPx, paddingPx, paddingPx, (int) (32 * density));
        rootFrame.addView(logo);


        View gradientView = new View(ctx);
        FrameLayout.LayoutParams gradientParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (55 * density)
        );
        gradientParams.gravity = Gravity.BOTTOM;
        gradientView.setLayoutParams(gradientParams);

        GradientDrawable shadow = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.TRANSPARENT, Color.parseColor("#E6000000")}
        );
        gradientView.setBackground(shadow);
        rootFrame.addView(gradientView);


        TextView name = new TextView(ctx);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.BOTTOM;
        textParams.setMargins(paddingPx, 0, paddingPx, (int) (8 * density));
        name.setLayoutParams(textParams);
        name.setTextSize(14);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setMaxLines(1);
        rootFrame.addView(name);


        ImageView favIcon = new ImageView(ctx);
        FrameLayout.LayoutParams favParams = new FrameLayout.LayoutParams(iconSizePx, iconSizePx);
        favParams.gravity = Gravity.TOP | Gravity.END;
        favParams.topMargin = (int) (6 * density);
        favParams.rightMargin = (int) (6 * density);
        favIcon.setLayoutParams(favParams);
        favIcon.setImageResource(R.drawable.tx_star);
        favIcon.setColorFilter(Color.parseColor("#FFD700"));
        favIcon.setAlpha(0.95f);


        GradientDrawable starBg = new GradientDrawable();
        starBg.setShape(GradientDrawable.OVAL);
        starBg.setColor(Color.parseColor("#80000000"));
        favIcon.setBackground(starBg);
        favIcon.setPadding((int) (4 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
        favIcon.setVisibility(View.GONE);
        rootFrame.addView(favIcon);


        View focusBorder = new View(ctx);
        focusBorder.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setShape(GradientDrawable.RECTANGLE);
        borderDrawable.setCornerRadius(12f * density);
        borderDrawable.setStroke((int) (3 * density), Color.parseColor("#FFD700"));
        borderDrawable.setColor(Color.TRANSPARENT);
        focusBorder.setBackground(borderDrawable);
        focusBorder.setAlpha(0f);
        rootFrame.addView(focusBorder);

        card.addView(rootFrame);

        return new VH(card, logo, name, favIcon, focusBorder);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChannelModel channel = list.get(position);
        holder.name.setText(channel.name);
        holder.favIcon.setVisibility(channel.isFavorite ? View.VISIBLE : View.GONE);


        holder.itemView.animate().cancel();
        holder.focusBorder.animate().cancel();


        if (position == movingPosition) {
            holder.itemView.setAlpha(0.7f);
            holder.itemView.setScaleX(0.92f);
            holder.itemView.setScaleY(0.92f);
            holder.focusBorder.setAlpha(1f);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
            holder.focusBorder.setAlpha(0f);
        }

        Glide.with(holder.itemView.getContext())
            .load(channel.logo)
            .placeholder(R.drawable.tx_broken_image)
            .dontAnimate()
            .into(holder.logo);


        holder.itemView.setOnClickListener(v -> clickListener.onClick(channel));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(channel);
            return true;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (position == movingPosition) return;

            float targetScale = hasFocus ? 1.08f : 1.0f;
            float targetElevation = hasFocus ? 12f : 4f;
            float targetBorderAlpha = hasFocus ? 1f : 0f;

            v.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(200)
                .start();

            ((CardView) v).setCardElevation(targetElevation * v.getContext().getResources().getDisplayMetrics().density);

            holder.focusBorder.animate()
                .alpha(targetBorderAlpha)
                .setDuration(200)
                .start();
        });
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.itemView.getContext()).clear(holder.logo);
        holder.itemView.animate().cancel();
        holder.focusBorder.animate().cancel();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnChannelClickListener {
        void onClick(ChannelModel channel);
    }

    public interface OnChannelLongClickListener {
        void onLongClick(ChannelModel channel);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name;
        ImageView favIcon;
        View focusBorder;

        VH(View v, ImageView logo, TextView name, ImageView favIcon, View focusBorder) {
            super(v);
            this.logo = logo;
            this.name = name;
            this.favIcon = favIcon;
            this.focusBorder = focusBorder;
        }
    }
}
