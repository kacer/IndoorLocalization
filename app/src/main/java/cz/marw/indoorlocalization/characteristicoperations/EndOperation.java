package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;

import cz.marw.indoorlocalization.managers.DataExportManager;
import cz.marw.indoorlocalization.model.Scan;

/**
 * Created by Martinek on 27. 3. 2018.
 */

public class EndOperation implements CharacteristicOperation {

    private DataExportManager exporter;
    private Scan scan;

    public EndOperation(DataExportManager exporter, Scan scan) {
        this.exporter = exporter;
        this.scan = scan;
    }

    @Override
    public boolean execute(BluetoothGatt gatt) {
        exporter.exportData(scan);
        return true;
    }
}
