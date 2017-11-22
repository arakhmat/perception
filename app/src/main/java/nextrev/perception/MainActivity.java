package nextrev.perception;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class MainActivity extends Activity {

    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Button cameraActivityButton = findViewById(R.id.camera_activity_button);
        Button connectButton = findViewById(R.id.connect_button);
        CheckBox statusCheckbox = findViewById(R.id.status_checkbox);

        /* Start the Game */
        cameraActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (connected) {
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

        /* Connect to bluetooth */
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BLEScanActivity.class));
            }
        });

        statusCheckbox.setChecked(connected);
    }
}
