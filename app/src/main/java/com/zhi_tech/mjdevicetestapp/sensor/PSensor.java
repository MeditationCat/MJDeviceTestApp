package com.zhi_tech.mjdevicetestapp.sensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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
public class PSensor extends MagicEyesActivity implements View.OnClickListener {

    private Button mBtOk;
    private Button mBtFailed;
    private TextView mPsensor;
    public final String TAG = "PSensor";
    private byte okFlag = 0x00;
    private static int Proximity_Threshold_Leave = 200;
    private static int Proximity_Threshold_Approach = 820;
    boolean mCheckDataSuccess;

    CountDownTimer mCountDownTimer;
    SharedPreferences mSp;

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

    @Override
    protected void OnSensorDataChangedHandler(final SensorPacketDataObject object) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                int value = object.getPacketDataPSensor();
                mPsensor.setText(String.format(Locale.US, "%s:%n%s %d",
                        getString(R.string.psensor_hello), getString(R.string.proximity), value));
                if (value < Proximity_Threshold_Leave) {
                    //leave away
                    okFlag |= 0x01;
                }
                if (value > Proximity_Threshold_Approach) {
                    //approach
                    okFlag |= 0x02;
                }
                if (okFlag == 0x03) {
                    mPsensor.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                    SaveToReport();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.psensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.psensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.psensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        mPsensor = (TextView) findViewById(R.id.proximity);
        mPsensor.setText(String.format(Locale.US, "%s:%n%s %d", getString(R.string.psensor_hello), getString(R.string.proximity), 0));

        Proximity_Threshold_Approach = MJDeviceTestApp.Proximity_Threshold_Approach;

        mCheckDataSuccess = false;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckDataSuccess) {
                    mPsensor.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }
                SaveToReport();
            }
        }, MJDeviceTestApp.ItemTestTimeout * 1000);
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        UtilTools.SetPreferences(this, mSp, R.string.psensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.psensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}