package com.zhi_tech.magiceyessdk.Utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

/**
 * Created by taipp on 7/11/2016.
 */
public class SensorPacketDataObject implements Parcelable {

    private static final String TAG = "SensorPacketDataObject";

    private char[] packetHeader;
    private int[] packetDataGyro;
    private int[] packetDataAccel;
    private int[] packetDataMSensor;
    private int packetDataTemp;
    private int packetDataLSensor;
    private int packetDataPSensor;
    private long packetDataTimestamp;
    private int[] packetDataTouchPad;

    public SensorPacketDataObject() {
        packetHeader = new char[2];
        packetDataGyro = new int[3];
        packetDataAccel = new int[3];
        packetDataMSensor = new int[3];
        packetDataTemp = 0;
        packetDataLSensor = 0;
        packetDataPSensor = 0;
        packetDataTimestamp = 0;
        packetDataTouchPad = new int[2];
    }

    protected SensorPacketDataObject(Parcel in) {
        packetHeader = in.createCharArray();
        packetDataGyro = in.createIntArray();
        packetDataAccel = in.createIntArray();
        packetDataMSensor = in.createIntArray();
        packetDataTemp = in.readInt();
        packetDataLSensor = in.readInt();
        packetDataPSensor = in.readInt();
        packetDataTimestamp = in.readLong();
        packetDataTouchPad = in.createIntArray();
    }

    public static final Creator<SensorPacketDataObject> CREATOR = new Creator<SensorPacketDataObject>() {
        @Override
        public SensorPacketDataObject createFromParcel(Parcel in) {
            return new SensorPacketDataObject(in);
        }

        @Override
        public SensorPacketDataObject[] newArray(int size) {
            return new SensorPacketDataObject[size];
        }
    };

    public void setSensorPacketDataObject(Object packetData) {
        int offset = 0;
        byte[] dataBuffer = (byte[]) packetData;

        // Touch pad coordinate
        if ((dataBuffer[0] & 0xFF) == 0xB4) {
            offset = 1;
            for (int i = 0; i < packetDataTouchPad.length; i++) {
                packetDataTouchPad[i] = dataBuffer[offset + i] & 0xFF;
            }
            return;
        }
        //packet header: char: 'M','5'
        for (int i = 0; i < packetHeader.length; i++) {
            packetHeader[i] = (char) dataBuffer[offset + i];
        }
        //packet data: gryo: short: x, y, z
        offset = 2;
        for (int i = 0; i < packetDataGyro.length; i++) {
            packetDataGyro[i] =
                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
        }
        //packet data: accelerometer: short: x, y, z
        offset = 8;
        for (int i = 0; i < packetDataAccel.length; i++) {
            packetDataAccel[i] =
                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
        }
        //packet data: magnetic: short: x, y, z
        offset = 14;
        for (int i = 0; i < packetDataMSensor.length; i++) {
            packetDataMSensor[i] =
                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
        }
        //packet data: temperature: short
        offset = 20;
        packetDataTemp =
                (short) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
        //packet data: light: short
        offset = 22;
        packetDataLSensor =
                (int) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
        //packet data: proximity: short
        offset = 24;
        packetDataPSensor =
                (int) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
        //packet data: timestamp: int
        offset = 26;
        packetDataTimestamp =
                (long) ((dataBuffer[offset] & 0xFF) | (dataBuffer[offset + 1] & 0xFF) << 8
                        | (dataBuffer[offset + 2] & 0xFF) << 16 | (dataBuffer[offset + 3] & 0xFF) << 24);

        /*
        Utils.dLog(TAG, String.format(Locale.US, "Header:%c%c%n" +
                "Accelerometer:%d,%d,%d%n" +
                "Magnetic:%d,%d,%d%n" +
                "Temperature:%d%n" +
                "Light:%d%n" +
                "Proximity:%d%n" +
                "Timestamp:%d%n",
                (char)packetHeader[0], (char)packetHeader[1],
                packetDataAccel[0], packetDataAccel[1], packetDataAccel[2],
                packetDataMSensor[0], packetDataMSensor[1], packetDataMSensor[2],
                packetDataTemp,
                packetDataLSensor,
                packetDataPSensor,
                packetDataTimestamp));

        for (int i = 0; i < 30; i++) {
            Utils.dLog(TAG, String.format(Locale.US, "dataBuffer[%d] = %#04x", i, dataBuffer[i]));
        }*/
    }

    public char[] getPacketHeader() {
        return packetHeader;
    }

    public int[] getPacketDataGyro() {
        return packetDataGyro;
    }

    public int[] getPacketDataAccel() {
        return packetDataAccel;
    }

    public int[] getPacketDataMSensor() {
        return packetDataMSensor;
    }

    public int getPacketDataTemp() {
        return packetDataTemp;
    }

    public int getPacketDataLSensor() {
        return packetDataLSensor;
    }

    public int getPacketDataPSensor() {
        return packetDataPSensor;
    }

    public long getPacketDataTimestamp() {
        return packetDataTimestamp;
    }

    public int[] getPacketDataTouchPad() {
        return packetDataTouchPad;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeCharArray(packetHeader);
        parcel.writeIntArray(packetDataGyro);
        parcel.writeIntArray(packetDataAccel);
        parcel.writeIntArray(packetDataMSensor);
        parcel.writeInt(packetDataTemp);
        parcel.writeInt(packetDataLSensor);
        parcel.writeInt(packetDataPSensor);
        parcel.writeLong(packetDataTimestamp);
        parcel.writeIntArray(packetDataTouchPad);
    }

    public void readFromParcel(Parcel parcel) {
        parcel.readCharArray(packetHeader);
        parcel.readIntArray(packetDataGyro);
        parcel.readIntArray(packetDataAccel);
        parcel.readIntArray(packetDataMSensor);
        packetDataTemp = parcel.readInt();
        packetDataLSensor = parcel.readInt();
        packetDataPSensor = parcel.readInt();
        packetDataTimestamp = parcel.readLong();
        parcel.readIntArray(packetDataTouchPad);
    }

}
