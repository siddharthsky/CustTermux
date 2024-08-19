package com.termux.tv_ui;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.termux.R;
import com.termux.tv_ui.VideoItemsTV;

public class CardPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.video_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        VideoItemsTV videoItem = (VideoItemsTV) item;

        TextView title = viewHolder.view.findViewById(R.id.video_title);
        ImageView thumbnail = viewHolder.view.findViewById(R.id.video_thumbnail);

        title.setText(videoItem.getTitle());

        // Set a default image or load an image from the network
        Drawable defaultThumbnail = viewHolder.view.getContext().getDrawable(R.drawable.movie);
        thumbnail.setImageDrawable(defaultThumbnail);

        // If you want to load the image dynamically from a URL, you can use a library like Glide or Picasso
        // Glide.with(viewHolder.view.getContext()).load(videoItem.getThumbnailUrl()).into(thumbnail);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Clean up resources if necessary
    }
}
