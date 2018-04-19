package cz.marw.indoorlocalization.managers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import cz.marw.indoorlocalization.managers.callbacks.BluetoothManagerCallback;
import cz.marw.indoorlocalization.characteristicoperations.CharacteristicOperation;
import cz.marw.indoorlocalization.characteristicoperations.EndOperation;
import cz.marw.indoorlocalization.characteristicoperations.ReadOperation;
import cz.marw.indoorlocalization.characteristicoperations.WriteOperation;
import cz.marw.indoorlocalization.model.RadioPrint;
import cz.marw.indoorlocalization.model.Scan;
import cz.marw.indoorlocalization.tools.MacAddress;

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

    private static final int SCANNING_DURATION_DELAY = 100;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt deviceGatt;
    private Scan scan;
    private RadioPrint actualRadioPrint;
    private Timer scanningTime = new Timer();
    private Queue<CharacteristicOperation> fifoOfChars = new LinkedList<>();
    private DataExportManager exporter;
    private boolean scanningStart = false;
    private BluetoothManagerCallback activityCallback;

    public BluetoothManager(android.bluetooth.BluetoothManager manager, BluetoothManagerCallback activityCallback) {
        bluetoothAdapter = manager.getAdapter();
        exporter = new DataExportManager();
        this.activityCallback = activityCallback;
    }

    public boolean isBluetoothActive() {
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return false;

        return true;
    }

    public void connectToDevice(final Context context, String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        deviceGatt = device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                gatt.discoverServices();

                if(status != BluetoothGatt.GATT_SUCCESS)
                    activityCallback.onErrorOccured();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //Umoznit start scan

                if(scanningStart)
                    readDataFromSensorTag();
                else
                    activityCallback.onServiceDiscovered();

                if(status != BluetoothGatt.GATT_SUCCESS)
                    activityCallback.onErrorOccured();
            }




            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case START_SCAN_CHAR_UUID:
                            deviceGatt.disconnect();
                            activityCallback.onScanningStarted();
                            scanningStart = true;
                            int scanDuration = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            System.out.println("SCAN DURATION onWrite: " + scanDuration);
                            scanningTime.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        deviceGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                                    }
                                    System.out.println("SKEN SKONČIL");
                                    activityCallback.onScanningEnded();
                                    deviceGatt.connect();
                                    //readDataFromSensorTag();
                                }
                            }, scanDuration + SCANNING_DURATION_DELAY);
                            break;
                        case BEACONS_LIST_GET_RECORD_UUID:
                            fetchNextCharacteristic();
                            break;
                    }
                } else {
                    activityCallback.onErrorOccured();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case BEACONS_LIST_TOTAL_COUNT_UUID:
                            int totalCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            System.out.println("Total count: " + totalCount);
                            readRadioPrintsFromSensorTag(totalCount);
                            break;
                        case BEACONS_LIST_MAC_ADDR_UUID:
                            actualRadioPrint = new RadioPrint();
                            actualRadioPrint.setMacAddr(MacAddress.macAsString(characteristic.getValue()));
                            System.out.println("MAC " + scan.getTotalCount());
                            break;
                        case BEACONS_LIST_RSSI_UUID:
                            actualRadioPrint.setRssi(-1 * characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                            System.out.println("RSSI " + scan.getTotalCount());
                            break;
                        case BEACONS_LIST_AGE_UUID:
                            int age = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            actualRadioPrint.setDiscoveryTime(age);
                            scan.addRadioPrint(actualRadioPrint);
                            System.out.println("AGE " + scan.getTotalCount());
                            break;
                        case BEACONS_LIST_FLAG_OF_MAC_UUID:
                            System.out.println("Flag Of MAC: " + characteristic.getValue()[0]);
                            scan.setFlag(characteristic.getValue()[0]);
                            break;
                        case BEACONS_LIST_AGE_OF_SCAN_UUID:
                            int ageOfScan = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            System.out.println("Age Of Scan: " + ageOfScan);
                            scan.setAgeOfScan(ageOfScan);
                            break;
                    }
                } else {
                    activityCallback.onErrorOccured();
                }

                fetchNextCharacteristic();
            }


        });
    }

    public void disconnect() {
        deviceGatt.disconnect();
        deviceGatt.close();

        stopAllActivity();
    }

    private void stopAllActivity() {
        scanningStart = false;
        scanningTime.cancel();
        fifoOfChars.clear();
    }

    public boolean startScan(int scanDuration) {
        BluetoothGattCharacteristic startScanCharacteristic = getCharacteristic(START_SCAN_CHAR_UUID);

        return new WriteOperation(startScanCharacteristic, scanDuration).execute(deviceGatt);
    }

    private void readDataFromSensorTag() {
        scan = new Scan();
        scanningStart = false;

        deviceGatt.readCharacteristic(getCharacteristic(BEACONS_LIST_AGE_OF_SCAN_UUID));

        fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_TOTAL_COUNT_UUID)));
        fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_FLAG_OF_MAC_UUID)));
    }

    private void fetchNextCharacteristic() {
        if(fifoOfChars.isEmpty())
            return;

        CharacteristicOperation nextChar = fifoOfChars.remove();

        nextChar.execute(deviceGatt);
    }

    private void readRadioPrintsFromSensorTag(int totalCount) {
        for(int i = 0; i < totalCount; i++) {
            BluetoothGattCharacteristic getRecordChar = getCharacteristic(BEACONS_LIST_GET_RECORD_UUID);

            if(i != 0)
                fifoOfChars.add(new WriteOperation(getRecordChar, i));

            fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_MAC_ADDR_UUID)));
            fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_RSSI_UUID)));
            fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_AGE_UUID)));
        }

        fifoOfChars.add(new EndOperation(exporter, scan, activityCallback));
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
