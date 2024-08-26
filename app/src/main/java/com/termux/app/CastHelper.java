package com.termux.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.mediarouter.app.MediaRouteButton;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class CastHelper {

    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private CastContext mCastContext;
    private Context mContext;

    public CastHelper(Context context) {
        mContext = context;
        mCastContext = CastContext.getSharedInstance(mContext);
        initSessionManagerListener();
    }

    public void setupMediaRouteButton(MediaRouteButton mediaRouteButton) {
        CastButtonFactory.setUpMediaRouteButton(mContext, mediaRouteButton);
    }

    public CastSession getCastSession() {
        return mCastSession;
    }

    private void initSessionManagerListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                mCastSession = session;
            }

            @Override
            public void onSessionStarting(@NonNull CastSession castSession) {

            }

            @Override
            public void onSessionSuspended(@NonNull CastSession castSession, int i) {

            }

            @Override
            public void onSessionEnded(CastSession session, int error) {
                mCastSession = null;
            }

            @Override
            public void onSessionEnding(@NonNull CastSession castSession) {

            }

            @Override
            public void onSessionResumeFailed(@NonNull CastSession castSession, int i) {

            }

            @Override
            public void onSessionResumed(@NonNull CastSession castSession, boolean b) {

            }

            @Override
            public void onSessionResuming(@NonNull CastSession castSession, @NonNull String s) {

            }

            @Override
            public void onSessionStartFailed(@NonNull CastSession castSession, int i) {

            }

            // Other overrides as needed...
        };
    }

    public void addSessionManagerListener() {
        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);
    }

    public void removeSessionManagerListener() {
        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);
    }

    public void loadMedia(CastSession castSession, String mediaUrl, String title) {
        if (castSession != null && castSession.getRemoteMediaClient() != null) {
            RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

            MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, title);

            MediaInfo mediaInfo = new MediaInfo.Builder(mediaUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/mp4")
                .setMetadata(mediaMetadata)
                .build();

            remoteMediaClient.load(mediaInfo, true, 0);
        }
    }
}
