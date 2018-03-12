package com.d.lib.commenplayer.util;

import android.content.Context;

/**
 * Preset configuration
 */
public class Settings {
    public Settings(Context context) {
    }

    public boolean getEnableBackgroundPlay() {
        return false;
    }

    public boolean getEnableNoView() {
        return false;
    }

    public boolean getEnableSurfaceView() {
        return true;
    }

    public boolean getEnableTextureView() {
        return false;
    }
}
