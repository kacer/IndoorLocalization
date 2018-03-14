package cz.marw.indoorlocalization.managers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.marw.indoorlocalization.tools.ByteConverter;
//import android.bluetooth.BluetoothManager;

/**
 * Created by Martinek on 13. 3. 2018.
 */

public class BluetoothManager {

    public static final String SENSOR_TAG_MAC = "B0:B4:48:BE:C5:85";

    public static final String START_SCAN_SERVICE_UUID = "f000a000-0451-4000-b000-000000000000";
    public static final String START_SCAN_CHAR_UUID = "f000a100-0451-4000-b000-000000000000";

    public static final String BEACONS_LIST_SERVICE_UUID = "f000b000-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_GET_RECORD_UUID = "f000b100-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_TOTAL_COUNT_UUID = "f000b200-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_MAC_ADDR_UUID = "f000b300-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_RSSI_UUID = "f000b400-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_AGE_UUID = "f000b500-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_FLAG_OF_MAC_UUID = "f000b600-0451-4000-b000-000000000000";
    public static final String BEACONS_LIST_AGE_OF_SCAN_UUID = "f000b700-0451-4000-b000-000000000000";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothGatt deviceGatt;

    public BluetoothManager(android.bluetooth.BluetoothManager manager) {
        bluetoothAdapter = manager.getAdapter();
    }

    public boolean isBluetoothActive() {
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return false;

        return true;
    }

    public void connectToDevice(Context context, String macAddress) {
        device = bluetoothAdapter.getRemoteDevice(macAddress);
        deviceGatt = device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                gatt.discoverServices();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    handleCharacteristic(characteristic);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                System.out.println("Services were discovered!");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }

    public boolean startScan(int scanDuration) {
        BluetoothGattCharacteristic startScanCharacteristic = getCharacteristic(START_SCAN_CHAR_UUID);
        startScanCharacteristic.setValue(ByteConverter.intToBytes(scanDuration, ByteConverter.FORMAT_UINT16));

        return deviceGatt.writeCharacteristic(startScanCharacteristic);
    }

    private void handleCharacteristic(BluetoothGattCharacteristic characteristic) {
        //TODO Implement handleCharacteristic
    }

    private BluetoothGattCharacteristic getCharacteristic(String uuid) {
        switch(String.valueOf(uuid.charAt(4))) {
            case "a":
                return deviceGatt.getService(UUID.fromString(START_SCAN_SERVICE_UUID)).getCharacteristic(UUID.fromString(uuid));
            case "b":
                return deviceGatt.getService(UUID.fromString(BEACONS_LIST_SERVICE_UUID)).getCharacteristic(UUID.fromString(uuid));
        }

        return null;
    }

}
