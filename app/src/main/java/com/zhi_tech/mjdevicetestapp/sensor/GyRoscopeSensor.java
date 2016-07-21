package com.zhi_tech.mjdevicetestapp.sensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhi_tech.magiceyessdk.MagicEyesActivity;
import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;
import com.zhi_tech.magiceyessdk.Utils.Utils;
import com.zhi_tech.mjdevicetestapp.AppDefine;
import com.zhi_tech.mjdevicetestapp.MJDeviceTestApp;
import com.zhi_tech.mjdevicetestapp.R;
import com.zhi_tech.mjdevicetestapp.UtilTools;

import java.util.Locale;

/**
 * Created by taipp on 5/20/2016.
 */

/*
 *
 * 陀螺仪的XYZ分别代表设备围绕XYZ三个轴旋转的角速度：radians/second。至于XYZ使用的坐标系与gsensor相同。
 * 逆时针方向旋转时，XYZ的值是正的
 *
 * */
public class GyRoscopeSensor extends MagicEyesActivity implements View.OnClickListener {

    private TextView tvdata;
    private Button mBtOk;
    private Button mBtFailed;

    SharedPreferences mSp;

    private static final float MS2S = 1.0f / 1000.0f;
    private float timestamp;
    private static final float Gyro_Sensitivity = 131.0f; // LSB/(º/s)
    private static float FullScale_Range = 300.0f; // º/s

    private float[] angle= new float[3];
    private final String TAG = "GyRoscopeSensor";
    boolean mCheckDataSuccess;
    private byte okFlag = 0x00;

    @Override
    public void OnServiceConnectedHandler(ComponentName componentName, IBinder iBinder) {
        SendCommand(Utils.CMD_RECV_SENSOR_DATA);
    }

    private Handler handler = new Handler();

    @Override
    protected void OnSensorDataChangedHandler(final SensorPacketDataObject object) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                /*final float EMAOFFSET = 0.04f;
                final float gyroRawOffset = 0.0f;
                float gOffset = 0;
                float gyroSpeed = 0;
                float[] gyroRaw = new float[3];
                int[] values = object.getPacketDataGyro();

                gyroRaw[0] = (values[0] - gyroRawOffset) / Gyro_Sensitivity;
                gyroRaw[1] = (values[1] - gyroRawOffset) / Gyro_Sensitivity;
                gyroRaw[2] = (values[2] - gyroRawOffset) / Gyro_Sensitivity;
                //Log.d(TAG,String.format("X: % d Y: % d Z: % d ",object.gyroscopeSensor.getX(),object.gyroscopeSensor.getY(),object.gyroscopeSensor.getZ()));

                if (timestamp != 0) {
                    final float dT = (object.getPacketDataTimestamp() - timestamp) * MS2S;
                    for (int i = 0; i < gyroRaw.length; i++) {
                        gOffset = EMAOFFSET * gyroRaw[i] + (1 - EMAOFFSET) * gOffset;
                        gyroSpeed = gyroRaw[i] - gOffset;
                        angle[i]  += gyroSpeed * dT;
                    }
                    //Log.d(TAG,String.format("X: %+f Y: %+f Z: %+f ",angle[0],angle[1],angle[2]));
                    tvdata.setText(String.format("%s:%nX:%+f%nY:%+f%nZ:%+f%n", getString(R.string.gyroscopesensor_value), angle[0], angle[1], angle[2]));
                }
                timestamp = object.getPacketDataTimestamp();//*/
                ///*
                int[] values = object.getPacketDataGyro();
                angle[0] = (float) values[0] / Gyro_Sensitivity;
                angle[1] = (float) values[1] / Gyro_Sensitivity;
                angle[2] = (float) values[2] / Gyro_Sensitivity;
                tvdata.setText(String.format(Locale.US, "%s:%nX: %+f%nY: %+f%nZ: %+f%n",
                        getString(R.string.gyroscopesensor_value), angle[0], angle[1], angle[2]));

                if (Math.abs(angle[0]) > FullScale_Range
                        || Math.abs(angle[1]) > FullScale_Range || Math.abs(angle[2]) > FullScale_Range) {
                    Log.d(TAG,String.format("X: %+f Y: %+f Z: %+f ",angle[0],angle[1],angle[2]));
                    okFlag |= 0x80;
                } else {
                    okFlag |= 0x08;
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.gyroscopesensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        tvdata = (TextView) findViewById(R.id.gyroscopesensor);
        tvdata.setText(String.format(Locale.US,"%s:%nX: %+f%nY: %+f%nZ: %+f%n",
                getString(R.string.gyroscopesensor_value), angle[0], angle[1], angle[2]));
        mBtOk = (Button) findViewById(R.id.gyroscopesensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gyroscopesensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        FullScale_Range = MJDeviceTestApp.Gyro_FullScale_Range;
        if (MJDeviceTestApp.IsFactoryMode) {
            FullScale_Range = FullScale_Range / 300.0f;
        }

        mCheckDataSuccess = false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if ((okFlag & 0x80) == 0x80) {
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                    mCheckDataSuccess = false;
                } else if ((okFlag & 0x08) == 0x08) {
                    tvdata.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                }
                SaveToReport();
            }
        }, 5 * 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckDataSuccess) {
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }
                SaveToReport();
            }
        }, MJDeviceTestApp.ItemTestTimeout * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    public void onClick(View v) {

        UtilTools.SetPreferences(this, mSp, R.string.gyroscopesensor_name, (v
                .getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS
                : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.gyroscopesensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}