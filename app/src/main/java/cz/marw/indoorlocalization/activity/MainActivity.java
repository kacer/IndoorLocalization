package cz.marw.indoorlocalization.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import cz.marw.indoorlocalization.R;
import cz.marw.indoorlocalization.managers.callbacks.BluetoothManagerCallback;
import cz.marw.indoorlocalization.managers.BluetoothManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUESTCODE_STORAGE_PERMISSION = 1;

    private static final int BUTTON = 0;
    private static final int TEXT_VIEW = 1;

    private static final int LOADING_SCREEN_FADE_OUT_DURATION = 2000;

    private EditText etMacAddr, etScanDuration;
    private Button btnConnect, btnScan;
    private FrameLayout loadingScreen;
    private TextView tvLoadingState;

    private Timer loadingScreenFadeOut;

    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etMacAddr = (EditText) findViewById(R.id.etMacAddr);
        etScanDuration = (EditText) findViewById(R.id.etScanDuration);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnScan = (Button) findViewById(R.id.btnScan);
        loadingScreen = (FrameLayout) findViewById(R.id.loading_screen);
        tvLoadingState = (TextView) findViewById(R.id.tvLoadingState);

        loadingScreenFadeOut = new Timer();

        etMacAddr.setText(BluetoothManager.SENSOR_TAG_MAC);

        bluetoothManager = new BluetoothManager((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE), new BluetoothManagerCallback() {
            @Override
            public void onServiceDiscovered() {

            }

            @Override
            public void onScanningStarted() {
                setVisibilityToLoadingScreen(View.VISIBLE);
                setText(tvLoadingState, getString(R.string.info_scanning_started), TEXT_VIEW);
            }

            @Override
            public void onScanningEnded() {
                setText(tvLoadingState, getString(R.string.info_scanning_stopped), TEXT_VIEW);
            }

            @Override
            public void onRadioPrintsRead() {
                setText(tvLoadingState, getString(R.string.info_reading_data), TEXT_VIEW);
            }

            @Override
            public void onRadioPrintsExported(File file) {
                if(file.exists()) {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);
                    setText(tvLoadingState, getString(R.string.info_data_exported), TEXT_VIEW);
                    loadingScreenFadeOut.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            setVisibilityToLoadingScreen(View.GONE);
                        }
                    }, LOADING_SCREEN_FADE_OUT_DURATION);
                } else {
                    setVisibilityToLoadingScreen(View.GONE);
                    Toast.makeText(MainActivity.this, getString(R.string.error_scan_was_not_exported), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onErrorOccured() {

            }
        });

        setListeners();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUESTCODE_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScan();
                } else {
                    Toast.makeText(this, getString(R.string.error_set_permission_to_storage), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUESTCODE_STORAGE_PERMISSION);

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUESTCODE_STORAGE_PERMISSION);
            }
        } else {
            startScan();
        }
    }

    private void startScan() {
        int scanDuration = 0;

        try {
            scanDuration = Integer.valueOf(etScanDuration.getText().toString());
        } catch(NumberFormatException e) {
            Toast.makeText(this, getString(R.string.error_parse_scan_duration), Toast.LENGTH_LONG).show();
        }

        bluetoothManager.startScan(scanDuration);
    }

    private void setVisibilityToLoadingScreen(final int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingScreen.setVisibility(visibility);
            }
        });
    }

    private void setText(final Object comp, final String value, final int type){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(type){
                    case BUTTON:
                        ((Button) comp).setText(value);
                        break;
                    case TEXT_VIEW:
                        ((TextView) comp).setText(value);
                        break;
                }
            }
        });
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
                checkStoragePermission();
            }
        });
    }
}
