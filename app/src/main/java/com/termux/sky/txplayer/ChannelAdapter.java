package com.termux.sky.txplayer;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.termux.R;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    private List<ChannelModel> channels;
    private final OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(ChannelModel channel);
    }

    public ChannelAdapter(List<ChannelModel> channels, OnChannelClickListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    public void updateList(List<ChannelModel> newList) {
        this.channels = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.channels_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChannelModel channel = channels.get(position);
        holder.txtName.setText(channel.name);

        // Show Group | Language | Type
        String info = channel.group;
        if (!channel.language.isEmpty()) info += " • " + channel.language;
        if (!channel.type.isEmpty()) info += " • " + channel.type;
        holder.txtGroup.setText(info);

        Glide.with(holder.itemView.getContext())
            .load(channel.logo)
            .placeholder(R.drawable.tv_plugin_add_btn)
            .into(holder.imgLogo);

        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);
        Drawable selector = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.tv_plugin_card_bg);
        holder.itemView.setBackground(selector);

        holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel));

    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtGroup;
        ImageView imgLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.channelName);
            txtGroup = itemView.findViewById(R.id.channelGroup);
            imgLogo = itemView.findViewById(R.id.channelLogo);
        }
    }
}
