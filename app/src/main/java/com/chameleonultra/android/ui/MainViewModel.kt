package com.chameleonultra.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chameleonultra.android.domain.card.CardReadUseCase
import com.chameleonultra.android.domain.card.CardEmulateUseCase
import com.chameleonultra.android.domain.card.MifareCard
import com.chameleonultra.android.domain.mfkey32.MfKey32UseCase
import com.chameleonultra.android.domain.nested.NestedUseCase
import com.chameleonultra.android.domain.darkside.DarksideUseCase
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
    private val bleRepository: BleRepository,
    private val cardReadUseCase: CardReadUseCase,
    private val cardEmulateUseCase: CardEmulateUseCase,
    private val mfKey32UseCase: MfKey32UseCase,
    private val nestedUseCase: NestedUseCase,
    private val darksideUseCase: DarksideUseCase
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState
    val discoveredDevices: StateFlow<List<BleDevice>> = bleRepository.discoveredDevices

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Card data state
    private val _cardData = MutableStateFlow<MifareCard?>(null)
    val cardData: StateFlow<MifareCard?> = _cardData.asStateFlow()

    // Key recovery state
    private val _keyRecoveryState = MutableStateFlow<KeyRecoveryState>(KeyRecoveryState.Idle)
    val keyRecoveryState: StateFlow<KeyRecoveryState> = _keyRecoveryState.asStateFlow()

    // Emulation slot selection (1-8)
    private val _selectedSlot = MutableStateFlow(1)
    val selectedSlot: StateFlow<Int> = _selectedSlot.asStateFlow()

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

    // Card operations
    fun readCard() {
        viewModelScope.launch {
            _keyRecoveryState.value = KeyRecoveryState.Running("Reading card...", 0f, "Starting...")
            try {
                val card = cardReadUseCase.readCard()
                _cardData.value = card
                _keyRecoveryState.value = KeyRecoveryState.Idle
            } catch (e: Exception) {
                _keyRecoveryState.value = KeyRecoveryState.Error("Read failed: ${e.message}")
            }
        }
    }

    fun emulateCard(slot: Int) {
        viewModelScope.launch {
            _keyRecoveryState.value = KeyRecoveryState.Running("Emulating...", 0f, "Loading card...")
            try {
                val card = _cardData.value
                if (card != null) {
                    cardEmulateUseCase.emulateCard(card, slot)
                    _keyRecoveryState.value = KeyRecoveryState.Idle
                } else {
                    _keyRecoveryState.value = KeyRecoveryState.Error("No card data loaded")
                }
            } catch (e: Exception) {
                _keyRecoveryState.value = KeyRecoveryState.Error("Emulation failed: ${e.message}")
            }
        }
    }

    fun selectSlot(slot: Int) {
        _selectedSlot.value = slot.coerceIn(1, 8)
    }

    // Key recovery attacks
    fun startMfKey32() {
        viewModelScope.launch {
            _keyRecoveryState.value = KeyRecoveryState.Running("mfkey32", 0f, "Enabling detection...")
            try {
                val keys = mfKey32UseCase.recoverKeys { progress, status ->
                    _keyRecoveryState.value = KeyRecoveryState.Running("mfkey32", progress, status)
                }
                _keyRecoveryState.value = KeyRecoveryState.Success(keys)
            } catch (e: Exception) {
                _keyRecoveryState.value = KeyRecoveryState.Error("mfkey32 failed: ${e.message}")
            }
        }
    }

    fun startNested() {
        viewModelScope.launch {
            _keyRecoveryState.value = KeyRecoveryState.Running("Nested", 0f, "Starting nested attack...")
            try {
                val keys = nestedUseCase.recoverKeys { progress, status ->
                    _keyRecoveryState.value = KeyRecoveryState.Running("Nested", progress, status)
                }
                _keyRecoveryState.value = KeyRecoveryState.Success(keys)
            } catch (e: Exception) {
                _keyRecoveryState.value = KeyRecoveryState.Error("Nested failed: ${e.message}")
            }
        }
    }

    fun startDarkside() {
        viewModelScope.launch {
            _keyRecoveryState.value = KeyRecoveryState.Running("Darkside", 0f, "Starting darkside attack...")
            try {
                val keys = darksideUseCase.recoverKeys { progress, status ->
                    _keyRecoveryState.value = KeyRecoveryState.Running("Darkside", progress, status)
                }
                _keyRecoveryState.value = KeyRecoveryState.Success(keys)
            } catch (e: Exception) {
                _keyRecoveryState.value = KeyRecoveryState.Error("Darkside failed: ${e.message}")
            }
        }
    }

    fun clearRecoveryState() {
        _keyRecoveryState.value = KeyRecoveryState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            bleRepository.disconnect()
        }
    }

    // Key recovery UI states
    sealed class KeyRecoveryState {
        object Idle : KeyRecoveryState()
        data class Running(
            val title: String,
            val progress: Float,
            val status: String
        ) : KeyRecoveryState()
        data class Success(
            val keys: Map<Int, Pair<String, String>> // sector -> (keyA, keyB)
        ) : KeyRecoveryState()
        data class Error(val message: String) : KeyRecoveryState()
    }
}
