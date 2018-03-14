package cz.marw.indoorlocalization.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cz.marw.indoorlocalization.R;
import cz.marw.indoorlocalization.managers.BluetoothManager;

public class MainActivity extends AppCompatActivity {

    private EditText etMacAddr;
    private Button btnConnect, btnScan;

    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etMacAddr = (EditText) findViewById(R.id.macAddr);
        btnConnect = (Button) findViewById(R.id.connect);
        btnScan = (Button) findViewById(R.id.btnScan);

        etMacAddr.setText(BluetoothManager.SENSOR_TAG_MAC);

        bluetoothManager = new BluetoothManager((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

        setListeners();
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
                bluetoothManager.startScan(5000);
            }
        });
    }
}
