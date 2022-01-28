package com.apenz1.blessed_android_ble_test

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import java.util.*

class BleQueue(private val mBluetoothGatt: BluetoothGatt) {
    enum class ActionType {
        WriteDescriptor, ReadCharacteristic, WriteCharacteristic
    }

    private val bleQueue: Queue<Action> = LinkedList()

    fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        addAction(ActionType.WriteDescriptor, descriptor)
    }

    fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?, status: Int
    ) {
        bleQueue.remove()
        nextAction()
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.ReadCharacteristic, characteristic)
    }

    fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?, status: Int
    ) {
        bleQueue.remove()
        nextAction()
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        addAction(ActionType.WriteCharacteristic, characteristic)
    }

    fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?, status: Int
    ) {
        bleQueue.remove()
        nextAction()
    }

    private fun addAction(actionType: ActionType, `object`: Any) {
        bleQueue.add(Action(actionType, `object`))
        // Process immediately if only item in queue
        if (bleQueue.size === 1) nextAction()
    }

    // Process next action
    private fun nextAction() {
        Log.d("BLE_QUEUE", "Start next action")
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
        if (success) {
            bleQueue.remove()
        } else {
            // TODO: add retry timeout
            nextAction()
        }
    }

    inner class Action(val type: ActionType, val `object`: Any)
}