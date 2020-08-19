/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.dvkitdemo;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.huawei.dmsdp.devicevirtualization.DvKit.VIRTUAL_DEVICE_CLASS;
import static com.huawei.dmsdp.devicevirtualization.DvKit.VIRTUAL_SENSOR_SERVICE;
import static com.huawei.dmsdp.devicevirtualization.DvKit.VIRTUAL_VIBRATE_SERVICE;
import static com.huawei.dmsdp.devicevirtualization.EventType.EVENT_DEVICE_CAPABILITY_BUSY;
import static com.huawei.dmsdp.devicevirtualization.ObserverEventType.VIRTUALDEVICE;
import static com.huawei.dmsdp.devicevirtualization.ReturnCode.ERROR_CODE_CAN_NOT_DISABLE;
import static com.huawei.dmsdp.devicevirtualization.ReturnCode.SUCCESS;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.dmsdp.devicevirtualization.Capability;
import com.huawei.dmsdp.devicevirtualization.Constants;
import com.huawei.dmsdp.devicevirtualization.DvKit;
import com.huawei.dmsdp.devicevirtualization.EventType;
import com.huawei.dmsdp.devicevirtualization.IDiscoveryCallback;
import com.huawei.dmsdp.devicevirtualization.IDvKitConnectCallback;
import com.huawei.dmsdp.devicevirtualization.IVirtualDeviceObserver;
import com.huawei.dmsdp.devicevirtualization.IVirtualSensorDataListener;
import com.huawei.dmsdp.devicevirtualization.SensorAgent;
import com.huawei.dmsdp.devicevirtualization.VibratorService;
import com.huawei.dmsdp.devicevirtualization.VirtualDevice;
import com.huawei.dmsdp.devicevirtualization.VirtualDeviceManager;
import com.huawei.dmsdp.devicevirtualization.VirtualSensor;
import com.huawei.dmsdp.devicevirtualization.VirtualSensorData;
import com.huawei.dmsdp.devicevirtualization.VirtualVibrator;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HiWearActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "HiWearActivity";

    private static final String HEART_RATE = "Heart Rate";

    private static final String ACCELEROMETER = "Accelerometer";

    private static final String GYROSCOPE = "Gyroscope";

    private static final String PRESSURE = "Pressure";

    private static final String FAST = "Fast";

    private static final String NORMAL = "Normal";

    private static final String SLOW = "Slow";

    private static final int LOG_LINES = 8;

    private static final int PRESSURE_UNIT = 1000;

    private static final int INDEX_HEART_RATE = 0;

    private static final int INDEX_ACCELEROMETER = 1;

    private static final int INDEX_GYROSCOPE = 2;

    private static final int INDEX_PRESSURE = 3;

    private static final String[] LINE_DESCRIPTIONS = {HEART_RATE, ACCELEROMETER, GYROSCOPE, PRESSURE};

    private static final int[] LINE_COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};

    private static Map<Integer, Integer> sensorTypeMap = new HashMap<>();

    private ArrayList<WearDevice> virtualDevices = new ArrayList<>();

    private ArrayList<WearSensor> virtualSenors = new ArrayList<>();

    private ArrayAdapter<WearDevice> spinnerAdapter;

    private ArrayAdapter<WearSensor> spinnerSenorsAdapter;

    private ArrayAdapter<String> spinnerSensorTypeAdapter;

    private ArrayAdapter<String> spinnerRateAdapter;

    private Button mGetSensorList;

    private Spinner mSensorIdList;

    private Button mVibrateRepeat;

    private RadioButton mNotifyService;

    private Button mNotify;

    private LinearLayout mNotifyLl;

    private LinearLayout mGetSensorListLl;

    private LinearLayout mSensorTypeListLl;

    private TextView mCurrentStatus;

    private LinkedList<String> logs;

    private VirtualDeviceManager mVirtualDeviceManager;

    private Button mConnectDmsdp;

    private Button mDisconnectDmsdp;

    private RadioButton mSensorService;

    private RadioButton mVibrateService;

    private RadioGroup mServicesRg;

    private Button mFindWear;

    private Button mStopFindWear;

    private Button mSubscribe;

    private Button mUnsubscribe;

    private Spinner mDeviceList;

    private Spinner mSensorTypeList;

    private Spinner mRateList;

    private Button mConnectWear;

    private Button mDisconnectWear;

    private Button mRegisterDataListener;

    private Button mUnregisterDataListener;

    private LinearLayout mSensorLl;

    private Button mVibrate;

    private Button mCancelVibrate;

    private LinearLayout mVibrateLl;

    private LinearLayout mWearData;

    private LineChart mSensorChart;

    private LinearLayout mEnableLl;

    private long firstTimestamp = -1;

    private DvKit mDvKit;

    private boolean isSubscribed = false;

    private IVirtualDeviceObserver observer = new IVirtualDeviceObserver() {
        @Override
        public void onDeviceStateChange(final VirtualDevice virtualDevice, int i) {
            if (virtualDevice == null) {
                Log.e(TAG, "onDeviceStateChange device is null");
                return;
            }
            Log.d(TAG, "onDeviceStateChange: " + virtualDevice.getDeviceName() + " stat " + i);
            updateLogText("I onDeviceStateChange  " + virtualDevice.getDeviceName() + " stat " + i);
            if (i == EventType.EVENT_DEVICE_DISCONNECT) {
                updateLogText("I device " + virtualDevice.getDeviceName() + " disconnect");
                WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();

                WearDevice device = new WearDevice(virtualDevice);
                if (device.equals(selectedItem)) {
                    ubSubscribe();
                    virtualDevices.remove(device);
                    virtualSenors.clear();
                    isSubscribed = false;
                    // update
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast
                                .makeText(HiWearActivity.this,
                                    "device " + virtualDevice.getDeviceName() + " disconnect", Toast.LENGTH_LONG)
                                .show();
                            spinnerAdapter.notifyDataSetChanged();
                            spinnerSenorsAdapter.notifyDataSetChanged();
                            LineData lineData = mSensorChart.getLineData();
                            lineData.clearValues();
                            mSensorChart.notifyDataSetChanged();
                            mSensorChart.invalidate();
                        }
                    });
                } else {
                    virtualDevices.remove(device);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast
                                .makeText(HiWearActivity.this,
                                    "device " + virtualDevice.getDeviceName() + " " + "disconnect", Toast.LENGTH_LONG)
                                .show();
                            spinnerAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }

        @Override
        public void onDeviceCapabilityStateChange(VirtualDevice virtualDevice, Capability capability, int i) {
            Log.d(TAG, "CapabilityStateChange: " + capability + virtualDevice + i);
            if (virtualDevice == null) {
                updateLogText("I CapabilityStateChange: virtualDevice == null ");
            } else {
                updateLogText("I CapabilityStateChange  " + virtualDevice.getDeviceName() + " " + capability + " " + i);
                if (i == EVENT_DEVICE_CAPABILITY_BUSY) {
                    Toast.makeText(HiWearActivity.this,
                        "device" + virtualDevice.getDeviceName() + capability + "capability busy", Toast.LENGTH_SHORT)
                        .show();
                }
            }
        }
    };

    private IDiscoveryCallback discoveryCallback = new IDiscoveryCallback() {
        @Override
        public void onFound(VirtualDevice virtualDevice, int i) {
            updateLogText("I onFound device: " + virtualDevice.getDeviceName());
            Log.d(TAG, "onFound: " + virtualDevice.getDeviceId() + virtualDevice.getDeviceName() + " i " + i);
            addNewDevices(virtualDevice);
        }

        @Override
        public void onState(int i) {
            Log.d(TAG, "onState: i " + i);
        }
    };

    private void addNewDevices(VirtualDevice virtualDevice) {
        WearDevice device = new WearDevice(virtualDevice);
        if (!virtualDevices.contains(device)) {
            virtualDevices.add(device);

            // update
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinnerAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public HiWearActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hi_wear);
        initView();
        logs = new LinkedList<>();
        initDMSDP();
        initDevicesSpinner();
        initSensorsSpinner();
        initSensorTypeSpinner();
        initRateSpinner();
        initChart();
    }

    private void initChart() {
        mSensorChart.setData(new LineData());
        mSensorChart.getLineData().setValueTextColor(Color.BLACK);
        mSensorChart.getDescription().setEnabled(false);
        mSensorChart.getLegend().setEnabled(true);
        mSensorChart.getLegend().setTextColor(Color.BLACK);
        XAxis xAxis = mSensorChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);

        YAxis leftAxis = mSensorChart.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = mSensorChart.getAxisRight();
        rightAxis.setTextColor(Color.BLACK);
        rightAxis.setAxisMaximum(120f);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setDrawGridLines(true);
    }

    private static LineDataSet createLineDataSet(String description, int color) {
        LineDataSet set = new LineDataSet(null, description);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setColor(color);
        set.setCircleColor(color);
        set.setDrawCircles(true);
        set.setDrawCircleHole(true);
        set.setLineWidth(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.WHITE);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawHighlightIndicators(true);
        set.setDrawIcons(false);
        set.setDrawHorizontalHighlightIndicator(false);
        set.setDrawFilled(false);
        return set;
    }

    private static void addPoint(LineData data, int dataSetIndex, float x, float y) {
        int index;
        if (sensorTypeMap.containsKey(dataSetIndex)) {
            index = sensorTypeMap.get(dataSetIndex);
        } else {
            ILineDataSet set = createLineDataSet(LINE_DESCRIPTIONS[dataSetIndex], LINE_COLORS[dataSetIndex]);
            data.addDataSet(set);
            sensorTypeMap.put(dataSetIndex, data.getDataSets().size() - 1);
            index = data.getDataSets().size() - 1;
        }

        data.addEntry(new Entry(x, y), index);
        data.notifyDataChanged();
    }

    private void initDevicesSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, virtualDevices);
        mDeviceList.setAdapter(spinnerAdapter);
    }

    private void initSensorsSpinner() {
        spinnerSenorsAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, virtualSenors);
        mSensorIdList.setAdapter(spinnerSenorsAdapter);
    }

    private void initSensorTypeSpinner() {
        List<String> sensorTypeList = new ArrayList<>();
        sensorTypeList.add(HEART_RATE);
        sensorTypeList.add(ACCELEROMETER);
        sensorTypeList.add(GYROSCOPE);
        sensorTypeList.add(PRESSURE);
        spinnerSensorTypeAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
            sensorTypeList);
        mSensorTypeList.setAdapter(spinnerSensorTypeAdapter);
    }

    private void initRateSpinner() {
        List<String> rateList = new ArrayList<>();
        rateList.add(FAST);
        rateList.add(NORMAL);
        rateList.add(SLOW);
        spinnerRateAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, rateList);
        mRateList.setAdapter(spinnerRateAdapter);
    }

    private IVirtualSensorDataListener sensorDataListener = new IVirtualSensorDataListener() {
        @Override
        public void onSensorChanged(final VirtualSensorData data) {
            final float[] values = data.getValues();
            if (values.length == 0) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (firstTimestamp == -1) {
                        firstTimestamp = System.currentTimeMillis();
                    }
                    long time = System.currentTimeMillis() - firstTimestamp;
                    LineData lineData = mSensorChart.getLineData();
                    if (data.getSensor().getSensorType() == VirtualSensor.TYPE_HEART_RATE) {
                        addPoint(lineData, INDEX_HEART_RATE, time, values[0]);
                    } else if (data.getSensor().getSensorType() == VirtualSensor.TYPE_PRESSURE) {
                        addPoint(lineData, INDEX_PRESSURE, time, values[0] / PRESSURE_UNIT);
                    } else if (data.getSensor().getSensorType() == VirtualSensor.TYPE_ACCELEROMETER) {
                        addPoint(lineData, INDEX_ACCELEROMETER, time, values[0]);
                    } else if (data.getSensor().getSensorType() == VirtualSensor.TYPE_GYROSCOPE) {
                        addPoint(lineData, INDEX_GYROSCOPE, time, values[0]);
                    } else {
                        Log.d(TAG, "unknown sensor type");
                    }
                    mSensorChart.notifyDataSetChanged();
                    mSensorChart.invalidate();
                }
            });
        }
    };

    private void updateLogText(final String update) {
        Log.d(TAG, "updateLogText: " + update);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (logs.size() > LOG_LINES) {
                    logs.poll();
                }
                logs.addLast(update);
                StringBuilder sb = new StringBuilder();

                int logSize = logs.size();
                for (int i = 0; i < logSize; ++i) {
                    sb.append(logs.get(i)).append("\n");
                }
                mCurrentStatus.setText(sb.toString());
            }
        });
    }

    private void initDMSDP() {
        updateLogText("init ...");
        mDvKit = DvKit.getInstance();
    }

    private void initView() {
        mCurrentStatus = (TextView) findViewById(R.id.current_status);
        mConnectDmsdp = (Button) findViewById(R.id.connect_dmsdp);
        mConnectDmsdp.setOnClickListener(this);
        mDisconnectDmsdp = (Button) findViewById(R.id.disconnect_dmsdp);
        mDisconnectDmsdp.setOnClickListener(this);
        mSensorService = (RadioButton) findViewById(R.id.sensor_service);
        mVibrateService = (RadioButton) findViewById(R.id.vibrate_service);
        mServicesRg = (RadioGroup) findViewById(R.id.services_rg);
        mFindWear = (Button) findViewById(R.id.find_wear);
        mFindWear.setOnClickListener(this);
        mStopFindWear = (Button) findViewById(R.id.stop_find_wear);
        mStopFindWear.setOnClickListener(this);
        mSubscribe = (Button) findViewById(R.id.subscribe);
        mSubscribe.setOnClickListener(this);
        mUnsubscribe = (Button) findViewById(R.id.unsubscribe);
        mUnsubscribe.setOnClickListener(this);
        mDeviceList = (Spinner) findViewById(R.id.device_list);
        mSensorTypeList = (Spinner) findViewById(R.id.sensor_type_list);
        mRateList = (Spinner) findViewById(R.id.rate_list);
        mConnectWear = (Button) findViewById(R.id.connect_wear);
        mConnectWear.setOnClickListener(this);
        mDisconnectWear = (Button) findViewById(R.id.disconnect_wear);
        mDisconnectWear.setOnClickListener(this);
        mRegisterDataListener = (Button) findViewById(R.id.register_data_listener);
        mRegisterDataListener.setOnClickListener(this);
        mUnregisterDataListener = (Button) findViewById(R.id.unregister_data_listener);
        mUnregisterDataListener.setOnClickListener(this);
        mSensorLl = (LinearLayout) findViewById(R.id.sensor_ll);
        mVibrate = (Button) findViewById(R.id.vibrate);
        mVibrate.setOnClickListener(this);
        mVibrateRepeat = (Button) findViewById(R.id.vibrate_repeat);
        mVibrateRepeat.setOnClickListener(this);
        mCancelVibrate = (Button) findViewById(R.id.cancel_vibrate);
        mCancelVibrate.setOnClickListener(this);
        mVibrateLl = (LinearLayout) findViewById(R.id.vibrate_ll);
        mCurrentStatus.setOnClickListener(this);
        mWearData = (LinearLayout) findViewById(R.id.wear_data);
        mSensorChart = (LineChart) findViewById(R.id.sensor_chart);
        mGetSensorList = (Button) findViewById(R.id.get_sensor_list);
        mGetSensorList.setOnClickListener(this);
        mSensorIdList = (Spinner) findViewById(R.id.sensor_id_list);
        dealRadioGroup();
        mSensorService.setChecked(true);
        mNotifyService = (RadioButton) findViewById(R.id.notify_service);
        mNotify = (Button) findViewById(R.id.notify);
        mNotify.setOnClickListener(this);
        mNotifyLl = (LinearLayout) findViewById(R.id.notify_ll);
        mGetSensorListLl = (LinearLayout) findViewById(R.id.get_sensor_list_ll);
        mEnableLl = (LinearLayout) findViewById(R.id.enable_ll);
        mSensorTypeListLl = (LinearLayout) findViewById(R.id.sensor_type_ll);
        updateLL();
    }

    private void dealRadioGroup() {
        mServicesRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                updateLL();
            }
        });
    }

    private void updateLL() {
        boolean isSensorServiceChecked = mSensorService.isChecked();
        boolean isVibrateServiceChecked = mVibrateService.isChecked();
        boolean isNotifyServiceChecked = mNotifyService.isChecked();

        if (isNotifyServiceChecked) {
            mGetSensorListLl.setVisibility(View.GONE);
            mNotifyLl.setVisibility(View.VISIBLE);
        } else {
            mNotifyLl.setVisibility(View.GONE);
        }
        if (isSensorServiceChecked) {
            mGetSensorListLl.setVisibility(View.VISIBLE);
            mSensorLl.setVisibility(View.VISIBLE);
            mGetSensorList.setText("get sensor list");
            mSensorTypeListLl.setVisibility(View.VISIBLE);
        } else {
            mSensorLl.setVisibility(View.GONE);
            mSensorTypeListLl.setVisibility(View.GONE);
        }
        if (isVibrateServiceChecked) {
            mGetSensorListLl.setVisibility(View.VISIBLE);
            mVibrateLl.setVisibility(View.VISIBLE);
            mGetSensorList.setText("get vibrator list");
        } else {
            mVibrateLl.setVisibility(View.GONE);
        }
        virtualSenors.clear();
        if (spinnerSenorsAdapter != null) {
            spinnerSenorsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_dmsdp:
                dealConnectDmsdpButton();
                break;
            case R.id.disconnect_dmsdp:
                dealDisConnectDmsdpButton();
                break;
            case R.id.find_wear:
                dealFindWearButton();
                break;
            case R.id.stop_find_wear:
                dealStopFindWearButton();
                break;
            case R.id.subscribe:
                dealSubScribeButton();
                break;
            case R.id.unsubscribe:
                dealUnSubScribeButton();
                break;
            case R.id.connect_wear:
                dealConnectWearButton();
                break;
            case R.id.disconnect_wear:
                dealDisConnectWearButton();
                break;
            case R.id.register_data_listener:
                dealRegisterDataListener();
                break;
            case R.id.unregister_data_listener:
                dealUnRegisterDataListener();
                break;
            case R.id.vibrate:
                dealVibrateButton();
                break;
            case R.id.cancel_vibrate:
                dealCancelVibrateButton();
                break;
            case R.id.get_sensor_list:
                dealGetSensorList();
                break;
            case R.id.vibrate_repeat:
                dealVibrateRepeatButton();
                break;
            case R.id.notify:
                dealNotifyButton();
                break;
            default:
                break;
        }
    }

    private void subscribe() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I subscribeSensorDataListener");
            SensorAgent kitService = (SensorAgent) DvKit.getInstance().getKitService(VIRTUAL_SENSOR_SERVICE);
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            WearSensor wearSensor = (WearSensor) mSensorIdList.getSelectedItem();
            if (selectedItem != null && wearSensor != null) {
                String rateString = (String) mRateList.getSelectedItem();
                int rate = 0;
                switch (rateString) {
                    case FAST: {
                        rate = SensorAgent.SENSOR_DELAY_FAST;
                        break;
                    }
                    case NORMAL: {
                        rate = SensorAgent.SENSOR_DELAY_NORMAL;
                        break;
                    }
                    case SLOW: {
                        rate = SensorAgent.SENSOR_DELAY_SLOW;
                        break;
                    }
                    default: {
                        break;
                    }
                }
                int state = kitService.subscribeSensorDataListener(sensorDataListener, wearSensor.getVirtualSensor(),
                    rate);
                updateLogText("I subscribeSensorDataListener state: " + state);
            } else {
                updateLogText("E subscribeSensorDataListener, select no device or sensor");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
            return;
        }
    }

    private void ubSubscribe() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I unSubscribeSensorDataListener");
            SensorAgent kitService = (SensorAgent) DvKit.getInstance().getKitService(VIRTUAL_SENSOR_SERVICE);
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            if (selectedItem != null) {
                kitService.unSubscribeSensorDataListener(sensorDataListener);
            } else {
                updateLogText("E unSubscribeSensorDataListener, select no sensor");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private boolean checkMyPermisson(String vibrate) {
        if (checkCallingOrSelfPermission(vibrate) != PERMISSION_GRANTED) {
            requestPermissions(new String[] {vibrate}, 0);
            Log.e(TAG, "onClick: no permission");
            updateLogText("E no permission");
            return true;
        }
        return false;
    }

    private void getVibratorList() {
        if (mVirtualDeviceManager != null) {
            VibratorService vibratorService =
                (VibratorService) DvKit.getInstance().getKitService(VIRTUAL_VIBRATE_SERVICE);
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            if (selectedItem != null || vibratorService != null) {
                List<VirtualVibrator> vibrateList = vibratorService.getVibrateList(selectedItem.getDeviceId());
                Log.d(TAG, "onClick: vibrateList " + vibrateList);
                virtualSenors.clear();
                if (vibrateList != null) {
                    for (VirtualVibrator virtualVibrator : vibrateList) {
                        WearSensor wearSensor = new WearSensor("vibrator" + virtualVibrator.getVibrateId(),
                            virtualVibrator.getDeviceId(), virtualVibrator.getVibrateId());
                        wearSensor.setVibrator(virtualVibrator);
                        virtualSenors.add(wearSensor);
                    }
                }
                spinnerSenorsAdapter.notifyDataSetChanged();
            } else {
                updateLogText("E getVibrateList，select no device");
            }
        }
    }

    private void getSensorList() {
        if (mVirtualDeviceManager != null) {
            SensorAgent kitService = (SensorAgent) DvKit.getInstance().getKitService(VIRTUAL_SENSOR_SERVICE);
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            if (selectedItem != null) {
                String sensorTypeString = (String) mSensorTypeList.getSelectedItem();
                int sensorType = 0;
                switch (sensorTypeString) {
                    case HEART_RATE: {
                        sensorType = VirtualSensor.TYPE_HEART_RATE;
                        break;
                    }
                    case ACCELEROMETER: {
                        sensorType = VirtualSensor.TYPE_ACCELEROMETER;
                        break;
                    }
                    case GYROSCOPE: {
                        sensorType = VirtualSensor.TYPE_GYROSCOPE;
                        break;
                    }
                    case PRESSURE: {
                        sensorType = VirtualSensor.TYPE_PRESSURE;
                        break;
                    }
                    default: {
                        break;
                    }
                }
                List<VirtualSensor> sensorList = kitService.getSensorList(selectedItem.getDeviceId(), sensorType);
                Log.d(TAG, "onClick: sensorList " + sensorList);
                virtualSenors.clear();
                if (sensorList != null) {
                    for (VirtualSensor virtualSensor : sensorList) {
                        WearSensor wearSensor = new WearSensor("Sensor" + virtualSensor.getSensorId(),
                            virtualSensor.getDeviceId(), virtualSensor.getSensorId());
                        wearSensor.setVirtualSensor(virtualSensor);
                        virtualSenors.add(wearSensor);
                    }
                }
                spinnerSenorsAdapter.notifyDataSetChanged();
            } else {
                updateLogText("E getSensorList, select no device");
            }
        }
    }

    @Override
    protected void onDestroy() {
        DvKit.getInstance().disConnect();
        super.onDestroy();
    }

    private void dealConnectDmsdpButton() {
        if (mDvKit != null) {
            updateLogText("I connect service");
            mDvKit.connect(this, new IDvKitConnectCallback() {
                @Override
                public void onConnect(int i) {
                    updateLogText("I onConnect connect service status:" + i);
                    Log.e(TAG, "onConnect: get msdp service ");
                    if (i == 0) {
                        mVirtualDeviceManager = (VirtualDeviceManager) mDvKit.getKitService(VIRTUAL_DEVICE_CLASS);
                    } else {
                        Log.e(TAG, "onConnect: get msdp service error");
                    }
                }

                @Override
                public void onDisconnect() {
                    updateLogText("I onDisconnect");
                    mVirtualDeviceManager = null;
                    virtualDevices.clear();
                    virtualSenors.clear();
                    isSubscribed = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinnerAdapter.notifyDataSetChanged();
                            spinnerSenorsAdapter.notifyDataSetChanged();
                            LineData lineData = mSensorChart.getLineData();
                            lineData.clearValues();
                            mSensorChart.notifyDataSetChanged();
                            mSensorChart.invalidate();
                            Toast.makeText(HiWearActivity.this, "dmsdp onDisconnect", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            updateLogText("E mDvKit is null");
        }
    }

    private void dealDisConnectDmsdpButton() {
        if (mDvKit != null) {
            mDvKit.disConnect();
            updateLogText("I disconnect");
        } else {
            updateLogText("E mDvKit is null");
        }
    }

    private void dealFindWearButton() {
        if (mVirtualDeviceManager != null) {
            mVirtualDeviceManager.startDiscovery(discoveryCallback);
            updateLogText("I start discovery");
        } else {
            updateLogText("E mDvKit is null");
        }
    }

    private void dealStopFindWearButton() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I cancel discovery");
            mVirtualDeviceManager.cancelDiscovery(discoveryCallback);
        } else {
            updateLogText("E mDvKit is null");
        }
    }

    private void dealSubScribeButton() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I subscribe device status");
            if (isSubscribed) {
                updateLogText("W subscribe device subscribed");
                return;
            }
            int subscribe = mVirtualDeviceManager.subscribe(EnumSet.of(VIRTUALDEVICE), observer);
            isSubscribed = subscribe == SUCCESS;
            updateLogText("I subscribe device status state" + subscribe);
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealUnSubScribeButton() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I unsubscribe device status");
            if (isSubscribed) {
                int unsubscribe = mVirtualDeviceManager.unsubscribe(EnumSet.of(VIRTUALDEVICE), observer);
                if (unsubscribe == SUCCESS) {
                    isSubscribed = false;
                }
                updateLogText("I subscribe device status state " + unsubscribe);
            } else {
                updateLogText("E subscribe device status unsubscribed");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealConnectWearButton() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I enable device service");
            if (!isSubscribed) {
                updateLogText("E enable device service, no subscribed device status");
                return;
            }
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            if (selectedItem != null) {
                Capability capability = null;
                if (mSensorService.isChecked()) {
                    capability = Capability.valueOf("SENSOR");
                }
                if (mVibrateService.isChecked()) {
                    capability = Capability.valueOf("VIBRATE");
                }
                if (mNotifyService.isChecked()) {
                    capability = Capability.valueOf("NOTIFICATION");
                }

                EnumSet<Capability> supports = selectedItem.getCapabilities();
                if (capability == null || !supports.contains(capability)) {
                    updateLogText("E enable device service, not support " + capability);
                    return;
                }
                int res =
                    mVirtualDeviceManager.enableVirtualDevice(selectedItem.getDeviceId(), EnumSet.of(capability), null);
                updateLogText("I enable device service state ：" + res);
                if (res == 0) {
                    mDeviceList.setClickable(false);
                }
            } else {
                updateLogText("E  enable device service, select no device");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealDisConnectWearButton() {
        if (mVirtualDeviceManager != null) {
            updateLogText("I disable device service");
            if (!isSubscribed) {
                updateLogText("E disable device service , no subscribed device status");
                return;
            }
            Capability capability = null;
            if (mSensorService.isChecked()) {
                capability = Capability.valueOf("SENSOR");
            }
            if (mVibrateService.isChecked()) {
                capability = Capability.valueOf("VIBRATE");
            }
            if (mNotifyService.isChecked()) {
                capability = Capability.valueOf("NOTIFICATION");
            }
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            EnumSet<Capability> supports = selectedItem.getCapabilities();
            if (capability == null || !supports.contains(capability)) {
                updateLogText("E disable device service, not support " + capability);
                return;
            }
            int res = mVirtualDeviceManager.disableVirtualDevice(selectedItem.getDeviceId(), EnumSet.of(capability));
            updateLogText("I disable device service state ：" + res);

            if (res == ERROR_CODE_CAN_NOT_DISABLE) {
                Toast.makeText(HiWearActivity.this, "can't disable device service", Toast.LENGTH_SHORT).show();
            }
            if (res == 0) {
                mDeviceList.setClickable(true);
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealRegisterDataListener() {
        if (checkMyPermisson(Manifest.permission.BODY_SENSORS)) {
            return;
        }
        subscribe();
    }

    private void dealUnRegisterDataListener() {
        if (checkMyPermisson(Manifest.permission.BODY_SENSORS)) {
            return;
        }
        ubSubscribe();
    }

    private void dealVibrateButton() {
        if (checkMyPermisson(Manifest.permission.VIBRATE)) {
            return;
        }
        if (mVirtualDeviceManager != null) {
            updateLogText("I vibrate");
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            WearSensor wearSensor = (WearSensor) mSensorIdList.getSelectedItem();
            if (selectedItem != null && wearSensor != null) {
                VirtualVibrator vibrator = wearSensor.getVibrator();
                vibrator.vibrate(2000);
            } else {
                updateLogText("E vibrate, select no device or vibrator");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealCancelVibrateButton() {
        if (checkMyPermisson(Manifest.permission.VIBRATE)) {
            return;
        }
        if (mVirtualDeviceManager != null) {
            updateLogText("I cancel vibrate");
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            WearSensor wearSensor = (WearSensor) mSensorIdList.getSelectedItem();

            if (selectedItem != null && wearSensor != null) {
                VirtualVibrator vibrator = wearSensor.getVibrator();
                vibrator.cancel();
            } else {
                updateLogText("E cancel vibrate, select no device or vibrator");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealGetSensorList() {
        if (mSensorService.isChecked()) {
            if (checkMyPermisson(Manifest.permission.BODY_SENSORS)) {
                return;
            }
            getSensorList();
        } else {
            if (checkMyPermisson(Manifest.permission.VIBRATE)) {
                return;
            }
            getVibratorList();
        }
    }

    private void dealVibrateRepeatButton() {
        if (checkMyPermisson(Manifest.permission.VIBRATE)) {
            return;
        }
        if (mVirtualDeviceManager != null) {
            updateLogText("I vibrate repeat");
            WearDevice selectedItem = (WearDevice) mDeviceList.getSelectedItem();
            WearSensor wearSensor = (WearSensor) mSensorIdList.getSelectedItem();
            if (selectedItem != null && wearSensor != null) {
                VirtualVibrator vibrator = wearSensor.getVibrator();
                vibrator.vibrate(new long[] {2000, 500, 1000, 200}, 0);
            } else {
                updateLogText("E vibrate repeat, select no device or vibrator");
            }
        } else {
            updateLogText("E mVirtualDeviceManager is null");
        }
    }

    private void dealNotifyButton() {
        if (mDvKit != null) {
            WearDevice wearDevice = (WearDevice) mDeviceList.getSelectedItem();
            if (wearDevice == null) {
                Toast.makeText(HiWearActivity.this, "select no device", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(HiWearActivity.this, NotificationTest.class);
            intent.putExtra("udid", wearDevice.getDeviceId());
            startActivity(intent);
        }
    }
}
