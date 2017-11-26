package nextrev.perception;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import java.util.UUID;

class BluetoothLeDevice {

    final private static String TAG = "BluetoothLeDevice";


    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mGatt;

    BluetoothManager getBluetoothManager() {
        return mBluetoothManager;
    }

    void setBluetoothManager(BluetoothManager mBluetoothManager) {
        this.mBluetoothManager = mBluetoothManager;
    }

    BluetoothGatt getBluetoothGatt() {
        return mGatt;
    }

    void setBluetoothGatt(BluetoothGatt mGatt) {
        this.mGatt = mGatt;
    }


    void sendData(String value) {
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
