package com.huawei.dmsdptest;

import com.huawei.dmsdpsdk.devicevirtualization.VirtualizationAdapter;

public class AdapterUtil {
    private VirtualizationAdapter mVirtualizationAdapter;
    private volatile boolean mSurfaceViewDone = false;
    private volatile String mConnectedDevice;
    private volatile boolean mDisplayService = false;
    private static AdapterUtil instance = new AdapterUtil();

    public static synchronized AdapterUtil getInstance() {
        return instance;
    }

    public boolean isSurfaceViewDone() {
        return mSurfaceViewDone;
    }

    public void setSurfaceViewDone(boolean surfaceViewDone) {
        this.mSurfaceViewDone = surfaceViewDone;
    }

    public String getConnectedDevice() {
        return mConnectedDevice;
    }

    public void setConnectedDevice(String connectedDevice) {
        this.mConnectedDevice = connectedDevice;
    }

    public boolean isDisplayService() {
        return mDisplayService;
    }

    public void setDisplayService(boolean displayService) {
        this.mDisplayService = displayService;
    }

    public VirtualizationAdapter getVirtualizationAdapter() {
        return mVirtualizationAdapter;
    }

    public void setVirtualizationAdapter(VirtualizationAdapter virtualizationAdapter) {
        this.mVirtualizationAdapter = virtualizationAdapter;
    }
}
