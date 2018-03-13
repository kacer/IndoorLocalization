package cz.marw.indoorlocalization.managers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.List;
import java.util.UUID;
//import android.bluetooth.BluetoothManager;

/**
 * Created by Martinek on 13. 3. 2018.
 */

public class BluetoothManager {

    public static final String SENSOR_TAG_MAC = "B0:B4:48:BE:C5:85";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;

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
        device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                //super.onConnectionStateChange(gatt, status, newState);
                System.out.println("Status: " + status + " NewState: " + newState);
                System.out.println("Service discovery: " + gatt.discoverServices());
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                System.out.println("Length: " + characteristic.getValue().length);
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    for(byte b : characteristic.getValue()) {
                        System.out.println(b);
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //super.onServicesDiscovered(gatt, status);
                //UUID uuid = gatt.getServices().get(1).getCharacteristics().get(0).getUuid();
                //System.out.println(uuid);
                List<BluetoothGattService> services = gatt.getServices();
                System.out.println("Services length: " + services.size());
                for(BluetoothGattService s : services) {
                    List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                    System.out.println("Char length: " + chars.size() + " in service: " + s.getUuid());
                    for(BluetoothGattCharacteristic characteristic : chars) {
                        System.out.println("Char uuid: " + characteristic.getUuid());
                    }
                }
                BluetoothGattCharacteristic totalCount = services.get(3).getCharacteristic(UUID.fromString("f000b200-0451-4000-b000-000000000000"));
                boolean state = gatt.readCharacteristic(totalCount);
                System.out.println("Read char state: " + state);
            }
        });
    }

}
