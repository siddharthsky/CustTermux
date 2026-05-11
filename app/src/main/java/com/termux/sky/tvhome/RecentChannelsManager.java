package com.termux.sky.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;
import com.termux.sky.txplayer.ChannelModel;

public class RecentChannelsManager {

    private static final int MAX_RECENT_CHANNELS = 7;
    private static final String PREF_NAME = "tv_home_prefs";
    private static final String KEY_CHANNEL_ID = "recent_channel_id";

    public static void addChannelToHome(Context context, ChannelModel channel) {
        long channelId = getOrCreateChannel(context);
        if (channelId == -1) return;

        ContentResolver resolver = context.getContentResolver();

        Uri channelUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId);
        String[] projection = {android.provider.BaseColumns._ID, TvContractCompat.PreviewPrograms.COLUMN_TITLE};

        Cursor cursor = resolver.query(channelUri, projection, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String existingTitle = cursor.getString(cursor.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms.COLUMN_TITLE));
                    if (channel.name != null && channel.name.equals(existingTitle)) {
                        long internalId = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID));
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
            "&license_type=" + Uri.encode(channel.licenseType != null ? channel.licenseType : "") +
            "&user_agent=" + Uri.encode(channel.userAgent != null ? channel.userAgent : "");

        PreviewProgram.Builder builder = new PreviewProgram.Builder();
        builder.setChannelId(channelId)
            .setTitle(channel.name)
            .setPosterArtUri(Uri.parse(channel.logo))
            .setIntentUri(Uri.parse(deepLink))
            .setType(TvContractCompat.PreviewPrograms.TYPE_CHANNEL);

        resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, builder.build().toContentValues());


        pruneOldPrograms(context, channelId);
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

        String idColumn = BaseColumns._ID;

        Cursor cursor = resolver.query(uri, new String[]{idColumn},
            null, null, idColumn + " DESC");

        if (cursor != null) {
            try {
                if (cursor.getCount() > MAX_RECENT_CHANNELS) {
                    // Move to the 7th item and delete everything older
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
