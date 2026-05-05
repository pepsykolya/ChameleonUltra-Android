package com.chameleonultra.android.domain.usecase

import com.chameleonultra.android.domain.model.BleDevice
import com.chameleonultra.android.domain.model.ConnectionState
import com.chameleonultra.android.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface BleRepository {
    val connectionState: Flow<ConnectionState>
    val logs: Flow<LogEntry>
    val discoveredDevices: Flow<List<BleDevice>>

    fun startScan()
    fun stopScan()
    suspend fun connect(device: BleDevice)
    suspend fun disconnect()
    suspend fun sendCommand(command: Int, status: Int = 0, data: ByteArray = byteArrayOf())
}
