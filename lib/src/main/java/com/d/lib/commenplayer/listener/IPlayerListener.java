package com.d.lib.commenplayer.listener;

import org.videolan.libvlc.MediaPlayer;

/**
 * IPlayerListener
 * Created by D on 2017/5/28.
 */
public interface IPlayerListener {

    void onLoading();

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     */
    void onCompletion(MediaPlayer mp);

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     */
    void onPrepared(MediaPlayer mp);

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     */
    boolean onError(MediaPlayer mp, int what, int extra);

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     */
    boolean onInfo(MediaPlayer mp, int what, int extra);

    void onVideoSizeChanged(MediaPlayer mp, int width, int height, int sarNum, int sarDen);
}
