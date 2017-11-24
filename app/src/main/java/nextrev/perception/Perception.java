package nextrev.perception;

import android.app.Application;

/* Class for storing global objects */
public class Perception extends Application {

    private BLEDevice bleDevice;

    public Perception() {
        bleDevice = new BLEDevice();
    }

    public BLEDevice getBLEDevice() {
        return bleDevice;
    }
}