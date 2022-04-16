package com.example.wallcar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    SeekBar prop;
    SeekBar drive;
    TextView connStatus;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice peripheral = null;
    BluetoothLeScanner leScanner;
    BluetoothGatt bluetoothGatt;
    BluetoothGattService service;
    BluetoothGattCharacteristic propCharacteristic;
    BluetoothGattCharacteristic driveCharacteristic;

    boolean scanning = false;
    String serviceUuid = "00000001-2276-4574-8adf-33af0ecdf20f";
    String propUuid = "00000001-6ebc-4ae9-8ca1-fec7da94d7d2";
    String driveUuid = "00000002-e451-4f97-983f-51cce04e34bf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 42);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN
            }, 42);
        }

        bluetoothAdapter = getSystemService(BluetoothManager.class).getAdapter();
        leScanner = bluetoothAdapter.getBluetoothLeScanner();

        connStatus = findViewById(R.id.textView3);
        prop = findViewById(R.id.seekBar);
        drive = findViewById(R.id.seekBar2);

        prop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (bluetoothGatt != null) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        drive.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void connect(View view) {
        if (scanning) return;
        scanning = true;
        connStatus.setText("Suche Arduino...");

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                leScanner.stopScan(leScanCallback);
            }
        }, 10000);

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(ParcelUuid.fromString(serviceUuid));
        leScanner.startScan(Collections.singletonList(builder.build()), null, leScanCallback);
        scanning = false;

        if (peripheral == null) {
            connStatus.setText("Kein Arduino mit den benötigten\nServices und Charakteristika\ngefunden.");
            return;
        }

        bluetoothGatt = peripheral.connectGatt(this, false, gattCallback);
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            peripheral = result.getDevice();
            leScanner.stopScan(leScanCallback);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                service = bluetoothGatt.getService(UUID.fromString(serviceUuid));
                propCharacteristic = service.getCharacteristic(UUID.fromString(propUuid));
                driveCharacteristic = service.getCharacteristic(UUID.fromString(driveUuid));

                connStatus.setText("Verbunden mit: " + peripheral.getName()
                        + "\nMAC-Adresse: " + peripheral.getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                peripheral = null;
                bluetoothGatt = null;
                service = null;
                propCharacteristic = null;
                driveCharacteristic = null;
            }
        }
    };
}