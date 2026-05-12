package com.termux.sky.tv_home_preview;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.tvprovider.media.tv.TvContractCompat;
import com.termux.sky.txplayer.ChannelModel;

public class RecentChannelsManager {

    private static final int MAX_RECENT_CHANNELS = 10;
    private static final String PREF_NAME = "tv_home_prefs_v3";
    private static final String KEY_CHANNEL_ID = "recent_channel_id";

    public static void addChannelToHome(final Context context, final ChannelModel channel) {
        new Thread(() -> {
            long channelId = getOrCreateChannel(context);
            if (channelId == -1) return;

            ContentResolver resolver = context.getContentResolver();

            Uri channelUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId);
            String[] projection = {BaseColumns._ID, TvContractCompat.PreviewPrograms.COLUMN_TITLE};

            Cursor cursor = resolver.query(channelUri, projection, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String existingTitle = cursor.getString(cursor.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms.COLUMN_TITLE));
                        if (channel.name != null && channel.name.equals(existingTitle)) {
                            long internalId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                            resolver.delete(TvContractCompat.buildPreviewProgramUri(internalId), null, null);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            String deepLink = "hanaplayer://play?" +
                "url=" + Uri.encode(channel.url) +
                "&name=" + Uri.encode(channel.name) +
                "&license=" + Uri.encode(channel.licenseKey != null ? channel.licenseKey : "") +
                "&user_agent=" + Uri.encode(channel.userAgent != null ? channel.userAgent : "");

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID, channelId);
            values.put(TvContractCompat.PreviewPrograms.COLUMN_TITLE, channel.name);
            values.put(TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI, deepLink);
            values.put(TvContractCompat.PreviewPrograms.COLUMN_TYPE, TvContractCompat.PreviewPrograms.TYPE_CHANNEL);

            values.put(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, TvContractCompat.PreviewPrograms.ASPECT_RATIO_4_3);

            values.put("weight", (int) (System.currentTimeMillis() / 1000));
            values.put("last_engagement_time_utc_millis", System.currentTimeMillis());

            values.put(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI, channel.logo);

            resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, values);
            pruneOldPrograms(context, channelId);

        }).start();
    }

    private static long getOrCreateChannel(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long id = prefs.getLong(KEY_CHANNEL_ID, -1);
        if (id == -1) {
            id = TvHomeScreenHelper.createPreviewChannel(context);
            prefs.edit().putLong(KEY_CHANNEL_ID, id).apply();
        }
        return id;
    }

    private static void pruneOldPrograms(Context context, long channelId) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId);
        Cursor cursor = resolver.query(uri, new String[]{BaseColumns._ID}, null, null, BaseColumns._ID + " DESC");

        if (cursor != null) {
            try {
                if (cursor.getCount() > MAX_RECENT_CHANNELS) {
                    cursor.moveToPosition(MAX_RECENT_CHANNELS - 1);
                    while (cursor.moveToNext()) {
                        long idToDelete = cursor.getLong(0);
                        resolver.delete(TvContractCompat.buildPreviewProgramUri(idToDelete), null, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }
}
