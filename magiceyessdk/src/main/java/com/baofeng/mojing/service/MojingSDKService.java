package com.baofeng.mojing.service;

import com.zhi_tech.magiceyessdk.Utils.IMojingSDKServiceMsg;
import com.zhi_tech.magiceyessdk.Utils.Utils;

/**
 * Created by taipp on 7/27/2016.
 */
public class MojingSDKService {

    private final static String TAG = "MojingSDKService";
    private IMojingSDKServiceMsg iMojingSDKServiceMsg;
    private static boolean Match_Flag_Usb_Create = false;
    private static boolean Match_Flag_Usb_Connect = false;

    static {
        System.loadLibrary("mojingservice");
    }

    public void setiMojingSDKServiceMsg(IMojingSDKServiceMsg iMojingSDKServiceMsg) {
        this.iMojingSDKServiceMsg = iMojingSDKServiceMsg;
    }

    public void removeiMojingSDKServiceMsg() {
        this.iMojingSDKServiceMsg = null;
    }

    public void OnNativeConnectUsb(String deviceName, int fd) {
        OnNativeReleaseUsb();
        if (!Match_Flag_Usb_Connect) {
            Match_Flag_Usb_Connect = true;
            ConnectUsb(deviceName, fd);
        }
    }

    public void OnNativeReleaseUsb() {
        if (Match_Flag_Usb_Connect) {
            Match_Flag_Usb_Connect = false;
            ReleaseUsb();
        }
    }

    public void OnNativeCreateObject() {
        if (!Match_Flag_Usb_Create) {
            Match_Flag_Usb_Create = true;
            OnNativeCreate();
        }
    }

    public void OnNativeDestroyObject() {
        if (Match_Flag_Usb_Create) {
            Match_Flag_Usb_Create = false;
            OnNativeDestroy();
        }
    }

    private void reportMsg(String reportMsg) {
        Utils.dLog(TAG, reportMsg);
        if (iMojingSDKServiceMsg != null) {
            iMojingSDKServiceMsg.JNI_reportMsg_Callback(reportMsg);
        }
    }

    private native void OnNativeCreate();

    private native void OnNativeDestroy();

    private native void ConnectUsb(String paramString, int paramInt);

    private native void ReleaseUsb();

    public native void StartSensor(String paramString);

    public native void StopSensor();

    public native int GetBLEVersion();

    public native long GetFlags();

    public native int GetMCUVersion();

    public native String GetSN();

    public native float GetUpgradeProgress();

    public native boolean IsRebooting();

    public native boolean IsUpgrading();

    public native void StartUpgrade(byte[] paramArrayOfByte, int paramInt);

}
