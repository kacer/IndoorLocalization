package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Martin Donát on 27. 3. 2018.
 */

public class WriteOperation implements CharacteristicOperation {

    private BluetoothGattCharacteristic characteristic;
    private int value;

    public WriteOperation(BluetoothGattCharacteristic characteristic, int value) {
        this.characteristic = characteristic;
        this.value = value;
    }

    @Override
    public boolean execute(BluetoothGatt gatt) {
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        return gatt.writeCharacteristic(characteristic);
    }
}
