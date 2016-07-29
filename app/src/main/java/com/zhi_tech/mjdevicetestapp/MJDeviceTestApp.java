package com.zhi_tech.mjdevicetestapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import com.zhi_tech.magiceyessdk.MagicEyesActivity;
import com.zhi_tech.magiceyessdk.MagicEyesService;
import com.zhi_tech.magiceyessdk.Utils.DeviceFilter;
import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;
import com.zhi_tech.magiceyessdk.Utils.UsbDeviceFilter;
import com.zhi_tech.magiceyessdk.Utils.Utils;

public class MJDeviceTestApp extends MagicEyesActivity implements OnItemClickListener, OnClickListener {
    private SharedPreferences mSp = null;
    private GridView mGrid;
    private MyAdapter mAdapter;
    public static ArrayList<Integer> itemIds;
    private ArrayList<String> mListData;
    private Button mBtAuto;
    private Button mBtStart;
    private Button mBtUpgrade;
    private Button mBtCalibration;
    private Button mBtCheckVersion;
    private Button mBtCheckSN;
    private Button mBleCy7c63813;

    private TextView textViewDeviceManufacturer, textViewDeviceProductName, textViewPacket,
            textViewVersionBLE, textViewVersion63813, textViewVersionStm32, textViewJoySickState,
            textViewBleAddr, textViewSN, textViewUsbStorageInfo, textViewHandShakeStatus;

    public static byte[] result; //0 default; 1,success; 2,fail; 3,notest
    private boolean mCheckDataSuccess;
    private byte okFlag = 0x00;
    private boolean mBleCy7c63813IsConnected = false;

    public static int ShowItemTestResultTimeout = 1; // s

    public static boolean IsFactoryMode = false;
    public static boolean AutoTestMode = false;
    public static int ItemTestTimeout = 120;
    public static int VersionCheckStm32 = 6;
    public static int VersionCheckBLE = 27;
    public static int VersionCheck63813 = 3;
    public static int Accel_FullScale_Range = 3; // g
    public static float Gyro_FullScale_Range = 300.0f; // o/s
    public static int Proximity_Threshold_Approach = 820; //
    public static int Light_Threshold_Approach = 100; //
    public static int Temperature_Range_Min = -40; //oC
    public static int Temperature_Range_Max = 85; //oC
    public static int Magnetic_Yaw_Offset = 90; // o

    private final String TAG = "MJDeviceTestApp";

    private Handler handler = new Handler();

