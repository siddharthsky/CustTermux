package com.termux.tv_ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.termux.R;

import com.termux.WebPlayerActivity;
import com.termux.tv_ui.CardPresenter;
import com.termux.tv_ui.VideoItemsTV;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MainFragmentTV extends BrowseSupportFragment {

    private String videoUrl1 = "http://localhost:5001/player/571";
    private String videoUrl2 = "http://localhost:5001/player/235";
    private String videoUrl3 = "http://localhost:5001/player/286";
    private String videoUrl4 = "http://localhost:5001/player/123";
    private String videoUrl5 = "http://localhost:5001/player/456";
    private String videoUrl6 = "http://localhost:5001/player/789";

    private WebView webView;

    private Drawable mDefaultBackground;

    private BackgroundManager mBackgroundManager;
    private DisplayMetrics mMetrics;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitle("Video Player");

        prepareBackgroundManager();

        // Initialize BackgroundManager
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        // Set the background
        String mBackgroundUri = "https://i.imgur.com/LK51VdT.png";
        updateBackground(mBackgroundUri);

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // First category "Videos"
        HeaderItem header = new HeaderItem(0, "Videos");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.add(new VideoItemsTV("Play Video 1", videoUrl1));
        listRowAdapter.add(new VideoItemsTV("Play Video 2", videoUrl2));
        listRowAdapter.add(new VideoItemsTV("Play Video 3", videoUrl3));
        rowsAdapter.add(new ListRow(header, listRowAdapter));

        // Second category "Videoss"
        HeaderItem header2 = new HeaderItem(1, "Videoss");
        ArrayObjectAdapter listRowAdapter2 = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter2.add(new VideoItemsTV("Play Video 4", videoUrl4));
        listRowAdapter2.add(new VideoItemsTV("Play Video 5", videoUrl5));
        listRowAdapter2.add(new VideoItemsTV("Play Video 6", videoUrl6));
        rowsAdapter.add(new ListRow(header2, listRowAdapter2));

        // Third category "v1"
        HeaderItem headerV1 = new HeaderItem(2, "v1");
        ArrayObjectAdapter listRowAdapterV1 = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapterV1.add(new VideoItemsTV("Play v1 Video 1", "http://localhost:5001/player/v1_1"));
        listRowAdapterV1.add(new VideoItemsTV("Play v1 Video 2", "http://localhost:5001/player/v1_2"));
        listRowAdapterV1.add(new VideoItemsTV("Play v1 Video 3", "http://localhost:5001/player/v1_3"));
        rowsAdapter.add(new ListRow(headerV1, listRowAdapterV1));

        // Fourth category "v4"
        HeaderItem headerV4 = new HeaderItem(3, "v4");
        ArrayObjectAdapter listRowAdapterV4 = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapterV4.add(new VideoItemsTV("Play v4 Video 1", "http://localhost:5001/player/v4_1"));
        listRowAdapterV4.add(new VideoItemsTV("Play v4 Video 2", "http://localhost:5001/player/v4_2"));
        listRowAdapterV4.add(new VideoItemsTV("Play v4 Video 3", "http://localhost:5001/player/v4_3"));
        rowsAdapter.add(new ListRow(headerV4, listRowAdapterV4));

        // Fifth category "mon 2"
        HeaderItem headerMon2 = new HeaderItem(4, "mon 2");
        ArrayObjectAdapter listRowAdapterMon2 = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapterMon2.add(new VideoItemsTV("Play mon 2 Video 1", "http://localhost:5001/player/mon2_1"));
        listRowAdapterMon2.add(new VideoItemsTV("Play mon 2 Video 2", "http://localhost:5001/player/mon2_2"));
        listRowAdapterMon2.add(new VideoItemsTV("Play mon 2 Video 3", "http://localhost:5001/player/mon2_3"));
        rowsAdapter.add(new ListRow(headerMon2, listRowAdapterMon2));

        setAdapter(rowsAdapter);

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void setBackground(String uri) {
        Glide.with(requireActivity())
            .load(uri)
            .into(new SimpleTarget<Drawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                @Override
                public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                    mBackgroundManager.setDrawable(resource);
                }
            });
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(requireActivity());
        mBackgroundManager.attach(requireActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into(new SimpleTarget<Drawable>(width, height) {
                @Override
                public void onResourceReady(@NonNull Drawable drawable,
                                            @Nullable Transition<? super Drawable> transition) {
                    mBackgroundManager.setDrawable(drawable);
                }
            });
    }

    private class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder viewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof VideoItemsTV) {
                VideoItemsTV VideoItemsTV = (VideoItemsTV) item;
                Intent intent = new Intent(getActivity(), WebPlayerActivity.class);
                intent.putExtra("play_url", Uri.parse(VideoItemsTV.getUrl()));
                startActivity(intent);
            }
        }
    }
}
