package com.chameleonultra.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chameleonultra.android.domain.model.BleDevice
import com.chameleonultra.android.domain.model.ConnectionState
import com.chameleonultra.android.domain.model.LogEntry
import com.chameleonultra.android.domain.usecase.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState
    val discoveredDevices: StateFlow<List<BleDevice>> = bleRepository.discoveredDevices

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            bleRepository.logs.collect { log ->
                _logs.update { current ->
                    (current + log).takeLast(100)
                }
            }
        }
    }

    fun startScan() {
        bleRepository.startScan()
    }

    fun stopScan() {
        bleRepository.stopScan()
    }

    fun connect(device: BleDevice) {
        viewModelScope.launch {
            bleRepository.connect(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bleRepository.disconnect()
        }
    }

    fun sendCommand(command: Int, status: Int = 0, data: ByteArray = byteArrayOf()) {
        viewModelScope.launch {
            bleRepository.sendCommand(command, status, data)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            bleRepository.disconnect()
        }
    }
}
