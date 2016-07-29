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
 * Created by Tiger on 6/5/2016.
 */
public class TSensor extends MagicEyesActivity implements View.OnClickListener {
    private TextView tvdata;
    private Button mBtOk;
    private Button mBtFailed;
    SharedPreferences mSp;
    boolean mCheckDataSuccess;
    private byte okFlag = 0x00;
    private int[] valueFlag = new int[3];
    private int errorCount = 3;
    private boolean reportIsSaved;

    private final String TAG = "TSensor";
    public static final float Temp_Sensitivity = (float) 326.8; //LSB/ºC
    public static final int RoomTemp_Offset = 25; //ºC
    public static int Temperature_Range_Min = -40; //ºC
    public static int Temperature_Range_Max = 85; //ºC
    /*
    TEMP_degC = (TEMP_OUT[15:0]/Temp_Sensitivity)
            + RoomTemp_Offset
    where Temp_Sensitivity = 326.8
    LSB/ºC and RoomTemp_Offset = 25ºC
    */
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
                int rawTemp = object.getPacketDataTemp();
                float TEMP_degC = (rawTemp / Temp_Sensitivity) + RoomTemp_Offset;
                tvdata.setText(String.format(Locale.US, "%.02f",TEMP_degC));
                //Log.d(TAG, String.format("TEMP_degC = %.02f",TEMP_degC));
                if (errorCount > 0) {
                    errorCount--;
                    if (valueFlag[errorCount] != rawTemp) {
                        valueFlag[errorCount] = rawTemp;
                    }
                } else {
                    if (valueFlag[0] == valueFlag[1] && valueFlag[1] == valueFlag[2]) {
                        mCheckDataSuccess = false;
                        tvdata.setTextColor(Color.RED);
                        mBtFailed.setBackgroundColor(Color.RED);
                        mBtOk.setBackgroundColor(Color.GRAY);
                        SaveToReport();
                        return;
                    }
                }

                if (TEMP_degC < Temperature_Range_Min || TEMP_degC > Temperature_Range_Max) {
                    okFlag |= 0x80;
                } else {
                    okFlag |= 0x01;
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.tsensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        tvdata = (TextView) findViewById(R.id.textView_tsensor_data);
        mBtOk = (Button) findViewById(R.id.tsensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.tsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        Temperature_Range_Min = MJDeviceTestApp.Temperature_Range_Min;
        Temperature_Range_Max = MJDeviceTestApp.Temperature_Range_Max;

        mCheckDataSuccess = false;
        reportIsSaved = false;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if ((okFlag & 0x80) == 0x80) {
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                    mCheckDataSuccess = false;
                } else if ((okFlag & 0x01) == 0x01) {
                    tvdata.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                }
                SaveToReport();
            }
        }, 10 * 1000);

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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        UtilTools.SetPreferences(this, mSp, R.string.tsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        if (reportIsSaved) {
            return;
        }
        UtilTools.SetPreferences(this, mSp, R.string.tsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}