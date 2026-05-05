package com.chameleonultra.android.domain.mfkey32

import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for mfkey32 key recovery.
 *
 * Flow:
 * 1. Enable detection mode on ChameleonUltra (MF1_SET_DETECTION_ENABLE)
 * 2. Wait for reader authentication attempts
 * 3. Retrieve detection logs (MF1_GET_DETECTION_LOG)
 * 4. Run mfkey32v2 calculation on each log
 * 5. Return recovered keys
 */
class MfKey32UseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Progress state for key recovery operation.
     */
    sealed class Progress {
        data object EnablingDetection : Progress()
        data object WaitingForAuth : Progress()
        data class LogsCollected(val count: Int) : Progress()
        data class RecoveringKeys(val current: Int, val total: Int) : Progress()
        data class Success(val keys: Map<Int, Pair<ByteArray, ByteArray>>) : Progress()
        data class Error(val message: String) : Progress()
    }

    /**
     * Result of key recovery for a sector.
     *
     * @param sector Sector number
     * @param keyA Recovered Key A (6 bytes), or null
     * @param keyB Recovered Key B (6 bytes), or null
     */
    data class SectorKeys(
        val sector: Int,
        val keyA: ByteArray?,
        val keyB: ByteArray?
    )

    private val calculator = MfKey32Calculator()

    /**
     * Execute the full mfkey32 key recovery flow.
     *
     * @param timeoutMs How long to wait for detection logs (default: 30000ms)
     * @return Flow of progress updates
     */
    fun execute(timeoutMs: Long = 30000L): Flow<Progress> = flow {
        try {
            // Step 1: Enable detection mode
            emit(Progress.EnablingDetection)
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_DETECTION_ENABLE,
                data = byteArrayOf(0x01) // enable
            )

            // Step 2: Wait for authentication attempts
            emit(Progress.WaitingForAuth)
            kotlinx.coroutines.delay(timeoutMs)

            // Step 3: Disable detection and get logs
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_DETECTION_ENABLE,
                data = byteArrayOf(0x00) // disable
            )

            // Step 4: Get detection count
            bleRepository.sendCommand(ChameleonCommands.MF1_GET_DETECTION_COUNT)

            // Step 5: Get detection logs
            bleRepository.sendCommand(ChameleonCommands.MF1_GET_DETECTION_LOG)

            // Note: In a real implementation, we would collect the response
            // from bleRepository.receivedData and parse it into DetectionLogs.
            // For now, we emit a placeholder progress.

            emit(Progress.LogsCollected(0))
            emit(Progress.RecoveringKeys(0, 0))

            // Step 6: Process logs and recover keys
            // This would parse the detection log response and run mfkey32
            val recoveredKeys = mutableMapOf<Int, Pair<ByteArray, ByteArray>>()
            emit(Progress.Success(recoveredKeys))

        } catch (e: Exception) {
            emit(Progress.Error("Key recovery failed: ${e.message}"))
        }
    }

    /**
     * Process raw detection log bytes and recover keys.
     *
     * @param logBytes Raw bytes from MF1_GET_DETECTION_LOG response
     * @return List of recovered sector keys
     */
    fun processDetectionLogs(logBytes: ByteArray): List<SectorKeys> {
        val logs = DetectionLog.parseAll(logBytes)
        val sectorKeys = mutableMapOf<Int, Pair<ByteArray?, ByteArray?>>()

        logs.forEach { log ->
            val key = calculator.recoverKey(log)
            if (key != null) {
                val current = sectorKeys.getOrDefault(log.sector, null to null)
                val updated = if (log.keyType == 0) {
                    key to current.second
                } else {
                    current.first to key
                }
                sectorKeys[log.sector] = updated
            }
        }

        return sectorKeys.map { (sector, keys) ->
            SectorKeys(sector, keys.first, keys.second)
        }.sortedBy { it.sector }
    }

    /**
     * Enable or disable detection mode.
     *
     * @param enabled true to enable, false to disable
     */
    suspend fun setDetectionEnabled(enabled: Boolean) {
        bleRepository.sendCommand(
            ChameleonCommands.MF1_SET_DETECTION_ENABLE,
            data = byteArrayOf(if (enabled) 0x01 else 0x00)
        )
    }

    /**
     * Get current detection count from device.
     */
    suspend fun getDetectionCount() {
        bleRepository.sendCommand(ChameleonCommands.MF1_GET_DETECTION_COUNT)
    }

    /**
     * Get detection logs from device.
     */
    suspend fun getDetectionLogs() {
        bleRepository.sendCommand(ChameleonCommands.MF1_GET_DETECTION_LOG)
    }
}
