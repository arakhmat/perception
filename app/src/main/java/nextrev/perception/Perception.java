package nextrev.perception;

import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import java.util.UUID;

/* Class for storing global objects */
public class Perception extends Application {

    final private static String TAG = "Perception";

    private BluetoothGatt mGatt;
    private boolean flashlightOn;
    private boolean isConnected;

    public Perception () {
        mGatt = null;
        flashlightOn = false;
        isConnected = false;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mGatt;
    }

    public void setBluetoothGatt(BluetoothGatt mGatt) { this.mGatt = mGatt; }

    public boolean flashlightOn() { return flashlightOn; }

    public void toggleFlashlight() {
        this.flashlightOn = !this.flashlightOn;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void disconnect() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
        isConnected = false;
    }

    public void sendPredictionToArduino(String value) {
        if (mGatt == null) {
            Log.w(TAG, "There is no BLE device connected");
            return;
        }

        BluetoothGattService mCustomService = mGatt.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"));
        if(mCustomService == null){
            /* HM-10 SERVICE 3 - CUSTOM SERVICE */
            Log.w(TAG, "Custom BLE Service not found \"0000FFE0-0000-1000-8000-00805F9B34FB\"");
            return;
        }

        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"));
        mWriteCharacteristic.setValue(value + '\n');

        if(!mGatt.writeCharacteristic(mWriteCharacteristic)){
            /* HM-10 CUSTOM CHARACTERISTIC of SERVICE 3 */
            Log.w(TAG, "Failed to write characteristic \"0000FFE1-0000-1000-8000-00805F9B34FB\"");
        }
    }
}