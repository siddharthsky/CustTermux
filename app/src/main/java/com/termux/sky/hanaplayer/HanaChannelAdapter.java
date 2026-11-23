package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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

    private boolean isFavoritesTab = false;
    private boolean showFavNumbers = false;
    private String favStartNumberStr = "001";

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
        if (old != -1 && old < list.size()) notifyItemChanged(old);
        if (position != -1 && position < list.size()) notifyItemChanged(position);
    }

    public void updateMovingPositionOnly(int position) {
        this.movingPosition = position;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFavNumberConfig(boolean isFavoritesTab, boolean showNumbers, String startNumberStr) {
        this.isFavoritesTab = isFavoritesTab;
        this.showFavNumbers = showNumbers;
        this.favStartNumberStr = (startNumberStr == null || startNumberStr.trim().isEmpty()) ? "001" : startNumberStr.trim();
        notifyDataSetChanged();
    }

    private String formatChannelNumber(int position) {
        try {
            int padLength = favStartNumberStr.length();
            int startNum = Integer.parseInt(favStartNumberStr);
            return String.format(java.util.Locale.US, "%0" + padLength + "d", startNum + position);
        } catch (NumberFormatException e) {
            return String.format(java.util.Locale.US, "%03d", 1 + position);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();

        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean isCatchupEnabled = prefs.getBoolean("5006_catchup", false);

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

        ImageView catchupIcon = new ImageView(ctx);
        FrameLayout.LayoutParams catchupParams = new FrameLayout.LayoutParams(iconSizePx, iconSizePx);
        catchupParams.gravity = Gravity.TOP | Gravity.START; 
        catchupParams.topMargin = (int) (6 * density);
        catchupParams.leftMargin = (int) (6 * density);
        catchupIcon.setLayoutParams(catchupParams);
        catchupIcon.setImageResource(R.drawable.tx_catch); 
        catchupIcon.setColorFilter(Color.parseColor("#00E5FF")); 
        catchupIcon.setAlpha(0.95f);
        GradientDrawable catchupBg = new GradientDrawable();
        catchupBg.setShape(GradientDrawable.OVAL);
        catchupBg.setColor(Color.parseColor("#80000000"));
        catchupIcon.setBackground(catchupBg);
        catchupIcon.setPadding((int) (4 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
        catchupIcon.setVisibility(View.GONE);
        rootFrame.addView(catchupIcon);

        TextView numberBadge = new TextView(ctx);
        FrameLayout.LayoutParams numParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numParams.gravity = Gravity.TOP | Gravity.START;
        numParams.topMargin = (int) (6 * density);
        numParams.leftMargin = (int) (6 * density);
        numberBadge.setLayoutParams(numParams);
        numberBadge.setTextSize(11);
        numberBadge.setTextColor(Color.parseColor("#FFD700"));
        numberBadge.setTypeface(null, Typeface.BOLD);
        numberBadge.setPadding((int) (6 * density), (int) (2 * density), (int) (6 * density), (int) (2 * density));

        GradientDrawable numBg = new GradientDrawable();
        numBg.setShape(GradientDrawable.RECTANGLE);
        numBg.setCornerRadius(6f * density);
        numBg.setColor(Color.parseColor("#B3000000"));
        numberBadge.setBackground(numBg);
        numberBadge.setVisibility(View.GONE);
        rootFrame.addView(numberBadge);

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

        return new VH(card, logo, name, favIcon, catchupIcon, numberBadge, focusBorder, isCatchupEnabled);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChannelModel channel = list.get(position);
        holder.name.setText(channel.name);
        holder.favIcon.setVisibility(channel.isFavorite ? View.VISIBLE : View.GONE);

        holder.itemView.animate().cancel();
        holder.focusBorder.animate().cancel();

        
        boolean isCatchupChannel = false;
        String activePort = channel.originPort;
        if (activePort == null || activePort.trim().isEmpty()) {
            activePort = channel.url != null && channel.url.contains("5007") ? "5007" : "0";
        }

        if ("5006".equals(activePort)) {
            if (holder.isCatchupEnabled) {
                isCatchupChannel = ChannelDataManager.isCatchupAvailable(holder.itemView.getContext(), channel.id);
            }
        }


        holder.catchupIcon.setVisibility(isCatchupChannel ? View.VISIBLE : View.GONE);

        boolean shouldShowNumber = isFavoritesTab && showFavNumbers;
        if (shouldShowNumber) {
            holder.numberBadge.setText(formatChannelNumber(position));
            holder.numberBadge.setVisibility(View.VISIBLE);
        } else {
            holder.numberBadge.setVisibility(View.GONE);
        }

        // Prevent catchupIcon from overlapping numberBadge when both are visible
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams catchupLp = (FrameLayout.LayoutParams) holder.catchupIcon.getLayoutParams();
        catchupLp.topMargin = (shouldShowNumber && isCatchupChannel) ? (int) (28 * density) : (int) (6 * density);
        holder.catchupIcon.setLayoutParams(catchupLp);
        

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

        
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < list.size()) {
                clickListener.onClick(list.get(pos));
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < list.size()) {
                longClickListener.onLongClick(list.get(pos));
            }
            return true;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos == movingPosition) return;

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
        ImageView catchupIcon;
        TextView numberBadge;
        View focusBorder;
        boolean isCatchupEnabled;

        VH(View v, ImageView logo, TextView name, ImageView favIcon, ImageView catchupIcon, TextView numberBadge, View focusBorder, boolean isCatchupEnabled) {
            super(v);
            this.logo = logo;
            this.name = name;
            this.favIcon = favIcon;
            this.catchupIcon = catchupIcon;
            this.numberBadge = numberBadge;
            this.focusBorder = focusBorder;
            this.isCatchupEnabled = isCatchupEnabled;
        }
    }
}
