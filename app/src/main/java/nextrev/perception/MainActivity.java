package nextrev.perception;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.List;

public class MainActivity extends Activity {

    private BLEDevice bleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        bleDevice = ((Perception) this.getApplication()).getBLEDevice();

        Button cameraActivityButton = findViewById(R.id.camera_activity_button);
        Button connectButton = findViewById(R.id.connect_button);
        final CheckBox statusCheckbox = findViewById(R.id.status_checkbox);

        /* Start the Game */
        cameraActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    startActivity(new Intent(MainActivity.this, CameraActivity.class));
                }
                else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("Connect to Arduino via Bluetooth first")
                        .setPositiveButton("ok", null).show();
                }
            }
        });

        /* Connect to Bluetooth */
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BLEScanActivity.class));
            }
        });

    }

    private boolean isConnected() {
        BluetoothGatt mGatt = bleDevice.getBluetoothGatt();
        BluetoothManager mBluetoothManager = bleDevice.getBluetoothManager();
        if (mGatt == null || mBluetoothManager == null) {
            return false;
        }
        else {
            List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            for (BluetoothDevice device : devices) {
                Log.d("Device", device.toString());
            }
            return true;
        }
    }
}
