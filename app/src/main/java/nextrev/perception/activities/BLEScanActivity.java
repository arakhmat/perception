package nextrev.perception.activities;

import android.Manifest;
import android.annotation.TargetApi;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nextrev.perception.Perception;
import nextrev.perception.R;
import nextrev.perception.adapters.DeviceListAdapter;

public class BLEScanActivity extends AppCompatActivity {

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;


    public static final String KEY_NAME = "name"; // parent node
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_CONNECTION_STATUS = "connection_status";

    public static final String STATUS_CONNECTED = "connected"; // parent node
    public static final String STATUS_CONNECTING = "connecting";
    public static final String STATUS_DISCONNECTED = "disconnected";

    Perception appContext;

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    Map<String, BluetoothDevice> deviceMap;
    ArrayList<HashMap<String, String>> deviceList;

    ProgressBar connectingProgressBar;
    TextView connectingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blescan_activity);

        appContext = ((Perception) this.getApplication());

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
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        deviceMap = new HashMap<>();
        deviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, deviceList);

        deviceListView = (ListView) findViewById(R.id.devices_list_view);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parentView, View view,
                                    int position, long id) {

                /*  If already connected, disconnect */
                BluetoothGatt mGatt = appContext.getBluetoothGatt();
                if (mGatt != null) {
                    mGatt.close();
                    appContext.setBluetoothGatt(null);
                }

                resetListView((ListView) parentView);

                RelativeLayout clickedView = (RelativeLayout) view;
                setListViewItem(clickedView, Color.DKGRAY, Color.WHITE);
                deviceList.get(position).put(KEY_CONNECTION_STATUS, STATUS_CONNECTING);

                String deviceAddress = deviceList.get(position).get(KEY_ADDRESS);
                connectToDevice(deviceMap.get(deviceAddress));
            }

        });

        connectingProgressBar = (ProgressBar) findViewById(R.id.connecting_progress_bar);
        connectingTextView = (TextView) findViewById(R.id.connecting_text_view);
        connectingTextView.setTextColor(Color.rgb(255, 165, 0));
        connectingProgressBar.getIndeterminateDrawable().setColorFilter(Color.rgb(255, 165, 0), PorterDuff.Mode.MULTIPLY);

        FrameLayout connectingFrameLayout = (FrameLayout) findViewById(R.id.connecting_frame_layout);
        connectingFrameLayout.bringToFront();

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            fixMarshmallowPermissions(); // https://stackoverflow.com/questions/32708374/bluetooth-le-scanfilters-dont-work-on-android-m
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        deviceMap.clear();
        deviceList.clear();
        deviceListAdapter.notifyDataSetChanged();

        if (appContext.isConnected())
            addToDevicesList(appContext.getBluetoothGatt().getDevice(), STATUS_CONNECTED);

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
        connectingProgressBar.setVisibility(View.INVISIBLE);
        connectingTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.blescan_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                appContext.disconnect();
                connectingProgressBar.setVisibility(View.INVISIBLE);
                connectingTextView.setVisibility(View.INVISIBLE);
                deviceMap.clear();
                deviceList.clear();
                deviceListAdapter.notifyDataSetChanged();
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
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setListViewItem(RelativeLayout view,
                                int backgroundColor,
                                int textColor) {

        view.setBackgroundColor(backgroundColor);
        view.getChildAt(0).setBackgroundColor(backgroundColor);
        view.getChildAt(1).setBackgroundColor(backgroundColor);
        view.getChildAt(2).setBackgroundColor(backgroundColor);
        ((TextView) view.getChildAt(1)).setTextColor(textColor);
        ((TextView) view.getChildAt(2)).setTextColor(textColor);
    }

    public void resetListView(ListView listView) {
        for (int j = 0; j < listView.getChildCount(); j++) {
            RelativeLayout childView = (RelativeLayout) listView.getChildAt(j);
            setListViewItem(childView, Color.TRANSPARENT, Color.BLACK);
            deviceList.get(j).put(KEY_CONNECTION_STATUS, STATUS_DISCONNECTED);
            deviceListAdapter.notifyDataSetChanged();
        }
    }

    private void addToDevicesList(BluetoothDevice device, String connectionStatus) {
        if (device != null) {
            String deviceName = device.getName();
            if (deviceName == null)
                deviceName = "Unknown Name";
            String deviceAddress = device.getAddress();

            if (!deviceMap.keySet().contains(deviceAddress))  {
                Log.d("addToDevicesList", "deviceMap does not have device");
                deviceMap.put(deviceAddress, device);

                HashMap<String, String> deviceListItem = new HashMap<>();
                deviceListItem.put(KEY_ADDRESS, deviceAddress);
                deviceListItem.put(KEY_NAME, deviceName);
                deviceListItem.put(KEY_CONNECTION_STATUS, connectionStatus);

                deviceList.add(deviceListItem);
                deviceListAdapter.notifyDataSetChanged();
            }
        }
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

        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                addToDevicesList(device, STATUS_DISCONNECTED);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                Log.i("ScanResult - Results", result.toString());
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {

        scanLeDevice(false);
        connectingProgressBar.setVisibility(View.VISIBLE);
        connectingTextView.setVisibility(View.VISIBLE);
        appContext.disconnect();
        appContext.setBluetoothGatt(device.connectGatt(this, false, gattCallback));
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        private void hideConnectingProgressBar(final BluetoothGatt gatt, final int connectionStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectingProgressBar.setVisibility(View.INVISIBLE);
                    connectingTextView.setVisibility(View.INVISIBLE);
                    for (HashMap<String, String> device : deviceList) {
                        if (device.get(KEY_ADDRESS).equals(gatt.getDevice().getAddress())) {
                            device.put(KEY_CONNECTION_STATUS,
                                    connectionStatus == BluetoothProfile.STATE_CONNECTED
                                            ? STATUS_CONNECTED
                                            : STATUS_DISCONNECTED);
                        }
                        deviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    hideConnectingProgressBar(gatt, newState);
                    appContext.setIsConnected(true);
                    finish();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    hideConnectingProgressBar(gatt, newState);
                    appContext.setIsConnected(false);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }
    };

    /*
       Code below was obtained from:
       https://stackoverflow.com/questions/32708374/bluetooth-le-scanfilters-dont-work-on-android-m
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);

                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        ) {
                    // All Permissions Granted

                    // Permission Denied
                    Toast.makeText(BLEScanActivity.this, "All Permission GRANTED !! Thank You :)", Toast.LENGTH_SHORT)
                            .show();


                } else {
                    // Permission Denied
                    Toast.makeText(BLEScanActivity.this, "One or More Permissions are DENIED Exiting App :(", Toast.LENGTH_SHORT)
                            .show();

                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void fixMarshmallowPermissions() {

        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("Show Location");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {

                // Need Rationale
                String message = "App need access to " + permissionsNeeded.get(0);

                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }

        Toast.makeText(BLEScanActivity.this, "No new Permission Required- Launching App .You are Awesome!!", Toast.LENGTH_SHORT)
                .show();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(BLEScanActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }
}