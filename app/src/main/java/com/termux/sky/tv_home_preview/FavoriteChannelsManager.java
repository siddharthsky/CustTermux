package com.termux.sky.tv_home_preview;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.termux.R;
import com.termux.sky.txplayer.ChannelModel;

import java.io.OutputStream;
import java.util.List;

public class FavoriteChannelsManager {

    private static final String PREF_NAME = "tv_home_prefs_v3";
    private static final String KEY_FAV_CHANNEL_ID = "favorite_channel_id";

    public static void syncFavoritesToHome(final Context context, final List<ChannelModel> allChannels) {
        new Thread(() -> {
            try {
                long channelId = getOrCreateFavoriteChannelRow(context);
                if (channelId == -1) return;

                ContentResolver resolver = context.getContentResolver();
                Uri programsUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId);

                resolver.delete(programsUri, null, null);

                int addedCount = 0;
                for (ChannelModel channel : allChannels) {
                    if (channel.isFavorite) {

                        String deepLink = "hanaplayer://play?" +
                            "url=" + Uri.encode(channel.url) +
                            "&name=" + Uri.encode(channel.name) +
                            "&license=" + Uri.encode(channel.licenseKey != null ? channel.licenseKey : "") +
                            "&user_agent=" + Uri.encode(channel.userAgent != null ? channel.userAgent : "") +
                            "&playlist_type=favorites";

                        ContentValues values = new ContentValues();
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID, channelId);
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_TITLE, channel.name);
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI, deepLink);
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_TYPE, TvContractCompat.PreviewPrograms.TYPE_CHANNEL);
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, TvContractCompat.PreviewPrograms.ASPECT_RATIO_4_3);
                        values.put(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI, channel.logo);

                        values.put("weight", allChannels.size() - addedCount);

                        resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, values);
                        addedCount++;
                    }
                }
            } catch (Exception e) {
                Log.e("TV_FAVORITES", "Error sync favorite channel row", e);
            }
        }).start();
    }

    private static long getOrCreateFavoriteChannelRow(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long id = prefs.getLong(KEY_FAV_CHANNEL_ID, -1);

        if (id == -1) {
            if (context.getPackageManager().resolveContentProvider(TvContractCompat.AUTHORITY, 0) == null) {
                return -1;
            }
            try {
                ContentValues values = new ContentValues();
                values.put(TvContractCompat.Channels.COLUMN_DISPLAY_NAME, "Favorite Channels");
                values.put(TvContractCompat.Channels.COLUMN_DESCRIPTION, "Your top choices");
                values.put(TvContractCompat.Channels.COLUMN_TYPE, TvContractCompat.Channels.TYPE_PREVIEW);

                Uri channelUri = context.getContentResolver().insert(TvContractCompat.Channels.CONTENT_URI, values);
                if (channelUri != null) {
                    id = ContentUris.parseId(channelUri);
                    prefs.edit().putLong(KEY_FAV_CHANNEL_ID, id).apply();
                    writeChannelLogo(context, id);
                    TvContractCompat.requestChannelBrowsable(context, id);
                }
            } catch (Exception e) {
                Log.e("TV_FAVORITES", "Failed to create preview favorite channel row", e);
            }
        }
        return id;
    }

    private static void writeChannelLogo(Context context, long channelId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
            if (drawable == null) return;

            int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 256;
            int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 256;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            Uri logoUri = TvContractCompat.buildChannelLogoUri(channelId);
            OutputStream outputStream = context.getContentResolver().openOutputStream(logoUri);
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e("TV_FAVORITES", "Logo error", e);
        }
    }
}
