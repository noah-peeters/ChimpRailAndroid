package com.apenz1.blessed_android_ble_test

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class BleQueue(private val mBluetoothGatt: BluetoothGatt) {
    inner class Action(val type: ActionType, val `object`: Any)
    enum class ActionType {
        WriteDescriptor, ReadCharacteristic, WriteCharacteristic
    }

    private val bleQueue = ConcurrentLinkedQueue<Action>()

    //-- Private functions --//

    private fun addAction(actionType: ActionType, `object`: Any) {
        bleQueue.add(Action(actionType, `object`))
        // Process immediately if only item in queue
        if (bleQueue.size === 1) nextAction()
    }

    // Process next action
    private fun nextAction() {
        if (bleQueue.isEmpty()) return
        val action: Action = bleQueue.element()

        var success = false
        when (action.type) {
            ActionType.WriteDescriptor -> {
                success = mBluetoothGatt.writeDescriptor(
                    action.`object` as BluetoothGattDescriptor
                )
            }
            ActionType.WriteCharacteristic -> {
                success = mBluetoothGatt.writeCharacteristic(
                    action.`object` as BluetoothGattCharacteristic
                )

            }
            ActionType.ReadCharacteristic -> {
                success = mBluetoothGatt.writeCharacteristic(
                    action.`object` as BluetoothGattCharacteristic
                )
            }
        }
        if (!success) {
            // TODO: add retry timeout
            // Retry executing task
            Thread.sleep(500)
            Log.d("BLE_QUEUE", "Failed; retrying...")
            nextAction()
        }
    }

    //-- Exposed functions --//

    // Add to queue functions
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.WriteCharacteristic, characteristic)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.ReadCharacteristic, characteristic)
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        addAction(ActionType.WriteDescriptor, descriptor)
    }

    // Remove from queue callback
    fun removeFromQueue() {
        bleQueue.remove()
        nextAction()
    }

    fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?, status: Int
    ) {
        bleQueue.remove()
        nextAction()
    }
}