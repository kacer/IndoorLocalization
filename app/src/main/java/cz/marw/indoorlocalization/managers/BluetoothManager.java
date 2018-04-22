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
 * Created by Martin Donát on 13. 3. 2018.
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
    private int connectionState;
    private Scan scan;
    private RadioPrint actualRadioPrint;
    private Timer scanningTime = new Timer();
    private Queue<CharacteristicOperation> fifoOfChars = new LinkedList<>();
    private DataExportManager exporter;
    private boolean scanningStarted = false;
    private BluetoothManagerCallback activityCallback;

    public BluetoothManager(android.bluetooth.BluetoothManager manager, BluetoothManagerCallback activityCallback) {
        bluetoothAdapter = manager.getAdapter();
        exporter = new DataExportManager();
        connectionState = BluetoothGatt.STATE_DISCONNECTED;
        this.activityCallback = activityCallback;
    }

    public boolean isBluetoothActive() {
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return false;

        return true;
    }

    public boolean connectToDevice(final Context context, String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        deviceGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.discoverServices();
                    connectionState = newState;
                    activityCallback.onConnectionStateChange(newState);

                    if(!scanningStarted && newState == BluetoothGatt.STATE_DISCONNECTED)
                        deviceGatt.close();
                } else {
                    activityCallback.onErrorOccurred();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    //When smartphone reconnected to Sensor Tag after scanning was completed
                    //then smartphone will start to read data from Sensor Tag
                    if(scanningStarted)
                        readDataFromSensorTag();

                    activityCallback.onServiceDiscovered();
                } else {
                    activityCallback.onErrorOccurred();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case START_SCAN_CHAR_UUID:
                            //Sensor Tag started scanning
                            scan = new Scan();
                            deviceGatt.disconnect();
                            activityCallback.onScanningStarted();
                            scanningStarted = true;
                            int scanDuration = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            scan.setScanDuration(scanDuration);

                            //When scanning ends, smartphone will reconnect
                            scanningTime.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        deviceGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                                    }

                                    activityCallback.onScanningEnded();
                                    deviceGatt.connect();
                                }
                            }, scanDuration + SCANNING_DURATION_DELAY);
                            break;
                        case BEACONS_LIST_GET_RECORD_UUID:
                            //Index was written successfully so we fetch next characteristic operation
                            fetchNextCharacteristic();
                            break;
                    }
                } else {
                    activityCallback.onErrorOccurred();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch(characteristic.getUuid().toString()) {
                        case BEACONS_LIST_TOTAL_COUNT_UUID:
                            int totalCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);

                            //Now is possible to start reading radio prints from Sensor Tag
                            readRadioPrintsFromSensorTag(totalCount);
                            break;
                        case BEACONS_LIST_MAC_ADDR_UUID:
                            actualRadioPrint = new RadioPrint();
                            actualRadioPrint.setMacAddr(MacAddress.macAsString(characteristic.getValue()));
                            break;
                        case BEACONS_LIST_RSSI_UUID:
                            actualRadioPrint.setRssi(-1 * characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                            break;
                        case BEACONS_LIST_AGE_UUID:
                            int age = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            actualRadioPrint.setDiscoveryTime(age);
                            scan.addRadioPrint(actualRadioPrint);
                            break;
                        case BEACONS_LIST_FLAG_OF_MAC_UUID:
                            scan.setFlag(characteristic.getValue()[0]);
                            break;
                        case BEACONS_LIST_AGE_OF_SCAN_UUID:
                            int ageOfScan = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            scan.setAgeOfScan(ageOfScan);
                            break;
                    }
                } else {
                    activityCallback.onErrorOccurred();
                }

                //After every read or write operation is needed to fetch next characteristic operation
                fetchNextCharacteristic();
            }


        });

        return deviceGatt.connect();
    }

    public void disconnect() {
        deviceGatt.disconnect();

        stopAllActivity();
    }

    private void stopAllActivity() {
        scanningStarted = false;
        fifoOfChars.clear();
    }

    public boolean startScan(int scanDuration) {
        if(scanDuration == 0)
            return false;

        BluetoothGattCharacteristic startScanCharacteristic = getCharacteristic(START_SCAN_CHAR_UUID);

        return new WriteOperation(startScanCharacteristic, scanDuration).execute(deviceGatt);
    }

    /**
     *  This function reads necessary data from Sensor Tag before radio prints are read.
     */
    private void readDataFromSensorTag() {
        scanningStarted = false;

        activityCallback.onRadioPrintsRead();
        deviceGatt.readCharacteristic(getCharacteristic(BEACONS_LIST_AGE_OF_SCAN_UUID));

        fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_TOTAL_COUNT_UUID)));
        fifoOfChars.add(new ReadOperation(getCharacteristic(BEACONS_LIST_FLAG_OF_MAC_UUID)));
    }

    /**
    *   Function is responsible for fetching next read or write operation from FIFO queue,
    *   this is needed  because R/W operations trough bluetooth are asynchronous. This function
    *   must be called after every R/W operation.
    * */
    private void fetchNextCharacteristic() {
        if(fifoOfChars.isEmpty())
            return;

        CharacteristicOperation nextChar = fifoOfChars.remove();

        nextChar.execute(deviceGatt);
    }

    /**
     *  Function fills the FIFO queue with R/W operations for reading all radio prints which were caught.
     * @param totalCount
     */
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

    public boolean isScanningStarted() {
        return scanningStarted;
    }

    public int getConnectionState() {
        return connectionState;
    }
}
