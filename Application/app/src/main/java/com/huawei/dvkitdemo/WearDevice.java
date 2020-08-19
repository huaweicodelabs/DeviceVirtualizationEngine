/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.dvkitdemo;

import com.huawei.dmsdp.devicevirtualization.Capability;
import com.huawei.dmsdp.devicevirtualization.VirtualDevice;

import java.util.EnumSet;
import java.util.Objects;

public class WearDevice {
    private String deviceId;

    private String deviceType;

    private String deviceName;

    private EnumSet<Capability> capabilities;

    private VirtualDevice device;

    /**
     * constructor of virtual device
     *
     * @param virtualDevice DMSDPDevice
     */
    public WearDevice(VirtualDevice virtualDevice) {
        deviceId = virtualDevice.getDeviceId();
        deviceType = virtualDevice.getDeviceType();
        deviceName = virtualDevice.getDeviceName();
        capabilities = virtualDevice.getDeviceCapability();
        device = virtualDevice;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        WearDevice that = (WearDevice) object;

        return Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId);
    }

    @Override
    public String toString() {
        return deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public EnumSet<Capability> getCapabilities() {
        return capabilities;
    }

    public VirtualDevice getVirtualDevice() {
        return device;
    }
}
