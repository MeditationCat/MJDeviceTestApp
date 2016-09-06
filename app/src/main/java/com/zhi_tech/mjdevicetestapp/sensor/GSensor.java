package com.zhi_tech.mjdevicetestapp.sensor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.zhi_tech.magiceyessdk.MagicEyesActivity;
import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;
import com.zhi_tech.magiceyessdk.Utils.Utils;
import com.zhi_tech.mjdevicetestapp.AppDefine;
import com.zhi_tech.mjdevicetestapp.MJDeviceTestApp;
import com.zhi_tech.mjdevicetestapp.R;
import com.zhi_tech.mjdevicetestapp.UtilTools;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created by taipp on 5/20/2016.
 */
public class GSensor extends MagicEyesActivity implements View.OnClickListener {

    private Button mBtCalibrate;
    private TextView tvdata;
    private ImageView ivimg;
    private Button mBtOk;
    private Button mBtFailed;

    private float mX;
    private float mY;
    private float mZ;

    SharedPreferences mSp;
    boolean mCheckDataSuccess;
    private final static int OFFSET = 2;
    private final static int Accl_Sensitivity = 16384; // LSB/g
    private final static float Gravity = (float) 9.8; // m/s²
    private static int FullScale_Range = 3; // g
    private byte okFlag = 0x00;
    private int[] valueFlag = new int[3];
    private int errorCount = 10;
    private boolean reportIsSaved;

    private final String TAG = "GSensor";

    @Override
    public void OnServiceConnectedHandler(ComponentName componentName, IBinder iBinder) {
        super.OnServiceConnectedHandler(componentName, iBinder);
        SendCommand(Utils.CMD_RECV_SENSOR_DATA);
    }

    @Override
    public void OnServiceDisconnectedHandler(ComponentName componentName) {
        super.OnServiceDisconnectedHandler(componentName);
    }

    private Handler handler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            tvdata.setText(String.format(Locale.US,"%s:%nX: %+f%nY: %+f%nZ: %+f%n",getString(R.string.GSensor), mX, mY, mZ));
            if (MJDeviceTestApp.IsFactoryMode) {
                if (Math.abs(mX) < FullScale_Range && Math.abs(mY) < FullScale_Range
                        && Math.abs(Math.abs(mZ) - Gravity) < FullScale_Range) {
                    okFlag |= 0x01;
                    ivimg.setBackgroundResource(R.drawable.gsensor_z);
                }
                if (Math.abs(mX) < FullScale_Range && Math.abs(mZ) < FullScale_Range
                        && Math.abs(Math.abs(mY) - Gravity) < FullScale_Range) {
                    okFlag |= 0x02;
                    ivimg.setBackgroundResource(mY > 0 ? R.drawable.gsensor_y : R.drawable.gsensor_2y);
                }
                if (Math.abs(mZ) < FullScale_Range && Math.abs(mY) < FullScale_Range
                        && Math.abs(Math.abs(mX) - Gravity) < FullScale_Range) {
                    okFlag |= 0x04;
                    ivimg.setBackgroundResource(mX > 0 ? R.drawable.gsensor_x : R.drawable.gsensor_x_2);
                }
            } else {
                if (Math.abs(mX) > Math.abs(mY) && Math.abs(mX) - OFFSET > Math.abs(mZ)) {
                    ivimg.setBackgroundResource(mX > 0? R.drawable.gsensor_x : R.drawable.gsensor_x_2);
                    okFlag |= 0x01;
                } else if (Math.abs(mY) - OFFSET > Math.abs(mX) && Math.abs(mY) - OFFSET > Math.abs(mZ)) {
                    ivimg.setBackgroundResource(mY > 0? R.drawable.gsensor_y : R.drawable.gsensor_2y);
                    okFlag |= 0x02;
                } else if (Math.abs(mZ) > Math.abs(mX) && Math.abs(mZ) > Math.abs(mY)) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_z);
                    okFlag |= 0x04;
                }
            }

            if (okFlag == 0x07 && !mCheckDataSuccess) {
                tvdata.setTextColor(Color.GREEN);
                mBtFailed.setBackgroundColor(Color.GRAY);
                mBtOk.setBackgroundColor(Color.GREEN);
                mCheckDataSuccess = true;
                SaveToReport();
            }
        }
    };

    @Override
    protected void OnSensorDataChangedHandler(SensorPacketDataObject object) {
        int[] values = object.getPacketDataAccel();
        if (!Arrays.equals(valueFlag, values)) {
            System.arraycopy(values, 0, valueFlag, 0, valueFlag.length);
        } else if (errorCount >= 0) {
            if (errorCount == 0) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCheckDataSuccess = false;
                        tvdata.setTextColor(Color.RED);
                        mBtFailed.setBackgroundColor(Color.RED);
                        mBtOk.setBackgroundColor(Color.GRAY);
                        SaveToReport();
                    }
                });
            }
            errorCount--;
        }
        mX = values[0] * Gravity / Accl_Sensitivity;
        mY = values[1] * Gravity / Accl_Sensitivity;
        mZ = values[2] * Gravity / Accl_Sensitivity;

        handler.post(runnable);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.gsensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mBtCalibrate = (Button) findViewById(R.id.gsensor_calibrate);
        mBtCalibrate.setOnClickListener(this);
        mBtCalibrate.setVisibility(View.GONE);
        tvdata = (TextView) findViewById(R.id.gsensor_tv_data);
        tvdata.setText(String.format(Locale.US,"%s:%nX: %+f%nY: %+f%nZ: %+f%n",getString(R.string.GSensor), 0.0f, 0.0f, 0.0f));
        ivimg = (ImageView) findViewById(R.id.gsensor_iv_img);
        mBtOk = (Button) findViewById(R.id.gsensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);
        FullScale_Range = MJDeviceTestApp.Accel_FullScale_Range;

        mCheckDataSuccess = false;
        reportIsSaved = false;
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
        mCheckDataSuccess = false;
    }

    @Override
    public void onClick(View v) {
        UtilTools.SetPreferences(this, mSp, R.string.gsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        if (reportIsSaved) {
            return;
        }
        UtilTools.SetPreferences(this, mSp, R.string.gsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}