    private final static int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    @Override
    public void OnServiceConnectedHandler(ComponentName componentName, IBinder iBinder) {
        super.OnServiceConnectedHandler(componentName, iBinder);
        DeviceFilter deviceFilter = null;
        try {
            deviceFilter = UsbDeviceFilter.ParseDeviceFilterFromResourceId(this, R.xml.device_filter);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, String.format(Locale.US, "DeviceFilter[vid:%d, pid:%d]", deviceFilter.mVendorId, deviceFilter.mProductId));
        SetDeviceFilter(deviceFilter.mVendorId, deviceFilter.mProductId);
        SetBulkTransferTimeout(1000);
        SetReceiveDataGapTime(50);
        SetMainActivityClassName(this.getClass().getName());
        //check WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //request WRITE_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            OnDeviceConnectHandler();
        }
    }

    @Override
    public void OnServiceDisconnectedHandler(ComponentName componentName) {
        super.OnServiceDisconnectedHandler(componentName);
    }

    private void OnDeviceConnectHandler() {
        ConnectToDevice();
        SendCommand(Utils.CMD_CHECK_VERSION);

        handler.post(new Runnable() {
            @Override
            public void run() {
                String info = GetDeviceInfo();
                if (info != null) {
                    Pattern pattern = Pattern.compile(",");
                    String[] information = pattern.split(info);
                    textViewDeviceManufacturer.setText(String.format("%s:%s", getString(R.string.device_manufacturer), information[0]));
                    textViewDeviceProductName.setText(String.format("%s:%s", getString(R.string.device_productname), information[1]));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    OnDeviceConnectHandler();
                } else {
                    Toast.makeText(this, "REQUEST WRITE_EXTERNAL_STORAGE PERMISSION FAILED!", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        initDefaultSetting();

        mBtAuto = (Button) findViewById(R.id.main_bt_autotest);
        assert mBtAuto != null;
        mBtAuto.setOnClickListener(this);
        mBtStart = (Button) findViewById(R.id.main_bt_start);
        assert mBtStart != null;
        mBtStart.setOnClickListener(this);
        mBtUpgrade = (Button) findViewById(R.id.main_bt_upgrade);
        assert mBtUpgrade != null;
        mBtUpgrade.setOnClickListener(this);
        mBtCalibration = (Button) findViewById(R.id.main_bt_calibration);
        assert mBtCalibration != null;
        mBtCalibration.setOnClickListener(this);
        mBtCheckVersion = (Button) findViewById(R.id.main_bt_checkversion);
        assert mBtCheckVersion != null;
        mBtCheckVersion.setOnClickListener(this);
        mBtCheckSN = (Button) findViewById(R.id.main_bt_checksn);
        assert mBtCheckSN != null;
        mBtCheckSN.setOnClickListener(this);
        mBleCy7c63813 = (Button) findViewById(R.id.main_bt_ble_cy7c63813);
        assert mBleCy7c63813 != null;
        mBleCy7c63813.setOnClickListener(this);

        textViewDeviceManufacturer = (TextView) findViewById(R.id.textViewDeviceManufacturer);
        textViewDeviceProductName = (TextView) findViewById(R.id.textViewDeviceProductName);
        textViewPacket = (TextView) findViewById(R.id.textViewPacket);
        textViewVersionBLE = (TextView) findViewById(R.id.textViewVersionBLE);
        textViewVersion63813 = (TextView) findViewById(R.id.textViewVersion63813);
        textViewVersionStm32 = (TextView) findViewById(R.id.textViewVersionStm32);
        textViewJoySickState = (TextView) findViewById(R.id.textViewJoySickState);
        textViewBleAddr = (TextView) findViewById(R.id.textViewBleAddr);
        textViewSN = (TextView) findViewById(R.id.textViewSN);
        textViewUsbStorageInfo = (TextView) findViewById(R.id.textViewUsbStorageInfo);
        textViewHandShakeStatus = (TextView) findViewById(R.id.textViewHandShakeStatus);

        if (IsFactoryMode) {
            //mBtAuto.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
            textViewUsbStorageInfo.setVisibility(View.GONE);
            mBtStart.setVisibility(View.GONE);
        } else {
            mBtCalibration.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
            //mBtStart.setVisibility(View.GONE);
        }

        // init grid view data
        initTestItems();
        setDefaultValues();

        mGrid = (GridView) findViewById(R.id.main_grid);
        mAdapter = new MyAdapter(this, mListData);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);

        IntentFilter mediaFilter = new IntentFilter();
        mediaFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mediaFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilter.addDataScheme("file");
        registerReceiver(mUsbMediaStorageReceiver, mediaFilter);
        IntentFilter deviceFilter = new IntentFilter();
        deviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        deviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        deviceFilter.addAction(Utils.ACTION_KILL_SELF);
        registerReceiver(mUsbReceiver, deviceFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onResume();
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);
    }

    public void initDefaultSetting() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //factory mode
        IsFactoryMode = sharedPreferences.getBoolean("factory_mode_switch", true);
        //auto test mode
        AutoTestMode = sharedPreferences.getBoolean("auto_test_mode_switch", true);
        //item test timeout
        String string = sharedPreferences.getString("item_test_timeout_list", "120");
        ItemTestTimeout = Integer.valueOf(string);
        //version check stm32
        string = sharedPreferences.getString("version_check_stm32", "6");
        VersionCheckStm32 = Integer.valueOf(string);
        //version check BLE
        string = sharedPreferences.getString("version_check_ble", "27");
        VersionCheckBLE = Integer.valueOf(string);
        //version check 63813
        string = sharedPreferences.getString("version_check_63813", "3");
        VersionCheck63813 = Integer.valueOf(string);
        // accel full scale range
        string = sharedPreferences.getString("accel_full_scale_select", "3");
        Accel_FullScale_Range = Integer.valueOf(string);
        // gyro full scale range
        string = sharedPreferences.getString("gyro_full_scale_select", "300");
        Gyro_FullScale_Range = Float.valueOf(string);
        // proximity sensor threshold approach
        string = sharedPreferences.getString("proximity_threshold_approach", "820");
        Proximity_Threshold_Approach = Integer.valueOf(string);
        // light sensor threshold approach
        string = sharedPreferences.getString("light_threshold_approach", "100");
        Light_Threshold_Approach = Integer.valueOf(string);
        // temp range min, max
        string = sharedPreferences.getString("temp_range_min", "-40");
        Temperature_Range_Min = Integer.valueOf(string);
        string = sharedPreferences.getString("temp_range_max", "85");
        Temperature_Range_Max = Integer.valueOf(string);
        // magnetic yaw offset
        string = sharedPreferences.getString("magnetic_yaw_offset", "90");
        Magnetic_Yaw_Offset = Integer.valueOf(string);
        //
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() +
                String.format(Locale.US,"->%n" +
                        "IsFactoryMode=%b%n" +
                        "AutoTestMode=%b%n" +
                        "ItemTestTimeout=%ds%n" +
                        "VersionCheckStm32=%d%n" +
                        "VersionCheckBLE=%d%n" +
                        "VersionCheck63813=%d%n" +
                        "Accel_FullScale_Range=%dg%n" +
                        "Gyro_FullScale_Range=%fdps%n" +
                        "Proximity_Threshold_Approach=%d%n" +
                        "Light_Threshold_Approach=%d%n" +
                        "Temperature_Range_Min=%d℃%n" +
                        "Temperature_Range_Max=%d℃%n" +
                        "Magnetic_Yaw_Offset=%do%n",
                        IsFactoryMode,
                        AutoTestMode,
                        ItemTestTimeout,
                        VersionCheckStm32,
                        VersionCheckBLE,
                        VersionCheck63813,
                        Accel_FullScale_Range,
                        Gyro_FullScale_Range,
                        Proximity_Threshold_Approach,
                        Light_Threshold_Approach,
                        Temperature_Range_Min,
                        Temperature_Range_Max,
                        Magnetic_Yaw_Offset
                        ));
    }

    private void initTestItems() {
        //add test items
        itemIds = new ArrayList<Integer>();
        itemIds.clear();
        if (!IsFactoryMode) {
            itemIds.add(R.string.KeyCode_name);
            itemIds.add(R.string.lsensor_name);
            itemIds.add(R.string.psensor_name);
        }

        itemIds.add(R.string.gsensor_name);
        itemIds.add(R.string.msensor_name);
        itemIds.add(R.string.gyroscopesensor_name);
        itemIds.add(R.string.tsensor_name);
        //set default value
        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        for (int item:itemIds) {
            editor.putString(getString(item), AppDefine.DT_DEFAULT);
        }
        editor.apply();
        //add test items string to mListData
        mListData = new ArrayList<String>();
        for (int item:itemIds) {
            mListData.add(getString(item));
        }
    }

    private void setDefaultValues() {
        mCheckDataSuccess = false;
        okFlag = 0x00;
        mBleCy7c63813IsConnected = false;
        result = new byte[AppDefine.DVT_NV_ARRAR_LEN];
        textViewDeviceManufacturer.setText(String.format("%s:%s", getString(R.string.device_manufacturer), getString(R.string.device_unknown)));
        textViewDeviceProductName.setText(String.format("%s:%s", getString(R.string.device_productname), getString(R.string.device_unknown)));
        textViewPacket.setText(String.format("%s: %s  %s: %s",
                getString(R.string.packetdata_header), getString(R.string.device_unknown),
                getString(R.string.packetdata_timestamp),getString(R.string.device_unknown)));
        textViewVersionBLE.setText(String.format("%s: %s",
                getString(R.string.device_version_ble), getString(R.string.device_unknown)));
        textViewVersion63813.setText(String.format("%s: %s",
                getString(R.string.device_version_63813), getString(R.string.device_unknown)));
        textViewVersionStm32.setText(String.format("%s: %s",
                getString(R.string.device_version_stm32), getString(R.string.device_unknown)));
        textViewJoySickState.setText(String.format("%s: %s",
                getString(R.string.device_ble_state), getString(R.string.device_unknown)));
        textViewBleAddr.setText(String.format(Locale.US, "%s: %s",
                getString(R.string.device_ble_mac_addr), getString(R.string.device_unknown)));
        textViewSN.setText(String.format(Locale.US, "%s: %s",
                getString(R.string.device_serial_number), getString(R.string.device_unknown)));
        textViewUsbStorageInfo.setText(String.format(Locale.US, "%s:%s",
                getString(R.string.usb_storage_info), getString(R.string.device_unknown)));
        textViewHandShakeStatus.setText(String.format(Locale.US, "%s: %s",
                getString(R.string.device_hand_shake_status), getString(R.string.device_unknown)));
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        switch (v.getId()) {
            case R.id.main_bt_autotest:
                startAutoTestActivity();
                break;
            case R.id.main_bt_start:
                SendCommand(Utils.CMD_RECV_SENSOR_DATA);
                break;
            case R.id.main_bt_upgrade:
                //start upgrade request
                SendCommand(Utils.CMD_IAP_UPGRADE);
                break;
            case R.id.main_bt_calibration:
                //start calibration request
                SendCommand(Utils.CMD_G_CALIBRATE);
                break;
            case R.id.main_bt_checkversion:
                //check version request
                SendCommand(Utils.CMD_CHECK_VERSION);
                break;
            case R.id.main_bt_checksn:
                //check sn request
                SendCommand(Utils.CMD_READ_SN);
                break;
            case R.id.main_bt_ble_cy7c63813:
                //read ble address request
                SendCommand(Utils.CMD_READ_BLE_MAC);
                break;
            default:
                break;
        }
    }

    public class MyAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<String> mDataList;

        public MyAdapter(Context context, ArrayList<String> mDataList) {
            this.context = context;
            this.mDataList = mDataList;
        }

        @Override
        public int getCount() {
            return (mDataList == null) ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return (mDataList == null) ? null : mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.main_grid,parent, false);
            }
            TextView textView = UtilTools.ViewHolder.get(convertView, R.id.factor_button);
            textView.setText(mDataList.get(position));
            try {
                String name = mSp.getString(mDataList.get(position), null);
                if (name != null) {
                    if (name.equals(AppDefine.DT_SUCCESS)) {
                        textView.setTextColor(Color.BLUE);
                    } else if (name.equals(AppDefine.DT_DEFAULT)) {
                        textView.setTextColor(Color.BLACK);
                    } else if (name.equals(AppDefine.DT_FAILED)) {
                        textView.setTextColor(Color.RED);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG,"SetColor ExException");
            }
            return convertView;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Intent intent = new Intent();
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            String name = mListData.get(position);
            String classname = null;
            if (name.equals(getString(R.string.gsensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.GSensor";
            } else if (name.equals(getString(R.string.msensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.MSensor";
            } else if (name.equals(getString(R.string.lsensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.LSensor";
            } else if (name.equals(getString(R.string.psensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.PSensor";
            } else if (name.equals(getString(R.string.KeyCode_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.KeyCode";
            }else if(name.equals(getString(R.string.gyroscopesensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.GyRoscopeSensor";
            }else if(name.equals(getString(R.string.tsensor_name))) {
                classname = "com.zhi_tech.mjdevicetestapp.sensor.TSensor";
            }
            intent.setClassName(this, classname);
            this.startActivity(intent);
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.PackageIerror);
            builder.setMessage(R.string.Packageerror);
            builder.setPositiveButton("OK", null);
            builder.create().show();
        }
    }

    private void startAutoTestActivity() {
        Intent intent = new Intent();
        int reqId = -1;
        intent.setClassName("com.zhi_tech.mjdevicetestapp", "com.zhi_tech.mjdevicetestapp.AutoTest");
        reqId = AppDefine.DT_AUTOTESTID;
        startActivityForResult(intent, reqId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onActivityResult(requestCode, resultCode, data);
        //System.gc();
        Intent intent = new Intent(MJDeviceTestApp.this, Report.class);
        startActivity(intent);

    }

    @Override
    protected void onStop() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onStop();
    }

    @Override
    public void finish() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onDestroy();
        unregisterReceiver(mUsbMediaStorageReceiver);
        unregisterReceiver(mUsbReceiver);
        //close app when usb detached
        //android.os. Process.killProcess(android.os.Process.myPid());
    }

    public void CheckVersionSaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.CheckVersion,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
    }

    private final BroadcastReceiver mUsbMediaStorageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " mUsbMediaStorageReceiver-->" + action);
            if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                //USB device ejected
                Log.d(TAG, "--> USB device eject!");
                textViewUsbStorageInfo.setText(String.format(Locale.US, "%s:%s",
                        getString(R.string.usb_storage_info), getString(R.string.device_unknown)));
                textViewUsbStorageInfo.invalidate();
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                //USB device mounted
                Log.d(TAG, "--> USB device mounted!");
                String path = intent.getData().getPath() + File.separator;
                File rootDir = new File(path);
                Log.d(TAG, String.format(Locale.US, "->USB Info:%n" +
                        "path:%s%n" +
                        "Name:%s%n" +
                        "TotalSpace:%dMB%n" +
                        "FreeSpace:%dMB%n" +
                        "%b",
                        path,
                        rootDir.getName(),
                        rootDir.getTotalSpace()/1024/1024,
                        rootDir.getFreeSpace()/1024/1024,
                        rootDir.canRead()));
                textViewUsbStorageInfo.setText(String.format(Locale.US, "%s:Name:%s TotalSpace:%dMB",
                        getString(R.string.usb_storage_info),
                        rootDir.getName(),
                        rootDir.getTotalSpace()/1024/1024));
                textViewUsbStorageInfo.invalidate();
            }
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " mUsbReceiver-->" + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            } else if (Utils.ACTION_KILL_SELF.equals(action)) {
                //close app when usb detached
                android.os. Process.killProcess(android.os.Process.myPid());
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent keyCode->" + event.getKeyCode() + "-->" + event.getSource());
        MyAdapter myAdapter = (MyAdapter) mGrid.getAdapter();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                //joystick
                case KeyEvent.KEYCODE_BUTTON_L2:
                case KeyEvent.KEYCODE_BUTTON_L1:
                case KeyEvent.KEYCODE_BUTTON_Y:
                    okFlag |= 0x80;
                    checkDataSuccess();
                    break;
                default:
                    break;
            }
        }
        return true; //super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if ((ev.getDevice().getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            Log.d(TAG, String.format("dispatchGenericMotionEvent ev->(%f, %f)", ev.getX(), ev.getY()));

            if (ev.getX() > 0.5) {
                okFlag |= 0x01;
            }
            if (ev.getX() < -0.5) {
                okFlag |= 0x02;
            }
            if (ev.getY() > 0.5) {
                okFlag |= 0x04;
            }
            if (ev.getY() < -0.5) {
                okFlag |= 0x08;
            }
            checkDataSuccess();
        }
        return true; //super.dispatchGenericMotionEvent(ev);
    }

    public void checkDataSuccess() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->okFlag = %#x", okFlag));
        if (IsFactoryMode) {
            if (okFlag != 0 && !mBleCy7c63813IsConnected) {
                mBleCy7c63813IsConnected = true;
                mBleCy7c63813.setTextColor(Color.GREEN);
                Toast toast=Toast.makeText(getApplicationContext(), getString(R.string.ble_test_tip), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                SendCommand(Utils.CMD_G_CALIBRATE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(MJDeviceTestApp.this, SettingsActivity.class);
                item.setIntent(intent);
                break;
            case R.id.action_about_info:
                Toast toast=Toast.makeText(getApplicationContext(),
                        String.format(Locale.US, "%s%n%s: %s",
                                getString(R.string.app_name), getString(R.string.about_us_info), getVersionName(this)),
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }

    @Override
    protected void OnSensorDataChangedHandler(final SensorPacketDataObject object) {
        if (object.getPacketHeader()[0] == 'M' && object.getPacketHeader()[1] == '5') {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textViewPacket.setText(String.format(Locale.US, "%s: %s  %s: %d",
                            getString(R.string.packetdata_header), String.valueOf(object.getPacketHeader()),
                            getString(R.string.packetdata_timestamp), object.getPacketDataTimestamp()));
                }
            });
        }
    }

    @Override
    protected void OnCommandResultChangedHandler(final int cmd, final byte[] data, final int length) {
        Utils.dLog(TAG, String.format(Locale.US, "cmd:%#04x", cmd));
        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (cmd) {
                    case 0xE0:
                        if (length > 0 && data[0] == 0x01) {
                            textViewHandShakeStatus.setText(String.format(Locale.US, "%s:%s",
                                    getString(R.string.device_hand_shake_status), "true"));
                            textViewHandShakeStatus.setTextColor(Color.GREEN);
                        } else {
                            textViewHandShakeStatus.setText(String.format(Locale.US, "%s:%s",
                                    getString(R.string.device_hand_shake_status), "false"));
                            textViewHandShakeStatus.setTextColor(Color.RED);
                        }
                        break;
                    case 0xA5: // iap upgrade return result value
                        int upgradeTip = 0;
                        if (data[0] == 1) {
                            upgradeTip = R.string.upgrade_failed;
                        } else {
                            upgradeTip = R.string.upgrade_complete;
                        }
                        Toast toast=Toast.makeText(getApplicationContext(), getString(upgradeTip), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        break;

                    case 0x2C: //G sensor calibration result send 0x2B feedback
                        int calibrationTip = 0;
                        if (data[0] == 0) {
                            calibrationTip = R.string.calibration_complete;
                            mBtCalibration.setTextColor(Color.GREEN);
                        } else {
                            calibrationTip = R.string.calibration_failed;
                            mBtCalibration.setTextColor(Color.RED);
                        }
                        toast=Toast.makeText(getApplicationContext(), getString(calibrationTip), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                        if (IsFactoryMode && AutoTestMode) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startAutoTestActivity();
                                }
                            }, 2 * 1000);
                        }
                        break;

                    case 0xB2: //check version result send 0xB1 feedback
                        if (data[0] == VersionCheckBLE) {
                            textViewVersionBLE.setTextColor(Color.GREEN);
                        } else {
                            textViewVersionBLE.setTextColor(Color.RED);
                        }
                        if (data[1] == VersionCheck63813) {
                            textViewVersion63813.setTextColor(Color.GREEN);
                        } else {
                            textViewVersion63813.setTextColor(Color.RED);
                        }
                        if (data[2] == VersionCheckStm32) {
                            textViewVersionStm32.setTextColor(Color.GREEN);
                        } else {
                            textViewVersionStm32.setTextColor(Color.RED);
                        }

                        if (data[0] == VersionCheckBLE && data[1] == VersionCheck63813 && data[2] == VersionCheckStm32) {
                            mCheckDataSuccess = true;
                            mBtCheckVersion.setTextColor(Color.GREEN);
                        } else {
                            mCheckDataSuccess = false;
                            mBtCheckVersion.setTextColor(Color.RED);
                        }
                        textViewVersionBLE.setText(String.format(Locale.US,"%s: %d",
                                getString(R.string.device_version_ble), data[0]));
                        textViewVersion63813.setText(String.format(Locale.US,"%s: %d",
                                getString(R.string.device_version_63813), data[1]));
                        textViewVersionStm32.setText(String.format(Locale.US,"%s: %d",
                                getString(R.string.device_version_stm32), data[2]));
                        textViewJoySickState.setText(String.format(Locale.US,"%s: %d",
                                getString(R.string.device_ble_state), data[3]));

                        if (AutoTestMode) {
                            SendCommand(Utils.CMD_READ_BLE_MAC);
                        }
                        break;

                    case 0xB6: //write ble mac return value
                        int bleWriteMacTip = 0;
                        if (data[0] == 1) {
                            bleWriteMacTip = R.string.ble_write_mac_failed;
                        } else {
                            bleWriteMacTip = R.string.ble_write_mac_complete;
                        }
                        toast=Toast.makeText(getApplicationContext(), getString(bleWriteMacTip), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        break;

                    case 0xB8: //read ble mac return value
                        if (length == 6) {
                            textViewBleAddr.setText(String.format(Locale.US, "%s: %02x:%02x:%02x:%02x:%02x:%02x",
                                    getString(R.string.device_ble_mac_addr),
                                    data[0], data[1], data[2],data[3], data[4], data[5]));
                            if (!IsFactoryMode) {
                                mBleCy7c63813.setTextColor(Color.GREEN);
                            }
                            textViewBleAddr.setTextColor(Color.GREEN);
                        }
                        if (AutoTestMode) {
                            SendCommand(Utils.CMD_READ_SN);
                        }
                        break;

                    case 0xBB: //result for serial number writing
                        int writeSNTip = 0;
                        if (data[0] == 1) {
                            writeSNTip = R.string.device_write_sn_failed;
                        } else {
                            writeSNTip = R.string.device_write_sn_complete;
                        }
                        toast=Toast.makeText(getApplicationContext(), getString(writeSNTip), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        break;

                    case 0xBD: //read serial number return value
                        String stringSn = null;
                        try {
                            stringSn = new String(data, "US-ASCII");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        textViewSN.setText(String.format(Locale.US, "%s:%s",
                                getString(R.string.device_serial_number), stringSn));

                        if (length == 0x0F) {
                            textViewSN.setTextColor(Color.GREEN);
                            mBtCheckSN.setTextColor(Color.GREEN);
                        } else {
                            textViewSN.setTextColor(Color.RED);
                            mBtCheckSN.setTextColor(Color.RED);
                        }
                        /*handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (AutoTestMode && !IsFactoryMode) {
                                    startAutoTestActivity();
                                }
                            }
                        }, 2 *1000);*/
                        break;
                    default:
                        break;
                }
            }
        });
    }
}
