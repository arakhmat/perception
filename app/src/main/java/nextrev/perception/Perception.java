package nextrev.perception;

import android.app.Application;

/* Class for storing global objects */
public class Perception extends Application {

    private BluetoothLeDevice bleDevice;

    public Perception() {
        bleDevice = new BluetoothLeDevice();
    }

    public BluetoothLeDevice getBluetoothLeDevice() {
        return bleDevice;
    }
}