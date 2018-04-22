package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Martin Donát on 27. 3. 2018.
 */

public class ReadOperation implements CharacteristicOperation {

    private BluetoothGattCharacteristic characteristic;

    public ReadOperation(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public boolean execute(BluetoothGatt gatt) {
        return gatt.readCharacteristic(characteristic);
    }
}
