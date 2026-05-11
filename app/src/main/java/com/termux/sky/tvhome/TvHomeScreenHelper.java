package com.termux.sky.tvhome;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.tvprovider.media.tv.TvContractCompat;
import java.io.OutputStream;

import com.termux.R;
// Import your main activity
import com.termux.sky.hanaplayer.HanaPlayerActivity;

public class TvHomeScreenHelper {

    public static long createPreviewChannel(Context context) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(TvContractCompat.Channels.COLUMN_DISPLAY_NAME, "Currently Playing");
        values.put(TvContractCompat.Channels.COLUMN_DESCRIPTION, "Recently watched channels");

        Intent appLaunchIntent = new Intent(context, HanaPlayerActivity.class);
        appLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String intentUriString = appLaunchIntent.toUri(Intent.URI_INTENT_SCHEME);

        values.put(TvContractCompat.Channels.COLUMN_APP_LINK_INTENT_URI, intentUriString);


        values.put(TvContractCompat.Channels.COLUMN_TYPE, TvContractCompat.Channels.TYPE_PREVIEW);

        Uri channelUri = context.getContentResolver().insert(
            TvContractCompat.Channels.CONTENT_URI,
            values
        );

        long channelId = ContentUris.parseId(channelUri);

        writeChannelLogo(context, channelId);

        TvContractCompat.requestChannelBrowsable(context, channelId);

        return channelId;
    }

    private static void writeChannelLogo(Context context, long channelId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);

            if (drawable == null) {
                Log.e("TV_HOME", "Could not find logo resource.");
                return;
            }

            Bitmap bitmap = getBitmapFromDrawable(drawable);

            Uri logoUri = TvContractCompat.buildChannelLogoUri(channelId);
            OutputStream outputStream = context.getContentResolver().openOutputStream(logoUri);

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e("TV_HOME", "Failed to write channel logo", e);
        }
    }

    private static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 256;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 256;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
