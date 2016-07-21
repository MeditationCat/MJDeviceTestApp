package com.zhi_tech.mjdevicetestapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by taipp on 5/23/2016.
 */
public class AutoTest extends Activity {

    private final String TAG = "AutoTest";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        setContentView(R.layout.autotest);

        Intent intent = new Intent();
        if (MJDeviceTestApp.IsFactoryMode) {
            intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.GSensor");
            this.startActivityForResult(intent, AppDefine.DT_GSENSORID);
        } else {
            intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.KeyCode");
            this.startActivityForResult(intent, AppDefine.DT_KEYCODEID);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " requestCode:" + requestCode + "resultCode:" + resultCode);
        Intent intent = new Intent();
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestid = -1;
        if (resultCode == RESULT_FIRST_USER) {
            finish();
            return;
        }
        switch (requestCode) {
            case AppDefine.DT_KEYCODEID:
                intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.GSensor");
                requestid = AppDefine.DT_GSENSORID;
                break;
            case AppDefine.DT_GSENSORID:
                intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.MSensor");
                requestid = AppDefine.DT_MSENSORID;
                break;
            case AppDefine.DT_MSENSORID:
                if (MJDeviceTestApp.IsFactoryMode) {
                    intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.GyRoscopeSensor");
                    requestid = AppDefine.DT_GYROSCOPESENSORID;
                } else {
                    intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.LSensor");
                    requestid = AppDefine.DT_LSENSORID;
                }
                break;
            case AppDefine.DT_LSENSORID:
                intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.PSensor");
                requestid = AppDefine.DT_PSENSORID;
                break;
            case AppDefine.DT_PSENSORID:
                intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.GyRoscopeSensor");
                requestid = AppDefine.DT_GYROSCOPESENSORID;
                break;
            case AppDefine.DT_GYROSCOPESENSORID:
                intent.setClassName(this, "com.zhi_tech.mjdevicetestapp.sensor.TSensor");
                requestid = AppDefine.DT_TSENSORID;
                break;
            case AppDefine.DT_TSENSORID:
                finish();
                return;
            default:
                break;
        }
        this.startActivityForResult(intent, requestid);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }
}
