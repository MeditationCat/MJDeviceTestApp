// IMagicEyesAidl.aidl
package com.zhi_tech.magiceyessdk;
import com.zhi_tech.magiceyessdk.IPacketDataAidl;
import com.zhi_tech.magiceyessdk.ICommandResultAidl;

// Declare any non-default types here with import statements

interface IMagicEyesAidl {
    //set device filter R.xml.device_filter
    //void setDeviceFilter(Context context, int resourceId);
    void RemoteSetDeviceFilter(int vendorId, int productId);
    //set bulk transfer timeout unit ms
    void RemoteSetBulkTransferTimeout(int timeout);
    //set receive data gap time
    void RemoteSetReceiveDataGapTime(int gaptime);
    //connect to the device
    void RemoteConnectToDevice();
    //set listener
    void RemoteRegisterOnPacketDataListener(in IPacketDataAidl onPacketDataListener);
    void RemoteRegisterOnCommandResultListener(in ICommandResultAidl onCommandResultListener);
    void RemoteUnregisterOnPacketDataListener(in IPacketDataAidl onPacketDataListener);
    void RemoteUnregisterOnCommandResultListener(in ICommandResultAidl onCommandResultListener);
    //send command to device
    int RemoteSendCommand(int cmd);
    int RemoteSendCommandWithData(int cmd, in byte[] data);
    //daemo thread control
    boolean RemoteCheckServiceListenerThreadIsRunnig();
    void RemoteRestartServiceListenerThread();
    //get device info
    String RemoteGetDeviceInfo();
    //set main activiy className
    void RemoteSetMainActivityClassName(String className);
}
