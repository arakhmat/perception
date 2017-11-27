package nextrev.perception.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.List;

import nextrev.perception.Perception;
import nextrev.perception.R;

public class MainActivity extends AppCompatActivity {

    Perception appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        appContext = ((Perception) this.getApplication());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setTitle("Perception");
        }

        Button cameraActivityButton = (Button) findViewById(R.id.camera_activity_button);
        Button connectButton = (Button) findViewById(R.id.connect_button);
//        CheckBox statusCheckbox = (CheckBox) findViewById(R.id.status_checkbox);

        /* Start the Game */
        cameraActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appContext.isConnected()) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_refresh:
//                devicesMap.clear();
//                devicesList.clear();
//                for (int j = 0; j < devicesListView.getChildCount(); j++) {
//                    TextView child = (TextView) devicesListView.getChildAt(j);
//                    child.setTextColor(Color.BLACK);
//                    child.setBackgroundColor(Color.TRANSPARENT);
//                }
//                BluetoothGatt mGatt = appContext.getBluetoothGatt();
//                if (mGatt != null) {
//                    mGatt.close();
//                    appContext.setBluetoothGatt(null);
//                }
//                scanLeDevice(true);
//                break;
//            default:
//                break;
//        }
        return true;
    }
}
