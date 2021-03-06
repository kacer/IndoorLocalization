package cz.marw.indoorlocalization.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import cz.marw.indoorlocalization.R;
import cz.marw.indoorlocalization.managers.callbacks.BluetoothManagerCallback;
import cz.marw.indoorlocalization.managers.BluetoothManager;

/*
* Created by Martin Donát
* */

public class MainActivity extends AppCompatActivity {

    private static final int REQUESTCODE_STORAGE_PERMISSION = 1;
    private static final int REQUESTCODE_ENABLE_BT = 2;

    private static final int LOADING_SCREEN_FADE_OUT_DURATION = 2000;

    private EditText etMacAddr, etScanDuration;
    private Button btnConnect, btnScan;
    private LinearLayout llStartScan;
    private FrameLayout loadingScreen;
    private TextView tvLoadingState, tvConnectionState;

    private AlertDialog errorHasOccurredDialog;

    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etMacAddr = (EditText) findViewById(R.id.etMacAddr);
        etScanDuration = (EditText) findViewById(R.id.etScanDuration);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnScan = (Button) findViewById(R.id.btnScan);
        llStartScan = (LinearLayout) findViewById(R.id.llStartScan);
        loadingScreen = (FrameLayout) findViewById(R.id.loading_screen);
        tvLoadingState = (TextView) findViewById(R.id.tvLoadingState);
        tvConnectionState = (TextView) findViewById(R.id.tvConnectionState);

        etMacAddr.setText(BluetoothManager.SENSOR_TAG_MAC);

        prepareAlertDiolog();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        bluetoothManager = new BluetoothManager((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE), new BluetoothManagerCallback() {
            @Override
            public void onConnectionStateChange(int newState) {
                if(newState == BluetoothGatt.STATE_CONNECTED) {
                    setEnabledToComponent(etMacAddr, false);

                    btnConnect.setClickable(true);
                    setTextToComponent(btnConnect, getString(R.string.btn_disconnect));

                    setTextColorToComponent(tvConnectionState, getColorFromRes(R.color.colorGreen));
                    setTextToComponent(tvConnectionState, getString(R.string.conn_state_connected));
                } else {
                    setGuiToDefault();
                }
            }

            @Override
            public void onServiceDiscovered() {
                setVisibilityToComponent(llStartScan, View.VISIBLE);
            }

            @Override
            public void onScanningStarted() {
                setVisibilityToComponent(loadingScreen, View.VISIBLE);
                setTextToComponent(tvLoadingState, getString(R.string.info_scanning_started));
            }

            @Override
            public void onScanningEnded() {
                setTextToComponent(tvLoadingState, getString(R.string.info_scanning_stopped));
            }

            @Override
            public void onRadioPrintsRead() {
                setTextToComponent(tvLoadingState, getString(R.string.info_reading_data));
            }

            @Override
            public void onRadioPrintsExported(File file) {
                if(file.exists()) {
                    //If radio prints were exported into file then we need to invoke media scanner for this file
                    //This will ensure that every new file will be immediately accessible from PC
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);

                    setTextToComponent(tvLoadingState, getString(R.string.info_data_exported));

                    Timer loadingScreenFadeOut = new Timer();
                    loadingScreenFadeOut.schedule(new TimerTask() {
                        @Override
                        public void run() {
                        setVisibilityToComponent(loadingScreen, View.GONE);
                        }
                    }, LOADING_SCREEN_FADE_OUT_DURATION);

                } else {
                    setVisibilityToComponent(loadingScreen, View.GONE);
                    Toast.makeText(MainActivity.this, getString(R.string.error_scan_was_not_exported), Toast.LENGTH_LONG).show();
                }
                setVisibilityToComponent(btnConnect, View.VISIBLE);
                btnScan.setClickable(true);
            }

            @Override
            public void onErrorOccurred() {
                setTextToComponent(tvLoadingState, getString(R.string.error_occurred));
                Timer fadeOut = new Timer();
                fadeOut.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setGuiToDefault();
                        setVisibilityToComponent(loadingScreen, View.GONE);
                        setVisibilityToComponent(btnConnect, View.VISIBLE);
                        btnScan.setClickable(true);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorHasOccurredDialog.show();
                            }
                        });
                    }
                }, LOADING_SCREEN_FADE_OUT_DURATION);
            }
        });

        setListeners();
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUESTCODE_ENABLE_BT:
                if(resultCode == RESULT_OK)
                    handleConnectButton();
                break;
        }
        if(requestCode == REQUESTCODE_ENABLE_BT)
            System.out.println(resultCode);
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

        if(bluetoothManager.isScanningStarted() ||
           bluetoothManager.getConnectionState() == BluetoothGatt.STATE_DISCONNECTED ||
           scanDuration == 0 ||
           scanDuration > 65535)
            return;

        //hide keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        btnScan.setClickable(false);
        setVisibilityToComponent(btnConnect, View.INVISIBLE);

        bluetoothManager.startScan(scanDuration);
    }

    private void connect() {
        String mac = etMacAddr.getText().toString();

        if(!BluetoothAdapter.checkBluetoothAddress(mac)) {
            Toast.makeText(MainActivity.this, getString(R.string.error_invalid_mac), Toast.LENGTH_LONG).show();
            return;
        }

        btnConnect.setClickable(false);

        boolean isConnecting = bluetoothManager.connectToDevice(MainActivity.this, mac);

        if(isConnecting) {
            tvConnectionState.setTextColor(getColorFromRes(android.R.color.holo_orange_dark));
            setTextToComponent(tvConnectionState, getString(R.string.conn_state_connecting));
        }
    }

    private void handleConnectButton() {
        if(bluetoothManager.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
            btnConnect.setClickable(false);
            bluetoothManager.disconnect();
        } else
            connect();
    }

    private void prepareAlertDiolog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.error_occurred));
        builder.setMessage(getString(R.string.alert_dialog_message));
        builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });

        errorHasOccurredDialog = builder.create();
    }

    private void setGuiToDefault() {
        setEnabledToComponent(etMacAddr, true);

        btnConnect.setClickable(true);
        setTextToComponent(btnConnect, getString(R.string.btn_connect));

        setTextColorToComponent(tvConnectionState, getColorFromRes(android.R.color.holo_red_light));
        setTextToComponent(tvConnectionState, getString(R.string.conn_state_disconnected));

        setVisibilityToComponent(llStartScan, View.INVISIBLE);
    }

    private void setVisibilityToComponent(final View comp, final int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                comp.setVisibility(visibility);
            }
        });
    }

    private void setTextToComponent(final TextView comp, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                comp.setText(value);
            }
        });
    }

    private void setEnabledToComponent(final View comp, final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                comp.setEnabled(enabled);
            }
        });
    }

    private void setTextColorToComponent(final TextView comp, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                comp.setTextColor(color);
            }
        });
    }

    private int getColorFromRes(int colorResource) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getResources().getColor(colorResource, null);

        return getResources().getColor(colorResource);
    }

    private void setListeners() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bluetoothManager.isBluetoothActive()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUESTCODE_ENABLE_BT);

                    return;
                }

                handleConnectButton();
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
