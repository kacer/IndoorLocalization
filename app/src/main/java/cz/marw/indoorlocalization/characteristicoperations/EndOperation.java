package cz.marw.indoorlocalization.characteristicoperations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;

import java.io.File;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
        }

        File file = exporter.exportData(scan);
        cb.onRadioPrintsExported(file);
        return true;
    }
}
