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
public class LSensor extends MagicEyesActivity {
    /** Called when the activity is first created. */
    TextView mAccuracyView = null;
    TextView mValueX = null;
    Button mBtOk;
    Button mBtFailed;
    SharedPreferences mSp;
    private final String TAG = "LSensor";
    private byte okFlag = 0x00;
    boolean mCheckDataSuccess;
    public static int Light_Threshold_Approach = 100; //
    public static int Light_Threshold_Leave = 1200; //

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
                int value = object.getPacketDataLSensor();
                mValueX.setText(String.format(Locale.US, "%s%d", getString(R.string.LSensor_value) ,value));
                if (value < Light_Threshold_Approach) {
                    okFlag |= 0x01;
                }
                if (value > Light_Threshold_Leave) {
                    okFlag |= 0x02;
                }
                if (okFlag == 0x03 && !mCheckDataSuccess) {
                    mValueX.setTextColor(Color.GREEN);
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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        setContentView(R.layout.lsensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mAccuracyView = (TextView) findViewById(R.id.lsensor_accuracy);
        mAccuracyView.setText(String.format(Locale.US, "%s%s", getString(R.string.LSensor), getString(R.string.LSensor_accuracy)));
        mValueX = (TextView) findViewById(R.id.lsensor_value);
        mBtOk = (Button) findViewById(R.id.lsensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.lsensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        Light_Threshold_Approach = MJDeviceTestApp.Light_Threshold_Approach;

        mCheckDataSuccess = false;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckDataSuccess) {
                    mValueX.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }
                SaveToReport();
            }
        }, MJDeviceTestApp.ItemTestTimeout * 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            UtilTools.SetPreferences(getApplicationContext(), mSp, R.string.lsensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
            finish();
        }
    };

    public void SaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.lsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}