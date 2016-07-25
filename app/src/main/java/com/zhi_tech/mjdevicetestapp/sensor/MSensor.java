package com.zhi_tech.mjdevicetestapp.sensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
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

import java.util.Locale;

/**
 * Created by taipp on 5/20/2016.
 */

public class MSensor extends MagicEyesActivity {

    private ImageView mImgCompass = null;
    private TextView mOrientText = null;
    private TextView mOrientValue = null;
    private RotateAnimation mMyAni = null;
    private float mDegressQuondam = -1.0f;
    private int Magnetic_Yaw_Offset = 90; // º
    private SharedPreferences mSp;
    private Button mBtOk;
    private Button mBtFailed;
    private final String TAG = "MSensor";
    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private float[] mOrientation = new float[3];
    private int[] values = new int[3];
    boolean mCheckDataSuccess = false;

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

    private void postUpdateHandlerMsg(final SensorPacketDataObject object) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (null == mOrientText || null == mOrientValue || null == mImgCompass) {
                    return;
                }
                int[] data = object.getPacketDataMSensor();
                float Mx = data[0];
                float My = data[1];
                float Mz = data[2];
                mOrientValue.setText(String.format(Locale.US, "%s:%nX: %+f%nY: %+f%nZ: %+f%n", getString(R.string.MSensor), Mx, My, Mz));
                //Log.d(TAG,String.format(" %s%n:X: %+d Y: %+d Z: %+d ", getString(R.string.MSensor), (int) Mx, (int) My, (int) Mz));
                float azimuth = (float) (Math.atan2(Mx, My) * (180 / Math.PI));
                if (azimuth < 0) {
                    azimuth = 360 - Math.abs(azimuth);
                }

                float pitch = (float) (Math.atan2(My, Mz) * 180 / Math.PI);
                float roll = (float) (Math.atan2(Mx, Mz) * 180 / Math.PI);
                if (roll > 90) {
                    roll = -(180 - roll);
                } else if (roll < -90) {
                    roll = 180 + roll;
                }
                float[] values = new float[3];
                values[0] = azimuth;
                values[1] = pitch;
                values[2] = roll;
                Log.d(TAG,String.format("azimuth: %d pitch: %d roll:%d", (int) values[0], (int) values[1], (int) values[2]));

                if (Math.abs(values[0] - mDegressQuondam) < 360 / 72) {
                    return;
                }

                if (Math.abs(values[0] - Math.abs(mDegressQuondam)) > Magnetic_Yaw_Offset
                        && !mCheckDataSuccess
                        && mDegressQuondam != -1.0f) {
                    mOrientText.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                    SaveToReport();
                }

