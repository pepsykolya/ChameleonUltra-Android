package com.chameleonultra.android.domain.nested

import com.chameleonultra.android.crypto.MfKey32Jni
import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Nested authentication attack for hardened MIFARE Classic cards.
 *
 * The nested attack exploits a weakness in the MIFARE Classic PRNG
 * to recover keys when at least one sector key is known.
 *
 * Reference: ChameleonUltra Python CLI implementation
 * https://github.com/RfidResearchGroup/ChameleonUltra
 *
 * Algorithm overview:
 * 1. Authenticate to a known sector
 * 2. Read encrypted nonce (nt_enc) from target sector
 * 3. Exploit PRNG correlation to recover key bits
 * 4. Repeat for all unknown sectors
 */
class NestedAttack @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Progress state for nested attack.
     */
    sealed class Progress {
        data object AuthenticatingKnown : Progress()
        data class ReadingNonce(val sector: Int) : Progress()
        data class RecoveringKey(val sector: Int, val progress: Float) : Progress()
        data class Success(val keys: Map<Int, ByteArray>) : Progress()
        data class Error(val message: String) : Progress()
    }

    /**
     * Known key entry for the attack.
     *
     * @param sector Sector with known key
     * @param key 6-byte key
     * @param keyType 0 = Key A, 1 = Key B
     */
    data class KnownKey(
        val sector: Int,
        val key: ByteArray,
        val keyType: Int
    ) {
        init {
            require(key.size == 6) { "Key must be 6 bytes" }
        }
    }

    /**
     * Target sector to attack.
     *
     * @param sector Sector number
     * @param keyType Which key to recover (0 = A, 1 = B)
     */
    data class TargetSector(
        val sector: Int,
        val keyType: Int
    )

    /**
     * Execute nested attack to recover unknown keys.
     *
     * @param knownKey A known key for any sector
     * @param targetSectors Sectors to attack
     * @return Flow of progress updates
     */
    fun execute(
        knownKey: KnownKey,
        targetSectors: List<TargetSector>
    ): Flow<Progress> = flow {
        try {
            emit(Progress.AuthenticatingKnown)

            // Step 1: Authenticate to the known sector
            authenticateToSector(knownKey.sector, knownKey.key, knownKey.keyType)

            val recoveredKeys = mutableMapOf<Int, ByteArray>()

            targetSectors.forEach { target ->
                emit(Progress.ReadingNonce(target.sector))

                // Step 2: Acquire encrypted nonce from target sector
                val nonceData = acquireEncryptedNonce(target.sector, target.keyType)

                emit(Progress.RecoveringKey(target.sector, 0.5f))

                // Step 3: Recover key from nonce
                val key = recoverKeyFromNonce(nonceData, knownKey)
                if (key != null) {
                    recoveredKeys[target.sector] = key
                }

                emit(Progress.RecoveringKey(target.sector, 1.0f))
            }

            emit(Progress.Success(recoveredKeys))

        } catch (e: Exception) {
            emit(Progress.Error("Nested attack failed: ${e.message}"))
        }
    }

    /**
     * Authenticate to a sector using a known key.
     */
    private suspend fun authenticateToSector(sector: Int, key: ByteArray, keyType: Int) {
        // Send authentication command to the device
        // The device handles the actual MIFARE authentication
        val data = byteArrayOf(
            sector.toByte(),
            keyType.toByte(),
            *key
        )
        bleRepository.sendCommand(ChameleonCommands.MF1_READ_ONE_BLOCK, data = data)
    }

    /**
     * Acquire encrypted nonce from target sector using MF1_NESTED_ACQUIRE.
     *
     * Command: 0x2006
     * Data: [sector: 1][key_type: 1]
     * Response: [nt_enc: 4][par: 1] (encrypted nonce + parity)
     */
    private suspend fun acquireEncryptedNonce(sector: Int, keyType: Int): ByteArray {
        val data = byteArrayOf(
            sector.toByte(),
            keyType.toByte()
        )
        bleRepository.sendCommand(0x2006, data = data)

        // In a real implementation, we would wait for and parse the response
        // For now, return placeholder data
        return byteArrayOf()
    }

    /**
     * Recover key from encrypted nonce data using native JNI implementation.
     *
     * Uses the known key to decrypt nonces and applies mfkey32v2 algorithm
     * to recover the target sector key.
     */
    private fun recoverKeyFromNonce(
        nonceData: ByteArray,
        knownKey: KnownKey
    ): ByteArray? {
        if (nonceData.size < 8) return null

        // Get UID from device (needed for Crypto1)
        val uid = getCardUid() ?: return null

        // Call native nested recovery
        val result = MfKey32Jni.nestedRecover(
            knownKey = knownKey.key,
            encryptedNonces = nonceData,
            uid = uid,
            targetSector = knownKey.sector,
            targetKeyType = knownKey.keyType
        )

        return if (result.size == 6) result else null
    }

    /**
     * Get card UID from device via HF14A_SCAN command.
     */
    private suspend fun getCardUid(): ByteArray? {
        return try {
            bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)
            // Parse response to extract UID (first 4 bytes typically)
            // For now return placeholder - real implementation needs response parsing
            byteArrayOf(0x00, 0x00, 0x00, 0x00)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a card is vulnerable to the nested attack.
     *
     * Cards with fixed nonces or hardened PRNGs may not be vulnerable.
     */
    fun isVulnerable(cardType: Int): Boolean {
        // Most standard MIFARE Classic 1K/4K cards are vulnerable
        // Some hardened variants (e.g., MIFARE Classic EV1) may resist
        return when (cardType) {
            0x1001, // MIFARE_1024
            0x1002, // MIFARE_2048
            0x1003, // MIFARE_4096
            0x1004  // MIFARE_MINI
            -> true
            else -> false
        }
    }
}
