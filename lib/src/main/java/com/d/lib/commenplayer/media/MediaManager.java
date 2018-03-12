package com.d.lib.commenplayer.media;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.MediaController;

import com.d.lib.commenplayer.listener.IPlayerListener;
import com.d.lib.commenplayer.util.Factory;
import com.d.lib.commenplayer.util.ULog;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.Map;

public class MediaManager implements MediaController.MediaPlayerControl, IPlayerListener {
    // All possible internal states
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    private static MediaManager mManager;
    private Handler handler;
    private MediaPlayer mediaPlayer;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    public int currentState = STATE_IDLE;
    public int targetState = STATE_IDLE;
    public int seekWhenPrepared;  // Recording the seek position while preparing
    private int currentBufferPercentage;
    private IPlayerListener listener;

    public void setListener(IPlayerListener listener) {
        this.listener = listener;
    }

    public static MediaManager instance(Context context) {
        if (mManager == null) {
            synchronized (MediaManager.class) {
                mManager = new MediaManager(context.getApplicationContext());
            }
        }
        return mManager;
    }

    private MediaManager(Context context) {
        handler = new Handler(Looper.getMainLooper());
    }

    public MediaPlayer prepare(Context context, final Uri uri, final Map<String, String> heads, boolean looping) {
        if (uri == null) {
            return null;
        }
        currentState = STATE_PREPARING;
        currentBufferPercentage = 0;
        seekWhenPrepared = 0;
        // We shouldn't clear the target state, because somebody might have
        // called start() previously
        release(context, false);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // AudioManager.AUDIOFOCUS_GAIN / AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        try {
            mediaPlayer = Factory.createPlayer(context);
            if (mediaPlayer == null) {
                return null;
            }
            mediaPlayer.setMedia(new Media(mediaPlayer.getLibVLC(), uri));
            mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    switch (event.type) {
                        case MediaPlayer.Event.Buffering:
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.pause();
                            }
                            if (event.getBuffering() >= 100.0f) {
                                onPrepared(mediaPlayer);
                                ULog.d("onEvent: buffer success...");
                                mediaPlayer.play();
                            } else {
                                onLoading();
                                ULog.d("缓冲: " + Math.floor(event.getBuffering()) + "%");
                            }
                            break;

                        case MediaPlayer.Event.Opening:
                            ULog.d("onEvent: opening...");
                            onLoading();
                            break;

                        case MediaPlayer.Event.Playing:
                            ULog.d("onEvent: playing...");
                            break;

                        case MediaPlayer.Event.EndReached:
                            ULog.d("onEvent: completion...");
                            onCompletion(mediaPlayer);
                            break;

                        case MediaPlayer.Event.EncounteredError:
                            ULog.d("onEvent: error...");
                            mediaPlayer.stop();
                            onError(mediaPlayer, -1, -1);
                            break;
                    }
                }
            });
            mediaPlayer.play();
            return mediaPlayer;
        } catch (Exception e) {
            ULog.w("Unable to open content: " + uri + e);
            onError(mediaPlayer, -1, 0);
            e.printStackTrace();
            return null;
        }
    }

    public void release(Context context, boolean clearTargetState) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.getVLCVout().detachViews();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentState = STATE_IDLE;
        if (clearTargetState) {
            targetState = STATE_IDLE;
        }
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        currentState = STATE_PREPARED;
        // mSeekWhenPrepared may be changed after seekTo() call
        if (seekWhenPrepared != 0) {
            seekTo(seekWhenPrepared);
        }
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onPrepared(mp);
                }
            });
        }
    }

    @Override
    public void onLoading() {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onLoading();
                }
            });
        }
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        currentState = STATE_PLAYBACK_COMPLETED;
        targetState = STATE_PLAYBACK_COMPLETED;
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onCompletion(mp);
                }
            });
        }
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        ULog.d("Error: " + what + "," + extra);
        currentState = STATE_ERROR;
        targetState = STATE_ERROR;
        /* If an error handler has been supplied, use it and finish. */
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener == null) {
                        return;
                    }
                    listener.onError(mp, what, extra);
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener == null) {
                        return;
                    }
                    listener.onInfo(mp, what, extra);
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onVideoSizeChanged(final MediaPlayer mp, final int width, final int height, final int sarNum, final int sarDen) {
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener == null) {
                        return;
                    }
                    listener.onVideoSizeChanged(mp, width, height, sarNum, sarDen);
                }
            });
        }
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mediaPlayer.play();
            currentState = STATE_PLAYING;
        }
        targetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            mediaPlayer.pause();
            currentState = STATE_PAUSED;
        }
        targetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mediaPlayer.getLength();
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mediaPlayer.getTime();
        }
        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mediaPlayer.setTime(msec);
            seekWhenPrepared = 0;
        } else {
            seekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return currentBufferPercentage;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mediaPlayer != null
                && currentState != STATE_ERROR
                && currentState != STATE_IDLE
                && currentState != STATE_PREPARING);
    }
}
