// IPacketDataAidl.aidl
package com.zhi_tech.magiceyessdk;

import com.zhi_tech.magiceyessdk.Utils.SensorPacketDataObject;

// Declare any non-default types here with import statements

interface IPacketDataAidl {
    void OnSensorDataChanged(in SensorPacketDataObject object);
    void OnTouchPadActonEvent(in int[] values);
}
