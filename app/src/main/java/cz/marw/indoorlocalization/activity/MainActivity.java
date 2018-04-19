package cz.marw.indoorlocalization.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import cz.marw.indoorlocalization.R;
import cz.marw.indoorlocalization.managers.callbacks.BluetoothManagerCallback;
import cz.marw.indoorlocalization.managers.BluetoothManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUESTCODE_STORAGE_PERMISSION = 1;

    private EditText etMacAddr, etScanDuration;
    private Button btnConnect, btnScan;

    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etMacAddr = (EditText) findViewById(R.id.etMacAddr);
        etScanDuration = (EditText) findViewById(R.id.etScanDuration);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnScan = (Button) findViewById(R.id.btnScan);

        etMacAddr.setText(BluetoothManager.SENSOR_TAG_MAC);

        bluetoothManager = new BluetoothManager((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE), new BluetoothManagerCallback() {
            @Override
            public void onServiceDiscovered() {

            }

            @Override
            public void onScanningStarted() {

            }

            @Override
            public void onScanningEnded() {

            }

            @Override
            public void onRadioPrintsRead() {

            }

            @Override
            public void onRadioPrintsExported() {

            }

            @Override
            public void onErrorOccured() {

            }
        });

        setListeners();

        testStorage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUESTCODE_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    createDir();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    System.out.println("permission denied, boo!");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void testStorage() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUESTCODE_STORAGE_PERMISSION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            createDir();
        }
    }

    private void createDir() {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        File testDir = new File(externalStorageDir, "SensorTag");
        if(!testDir.exists())
            testDir.mkdir();
    }

    private void setListeners() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mac = etMacAddr.getText().toString();

                if(!BluetoothAdapter.checkBluetoothAddress(mac)) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_invalid_mac), Toast.LENGTH_LONG).show();
                    return;
                }

                bluetoothManager.connectToDevice(MainActivity.this, mac);
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int scanDuration = Integer.valueOf(etScanDuration.getText().toString());

                bluetoothManager.startScan(scanDuration);
            }
        });
    }
}
