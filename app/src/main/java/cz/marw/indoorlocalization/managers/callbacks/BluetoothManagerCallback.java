package cz.marw.indoorlocalization.managers.callbacks;

import java.io.File;

/**
 * Created by Martinek on 6. 4. 2018.
 */

public interface BluetoothManagerCallback {

    public void onServiceDiscovered();

    public void onScanningStarted();

    public void onScanningEnded();

    public void onRadioPrintsRead();

    public void onRadioPrintsExported(File file);

    public void onErrorOccured();

}
