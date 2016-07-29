package com.zhi_tech.magiceyessdk.Utils;

import android.util.Log;

/**
 * Created by taipp on 7/8/2016.
 */
public class Utils {
    public static final String ACTION_KILL_SELF = "com.android.example.KILL_SELF";
    // cmd
    public final static int CMD_IAP_UPGRADE = 0xA0;
    public final static int CMD_G_CALIBRATE = 0x2B;
    public final static int CMD_CHECK_VERSION = 0xB1;
    public final static int CMD_RECV_SENSOR_DATA = 0x0B;
    public final static int CMD_STOP_SENSOR_DATA = 0x07;
    public final static int CMD_RECV_TP_EVENT = 0xB3;
    public final static int CMD_STOP_TP_EVENT = 0xB9;
    public final static int CMD_WRITE_SN = 0xBA;
    public final static int CMD_READ_SN = 0xBC;
    public final static int CMD_WRITE_BLE_MAC = 0xB5;
    public final static int CMD_READ_BLE_MAC = 0xB7;


    public static void dLog(String Tag) {
        Log.d(Tag, Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    public static void dLog(String Tag, String log) {
        Log.d(Tag, Thread.currentThread().getStackTrace()[3].getMethodName() + "->" + log);
    }
}