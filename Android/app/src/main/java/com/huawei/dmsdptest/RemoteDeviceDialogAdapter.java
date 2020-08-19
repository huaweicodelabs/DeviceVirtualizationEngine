package com.huawei.dmsdptest;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.huawei.dmsdpsdk.HwLog;
import com.huawei.dmsdpsdk.devicevirtualization.RemoteDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述
 *
 * @author z00408817
 * @since 2019-11-04
 */
public class RemoteDeviceDialogAdapter extends BaseAdapter {
    private static final String TAG = "RemoteDeviceDialogAdapt";
    private Context mContext;

    public List<RemoteDevice> getRemoteDevices() {
        return mRemoteDevices;
    }

    private List<RemoteDevice> mRemoteDevices = new ArrayList<>();

    public RemoteDeviceDialogAdapter(Context mContext, List<RemoteDevice> deviceList) {
        this.mRemoteDevices.clear();
        this.mRemoteDevices.addAll(deviceList);
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mRemoteDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.dialog_remote_device_item, null, false);
            holder = new ViewHolder();
            //holder.deviceId = convertView.findViewById(R.id.device_id);
            holder.deviceName = convertView.findViewById(R.id.device_name);
            //holder.deleteDevice = convertView.findViewById(R.id.device_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final String deviceId = mRemoteDevices.get(position).getDeviceId();
        //holder.deviceId.setText(deviceId);
        String deviceName = mRemoteDevices.get(position).getDeviceName();
        HwLog.i(TAG, "device name:" + deviceName + ",device id:" + deviceId);
        if (TextUtils.isEmpty(deviceName)) {
            HwLog.i(TAG, "device name is empty");
        }
        holder.deviceName.setText(deviceName);

        return convertView;
    }

    static class ViewHolder {
        //TextView deviceId;
        TextView deviceName;
        //Button deleteDevice;
    }
}