                switch ((int) values[0]) {
                    case 0: // North
                        mOrientText.setText(R.string.MSensor_North);
                        break;
                    case 90: // East
                        mOrientText.setText(R.string.MSensor_East);
                        break;
                    case 180: // South
                        mOrientText.setText(R.string.MSensor_South);
                        break;
                    case 270: // West
                        mOrientText.setText(R.string.MSensor_West);
                        break;
                    default: {
                        int v = (int) values[0];
                        if (v > 0 && v < 90) {
                            mOrientText.setText(getString(R.string.MSensor_north_east) + String.format(" %02d °", v));
                        }

                        if (v > 90 && v < 180) {
                            v = 180 - v;
                            mOrientText.setText(getString(R.string.MSensor_south_east) + String.format(" %02d °", v));
                        }

                        if (v > 180 && v < 270) {
                            v = v - 180;
                            mOrientText.setText(getString(R.string.MSensor_south_west) + String.format(" %02d °", v));
                        }
                        if (v > 270 && v < 360) {
                            v = 360 - v;
                            mOrientText.setText(getString(R.string.MSensor_north_west) + String.format(" %02d °", v));
                        }
                    }
                }
                if (mDegressQuondam != -values[0])
                    AniRotateImage(-values[0]);
            }
        });
    }

    private void postUpdateHandlerMsg2(final SensorPacketDataObject object) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (null == mOrientText || null == mOrientValue || null == mImgCompass) {
                    return;
                }
                int[] values = object.getPacketDataMSensor();
                mMData[0] = values[0];
                mMData[1] = values[1];
                mMData[2] = values[2];
                values = object.getPacketDataAccel();
                mGData[0] = values[0];
                mGData[1] = values[1];
                mGData[2] = values[2];

                SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
                SensorManager.getOrientation(mR, mOrientation);
                float incl = SensorManager.getInclination(mI);

                mOrientValue.setText(String.format(Locale.US, "%s:%nX: %+f%nY: %+f%nZ: %+f%n", getString(R.string.MSensor), mMData[0], mMData[1], mMData[2]));

                final float rad2deg = (float)(180.0f/Math.PI);

                values[0] = (int)(mOrientation[0]*rad2deg);
                values[1] = (int)(mOrientation[1]*rad2deg);
                values[2] = (int)(mOrientation[2]*rad2deg);

                Log.d(TAG,String.format("Compass: Yaw:%d, Pitch:%d, Roll:%d, Incl:%f", values[0], values[1], values[2], incl));

                if (values[0] < 0) {
                    values[0] = 360 - values[0];
                }

                if (Math.abs(values[0] - mDegressQuondam) < 360 / 72) {
                    return;
                }

                if (Math.abs(values[0] - Math.abs(mDegressQuondam)) > Magnetic_Yaw_Offset
                        && !mCheckDataSuccess
                        && mDegressQuondam != -1.0f) {
                    mOrientText.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                    SaveToReport();
                }

                switch ((int) values[0]) {
                    case 0: // North
                        mOrientText.setText(R.string.MSensor_North);
                        break;
                    case 90: // East
                        mOrientText.setText(R.string.MSensor_East);
                        break;
                    case 180: // South
                        mOrientText.setText(R.string.MSensor_South);
                        break;
                    case 270: // West
                        mOrientText.setText(R.string.MSensor_West);
                        break;
                    default: {
                        int v = (int) values[0];
                        if (v > 0 && v < 90) {
                            mOrientText.setText(getString(R.string.MSensor_north_east) + String.format(" %02d °", v));
                        }

                        if (v > 90 && v < 180) {
                            v = 180 - v;
                            mOrientText.setText(getString(R.string.MSensor_south_east) + String.format(" %02d °", v));
                        }

                        if (v > 180 && v < 270) {
                            v = v - 180;
                            mOrientText.setText(getString(R.string.MSensor_south_west) + String.format(" %02d °", v));
                        }
                        if (v > 270 && v < 360) {
                            v = 360 - v;
                            mOrientText.setText(getString(R.string.MSensor_north_west) + String.format(" %02d °", v));
                        }
                    }
                }
                if (mDegressQuondam != -values[0]) {
                    AniRotateImage(-values[0]);
                }
            }
        });
    }

    @Override
    protected void OnSensorDataChangedHandler(SensorPacketDataObject object) {
        postUpdateHandlerMsg(object);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.msensor);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mOrientText = (TextView) findViewById(R.id.OrientText);
        mImgCompass = (ImageView) findViewById(R.id.ivCompass);
        mOrientValue = (TextView) findViewById(R.id.OrientValue);
        mOrientValue.setText(String.format(Locale.US, "%s:%nX: %+f%nY: %+f%nZ: %+f%n", getString(R.string.MSensor), 0.0f, 0.0f, 0.0f));
        mBtOk = (Button) findViewById(R.id.msensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.msensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        Magnetic_Yaw_Offset = MJDeviceTestApp.Magnetic_Yaw_Offset;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckDataSuccess) {
                    mOrientText.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }
                SaveToReport();
            }
        }, MJDeviceTestApp.ItemTestTimeout * 1000);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    private void AniRotateImage(float fDegress) {
        if (Math.abs(fDegress - mDegressQuondam) < 1) {
            return;
        }
        mMyAni = new RotateAnimation(mDegressQuondam, fDegress, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mMyAni.setDuration(200);
        mMyAni.setFillAfter(true);

        mImgCompass.startAnimation(mMyAni);
        mDegressQuondam = fDegress;
    }

    private View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            UtilTools.SetPreferences(getApplicationContext(), mSp, R.string.msensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
            finish();
        }
    };

    public void SaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.msensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }
}
