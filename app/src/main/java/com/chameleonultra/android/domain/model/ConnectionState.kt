package com.chameleonultra.android.domain.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val device: BleDevice) : ConnectionState()
    data class Connected(val device: BleDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
