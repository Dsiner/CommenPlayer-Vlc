package com.d.lib.commenplayer.util;

import android.content.Context;
import android.os.Build;

import com.d.lib.commenplayer.listener.IRenderView;
import com.d.lib.commenplayer.media.SurfaceRenderView;
import com.d.lib.commenplayer.media.TextureRenderView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;

import java.util.Locale;

/**
 * Factory
 * Created by D on 2017/5/28.
 */
public class Factory {
    //-------------------------
    // Extend: Render
    //-------------------------
    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;

    public static IRenderView initRenders(Context context) {
        int render = RENDER_NONE;
        Settings settings = new Settings(context.getApplicationContext());

        if (settings.getEnableTextureView()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                render = RENDER_TEXTURE_VIEW;
            } else {
                render = RENDER_SURFACE_VIEW;
            }
        } else if (settings.getEnableSurfaceView()) {
            render = RENDER_SURFACE_VIEW;
        } else if (settings.getEnableNoView()) {
            render = RENDER_NONE;
        }

        switch (render) {
            case RENDER_NONE:
                return null;
            case RENDER_TEXTURE_VIEW:
                return new TextureRenderView(context);
            case RENDER_SURFACE_VIEW:
                return new SurfaceRenderView(context);
            default:
                ULog.e(String.format(Locale.getDefault(), "invalid render %d", render));
                return null;
        }
    }

    public static MediaPlayer createPlayer(Context context) {
//        ArrayList<String> options = new ArrayList<>();
//        options.add("--aout=opensles");
//        options.add("--audio-time-stretch");
//        options.add("-vvv");
        LibVLC libvlc = new LibVLC(context/*, options*/);
        MediaPlayer mediaPlayer = new MediaPlayer(libvlc);
        return mediaPlayer;
    }
}
