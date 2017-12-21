package nextrev.perception.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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

        appContext = (Perception) this.getApplication();

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
                    flashlightButton.setText(R.string.turn_on);
                    flashlightImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_disconnected));
                }
                else {
                    flashlightButton.setText(R.string.turn_off);
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
            connectButton.setText(R.string.search);
            connectImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_connected));
            String deviceName = appContext.getBluetoothGatt().getDevice().getName();
            if (deviceName == null)
                deviceName = getBaseContext().getString(R.string.unknown_name);
            nameTextView.setText(deviceName);
            addressTextView.setText(appContext.getBluetoothGatt().getDevice().getAddress());
        }
        else {
            connectButton.setText(R.string.connect);
            connectImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_status_disconnected));
            nameTextView.setText(R.string.n_a);
            addressTextView.setText(R.string.n_a);
        }
    }
}
