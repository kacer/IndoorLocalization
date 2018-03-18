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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import cz.marw.indoorlocalization.model.RadioPrint;
import cz.marw.indoorlocalization.model.Scan;
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

    private static final int SCANNING_DURATION_DELAY = 200;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt deviceGatt;
    private Scan scan;
    private RadioPrint actualRadioPrint;
    private Timer scanningTime = new Timer();
    private int scanDuration;
    private Queue<BluetoothGattCharacteristic> fifoOfChars = new LinkedList<>();

    public BluetoothManager(android.bluetooth.BluetoothManager manager) {
        bluetoothAdapter = manager.getAdapter();
    }

    public boolean isBluetoothActive() {
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return false;

        return true;
    }

    public void connectToDevice(Context context, String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        deviceGatt = device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //Umoznit start scan
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                //Nastavit si casovac na scan duration + 200ms
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case START_SCAN_CHAR_UUID:
                            int scanDuration = ByteConverter.bytesToInt(characteristic.getValue(), ByteConverter.FORMAT_UINT16);
                            scanningTime.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    System.out.println("SKEN SKONČIL");
                                    readDataFromSensorTag();
                                }
                            }, scanDuration + SCANNING_DURATION_DELAY);
                            break;
                        case BEACONS_LIST_GET_RECORD_UUID:
                            fetchNextCharacteristic();
                            break;
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                fetchNextCharacteristic();

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case BEACONS_LIST_TOTAL_COUNT_UUID:
                            int totalCount = ByteConverter.bytesToInt(characteristic.getValue(), ByteConverter.FORMAT_UINT16);
                            System.out.println("Total count: " + totalCount);
                            readRadioPrintsFromSensorTag(totalCount);
                            break;
                        case BEACONS_LIST_MAC_ADDR_UUID:
                            actualRadioPrint = new RadioPrint();
                            actualRadioPrint.setMacAddr(characteristic.getStringValue(0));
                            break;
                        case BEACONS_LIST_RSSI_UUID:
                            actualRadioPrint.setRssi(-1 * characteristic.getValue()[0]);
                            break;
                        case BEACONS_LIST_AGE_UUID:
                            int age = ByteConverter.bytesToInt(characteristic.getValue(), ByteConverter.FORMAT_UINT16);
                            actualRadioPrint.setDiscoveryTime(age);
                            scan.addRadioPrint(actualRadioPrint);
                            break;
                        case BEACONS_LIST_FLAG_OF_MAC_UUID:
                            System.out.println("Flag Of MAC: " + ByteConverter.bytesToInt(characteristic.getValue(), ByteConverter.FORMAT_UINT8));
                            scan.setFlag(characteristic.getValue()[0]);
                            break;
                        case BEACONS_LIST_AGE_OF_SCAN_UUID:
                            int ageOfScan = ByteConverter.bytesToInt(characteristic.getValue(), ByteConverter.FORMAT_UINT16);
                            System.out.println("Age Of Scan: " + ageOfScan);
                            scan.setAgeOfScan(ageOfScan);
                            break;
                    }
                }

                if(fifoOfChars.isEmpty()) {
                    System.out.println("--- DISCOVERY COMPLETED ---");
                    System.out.println("Age of Scan: " + scan.getAgeOfScan() + " Total count of radio prints: " + scan.getTotalCount() + " Flag of more devices were discovered: " + scan.getFlag());
                    System.out.println();
                    System.out.println("******* RADIO PRINTS *******");
                    for(RadioPrint p : scan.getPrints()) {
                        System.out.println("MAC: " + p.getMacAddr() + " RSSI: " + p.getRssi() + " Discovery time: " + p.getDiscoveryTime());
                    }
                }
            }
        });
    }

    public boolean startScan(int scanDuration) {
        BluetoothGattCharacteristic startScanCharacteristic = getCharacteristic(START_SCAN_CHAR_UUID);
        startScanCharacteristic.setValue(ByteConverter.intToBytes(scanDuration, ByteConverter.FORMAT_UINT16));
        this.scanDuration = scanDuration;

        return deviceGatt.writeCharacteristic(startScanCharacteristic);
    }

    private void readDataFromSensorTag() {
        scan = new Scan();

        deviceGatt.readCharacteristic(getCharacteristic(BEACONS_LIST_AGE_OF_SCAN_UUID));

        fifoOfChars.add(getCharacteristic(BEACONS_LIST_TOTAL_COUNT_UUID));
        fifoOfChars.add(getCharacteristic(BEACONS_LIST_FLAG_OF_MAC_UUID));
    }

    private void fetchNextCharacteristic() {
        if(fifoOfChars.isEmpty())
            return;

        BluetoothGattCharacteristic nextChar = fifoOfChars.remove();

        if(nextChar.getUuid().toString().equals(BEACONS_LIST_GET_RECORD_UUID)) {
            deviceGatt.writeCharacteristic(nextChar);

            return;
        }

        deviceGatt.readCharacteristic(nextChar);
    }

    private void readRadioPrintsFromSensorTag(int totalCount) {
        fifoOfChars.add(getCharacteristic(BEACONS_LIST_MAC_ADDR_UUID));
        fifoOfChars.add(getCharacteristic(BEACONS_LIST_RSSI_UUID));
        fifoOfChars.add(getCharacteristic(BEACONS_LIST_AGE_UUID));

        for(int i = 1; i < totalCount; i++) {
            BluetoothGattCharacteristic getRecordChar = getCharacteristic(BEACONS_LIST_GET_RECORD_UUID);
            getRecordChar.setValue(ByteConverter.intToBytes(i, ByteConverter.FORMAT_UINT16));

            fifoOfChars.add(getRecordChar);
            fifoOfChars.add(getCharacteristic(BEACONS_LIST_MAC_ADDR_UUID));
            fifoOfChars.add(getCharacteristic(BEACONS_LIST_RSSI_UUID));
            fifoOfChars.add(getCharacteristic(BEACONS_LIST_AGE_UUID));
        }
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
