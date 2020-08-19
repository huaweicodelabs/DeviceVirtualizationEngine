package com.huawei.dmsdptest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.huawei.dmsdpsdk.HwLog;

import static com.huawei.dmsdptest.Constants.ACTION_CLOSE_SURFACE_ACTIVITY;

public class SurfaceActivity extends Activity {
    private static final String TAG = SurfaceActivity.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private CloseActivityReceiver mCloseActivityReceiver = new CloseActivityReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_surface);
        mSurfaceView = findViewById(R.id.surface);
        createSurfaceLayout();
        AdapterUtil.getInstance().setDisplayService(true);
        HwLog.i(TAG, "surface:" + this);
        IntentFilter intentFilter = new IntentFilter(ACTION_CLOSE_SURFACE_ACTIVITY);
        registerReceiver(mCloseActivityReceiver, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        HwLog.i(TAG, "onResume");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Surface surface = mSurfaceView.getHolder().getSurface();
        if (!surface.isValid()) {
            HwLog.i(TAG, "start project surface is not valid");
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        HwLog.i(TAG, "onPause");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (AdapterUtil.getInstance().getVirtualizationAdapter() != null) {
            HwLog.i(TAG, "onPause adapter is not null,disconnectDevice");
            AdapterUtil.getInstance().getVirtualizationAdapter().disconnectDevice();
        }
    }

    @Override
    protected void onDestroy() {
        HwLog.i(TAG, "onDestroy");
        if (mSurfaceView != null) {
            mSurfaceView.destroyDrawingCache();
        }
        mSurfaceView = null;
        unregisterReceiver(mCloseActivityReceiver);
        AdapterUtil.getInstance().setDisplayService(false);
        AdapterUtil.getInstance().setSurfaceViewDone(false);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void createSurfaceLayout() {
        if (mSurfaceView.getHolder() != null
                && mSurfaceView.getHolder().getSurface() != null
                && mSurfaceView.getHolder().getSurface().isValid()) {
            HwLog.i(TAG, "surface is ok");
            AdapterUtil.getInstance().setSurfaceViewDone(true);
        } else {
            HwLog.i(TAG, "surface holder or surface is null or invalid");
        }

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                HwLog.i(TAG, "surface view created");
                if (!AdapterUtil.getInstance().isSurfaceViewDone()) {
                    AdapterUtil.getInstance().setSurfaceViewDone(true);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                HwLog.i(TAG, "surface view changed");
                if (AdapterUtil.getInstance().getConnectedDevice() != null && AdapterUtil.getInstance().getVirtualizationAdapter() != null) {
                    HwLog.i(TAG, "start projection");
                    if (AdapterUtil.getInstance().isDisplayService()) {
                        HwLog.i(TAG, "start project,view width:" + mSurfaceView.getWidth() + ",height:" + mSurfaceView.getHeight());
                        int status = AdapterUtil.getInstance().getVirtualizationAdapter().startProjection(mSurfaceView.getHolder().getSurface(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
                        HwLog.i(TAG, "startProjection status:" + status);
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                HwLog.i(TAG, "surface view destroyed");
            }
        });
    }

    class CloseActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            HwLog.i(TAG, "receiver Broadcast");
            SurfaceActivity.this.finish();
        }
    }
}

