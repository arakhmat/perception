package nextrev.perception;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import java.util.List;
import java.util.UUID;

class BLEDevice {
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

    public void writeCustomCharacteristic(int value) {
        if (mBluetoothManager == null || mGatt == null) {
            Log.w("BLEDevice", "BluetoothAdapter not initialized");
            return;
        }

        ;
        List<BluetoothGattService> gattServices = this.mGatt.getServices();
        if (gattServices != null) {
            for (BluetoothGattService gattService : gattServices) {
                for (BluetoothGattCharacteristic c : gattService.getCharacteristics()) {
                    List<BluetoothGattDescriptor> ds = c.getDescriptors();
                    for (BluetoothGattDescriptor d : ds)
                        Log.d("descriptor", d.getUuid().toString());
                }
                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (characteristic != null) {
                    this.setCharacteristicNotification(characteristic, true);
                    characteristic.setValue(toByte("9"));
                    this.writeCharacteristic(characteristic);
                }
            }
        }


        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mGatt.getService(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if(mCustomService == null){
            Log.w("BLEDevice", "Custom BLE Service not found");
            return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        mWriteCharacteristic.setValue(value, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
        if(mGatt.writeCharacteristic(mWriteCharacteristic) == false){
            Log.w("BLEDevice", "Failed to write characteristic");
        }
    }
    public void writeCharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
        this.mGatt.writeCharacteristic(gattCharacteristic);
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(i * 2, (i * 2) + 2), 16).byteValue();
        }
        return result;
    }

    void setBluetoothGatt(BluetoothGatt mGatt) {
        this.mGatt = mGatt;
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (this.mBluetoothManager == null || this.mGatt == null) {
            Log.w("BLEDevice", "BluetoothAdapter not initialized");
        } else {
            this.mGatt.setCharacteristicNotification(characteristic, enabled);
        }
    }
}
