package com.termux.app.sky;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;

import com.termux.R;

public class ButtonPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.button_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        VideoItem videoItem = (VideoItem) item;
        TextView title = viewHolder.view.findViewById(R.id.button_text);
        title.setText(videoItem.getTitle());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // no op
    }
}
