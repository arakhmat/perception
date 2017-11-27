package nextrev.perception.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import nextrev.perception.Perception;
import nextrev.perception.R;

public class MainActivity extends AppCompatActivity {

    Perception appContext;

    private Button connectButton;
    private Button flashlightButton;

    private ImageView connectImageView;
    private ImageView flashlightImageView;


    private TextView nameTextView;
    private TextView addressTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        appContext = ((Perception) this.getApplication());

        connectButton = (Button) findViewById(R.id.connect_button);
        flashlightButton = (Button) findViewById(R.id.flashlight_button);
        Button cameraActivityButton = (Button) findViewById(R.id.camera_activity_button);

        connectImageView = (ImageView) findViewById(R.id.connect_image_view);
        flashlightImageView = (ImageView) findViewById(R.id.flashlight_image_view);

        nameTextView = (TextView) findViewById(R.id.name_text_view);
        addressTextView = (TextView) findViewById(R.id.address_text_view);


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
                        .setMessage("Connect to Arduino first")
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


        /* Connect to Bluetooth */
        flashlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appContext.flashlightOn()) {
                    flashlightButton.setText("Turn On");
                    flashlightImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_disconnected));
                }
                else {
                    flashlightButton.setText("Turn Off");
                    flashlightImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_connected));
                }
                appContext.toggleFlashlight();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (appContext.isConnected()) {
            connectButton.setText("Search");
            connectImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_connected));
            String deviceName = appContext.getBluetoothGatt().getDevice().getName();
            if (deviceName == null)
                deviceName = "Unknown Name";
            nameTextView.setText(deviceName);
            addressTextView.setText(appContext.getBluetoothGatt().getDevice().getAddress());
        }
        else {
            connectButton.setText("Connect");
            connectImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_disconnected));
            nameTextView.setText("N/A");
            addressTextView.setText("N/A");
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main_activity_actions, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
////        switch (item.getItemId()) {
////            case R.id.action_refresh:
////                devicesMap.clear();
////                devicesList.clear();
////                for (int j = 0; j < devicesListView.getChildCount(); j++) {
////                    TextView child = (TextView) devicesListView.getChildAt(j);
////                    child.setTextColor(Color.BLACK);
////                    child.setBackgroundColor(Color.TRANSPARENT);
////                }
////                BluetoothGatt mGatt = appContext.getBluetoothGatt();
////                if (mGatt != null) {
////                    mGatt.close();
////                    appContext.setBluetoothGatt(null);
////                }
////                scanLeDevice(true);
////                break;
////            default:
////                break;
////        }
//        return true;
//    }
}
