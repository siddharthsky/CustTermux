package com.termux.tv_ui;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import com.termux.SkySharedPref;

/**
 * Handles video playback with media controls.
 */
public class PlaybackVideoFragment extends VideoSupportFragment {

    private CustomPlaybackGlue mTransportControlGlue;
    private SkySharedPref preferenceManager;
    private static final String TAG = "PlaybackVideoFragment";

    // Define scale modes
    private static final int SCALE_MODE_STRETCH = 1;
    private static final int SCALE_MODE_FIT_WIDTH = 2;
    private static final int SCALE_MODE_FIT = 3; // Default fit mode
    private int scaleMode = SCALE_MODE_FIT; // Default scale mode

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            Log.e(TAG, "Activity is null. Cannot initialize SkySharedPref.");
            return;
        }
        preferenceManager = new SkySharedPref(getActivity());

//        final Movie movie =
//                (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);

        // Retrieve playerSize preference and set scaleMode
        String playerSize = preferenceManager.getKey("playerSize");
        if (playerSize == null) {
            scaleMode = SCALE_MODE_FIT; // Default behavior when playerSize is null
        } else {
            switch (playerSize) {
                case "Fit Width":
                    scaleMode = SCALE_MODE_FIT_WIDTH;
                    break;
                case "Stretch":
                    scaleMode = SCALE_MODE_STRETCH;
                    break;
                default:
                    scaleMode = SCALE_MODE_FIT; // Default behavior for unexpected values
                    break;
            }
        }


        VideoSupportFragmentGlueHost glueHost =
                new VideoSupportFragmentGlueHost(PlaybackVideoFragment.this);

        MediaPlayerAdapter playerAdapter = new MediaPlayerAdapter(getActivity());
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE);

        mTransportControlGlue = new CustomPlaybackGlue(getActivity(), playerAdapter);
        mTransportControlGlue.setHost(glueHost);
        ////mTransportControlGlue.setTitle(movie.getTitle());
        //mTransportControlGlue.setSubtitle(movie.getDescription());

        // Update the video URL based on the quality preference and set it to the player
        ////  String newVideoUrl = updateVideoUrl(movie.getVideoUrl());
        //// playerAdapter.setDataSource(Uri.parse(newVideoUrl));

        mTransportControlGlue.playWhenPrepared();

        // Hide the controls after 3 seconds
        new Handler().postDelayed(() -> {
            PlaybackControlsRow playbackControlsRow = mTransportControlGlue.getControlsRow();
            if (playbackControlsRow != null) {
                glueHost.hideControlsOverlay(false); // Use this method to hide the controls
            }
        }, 3000); // 3 seconds
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTransportControlGlue != null) {
            mTransportControlGlue.pause();
        }
    }

    public String updateVideoUrl(String baseUrl) {
        String newVideoUrl = baseUrl;  // Default to the original URL
        try {
            String playlistQuality = preferenceManager.getKey("playerQuality");
            if (playlistQuality == null) {
                // Default behavior when playerQuality is null
                newVideoUrl = baseUrl;
            } else {
                switch (playlistQuality) {
                    case "High":
                        newVideoUrl = baseUrl.replace("/live/", "/live/high/");
                        break;
                    case "Medium":
                        newVideoUrl = baseUrl.replace("/live/", "/live/medium/");
                        break;
                    case "Low":
                        newVideoUrl = baseUrl.replace("/live/", "/live/low/");
                        break;
                    case "Auto":
                        newVideoUrl = baseUrl.replace("/live/", "/live/");
                        break;
                    default:
                        // No change needed for unexpected values
                        break;
                }
            }
            Log.d(TAG, "Updated Video URL: " + newVideoUrl);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Context error: " + e.getMessage());
        }

        return newVideoUrl;  // Return the updated URL
    }

    @Override
    protected void onVideoSizeChanged(int width, int height) {
        switch (scaleMode) {
            // `scaleMode` flag indicates that this video should stretch to screen
            case SCALE_MODE_STRETCH:
                View rootView = getView();
                SurfaceView surfaceView = getSurfaceView();
                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                params.height = rootView.getHeight();
                params.width = rootView.getWidth();
                surfaceView.setLayoutParams(params);
                break;
            // `scaleMode` flag indicates that this video should fit the width of the screen
            case SCALE_MODE_FIT_WIDTH:
//                View rootViewWidth = getView();
//                SurfaceView surfaceViewWidth = getSurfaceView();
//                ViewGroup.LayoutParams paramsWidth = surfaceViewWidth.getLayoutParams();
//                paramsWidth.width = rootViewWidth.getWidth();
//                paramsWidth.height = (int) ((float) rootViewWidth.getWidth() / width * height); // Maintain aspect ratio
//                surfaceViewWidth.setLayoutParams(paramsWidth);
                super.onVideoSizeChanged(width, height);
                break;
            // When the video shouldn't stretch or fit width, just invoke super to have the VideoFragment's default behavior which is fit to screen
            default:
                super.onVideoSizeChanged(width, height);
        }
    }
}
