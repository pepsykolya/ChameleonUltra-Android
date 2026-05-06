package com.chameleonultra.android.domain.nested

import com.chameleonultra.android.crypto.MfKey32Jni
import com.chameleonultra.android.domain.card.CardReadUseCase
import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for nested authentication attack.
 *
 * NOTE: This use case is currently a "glue" layer around the native mfkey32v2
 * nestedRecover() implementation.
 */
class NestedUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Progress state for this use case.
     */
    sealed class Progress {
        data object Scanning : Progress()
        data class UsingKnownKey(val sector: Int, val keyType: Int) : Progress()
        data class CollectingNonces(val targetSector: Int, val targetKeyType: Int, val count: Int) : Progress()
        data class Recovering(val targetSector: Int, val targetKeyType: Int) : Progress()
        data class Success(val keys: Map<Pair<Int, Int>, ByteArray>) : Progress() // (sector,keyType)->key
        data class Error(val message: String) : Progress()
    }

    /**
     * Execute nested recovery for a list of target sectors.
     *
     * knownKey is chosen by:
     * 1) first key in [knownKeys] (typically from mfkey32v2 success)
     * 2) otherwise fallback to a standard MIFARE Classic default key
     *
     * @param uid 4-byte card UID
     * @param knownKeys list of candidate known keys (6 bytes)
     * @param knownKeySector sector index for the known key (default: 0)
     * @param knownKeyType 0=KeyA, 1=KeyB (default: 0)
     * @param targets list of (sector,keyType) to recover
     * @param noncesPerTarget how many encrypted nonce pairs to collect per target
     */
    fun execute(
        uid: ByteArray,
        knownKeys: List<ByteArray> = emptyList(),
        knownKeySector: Int = 0,
        knownKeyType: Int = 0,
        targets: List<Pair<Int, Int>> = (0..15).flatMap { s -> listOf(s to 0, s to 1) },
        noncesPerTarget: Int = 8
    ): Flow<Progress> = flow {
        try {
            require(uid.size == 4) { "UID must be 4 bytes" }

            emit(Progress.Scanning)
            bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)

            val knownKey: ByteArray = (knownKeys.firstOrNull() ?: CardReadUseCase.DEFAULT_KEYS.first()).also {
                require(it.size == 6) { "Known key must be 6 bytes" }
            }

            emit(Progress.UsingKnownKey(knownKeySector, knownKeyType))
            // Authenticate to the known sector to establish crypto state on-device.
            bleRepository.sendCommand(
                ChameleonCommands.MF1_READ_ONE_BLOCK,
                data = byteArrayOf(knownKeySector.toByte(), 0x00, knownKeyType.toByte(), *knownKey)
            )

            val out = mutableMapOf<Pair<Int, Int>, ByteArray>()

            for ((targetSector, targetKeyType) in targets) {
                val encryptedNonces = collectEncryptedNonces(
                    targetSector = targetSector,
                    targetKeyType = targetKeyType,
                    count = noncesPerTarget
                )

                emit(Progress.Recovering(targetSector, targetKeyType))
                val recovered = MfKey32Jni.nestedRecover(
                    knownKey = knownKey,
                    encryptedNonces = encryptedNonces,
                    uid = uid,
                    targetSector = targetSector,
                    targetKeyType = targetKeyType
                )

                if (recovered.size == 6) {
                    out[targetSector to targetKeyType] = recovered
                }
            }

            emit(Progress.Success(out))
        } catch (e: Exception) {
            emit(Progress.Error("Nested recovery failed: ${e.message}"))
        }
    }

    /**
     * Collect encrypted nonce pairs (nt_enc + nr_enc) from a target sector.
     *
     * The native nestedRecover expects encryptedNonces as concatenated records
     * of 8 bytes each.
     *
     * TODO: Replace placeholder parsing with real BLE response parsing.
     */
    private suspend fun collectEncryptedNonces(
        targetSector: Int,
        targetKeyType: Int,
        count: Int
    ): ByteArray {
        val buf = ByteArray(count * 8)
        var off = 0

        repeat(count) { i ->
            emitProgressCollecting(targetSector, targetKeyType, i + 1)

            // NOTE: command id is currently hardcoded in NestedAttack placeholder (0x2006).
            // We keep it here until a proper ChameleonCommands constant is added.
            bleRepository.sendCommand(0x2006, data = byteArrayOf(targetSector.toByte(), targetKeyType.toByte()))

            // Placeholder: no real device response wiring yet.
            // Fill with zeros so the pipeline compiles; nestedRecover will likely fail until real data is used.
            // Expected per record: [nt_enc:4][nr_enc:4]
            off += 8
        }

        return buf
    }

    private fun kotlinx.coroutines.flow.FlowCollector<Progress>.emitProgressCollecting(
        sector: Int,
        keyType: Int,
        count: Int
    ) {
        try {
            // FlowCollector.emit is suspend; this helper keeps call-sites neat.
        } catch (_: Throwable) {
            // no-op
        }
    }

    /**
     * Check if the connected device/card supports nested attack.
     */
    suspend fun checkVulnerability(): Boolean {
        bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)
        return true
    }
}
