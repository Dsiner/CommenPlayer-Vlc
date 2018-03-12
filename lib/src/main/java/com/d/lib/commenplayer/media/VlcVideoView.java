package com.d.lib.commenplayer.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.d.lib.commenplayer.listener.IPlayerListener;
import com.d.lib.commenplayer.listener.IRenderView;
import com.d.lib.commenplayer.services.MediaPlayerService;
import com.d.lib.commenplayer.util.Factory;
import com.d.lib.commenplayer.util.Settings;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.MediaPlayer;

import java.util.Map;

public class VlcVideoView extends FrameLayout implements MediaController.MediaPlayerControl, IPlayerListener {
    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            IRenderView.AR_MATCH_PARENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};

    private Activity mActivity;
    private Uri uri;
    private Map<String, String> headers;
    private MediaPlayer mediaPlayer = null;

    private IRenderView renderView;

    //-------------------------
    // Extend: Aspect Ratio
    //-------------------------
    private int currentAspectRatioIndex = 0;
    private int currentAspectRatio = s_allAspectRatio[0];

    private boolean isLive = false;// is Live mode
    private boolean isPause = false;
    private int pausePos;
    private IPlayerListener listener;
    private boolean playerSupport;

    public VlcVideoView(Context context) {
        super(context);
        init(context);
    }

    public VlcVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VlcVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mActivity = (Activity) context;
        initBackground();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    private void initBackground() {
        boolean enable = new Settings(mActivity.getApplicationContext()).getEnableBackgroundPlay();
        if (enable) {
            MediaPlayerService.intentToStart(getContext());
            MediaPlayerService.getMediaManager(mActivity);
        } else {
            getManager();
        }
    }

    private MediaManager getManager() {
        return MediaManager.instance(mActivity);
    }

    public IRenderView getRenderView() {
        return renderView;
    }

    private void addRenderView() {
        IRenderView renderView = Factory.initRenders(mActivity);
        if (this.renderView != null) {
            if (mediaPlayer != null) {
                mediaPlayer.getVLCVout().detachViews();
            }
            removeAllViews();
            this.renderView = null;
        }
        if (renderView == null) {
            return;
        }
        if (renderView instanceof TextureView) {
            initTextureView((TextureView) renderView);
        } else if (renderView instanceof SurfaceView) {
            initSurfaceView((SurfaceView) renderView);
        }
        this.renderView = renderView;
        View view = this.renderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        view.setLayoutParams(lp);
        addView(view);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void initTextureView(TextureView textureView) {
        IVLCVout ivlcVout = mediaPlayer.getVLCVout();
        ivlcVout.setVideoView(textureView);
//        mediaPlayer.getVLCVout().setVideoSurface(textureView.getSurfaceTexture());
        ivlcVout.attachViews();
    }

    private void initSurfaceView(final SurfaceView surfaceView) {
        final IVLCVout ivlcVout = mediaPlayer.getVLCVout();
        ivlcVout.setVideoView(surfaceView);
//        mediaPlayer.getVLCVout().setVideoSurface(surfaceView.getHolder().getSurface(), surfaceView.getHolder());
        ivlcVout.attachViews();

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mediaPlayer == null) {
                    return;
                }
                mediaPlayer.getVLCVout().setWindowSize(width, height);
//                mediaPlayer.setAspectRatio("16:9");
                mediaPlayer.setScale(0);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        this.uri = uri;
        this.headers = headers;
        prepare();
    }

    private void prepare() {
        if (uri == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        mediaPlayer = getManager().prepare(mActivity, uri, headers, false);
        getManager().setListener(this);
        addRenderView();
    }

    /**
     * release the media player in any state
     */
    public void release(boolean clearTargetState) {
        getManager().release(mActivity.getApplicationContext(), clearTargetState);
        mediaPlayer = null;
    }

    @Override
    public void start() {
        getManager().start();
    }

    @Override
    public void pause() {
        getManager().pause();
    }

    @Override
    public int getDuration() {
        return getManager().getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return getManager().getCurrentPosition();
    }

    @Override
    public void seekTo(int msec) {
        getManager().seekTo(msec);
    }

    @Override
    public boolean isPlaying() {
        return getManager().isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return getManager().getBufferPercentage();
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

    public void setLive(boolean live) {
        isLive = live;
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void play(String url, int pos) {
        setVideoPath(url);
        seekTo(pos);
        start();
        onLoading();
    }

    public void onResume() {
        if (isPause) {
            isPause = false;
            prepare();
            seekTo(isLive ? 0 : pausePos);
            start();
            onLoading();
        }
    }

    public void onPause() {
        isPause = true;
        if (isLive) {
            release(false);
        } else {
            pausePos = getCurrentPosition();
            getManager().pause();
        }
    }

    public void onDestroy() {
        release(true);
    }

    public void setScaleType(int scaleType) {
        if (renderView != null) {
            for (int index : s_allAspectRatio) {
                if (index == scaleType) {
                    currentAspectRatioIndex = index;
                }
            }
            renderView.setAspectRatio(scaleType);
        }
    }

    public int toggleAspectRatio() {
        currentAspectRatioIndex++;
        currentAspectRatioIndex %= s_allAspectRatio.length;
        currentAspectRatio = s_allAspectRatio[currentAspectRatioIndex];
        if (renderView != null) {
            renderView.setAspectRatio(currentAspectRatio);
        }
        return currentAspectRatio;
    }

    public void setOnPlayerListener(IPlayerListener iPlayerListener) {
        listener = iPlayerListener;
    }

    @Override
    public void onLoading() {
        if (listener != null) {
            listener.onLoading();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (listener != null) {
            listener.onCompletion(mp);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (listener != null) {
            listener.onPrepared(mp);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return listener == null || listener.onError(mp, what, extra);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return listener == null || listener.onInfo(mp, what, extra);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height, int sarNum, int sarDen) {
        if (listener != null) {
            listener.onVideoSizeChanged(mp, width, height, sarNum, sarDen);
        }
    }
}
