package com.termux.tv_ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;


import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.Action;

import com.termux.SkySharedPref;

public class CustomPlaybackGlue extends PlaybackTransportControlGlue<MediaPlayerAdapter> {
    private SkySharedPref preferenceManager;
    private static final String TAG = "CustomPlaybackGlue";

    private PlaybackControlsRow.HighQualityAction highQualityAction;
    private PlaybackControlsRow.ClosedCaptioningAction closedCaptioningAction;
    private PlaybackControlsRow.PictureInPictureAction resizeAction;

    private String currentVideoUrl;

    public CustomPlaybackGlue(Context context, MediaPlayerAdapter impl) {
        super(context, impl);

        // Initialize SkySharedPref instance with the provided context
        preferenceManager = new SkySharedPref(context);

        // Initialize custom actions
        closedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        resizeAction = new PlaybackControlsRow.PictureInPictureAction(context);
        highQualityAction = new PlaybackControlsRow.HighQualityAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        super.onCreatePrimaryActions(primaryActionsAdapter);
        // Add custom actions to the primary actions row
        //primaryActionsAdapter.add(highQualityAction);
        //primaryActionsAdapter.add(resizeAction); // Add ResizeAction
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        super.onCreateSecondaryActions(secondaryActionsAdapter);
        // Add custom actions to the secondary actions row
        secondaryActionsAdapter.add(highQualityAction);
       // secondaryActionsAdapter.add(closedCaptioningAction);
        secondaryActionsAdapter.add(resizeAction);
    }

    @Override
    public void onActionClicked(Action action) {
        super.onActionClicked(action);
        // Handle action clicks here
        if (action == highQualityAction) {
            showQualitySelectionDialog();
        } else if (action == resizeAction) {
            showResizeOptionsDialog(); // Handle ResizeAction click
        } else if (action == closedCaptioningAction) {
            Toast.makeText(getContext(), "Closed Captioning Toggled", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQualitySelectionDialog() {
        // Define the available quality options
        final String[] qualityOptions = {"Auto", "High", "Medium", "Low"};
        String currentQuality = preferenceManager.getKey("playerQuality");

        int checkedItem = -1;
        for (int i = 0; i < qualityOptions.length; i++) {
            if (qualityOptions[i].equalsIgnoreCase(currentQuality)) {
                checkedItem = i;
                break;
            }
        }

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Video Quality")
                .setSingleChoiceItems(qualityOptions, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedQuality = qualityOptions[which];
                        preferenceManager.setKey("playerQuality", selectedQuality);
                        Toast.makeText(getContext(), selectedQuality + " Quality Selected", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getContext(), "Replay to apply changes", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Restart the video with the selected quality
                        //updateVideoUrlBasedOnQuality(selectedQuality);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showResizeOptionsDialog() {
        // Define the available resize options
        final String[] resizeOptions = {"Fit Width", "Stretch"};
        String currentSize = preferenceManager.getKey("playerSize");

        int checkedItem = -1;
        for (int i = 0; i < resizeOptions.length; i++) {
            if (resizeOptions[i].equalsIgnoreCase(currentSize)) {
                checkedItem = i;
                break;
            }
        }

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Video Size")
                .setSingleChoiceItems(resizeOptions, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedSize = resizeOptions[which];
                        preferenceManager.setKey("playerSize", selectedSize);
                        Toast.makeText(getContext(), selectedSize + " Size Selected", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getContext(), "Replay to apply changes", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Handle resizing the video based on the selected size
                        //updateVideoSizeBasedOnSelection(selectedSize);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void updateVideoUrl(String newUrl) {
        try {
            currentVideoUrl = newUrl;
            String playlistM3u = preferenceManager.getKey("playlist_m3u");

            // Log the preference value for debugging
            Log.d(TAG, "Playlist M3U value: " + playlistM3u);

            // Handle cases where the value is null or empty
            if (playlistM3u != null && !playlistM3u.isEmpty()) {
                // Perform actions with the preference value
            } else {
                // Handle missing or empty preference value
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Context error: " + e.getMessage());
        }
    }
}
