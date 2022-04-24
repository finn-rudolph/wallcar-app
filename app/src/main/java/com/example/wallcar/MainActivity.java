package com.example.wallcar;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
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
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Collections;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public SeekBar prop;
    public SeekBar drive;
    public TextView connStatus;
    public Button connButton;

    public BluetoothAdapter bluetoothAdapter;
    public BluetoothLeScanner leScanner;

    public boolean scanning = false;
    public boolean connected = false;
    public UUID serviceUuid = UUID.fromString("00000001-2276-4574-8adf-33af0ecdf20f");
    public UUID propUuid = UUID.fromString("00000001-6ebc-4ae9-8ca1-fec7da94d7d2");
    public UUID driveUuid = UUID.fromString("00000002-e451-4f97-983f-51cce04e34bf");

    public BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                connButton.setText("Trennen");
                connButton.setOnClickListener(view -> disconnect(gatt));
                connStatus.setText("Verbunden mit: " + gatt.getDevice().getName()
                        + "\nMAC-Adresse: " + gatt.getDevice().getAddress());
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                connected = false;
                connButton.setText("Verbinden");
                connButton.setOnClickListener(view -> connect());
                connStatus.setText("Kein Arduino verbunden.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(serviceUuid);
            BluetoothGattCharacteristic propCharacteristic = service.getCharacteristic(propUuid);
            BluetoothGattCharacteristic driveCharacteristic = service.getCharacteristic(driveUuid);

            propCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            driveCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            prop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    if (connected) {
                        // Replace writing method when Android 13 arrives
                        propCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        gatt.writeCharacteristic(propCharacteristic);
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
                public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    if (connected) {
                        driveCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        gatt.writeCharacteristic(driveCharacteristic);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
    };

    public ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            scanning = false;
            result.getDevice().connectGatt(getApplicationContext(), true, gattCallback);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = getSystemService(BluetoothManager.class).getAdapter();
        leScanner = bluetoothAdapter.getBluetoothLeScanner();

        connStatus = findViewById(R.id.textView3);
        connButton = findViewById(R.id.button);
        prop = findViewById(R.id.seekBar);
        drive = findViewById(R.id.seekBar2);

        connButton.setOnClickListener(view -> connect());
    }

    public void connect() {
        if (scanning) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 42);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 43);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, 44);

        scanning = true;
        connStatus.setText("Suche Arduino...");

        ScanFilter.Builder filter = new ScanFilter.Builder();
        filter.setServiceUuid(new ParcelUuid(serviceUuid));
        ScanSettings.Builder settings = new ScanSettings.Builder();
        settings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        (new Handler()).postDelayed(() -> {
            if (!connected) {
                leScanner.stopScan(leScanCallback);
                scanning = false;
                connStatus.setText("Kein Arduino mit passender\nService-UUID gefunden.");
            }
        }, 10000);
        leScanner.startScan(Collections.singletonList(filter.build()), settings.build(), leScanCallback);
    }

    public void disconnect(BluetoothGatt gatt) {
        gatt.disconnect();
    }
}