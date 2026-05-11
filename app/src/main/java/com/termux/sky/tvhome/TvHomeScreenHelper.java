package com.termux.sky.tvhome;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import androidx.tvprovider.media.tv.PreviewChannel;
import androidx.tvprovider.media.tv.TvContractCompat;

public class TvHomeScreenHelper {

    public static long createPreviewChannel(Context context) {
        PreviewChannel.Builder builder = new PreviewChannel.Builder();
        builder.setDisplayName("Currently Playing")
            .setDescription("Live channels")
            .setAppLinkIntentUri(Uri.parse("myapp://home"));

        PreviewChannel channel = builder.build();


        Uri channelUri = context.getContentResolver().insert(
            TvContractCompat.Channels.CONTENT_URI,
            channel.toContentValues()
        );

        long channelId = ContentUris.parseId(channelUri);
        
        TvContractCompat.requestChannelBrowsable(context, channelId);

        return channelId;
    }
}
