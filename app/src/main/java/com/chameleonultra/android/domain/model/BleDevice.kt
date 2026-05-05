package com.chameleonultra.android.domain.model

import android.bluetooth.BluetoothDevice

sealed class BleDevice(val device: BluetoothDevice) {
    val address: String get() = device.address
    val name: String? get() = device.name

    class ChameleonUltra(device: BluetoothDevice) : BleDevice(device)
    class ChameleonLite(device: BluetoothDevice) : BleDevice(device)
}

fun BluetoothDevice.toBleDevice(): BleDevice? {
    val deviceName = name ?: return null
    return when {
        deviceName.contains("ChameleonUltra", ignoreCase = true) -> BleDevice.ChameleonUltra(this)
        deviceName.contains("ChameleonLite", ignoreCase = true) -> BleDevice.ChameleonLite(this)
        else -> null
    }
}
