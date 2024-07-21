package com.termux;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.termux.app.sky.ButtonPresenter;
import com.termux.app.sky.VideoItem;

public class MainFragment extends BrowseSupportFragment {

    private String videoUrl1 = "http://localhost:5001/player/571";
    private String videoUrl2 = "http://localhost:5001/player/235";
    private String videoUrl3 = "http://localhost:5001/player/286";

    private WebView webView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitle("Video Player");

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        HeaderItem header = new HeaderItem(0, "Videos");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new ButtonPresenter());

        listRowAdapter.add(new VideoItem("Play Video 1", videoUrl1));
        listRowAdapter.add(new VideoItem("Play Video 2", videoUrl2));
        listRowAdapter.add(new VideoItem("Play Video 3", videoUrl3));

        rowsAdapter.add(new ListRow(header, listRowAdapter));
        setAdapter(rowsAdapter);

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder viewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof VideoItem) {

//                String urlToPlay = "http://localhost:5001/player/153";
//                //Create an intent to start WebPlayerActivity
//                Intent intent = new Intent(getActivity(), WebPlayerActivity.class);
//                intent.putExtra("play_url", urlToPlay);
                VideoItem videoItem = (VideoItem) item;
                Intent intent = new Intent(getActivity(), WebPlayerActivity.class);
                intent.putExtra("play_url", Uri.parse(videoItem.getUrl()));
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoItem.getUrl()));
//                intent.setDataAndType(Uri.parse(videoItem.getUrl()), "video/*");
                startActivity(intent);
            }
        }
    }
}
