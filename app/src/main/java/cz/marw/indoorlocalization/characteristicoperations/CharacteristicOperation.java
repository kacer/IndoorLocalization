package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;

/**
 * Created by Martin Donát on 27. 3. 2018.
 */

public interface CharacteristicOperation {

    public boolean execute(BluetoothGatt gatt);

}
