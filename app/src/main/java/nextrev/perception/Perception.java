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

    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mGatt;
    private boolean flashlightOn;
    private boolean isConnected;

    public Perception () {
        mBluetoothManager = null;
        mGatt = null;
        flashlightOn = false;
        isConnected = false;
    }

    public BluetoothManager getBluetoothManager() {
        return mBluetoothManager;
    }

    public void setBluetoothManager(BluetoothManager mBluetoothManager) {
        this.mBluetoothManager = mBluetoothManager;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mGatt;
    }

    public void setBluetoothGatt(BluetoothGatt mGatt) {
        this.mGatt = mGatt;
    }

    public boolean getFlashlightOn() {
        return flashlightOn;
    }

    public void setFlashlightOn(boolean flashlightOn) {
        this.flashlightOn = flashlightOn;
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
        if (mBluetoothManager == null || mGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGattService mCustomService = mGatt.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found \"0000FFE0-0000-1000-8000-00805F9B34FB\"");
            return;
        }

        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"));
        mWriteCharacteristic.setValue(value + '\n');

        if(!mGatt.writeCharacteristic(mWriteCharacteristic)){
            Log.w(TAG, "Failed to write characteristic \"0000FFE1-0000-1000-8000-00805F9B34FB\"");
        }
    }
}