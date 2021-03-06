package com.d.commenplayer.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.d.commenplayer.R;
import com.d.commenplayer.netstate.NetBus;
import com.d.commenplayer.netstate.NetCompat;
import com.d.commenplayer.netstate.NetState;
import com.d.lib.commenplayer.CommenPlayer;
import com.d.lib.commenplayer.listener.IPlayerListener;
import com.d.lib.commenplayer.listener.OnNetListener;
import com.d.lib.commenplayer.ui.ControlLayout;
import com.d.lib.commenplayer.util.ULog;
import com.d.lib.commenplayer.util.Util;

import org.videolan.libvlc.MediaPlayer;

public class SimpleActivity extends Activity implements NetBus.OnNetListener {
    private CommenPlayer player;
    private boolean ignoreNet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        NetBus.getIns().addListener(this);
        initView();
        initPlayer();
    }

    private void initView() {
        player = (CommenPlayer) findViewById(R.id.player);
        findViewById(R.id.btn_view_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SimpleActivity.this, ListActivity.class));
            }
        });
    }

    private void initPlayer() {
        player.setLive(false);
        player.setOnNetListener(new OnNetListener() {
            @Override
            public void onIgnoreMobileNet() {
                ignoreNet = true;
            }
        }).setOnPlayerListener(new IPlayerListener() {
            @Override
            public void onLoading() {
                player.getControl().setState(ControlLayout.STATE_LOADING);
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                player.getControl().setState(ControlLayout.STATE_COMPLETION);
            }

            @Override
            public void onPrepared(MediaPlayer mp) {
                if (!ignoreNet && NetCompat.getStatus() == NetState.CONNECTED_MOBILE) {
                    player.pause();
                    player.getControl().setState(ControlLayout.STATE_MOBILE_NET);
                } else {
                    player.getControl().setState(ControlLayout.STATE_PREPARED);
                }
            }

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                player.getControl().setState(ControlLayout.STATE_ERROR);
                return false;
            }

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                return false;
            }

            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height, int sarNum, int sarDen) {

            }
        });
        player.play(getResources().getString(R.string.url1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (player != null) {
            player.onPause();
        }
        super.onPause();
    }

    @Override
    public void onNetChange(int state) {
        if (isFinishing()) {
            return;
        }
        ULog.d("dsiner: Network state--> " + state);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.LayoutParams lp = player.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            player.setLayoutParams(lp);
        } else {
            lp.height = Util.dip2px(getApplicationContext(), 180);
            player.setLayoutParams(lp);
        }
        if (player != null) {
            player.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.onBackPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        if (player != null) {
            player.onDestroy();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        NetBus.getIns().removeListener(this);
        super.onDestroy();
    }
}
