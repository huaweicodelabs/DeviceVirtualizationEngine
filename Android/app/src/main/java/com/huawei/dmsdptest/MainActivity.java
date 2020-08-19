package com.huawei.dmsdptest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.huawei.dmsdpsdk.HwLog;
import com.huawei.dmsdpsdk.devicevirtualization.DeviceInfo;
import com.huawei.dmsdpsdk.devicevirtualization.ErrorCode;
import com.huawei.dmsdpsdk.devicevirtualization.IInitCallback;
import com.huawei.dmsdpsdk.devicevirtualization.IVirtualizationCallback;
import com.huawei.dmsdpsdk.devicevirtualization.RemoteDevice;
import com.huawei.dmsdpsdk.devicevirtualization.VirtualizationAdapter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.huawei.dmsdpsdk.devicevirtualization.DeviceEvent.CONNECT;
import static com.huawei.dmsdpsdk.devicevirtualization.DeviceEvent.DISCONNECT;
import static com.huawei.dmsdpsdk.devicevirtualization.DeviceEvent.REQUEST_CONNECTION;
import static com.huawei.dmsdpsdk.devicevirtualization.DeviceEvent.START_PROJECTION;
import static com.huawei.dmsdpsdk.devicevirtualization.DeviceEvent.STOP_PROJECTION;
import static com.huawei.dmsdptest.Constants.ACTION_CLOSE_SURFACE_ACTIVITY;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEVICE_NAME = "bluetooth_name";
    private AtomicBoolean mDisplayService = new AtomicBoolean(false);
    private VirtualizationAdapter mVirtualizationAdapter;
    private TextView mCode;

    private Context mContext;
    private Button mStartButton;
    private Button mGetRemoteDeviceButton;
    RemoteDeviceDialogAdapter adapter = null;
    AlertDialog mAlertDialog = null;

    private IVirtualizationCallback mVirtualizationCallback = new IVirtualizationCallback() {
        @Override
        public void onDeviceChange(String remoteDevice, int state) {
            if (state == CONNECT) {
                HwLog.i(TAG, "device has connected");
                AdapterUtil.getInstance().setConnectedDevice(remoteDevice);
            } else if (state == DISCONNECT) {
                HwLog.i(TAG, "device has disconnected");
                AdapterUtil.getInstance().setConnectedDevice(null);
                mDisplayService.set(false);
                //设备断开连接，需销毁显示投屏画面的Surface
                MainActivity.this.sendBroadcast(new Intent(ACTION_CLOSE_SURFACE_ACTIVITY));
            } else if (state == START_PROJECTION) {
                HwLog.i(TAG, "service all ready,start project");
                mDisplayService.set(true);
                //创建Surface，并通过startProjection接口传递给DV服务，用来显示投屏画面
                Intent intent = new Intent(MainActivity.this, SurfaceActivity.class);
                MainActivity.this.startActivity(intent);
            } else if (state == STOP_PROJECTION) {
                HwLog.i(TAG, "stopProjection");
                mDisplayService.set(false);
                //投屏能力停止，也需销毁显示投屏画面的Surface
                MainActivity.this.sendBroadcast(new Intent(ACTION_CLOSE_SURFACE_ACTIVITY));
            } else if (state == REQUEST_CONNECTION) {
                View dialogView = View.inflate(mContext, R.layout.dialog_permission, null);
                final AlertDialog dialog = new AlertDialog.Builder(mContext).setView(dialogView).create();

                TextView titleTextView = dialogView.findViewById(R.id.title_id);
                TextView informationTextView = dialogView.findViewById(R.id.information_id);
                Button refuseButton = dialogView.findViewById(R.id.button_prohibition);
                Button permanentButton = dialogView.findViewById(R.id.button_permission_permanent);
                Button temporaryButton = dialogView.findViewById(R.id.button_permission_temporary);
                temporaryButton.requestFocus();

                titleTextView.setText("是否允许“" + remoteDevice + "”访问本设备？");
                informationTextView.setText("使用中可能会调用麦克风、摄像头用于音视频协同等服务。");

                refuseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mVirtualizationAdapter != null) {
                            int result = mVirtualizationAdapter.disconnectDevice();
                            HwLog.i(TAG, "disconnect device result:" + result);
                            dialog.dismiss();
                        }
                    }
                });

                permanentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int result = mVirtualizationAdapter.acceptConnection(true);
                        HwLog.i(TAG, "permanent permit result:" + result);
                        dialog.dismiss();
                    }
                });

                temporaryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int result = mVirtualizationAdapter.acceptConnection(false);
                        HwLog.i(TAG, "temporary permit result:" + result);
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        }

        @Override
        public void onPinCode(String deviceName, final String pinCode) {
            HwLog.i(TAG, "pinCode:" + pinCode);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!TextUtils.isEmpty(pinCode)) {
                        //显示pinCode
                        mCode.setText(pinCode);
                    } else {
                        //如果pinCode是空串，不显示pinCode
                        mCode.setText("");
                    }
                }
            });
        }

        @Override
        public long getSecureFileSize(String fileName) {
            HwLog.i(TAG, "readSecureFile fileName:" + fileName);
            try (FileInputStream fileInputStream = openFileInput(fileName)) {
                FileChannel channel = fileInputStream.getChannel();
                return channel.size();
            } catch (IOException e) {
                HwLog.e(TAG, "getSecureFileSize failed");
            }
            return 0;
        }

        @Override
        public byte[] readSecureFile(String fileName) {
            HwLog.i(TAG, "readSecureFile fileName:" + fileName);
            try (FileInputStream fileInputStream = openFileInput(fileName)) {
                int canRead = fileInputStream.available();
                byte[] buffer = new byte[canRead];
                int read = fileInputStream.read(buffer);
                if (read != canRead) {
                    HwLog.e(TAG, "read file size failed");
                } else {
                    HwLog.i(TAG, "read file success");
                    return buffer;
                }
            } catch (IOException e) {
                HwLog.e(TAG, "read file failed");
            }
            return new byte[0];
        }

        @Override
        public boolean writeSecureFile(String fileName, byte[] bytes) {
            HwLog.i(TAG, "writeSecureFile fileName:" + fileName);
            try (FileOutputStream fileOutputStream = openFileOutput(fileName, MODE_PRIVATE)) {
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
            } catch (IOException e) {
                HwLog.e(TAG, "write file exception," + e);
                return false;
            }

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mContext = this;

        setContentView(R.layout.activity_start_project);
        mStartButton = findViewById(R.id.button_start);
        mGetRemoteDeviceButton = findViewById(R.id.button_get_remote_device);
        mGetRemoteDeviceButton.setEnabled(true);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HwLog.d(TAG, "start project");
                mStartButton.setEnabled(false);
                initMSDPService();
            }
        });

        Button stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HwLog.d(TAG, "stop project");
                mStartButton.setEnabled(true);
                disconnect();
            }
        });

        Button mUpdateDeviceNameButton = findViewById(R.id.button_update_device_name);
        mUpdateDeviceNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HwLog.d(TAG, "update device name");
                if (mVirtualizationAdapter == null) {
                    HwLog.i(TAG, "mVirtualizationAdapter is null");
                    return;
                }
                //更改本地设备名字
                int updateResult = mVirtualizationAdapter.updateDeviceName(getDeviceName());
                if (updateResult != ErrorCode.SUCCESS) {
                    HwLog.i(TAG, "update device name failed");
                    return;
                }
                if (!mStartButton.isEnabled()) {
                    //已经启动adv的情况下，要先关掉adv，再启动adv
                    int stopResult = mVirtualizationAdapter.stopAdv();
                    HwLog.i(TAG, "stop listen result:" + stopResult);
                    int startResult = mVirtualizationAdapter.startAdv();
                    HwLog.i(TAG, "start listen result:" + startResult);
                }
            }
        });
        mGetRemoteDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVirtualizationAdapter == null) {
                    HwLog.d(TAG, "mVirtualizationAdapter is null");
                    return;
                }

                List<RemoteDevice> remoteDevices = new ArrayList<>();
                int getDevicesResult = mVirtualizationAdapter.getTrustDeviceList(remoteDevices);
                HwLog.i(TAG, "size:" + remoteDevices.size() + ",getDevicesResult:" + getDevicesResult);
                for (RemoteDevice device : remoteDevices) {
                    HwLog.i(TAG, "device id:" + device.getDeviceId() + ",device name:" + device.getDeviceName());
                }

                HwLog.i(TAG, "mGetRemoteDeviceButton mVirtualizationAdapter:" + mVirtualizationAdapter);
                View dialogView = View.inflate(mContext, R.layout.dialog_remote_device, null);
                ListView remoteDeviceListView = dialogView.findViewById(R.id.dialog_remote_device);
                adapter = new RemoteDeviceDialogAdapter(mContext, remoteDevices);
                remoteDeviceListView.setAdapter(adapter);

                remoteDeviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setMessage("确定删除?");
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setTitle("提示");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                if (mVirtualizationAdapter == null) {
                                    return;
                                }
                                mVirtualizationAdapter.deleteTrustDevice(adapter.getRemoteDevices().get(position).getDeviceId());
                                mAlertDialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        AlertDialog subDialog = builder.create();
                        Window window = subDialog.getWindow();
                        window.setWindowAnimations(R.style.NoAnimationDialog);
                        subDialog.show();
                        return false;
                    }
                });

                final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setTitle(getString(R.string.all_trust_devices) + "(" + remoteDevices.size() + ")");
                builder.setView(dialogView);
                builder.setCancelable(true);
                builder.setNegativeButton(getString(R.string.back), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();

                mAlertDialog = builder.create();
                Window window = mAlertDialog.getWindow();
                window.setWindowAnimations(R.style.NoAnimationDialog);
                mAlertDialog.show();
            }
        });
        mCode = findViewById(R.id.pin_code);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HwLog.i(TAG, "onResume");
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        HwLog.i(TAG, "onPause");
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //全部释放干净
    private void disconnect() {
        if (mVirtualizationAdapter != null) {
            //停止投屏
            if (mDisplayService.get()) {
                int stopStatus = mVirtualizationAdapter.stopProjection();
                HwLog.d(TAG, "disconnect stopProjection stopStatus:" + stopStatus);
                mDisplayService.set(false);
            }
            //停止连接监听
            int disconnectDeviceStatus = mVirtualizationAdapter.disconnectDevice();
            HwLog.i(TAG, "disconnect disconnectDevice status:" + disconnectDeviceStatus);
            //停止发现监听
            int result = mVirtualizationAdapter.stopAdv();
            HwLog.i(TAG, "disconnect stop listen result:" + result);
        }
        //释放adapter对象
        VirtualizationAdapter.releaseInstance();
        mVirtualizationAdapter = null;
    }

    @Override
    protected void onDestroy() {
        HwLog.i(TAG, "onDestroy");
        disconnect();
        HwLog.i(TAG, "onDestroy end");
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void initMSDPService() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceName(getDeviceName());
        deviceInfo.setDeviceType(DeviceInfo.DEVICE_TYPE_TV);
        HwLog.d(TAG, "initMSDPService info:" + deviceInfo);
        int initInstance = VirtualizationAdapter.initInstance(getApplicationContext(), deviceInfo, new IInitCallback() {
            @Override
            public void onInitSuccess(VirtualizationAdapter virtualizationAdapter) {
                HwLog.i(TAG, "onInitSuccess");
                mVirtualizationAdapter = virtualizationAdapter;
                AdapterUtil.getInstance().setVirtualizationAdapter(mVirtualizationAdapter);

                int status = mVirtualizationAdapter.setVirtualizationCallback(mVirtualizationCallback);
                HwLog.i(TAG, "setVirtualizationCallback status:" + status);
                int result = mVirtualizationAdapter.startAdv();
                HwLog.i(TAG, "start listen result:" + result);
            }

            @Override
            public void onInitFail(int code) {
                HwLog.i(TAG, "onInitFail: " + code);
                disconnect();
            }

            @Override
            public void onBinderDied() {
                HwLog.e(TAG, "onBinderDied service died!");
                //复位 可能存在并发
                AdapterUtil.getInstance().setConnectedDevice(null);
                mVirtualizationAdapter = null;
                if (mDisplayService.get()) {
                    HwLog.i(TAG, "close surface");
                    //发送广播，关闭SurfaceActivity
                    MainActivity.this.sendBroadcast(new Intent(ACTION_CLOSE_SURFACE_ACTIVITY));
                    mDisplayService.set(false);
                }
            }
        });
        HwLog.i(TAG, "initInstance result:" + initInstance);
    }

    private String getDeviceName() {
        String result = android.os.Build.BRAND;
        String deviceName = Settings.Secure.getString(mContext.getContentResolver(), DEVICE_NAME);
        if (deviceName == null || deviceName.length() == 0) {
            HwLog.i(TAG, "getDeviceName deviceName is null");
            return result;
        }
        HwLog.i(TAG, "deviceName:" + deviceName);
        return deviceName;
    }
}
