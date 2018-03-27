package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;

/**
 * Created by Martinek on 27. 3. 2018.
 */

public interface CharacteristicOperation {

    public boolean execute(BluetoothGatt gatt);

}
