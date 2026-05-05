package com.chameleonultra.android.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.chameleonultra.android.domain.model.BleDevice
import com.chameleonultra.android.domain.model.ConnectionState
import com.chameleonultra.android.domain.model.LogEntry
import com.chameleonultra.android.domain.model.LogType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_RX_CHARACTERISTIC: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_CHARACTERISTIC: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val DEVICE_NAMES = listOf("ChameleonUltra", "ChameleonLite", "CU-", "CL-")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _logs = MutableSharedFlow<LogEntry>(extraBufferCapacity = 100)
    val logs = _logs.asSharedFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 50)
    val receivedData = _receivedData.asSharedFlow()

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (isChameleonDevice(device)) {
                    addDevice(device)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                result.device?.let { device ->
                    if (isChameleonDevice(device)) {
                        addDevice(device)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Scan failed: error code $errorCode"))
            isScanning = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _logs.tryEmit(LogEntry(LogType.INFO, "Connected to ${gatt?.device?.address}"))
                    _connectionState.value = ConnectionState.Connected(gatt?.device?.address ?: "")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _logs.tryEmit(LogEntry(LogType.INFO, "Disconnected"))
                    _connectionState.value = ConnectionState.Disconnected
                    cleanupGatt()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(NUS_SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(NUS_RX_CHARACTERISTIC)
                    txCharacteristic = service.getCharacteristic(NUS_TX_CHARACTERISTIC)

                    if (txCharacteristic != null) {
                        enableNotifications(gatt, txCharacteristic!!)
                    } else {
                        _logs.tryEmit(LogEntry(LogType.ERR, "TX characteristic not found"))
                    }
                } else {
                    _logs.tryEmit(LogEntry(LogType.ERR, "NUS service not found"))
                }
            } else {
                _logs.tryEmit(LogEntry(LogType.ERR, "Service discovery failed: $status"))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _logs.tryEmit(LogEntry(LogType.INFO, "Notifications enabled"))
            } else {
                _logs.tryEmit(LogEntry(LogType.ERR, "Failed to enable notifications: $status"))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                _logs.tryEmit(LogEntry(LogType.RSP, data.toHex()))
                _receivedData.tryEmit(data)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.value?.let { data ->
                    _logs.tryEmit(LogEntry(LogType.RSP, data.toHex()))
                    _receivedData.tryEmit(data)
                }
            }
        }
    }

    fun startScan() {
        if (isScanning) return
        if (scanner == null) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Bluetooth LE scanner not available"))
            return
        }

        _devices.value = emptyList()
        isScanning = true
        _connectionState.value = ConnectionState.Scanning

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(NUS_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            _logs.tryEmit(LogEntry(LogType.INFO, "Scanning started..."))
        } catch (e: Exception) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Failed to start scan: ${e.message}"))
            isScanning = false
        }
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
            _logs.tryEmit(LogEntry(LogType.INFO, "Scanning stopped"))
        } catch (e: Exception) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Failed to stop scan: ${e.message}"))
        }
        isScanning = false
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun connect(device: BleDevice) {
        stopScan()
        _connectionState.value = ConnectionState.Connecting(device.address)

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (bluetoothDevice == null) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Device not found: ${device.address}"))
            _connectionState.value = ConnectionState.Disconnected
            return
        }

        try {
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                bluetoothDevice.connectGatt(context, false, gattCallback)
            }
        } catch (e: Exception) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Failed to connect: ${e.message}"))
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    fun sendCommand(data: ByteArray): Boolean {
        val characteristic = rxCharacteristic ?: run {
            _logs.tryEmit(LogEntry(LogType.ERR, "RX characteristic not available"))
            return false
        }

        val gatt = bluetoothGatt ?: run {
            _logs.tryEmit(LogEntry(LogType.ERR, "Not connected"))
            return false
        }

        return try {
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val success = gatt.writeCharacteristic(characteristic)
            if (success) {
                _logs.tryEmit(LogEntry(LogType.CMD, data.toHex()))
            } else {
                _logs.tryEmit(LogEntry(LogType.ERR, "Failed to write characteristic"))
            }
            success
        } catch (e: Exception) {
            _logs.tryEmit(LogEntry(LogType.ERR, "Write error: ${e.message}"))
            false
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun isChameleonDevice(device: BluetoothDevice): Boolean {
        val name = device.name ?: return false
        return DEVICE_NAMES.any { name.contains(it, ignoreCase = true) }
    }

    private fun addDevice(device: BluetoothDevice) {
        val currentDevices = _devices.value.toMutableList()
        if (currentDevices.none { it.address == device.address }) {
            val bleDevice = BleDevice(
                name = device.name ?: "Unknown",
                address = device.address,
                type = when {
                    device.name?.contains("Lite", ignoreCase = true) == true -> BleDevice.Type.LITE
                    else -> BleDevice.Type.ULTRA
                }
            )
            currentDevices.add(bleDevice)
            _devices.value = currentDevices
            _logs.tryEmit(LogEntry(LogType.INFO, "Found: ${bleDevice.name} (${bleDevice.address})"))
        }
    }

    private fun cleanupGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }

    fun cleanup() {
        stopScan()
        disconnect()
        cleanupGatt()
        scope.cancel()
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
