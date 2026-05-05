package com.chameleonultra.android.data

import com.chameleonultra.android.data.ble.BleManager
import com.chameleonultra.android.domain.model.BleDevice
import com.chameleonultra.android.domain.model.ConnectionState
import com.chameleonultra.android.domain.model.LogEntry
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    private val bleManager: BleManager
) : BleRepository {

    override val connectionState: Flow<ConnectionState> = bleManager.connectionState
    override val logs: Flow<LogEntry> = bleManager.logs
    override val discoveredDevices: Flow<List<BleDevice>> = bleManager.discoveredDevices

    override fun startScan() = bleManager.startScan()
    override fun stopScan() = bleManager.stopScan()
    override suspend fun connect(device: BleDevice) = bleManager.connect(device)
    override suspend fun disconnect() = bleManager.disconnect()
    override suspend fun sendCommand(command: Byte, data: ByteArray) = bleManager.sendCommand(command, data)
}
