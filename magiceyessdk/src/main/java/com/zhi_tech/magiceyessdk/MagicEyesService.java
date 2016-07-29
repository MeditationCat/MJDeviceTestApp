package com.zhi_tech.magiceyessdk;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.baofeng.mojing.service.MojingSDKService;
import com.zhi_tech.magiceyessdk.Utils.IMojingSDKServiceMsg;
import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;
import com.zhi_tech.magiceyessdk.Utils.UsbDeviceFilter;
import com.zhi_tech.magiceyessdk.Utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MagicEyesService extends Service {

    private static final String TAG = "MagicEyesService";

    private static boolean runningFlag = false;
    private static boolean handShakeSuccess = false;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static String mainActivityClassName = null;

    private static int mVendorId = 0;//0x2d29; //11561;
    private static int mProductId = 0;//0x1001; //4097;
    //JNI call obj
    private MojingSDKService JNI_Obj;

    private int mCommandTemp = -1;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private List<UsbInterface> usbInterfaceList;
    private UsbInterface usbInterface;
    private UsbEndpoint EPCTRL;
    private UsbEndpoint[] EPIN, EPOUT;
    private UsbDeviceConnection usbConn;
    private static int BULK_TRANSFER_TIMEOUT = 1000; // ms
    private static int RECV_DATA_GAP_TIME = 50; // ms
    private PendingIntent pendingIntent;

    private Thread PacketDataListenerThread;
    private Thread CommandResultListenerThread;
    private Thread SendCommandLooperThread;

    private Handler myHandler;

    class MyLooperThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            myHandler = new Handler();
            Looper.loop();
        }
    }

    public MagicEyesService() {
    }

    @Override
    public void onCreate() {
        Utils.dLog(TAG);
        super.onCreate();
        int MaxEpCount = 0x0E;
        usbInterfaceList = new ArrayList<>();
        EPIN = new UsbEndpoint[MaxEpCount];
        EPOUT = new UsbEndpoint[MaxEpCount];

        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        SendCommandLooperThread = new MyLooperThread();
        SendCommandLooperThread.start();
        //jni
        JNI_Obj = new MojingSDKService();
        JNI_Obj.OnNativeCreateObject();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.dLog(TAG);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Utils.dLog(TAG);
        super.onDestroy();
        runningFlag = false;
        unregisterReceiver(mUsbReceiver);
        mIPacketDataCallbackList.kill();
        mICommandResultCallbackList.kill();
        //jni
        JNI_Obj.OnNativeDestroyObject();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Utils.dLog(TAG);
        Bundle bundle = intent.getExtras();
        runningFlag = bundle.getBoolean("create_flag", false);
        startServiceListenerThread();
        return mStub;
    }

    final RemoteCallbackList<IPacketDataAidl> mIPacketDataCallbackList = new RemoteCallbackList<IPacketDataAidl>();

    public void registerOnPacketDataListener(IPacketDataAidl onPacketDataListener) {
        if (onPacketDataListener != null) {
            mIPacketDataCallbackList.register(onPacketDataListener);
        }
    }

    public void unregisterOnPacketDataListener(IPacketDataAidl onPacketDataListener) {
        if (onPacketDataListener != null) {
            mIPacketDataCallbackList.unregister(onPacketDataListener);
        }
    }

    private void OnSensorDataChangedCallback(SensorPacketDataObject object) {
        final int count = mIPacketDataCallbackList.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                mIPacketDataCallbackList.getBroadcastItem(i).OnSensorDataChanged(object);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mIPacketDataCallbackList.finishBroadcast();
    }

    private void OnTouchPadActonEventCallback(int[] values) {
        final int count = mIPacketDataCallbackList.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                mIPacketDataCallbackList.getBroadcastItem(i).OnTouchPadActonEvent(values);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mIPacketDataCallbackList.finishBroadcast();
    }

    final RemoteCallbackList<ICommandResultAidl> mICommandResultCallbackList = new RemoteCallbackList<ICommandResultAidl>();

    public void registerOnCommandResultListener(ICommandResultAidl onCommandResultListener) {
        if (onCommandResultListener != null) {
            mICommandResultCallbackList.register(onCommandResultListener);
        }
    }

    public void unregisterOnCommandResultListener(ICommandResultAidl onCommandResultListener) {
        if (onCommandResultListener != null) {
            mICommandResultCallbackList.unregister(onCommandResultListener);
        }
    }

    private void OnCommandResultChangedCallback(int cmd, byte[] data, int length) {
        final int count = mICommandResultCallbackList.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                mICommandResultCallbackList.getBroadcastItem(i).OnCommandResultChanged(cmd, data, length);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mICommandResultCallbackList.finishBroadcast();
    }

    IMagicEyesAidl.Stub mStub = new IMagicEyesAidl.Stub() {

        @Override
        public void RemoteSetDeviceFilter(int vendorId, int productId) throws RemoteException {
            setDeviceFilter(vendorId, productId);
        }

        @Override
        public void RemoteSetBulkTransferTimeout(int timeout) throws RemoteException {
            BULK_TRANSFER_TIMEOUT = timeout;
        }

        @Override
        public void RemoteSetReceiveDataGapTime(int gaptime) throws RemoteException {
            RECV_DATA_GAP_TIME = gaptime;
        }

        @Override
        public void RemoteConnectToDevice() throws RemoteException {
            connectToDevice();
        }

        @Override
        public void RemoteRegisterOnPacketDataListener(IPacketDataAidl onPacketDataListener) throws RemoteException {
            registerOnPacketDataListener(onPacketDataListener);
        }

        @Override
        public void RemoteRegisterOnCommandResultListener(ICommandResultAidl onCommandResultListener) throws RemoteException {
            registerOnCommandResultListener(onCommandResultListener);
        }

        @Override
        public void RemoteUnregisterOnPacketDataListener(IPacketDataAidl onPacketDataListener) throws RemoteException {
            unregisterOnPacketDataListener(onPacketDataListener);
        }

        @Override
        public void RemoteUnregisterOnCommandResultListener(ICommandResultAidl onCommandResultListener) throws RemoteException {
            unregisterOnCommandResultListener(onCommandResultListener);
        }

        @Override
        public int RemoteSendCommand(int cmd) throws RemoteException {
            return SendCommand(cmd);
        }

        @Override
        public int RemoteSendCommandWithData(int cmd, byte[] data) throws RemoteException {
            return SendCommandWithData(cmd, data);
        }

        @Override
        public boolean RemoteCheckServiceListenerThreadIsRunnig() throws RemoteException {
            return checkServiceListenerThreadIsRunnig();
        }

        @Override
        public void RemoteRestartServiceListenerThread() throws RemoteException {
            restartServiceListenerThread();
        }

        @Override
        public String RemoteGetDeviceInfo() throws RemoteException {
            return getDeviceInfo();
        }

        @Override
        public void RemoteSetMainActivityClassName(String className) throws RemoteException {
            mainActivityClassName = className;
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Utils.dLog(TAG, action);
            synchronized (this) {
                if (ACTION_USB_PERMISSION.equals(action)) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Utils.dLog(TAG, String.format(Locale.US, "extra permission is granted for device[pid:%d, vid:%d]",
                                device.getProductId(), device.getVendorId()));
                        if (device.equals(usbDevice)) {
                            openTargetDevice(device);
                        }
                    } else {
                        Utils.dLog(TAG, String.format(Locale.US, "permission is denied for device[pid:%d, vid:%d]",
                                device.getProductId(), device.getVendorId()));
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    if (device != null && device.equals(usbDevice)) {
                        Utils.dLog(TAG, "connectToDevice()!");
                        //connectToDevice();
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    Utils.dLog(TAG, "close the device connection!");
                    try {
                        if (device != null && device.equals(usbDevice)) {
                            if (usbConn != null) {
                                JNI_Obj.OnNativeReleaseUsb();
                                usbConn.close();
                            }
                            handShakeSuccess = false;
                            initConnection();
                            mIPacketDataCallbackList.kill();
                            mICommandResultCallbackList.kill();
                            //go to main activity
                            if (mainActivityClassName != null) {
                                Intent mainActivity = new Intent();
                                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                mainActivity.setClassName(context, mainActivityClassName);
                                startActivity(mainActivity);
                                sendBroadcast(new Intent(Utils.ACTION_KILL_SELF));
                                Utils.dLog(TAG, "sendBroadcast(new Intent(Utils.ACTION_KILL_SELF)");
                            }
                            stopSelf();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void startServiceListenerThread() {
        runningFlag = true;

        if (PacketDataListenerThread == null) {
            PacketDataListenerThread = new Thread(new PacketDataListenerRunnable(), "PacketDataListenerThread");
            PacketDataListenerThread.start();
        }
        if (CommandResultListenerThread == null) {
            CommandResultListenerThread = new Thread(new CommandResultListenerRunnable(), "CommandResultListenerThread");
            CommandResultListenerThread.start();
        }
    }

        public void restartServiceListenerThread() {
        runningFlag = true;

        if (PacketDataListenerThread != null) {
            PacketDataListenerThread.interrupt();
        }
        if (CommandResultListenerThread != null) {
            CommandResultListenerThread.interrupt();
        }
        PacketDataListenerThread = new Thread(new PacketDataListenerRunnable(), "PacketDataListenerThread");
        CommandResultListenerThread = new Thread(new CommandResultListenerRunnable(), "CommandResultListenerThread");
        PacketDataListenerThread.start();
        CommandResultListenerThread.start();
    }

    public boolean checkServiceListenerThreadIsRunnig() {
        return PacketDataListenerThread.getState().equals(Thread.State.RUNNABLE)
                && CommandResultListenerThread.getState().equals(Thread.State.RUNNABLE);
    }

    /* usb device operation methods */
    private  void initConnection() {
        usbManager = null;
        usbDevice = null;
        usbInterfaceList.clear();
        usbInterface = null;
        for (int i = 0; i < EPIN.length; i++) {
            EPIN[i] = null;
        }
        for (int i = 0; i < EPOUT.length; i++) {
            EPOUT[i] = null;
        }
        EPCTRL = null;
        usbConn = null;
    }

    private void enumerateDevice(Context context, int resourceId) {
        ArrayList<UsbDevice> devices;
        try {
            devices = UsbDeviceFilter.getMatchingHostDevices(context, resourceId);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.dLog(TAG, "Failed to parse device_filter.xml");
            return;
        }
        for (UsbDevice device : devices) {
            usbDevice = device;
            Utils.dLog(TAG, String.format(Locale.US, "Device matched:{pid=%d, vid=%d} name:%s",
                    device.getProductId(), device.getVendorId(), device.getDeviceName()));
        }
    }

    public void setDeviceFilter(int mVendorId, int mProductId) {
        this.mVendorId = mVendorId;
        this.mProductId = mProductId;
    }

    private void enumerateDevice() {
        if (usbManager != null) {
            ArrayList<UsbDevice> devices;
            try {
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                if (!(deviceList.isEmpty())) {
                    for (UsbDevice device : deviceList.values()) {
                        Utils.dLog(TAG, String.format(Locale.US, "device[pid:%d, vid:%d]",
                                device.getProductId(), device.getVendorId()));
                        if (device.getVendorId() == mVendorId && device.getProductId() == mProductId) {
                            usbDevice = device;
                        }
                    }
                } else {
                    Utils.dLog(TAG, "deviceList.isEmpty()!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Utils.dLog(TAG, "usbManager = null!");
        }
    }

    private void getDeviceInterface() {
        if (usbDevice != null) {
            Utils.dLog(TAG, "usbDevice.getInterfaceCount() : " + usbDevice.getInterfaceCount());
            try {
                for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                    UsbInterface intf = usbDevice.getInterface(i);
                    usbInterfaceList.add(intf);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Utils.dLog(TAG, "usbDevice = null!");
        }
    }

    private void setDeviceInterface(List<UsbInterface> interfaceList) {
        if (!interfaceList.isEmpty()) {
            for (int i = 0; i < interfaceList.size(); i++) {
                if (interfaceList.get(i).getId() == 0) {
                    usbInterface = interfaceList.get(i);
                } else {
                    Utils.dLog(TAG, String.format(Locale.US, "interface[%d]: %d", i, interfaceList.get(i).getId()));
                }
            }
        }
    }

    private void assignEndpoint(List<UsbInterface> interfaceList) {
        for (UsbInterface mInterface : interfaceList) {
            if (mInterface != null) {
                try {
                    for (int i = 0; i < mInterface.getEndpointCount(); i++) {
                        UsbEndpoint ep = mInterface.getEndpoint(i);
                        Utils.dLog(TAG,String.format(Locale.US, "EP[%d] =%s",i, ep.toString()));
                        // look for bulk endpoint
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                                EPOUT[ep.getEndpointNumber()] = ep;
                                Utils.dLog(TAG, String.format(Locale.US, "EPOUT[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            } else if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                EPIN[ep.getEndpointNumber()] = ep;
                                Utils.dLog(TAG, String.format(Locale.US, "EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            }
                        }
                        // look for contorl endpoint
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                            EPCTRL = ep;
                            Utils.dLog(TAG, String.format(Locale.US, "EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                        }
                        // look for interrupt endpoint
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                                EPOUT[ep.getEndpointNumber()] = ep;
                                Utils.dLog(TAG, String.format(Locale.US, "EPOUT[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            } else if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                EPIN[ep.getEndpointNumber()] = ep;
                                Utils.dLog(TAG, String.format(Locale.US, "EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean checkDevicePermission(UsbDevice mUsbDevice) {
        if (mUsbDevice != null && usbManager != null) {
            try {
                if (usbManager.hasPermission(mUsbDevice)) {
                    return true;
                } else {
                    usbManager.requestPermission(mUsbDevice, pendingIntent);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private IMojingSDKServiceMsg iMojingSDKServiceMsgHandler = new IMojingSDKServiceMsg() {
        @Override
        public void JNI_reportMsg_Callback(final String reportMsg) {
            //handle reportMsg
            if (handShakeSuccess) {
                return;
            }
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    String flag = "r=";
                    int index = reportMsg.indexOf(flag);
                    char err = reportMsg.charAt(index + flag.length());
                    Utils.dLog(TAG, "========= return code->" + err);
                    if (err == '0' && reportMsg.contains("received 30")) {
                        handShakeSuccess = true;
                        JNI_Obj.removeiMojingSDKServiceMsg();
                        //send handshake ok command
                        byte[] buffer = new byte[2];
                        buffer[0] = 0x01;
                        OnCommandResultChangedCallback(0xE0, buffer, 1);
                        JNI_Obj.StopSensor();
                        if (mCommandTemp != -1) {
                            SendCommand(mCommandTemp);
                            mCommandTemp = -1;
                        }/*
                        JNI_Obj.OnNativeReleaseUsb();
                        if (usbConn != null) {
                            usbConn.close();
                            usbConn = null;
                        }
                        connectToDevice();*/
                    } else if (reportMsg.contains("request failed")) {
                        handShakeSuccess = false;
                        JNI_Obj.removeiMojingSDKServiceMsg();
                        //send handshake ok command
                        byte[] buffer = new byte[2];
                        buffer[0] = 0x00;
                        OnCommandResultChangedCallback(0xE0, buffer, 1);
                    }
                }
            });
        }
    };

    private void openTargetDevice(UsbDevice mUsbDevice) {
        try {
            if (usbConn == null) {
                usbConn = usbManager.openDevice(mUsbDevice);
            }
            if (usbConn != null) {
                if (!handShakeSuccess) {
                    JNI_Obj.OnNativeConnectUsb(mUsbDevice.getDeviceName(), usbConn.getFileDescriptor());
                    JNI_Obj.setiMojingSDKServiceMsg(iMojingSDKServiceMsgHandler);
                    JNI_Obj.StartSensor("mainActivityClassName");
                } else {
                    if (usbInterface != null) {
                        boolean openResult = usbConn.claimInterface(usbInterface, true);
                        Utils.dLog(TAG, String.format(Locale.US, "openResult:%b, usbConn.getSerial():%s ",openResult,  usbConn.getSerial()));
                        if (openResult) {
                            if (mCommandTemp != -1) {
                                SendCommand(mCommandTemp);
                                mCommandTemp = -1;
                            }
                        }
                    } else {
                        Utils.dLog(TAG, "usbInterface = null!");
                    }
                }
            } else {
                Utils.dLog(TAG, "usbConn = null!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToDevice() {
        Utils.dLog(TAG);
        if (deviceIsOpened()) {
            Utils.dLog(TAG, "device has been already connected!");
            return;
        }
        try {
            initConnection();
            usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            enumerateDevice();
            getDeviceInterface();
            if (!usbInterfaceList.isEmpty()) {
                setDeviceInterface(usbInterfaceList);
            }
            assignEndpoint(usbInterfaceList);
            if (checkDevicePermission(usbDevice)) {
                openTargetDevice(usbDevice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDeviceInfo() {
        if (usbDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return usbDevice.getManufacturerName() + "," + usbDevice.getProductName();
            }
        }
        return null;
    }

    private boolean deviceIsOpened() {
        return (usbConn != null && usbConn.getSerial() != null);
    }

    private class PacketDataListenerRunnable implements Runnable {
        @Override
        public void run() {
            int retVal = -1;
            int offset = 0;
            byte[] dataBuffer = null;
            boolean bufferInitFlag = false;
            SensorPacketDataObject sensorPacketDataObject = new SensorPacketDataObject();;

            while (runningFlag) {
                synchronized (this) {
                    if (usbConn != null && EPIN[2] != null && handShakeSuccess) {
                        if (!bufferInitFlag) {
                            dataBuffer = new byte[EPIN[2].getMaxPacketSize()];
                            bufferInitFlag = true;
                        }
                        retVal = usbConn.bulkTransfer(EPIN[2], dataBuffer, dataBuffer.length, BULK_TRANSFER_TIMEOUT);
                    } else {
                        retVal = -1;
                        bufferInitFlag = false;
                    }
                    if (retVal > 0) {
                        sensorPacketDataObject.setSensorPacketDataObject(dataBuffer);
                        if (dataBuffer[0] == 'M' && dataBuffer[1] == '5') {
                            OnSensorDataChangedCallback(sensorPacketDataObject);
                        } else if ((dataBuffer[0] & 0xFF) == 0xB4) {
                            OnTouchPadActonEventCallback(sensorPacketDataObject.getPacketDataTouchPad());
                        } else if ((dataBuffer[0]&0xFF) == 0x45) {
                            Utils.dLog(TAG, Thread.currentThread().getName() + "->stop sensor command 0x07 effected!");
                        }
                    }
                    try {
                        Thread.sleep(RECV_DATA_GAP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Utils.dLog(TAG, Thread.currentThread().getName() + "->InterruptedException!");
                        break;
                    }
                }
            }
            PacketDataListenerThread = null;
        }
    }

    private int SendCommandToDevice(byte cmd, byte[] data, int dataLength) {
        byte[] cmdBuffer = new byte[dataLength + 2];
        cmdBuffer[0] = cmd;
        cmdBuffer[1] = (byte) dataLength;
        if (data != null && dataLength > 0) {
            System.arraycopy(data, 0, cmdBuffer, 2, dataLength);
        }

        return SendCommandToDevice(cmdBuffer);
    }

    private int SendCommandToDevice(byte cmd) {
        return SendCommandToDevice(cmd, null, (byte) 0x00);
    }

    private int SendCommandToDevice(final byte[] cmdBuffer) {
        if (!handShakeSuccess) {
            mCommandTemp = cmdBuffer[0] & 0xFF;
            return 0;
        }
        if (!deviceIsOpened()) {
            mCommandTemp = cmdBuffer[0] & 0xFF;
            connectToDevice();
            return 0;
        }
        if (handShakeSuccess) {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    int retVal = -1;
                    if (usbConn != null && EPOUT[1] != null) {
                        retVal = usbConn.bulkTransfer(EPOUT[1], cmdBuffer, cmdBuffer.length, BULK_TRANSFER_TIMEOUT);
                        if (retVal > 0) {
                            Utils.dLog(TAG, String.format("send cmd %#04x OK!", cmdBuffer[0] & 0xFF));
                        } else {
                            Utils.dLog(TAG, String.format("send cmd %#04x failed!", cmdBuffer[0] & 0xFF));
                        }
                    }
                }
            });
        }
        return 0;
    }

    private class CommandResultListenerRunnable implements Runnable {
        @Override
        public void run() {
            int retVal = -1;
            byte[] dataBuffer = null;
            boolean bufferInitFlag = false;

            while (runningFlag) {
                synchronized (this) {
                    if (usbConn != null && EPIN[1] != null && handShakeSuccess) {
                        if (!bufferInitFlag) {
                            dataBuffer = new byte[EPIN[1].getMaxPacketSize()];
                            bufferInitFlag = true;
                        }
                        retVal = usbConn.bulkTransfer(EPIN[1], dataBuffer, dataBuffer.length, BULK_TRANSFER_TIMEOUT);
                    } else {
                        retVal = -1;
                        bufferInitFlag = false;
                    }
                    if (retVal > 0) {
                        ProcessingCommandFeedback(dataBuffer, retVal);
                    }
                    try {
                        Thread.sleep(RECV_DATA_GAP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Utils.dLog(TAG, Thread.currentThread().getName() + "->InterruptedException!");
                        break;
                    }
                }
            }
            CommandResultListenerThread = null;
        }
    }

    private void ProcessingCommandFeedback(byte[] buffer, int retVal) {
        int cmd = buffer[0] & 0xFF;
        Utils.dLog(TAG, String.format(Locale.US, "->cmd:%#04x length:%d data:%#04x,%#04x,%#04x,%#04x,%#04x,%#04x",
                buffer[0], buffer[1], buffer[2], buffer[3], buffer[4],buffer[5], buffer[6], buffer[7]));
        switch (cmd) {
            case 0xA5: // result for iap upgrade
            case 0x2C: //G sensor calibration return value;
            case 0xB2: //version number for BLE and CY7C63813
            case 0xB6: //write ble mac return value
            case 0xB8: //read ble mac return value
            case 0xBB: //result for serial number writing
            case 0xBD: //read serial number return value
                ByteBuffer data = ByteBuffer.allocate(buffer[1] + 1);
                data.put(buffer, 2, buffer[1] + 1);
                OnCommandResultChangedCallback(cmd, data.array(), buffer[1]);
                break;
            // dfu upgrade return value;
            case 0xA0:
            case 0xA1:
            case 0xA2:
            case 0xA3:
            case 0xA4:
                DfuUpgradeCase(cmd, buffer);
                break;
            default:
                break;
        }
    }

    private void DfuUpgradeCase(int cmd, byte[] buffer) {
        String path;
        File binFile;
        FileInputStream inputStream = null;
        int fileSize = 0;
        try {
            path = Environment.getExternalStorageDirectory().getPath() + "/USBIAP.bin";
            Utils.dLog(TAG, String.format("filepath:%s", path));
            binFile = new File(path);
            if (!binFile.exists()) {
                Utils.dLog(TAG, "Failed to open file!");
                return;
            }
            inputStream = new FileInputStream(path);
            fileSize = (int) inputStream.getChannel().size();
            Utils.dLog(TAG, String.format(Locale.US, "path = %s fileSize = %d", path, fileSize));
            if (fileSize == 0) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Utils.dLog(TAG, String.format("cmd = %#04x", cmd));
        switch (cmd) {
            case 0xA1:
                byte[] cmdBuffer = new byte[6];
                cmdBuffer[0] = (byte) 0xA2;
                cmdBuffer[1] = 0x04;
                cmdBuffer[2] = (byte) ((fileSize >> 24) & 0xFF);
                cmdBuffer[3] = (byte) ((fileSize >> 16) & 0xFF);
                cmdBuffer[4] = (byte) ((fileSize >> 8) & 0xFF);
                cmdBuffer[5] = (byte) (fileSize & 0xFF);
                SendCommandToDevice(cmdBuffer);
                Utils.dLog(TAG, String.format(Locale.US, "fileSize:%d", fileSize));
                break;
            case 0xA3:
                DfuUpgradeSendFile(inputStream);
                break;
            default:
                break;
        }
    }

    private void DfuUpgradeSendFile(InputStream inputStream) {
        int readBytes = 0;
        byte[] readBuffer = new byte[60];
        ByteBuffer sendBuffer = ByteBuffer.allocate(EPOUT[1].getMaxPacketSize());
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(usbConn, EPOUT[1]);

        try {
            if (inputStream.available() > 0) {
                int count = 0;
                while ((readBytes = inputStream.read(readBuffer)) != -1) {
                    sendBuffer.rewind();
                    sendBuffer.clear();
                    sendBuffer.put((byte) 0xA4);
                    sendBuffer.put((byte) readBytes);
                    sendBuffer.put(readBuffer);
                    if (usbRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
                        count++;
                        Utils.dLog(TAG, String.format(Locale.US, "bytes:%d, count:%d", readBytes, count));
                    } else {
                        Utils.dLog(TAG, String.format(Locale.US, "##LOST:bytes:%d, count:%d", readBytes, count));
                    }
                }
                usbRequest.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int SendCommandWithData(int cmd, byte[] data) {
        byte cmdValue = 0x00;
        switch (cmd) {
            case Utils.CMD_WRITE_SN:
                cmdValue = (byte) 0xBA;
                break;
            case Utils.CMD_WRITE_BLE_MAC:
                cmdValue = (byte) 0xB5;
                break;
            default:
                break;
        }
        return SendCommandToDevice(cmdValue, data, data.length);
    }

    public int SendCommand(int cmd) {
        byte cmdValue = 0x00;
        switch (cmd) {
            case Utils.CMD_IAP_UPGRADE:
                cmdValue = (byte) 0xA0;
                break;
            case Utils.CMD_G_CALIBRATE:
                cmdValue = 0x2B;
                break;
            case Utils.CMD_CHECK_VERSION:
                cmdValue = (byte) 0xB1;
                break;
            case Utils.CMD_RECV_SENSOR_DATA:
                cmdValue = 0x0B;
                break;
            case Utils.CMD_STOP_SENSOR_DATA:
                cmdValue = 0x07;
                break;
            case Utils.CMD_RECV_TP_EVENT:
                cmdValue = (byte) 0xB3;
                break;
            case Utils.CMD_STOP_TP_EVENT:
                cmdValue = (byte) 0xB9;
                break;
            case Utils.CMD_READ_SN:
                cmdValue = (byte) 0xBC;
                break;
            case Utils.CMD_READ_BLE_MAC:
                cmdValue = (byte) 0xB7;
                break;
            default:
                break;
        }
        return SendCommandToDevice(cmdValue);
    }
}
