package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;

import cz.marw.indoorlocalization.managers.callbacks.BluetoothManagerCallback;
import cz.marw.indoorlocalization.managers.DataExportManager;
import cz.marw.indoorlocalization.model.Scan;

/**
 * Created by Martinek on 27. 3. 2018.
 */

public class EndOperation implements CharacteristicOperation {

    private DataExportManager exporter;
    private Scan scan;
    private BluetoothManagerCallback cb;

    public EndOperation(DataExportManager exporter, Scan scan, BluetoothManagerCallback cb) {
        this.exporter = exporter;
        this.scan = scan;
        this.cb = cb;
    }

    @Override
    public boolean execute(BluetoothGatt gatt) {
        cb.onRadioPrintsRead();
        exporter.exportData(scan);
        cb.onRadioPrintsExported();
        return true;
    }
}
