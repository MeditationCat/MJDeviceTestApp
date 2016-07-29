package com.zhi_tech.magiceyessdk.Utils;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;

import org.xmlpull.v1.XmlPullParser;

public class DeviceFilter {
    // USB Vendor ID (or -1 for unspecified)
    public final int mVendorId;
    // USB Product ID (or -1 for unspecified)
    public final int mProductId;
    // USB device or interface class (or -1 for unspecified)
    public final int mClass;
    // USB device subclass (or -1 for unspecified)
    public final int mSubclass;
    // USB device protocol (or -1 for unspecified)
    public final int mProtocol;

    public DeviceFilter(int vid, int pid, int clasz, int subclass, int protocol) {
        mVendorId = vid;
        mProductId = pid;
        mClass = clasz;
        mSubclass = subclass;
        mProtocol = protocol;
    }

    public static DeviceFilter read(XmlPullParser parser) {
        int vendorId = -1;
        int productId = -1;
        int deviceClass = -1;
        int deviceSubclass = -1;
        int deviceProtocol = -1;

        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            // All attribute values are ints
            int value = Integer.parseInt(parser.getAttributeValue(i));

            if ("vendor-id".equals(name)) {
                vendorId = value;
            } else if ("product-id".equals(name)) {
                productId = value;
            } else if ("class".equals(name)) {
                deviceClass = value;
            } else if ("subclass".equals(name)) {
                deviceSubclass = value;
            } else if ("protocol".equals(name)) {
                deviceProtocol = value;
            }
        }

        return new DeviceFilter(vendorId, productId, deviceClass, deviceSubclass, deviceProtocol);
    }

    private boolean matches(int clasz, int subclass, int protocol) {
        return ((mClass == -1 || clasz == mClass)
                && (mSubclass == -1 || subclass == mSubclass)
                && (mProtocol == -1 || protocol == mProtocol));
    }

    public boolean matches(UsbDevice device) {
        if (mVendorId != -1 && device.getVendorId() != mVendorId)
            return false;
        if (mProductId != -1 && device.getProductId() != mProductId)
            return false;

        // check device class/subclass/protocol
        if (matches(device.getDeviceClass(), device.getDeviceSubclass(),
                device.getDeviceProtocol()))
            return true;

        // if device doesn't match, check the interfaces
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (matches(intf.getInterfaceClass(),
                    intf.getInterfaceSubclass(),
                    intf.getInterfaceProtocol()))
                return true;
        }
        return false;
    }
}
