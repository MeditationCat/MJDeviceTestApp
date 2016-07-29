package com.zhi_tech.magiceyessdk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;
import com.zhi_tech.magiceyessdk.Utils.Utils;

/**
 * Created by taipp on 7/8/2016.
 */
public class MagicEyesActivity extends AppCompatActivity {

    private static final String TAG = "MagicEyesActivity";

    private static IMagicEyesAidl iMagicEyesAidl = null;

    private IPacketDataAidl.Stub iPacketDataAidlStub = new IPacketDataAidl.Stub() {
        @Override
        public void OnSensorDataChanged(SensorPacketDataObject object) throws RemoteException {
            OnSensorDataChangedHandler(object);
        }

        @Override
        public void OnTouchPadActonEvent(int[] values) throws RemoteException {
            OnTouchPadActonEventHandler(values);
        }
    };

    private ICommandResultAidl.Stub iCommandResultAidlStub = new ICommandResultAidl.Stub() {
        @Override
        public void OnCommandResultChanged(int cmd, byte[] data, int length) throws RemoteException {
            OnCommandResultChangedHandler(cmd, data, length);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OnServiceConnectedHandler(componentName, iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            OnServiceDisconnectedHandler(componentName);
        }
    };

    //public method for aidl
    //set device pid vid
    public static void SetDeviceFilter(int vendorId, int productId) {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteSetDeviceFilter(vendorId, productId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void SetBulkTransferTimeout(int timeout) {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteSetBulkTransferTimeout(timeout);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void SetReceiveDataGapTime(int gaptime) {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteSetReceiveDataGapTime(gaptime);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void SetMainActivityClassName(String className) {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteSetMainActivityClassName(className);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static String GetDeviceInfo() {
        if (iMagicEyesAidl != null) {
            try {
                return iMagicEyesAidl.RemoteGetDeviceInfo();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    //connect to the device
    public static void ConnectToDevice() {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteConnectToDevice();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    //send command to device
    public static int SendCommand(int cmd) {
        int retValue = -1;
        if (iMagicEyesAidl != null) {
            try {
                retValue = iMagicEyesAidl.RemoteSendCommand(cmd);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return retValue;
    }

    public static int SendCommandWithData(int cmd, byte[] data) {
        int retValue = -1;
        if (iMagicEyesAidl != null) {
            try {
                retValue = iMagicEyesAidl.RemoteSendCommandWithData(cmd, data);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return retValue;
    }
    //daemo thread control
    public static boolean CheckServiceStatus() {
        boolean flag = false;
        try {
            flag = iMagicEyesAidl.RemoteCheckServiceListenerThreadIsRunnig();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return flag;
    }
    public static void RestartService() {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteRestartServiceListenerThread();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //public method for override
    protected void OnSensorDataChangedHandler(SensorPacketDataObject object) {

    }

    protected void OnTouchPadActonEventHandler(int[] values) {

    }

    protected void OnCommandResultChangedHandler(int cmd, byte[] data, int length) {

    }

    public void OnServiceConnectedHandler(ComponentName componentName, IBinder iBinder) {
        iMagicEyesAidl = IMagicEyesAidl.Stub.asInterface(iBinder);
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteRegisterOnPacketDataListener(iPacketDataAidlStub);
                iMagicEyesAidl.RemoteRegisterOnCommandResultListener(iCommandResultAidlStub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void OnServiceDisconnectedHandler(ComponentName componentName) {
        if (iMagicEyesAidl != null) {
            try {
                iMagicEyesAidl.RemoteUnregisterOnPacketDataListener(iPacketDataAidlStub);
                iMagicEyesAidl.RemoteUnregisterOnCommandResultListener(iCommandResultAidlStub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        iMagicEyesAidl = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.dLog(TAG);
        super.onCreate(savedInstanceState);
        //start service
        startService(new Intent(this, MagicEyesService.class));
        //bind remote service
        Intent binderIntent = new Intent(this, MagicEyesService.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("create_flag", true);
        binderIntent.putExtras(bundle);
        bindService(binderIntent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        Utils.dLog(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Utils.dLog(TAG);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Utils.dLog(TAG);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Utils.dLog(TAG);
        super.onDestroy();
        unbindService(connection);
        //stopService(new Intent(this, MagicEyesService.class));
    }
}
