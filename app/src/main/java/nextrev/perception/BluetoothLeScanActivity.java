package nextrev.perception;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothLeScanActivity extends AppCompatActivity {

    private BluetoothLeDevice bleDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    ListView devicesListView;
    Map<String, BluetoothDevice> devicesMap;
    List<String> devicesList;
    ArrayAdapter<String> devicesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blescan_activity);

        bleDevice = ((Perception) this.getApplication()).getBluetoothLeDevice();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setTitle("BLE Devices:");
        }

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleDevice.setBluetoothManager(mBluetoothManager);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        devicesListView = (ListView) findViewById(R.id.devices_list_view);

        devicesMap = new HashMap<>();
        devicesList = new ArrayList<>();
        devicesListAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_list_item_1, devicesList);
        devicesListView.setAdapter(devicesListAdapter);


        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                /*  If already connected, disconnect */
                BluetoothGatt mGatt = bleDevice.getBluetoothGatt();
                if (mGatt != null) {
                    mGatt.close();
                    bleDevice.setBluetoothGatt(null);
                }

                for (int j = 0; j < parent.getChildCount(); j++)
                    parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                view.setBackgroundColor(Color.LTGRAY);

                String deviceName = (String) parent.getItemAtPosition(position);
                connectToDevice(devicesMap.get(deviceName));

            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        BluetoothGatt mGatt = bleDevice.getBluetoothGatt();
        if (mGatt != null) {
            mGatt.close();
            bleDevice.setBluetoothGatt(null);
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.blescan_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                devicesMap.clear();
                devicesList.clear();
                for (int j = 0; j < devicesListView.getChildCount(); j++)
                    devicesListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                BluetoothGatt mGatt = bleDevice.getBluetoothGatt();
                if (mGatt != null) {
                    mGatt.close();
                    bleDevice.setBluetoothGatt(null);
                }
                scanLeDevice(true);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        private void addToDevicesList(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                String deviceName = device.getName();
                if (deviceName == null)
                    deviceName = device.toString();
                else
                    deviceName += " (" + device.toString() + ")";

                if (!devicesMap.keySet().contains(deviceName))  {
                    devicesMap.put(deviceName, device);
                    devicesList.add(deviceName);
                    devicesListAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addToDevicesList(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                Log.i("ScanResult - Results", result.toString());
                addToDevicesList(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        BluetoothGatt mGatt = bleDevice.getBluetoothGatt();
        BluetoothManager mBluetoothManager = bleDevice.getBluetoothManager();
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            bleDevice.setBluetoothGatt(mGatt);
            scanLeDevice(false);// will stop after first device detection
            mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }
        else {
            mGatt.close();
            bleDevice.setBluetoothGatt(mGatt);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    gatt.discoverServices();
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }
    };
}