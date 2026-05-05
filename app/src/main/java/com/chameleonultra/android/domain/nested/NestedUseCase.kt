package com.chameleonultra.android.domain.nested

import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for nested authentication attack.
 *
 * Wraps NestedAttack with higher-level operations for the UI layer.
 */
class NestedUseCase @Inject constructor(
    private val nestedAttack: NestedAttack,
    private val bleRepository: BleRepository
) {

    /**
     * Execute nested attack with default parameters.
     *
     * @param knownKey A known key for any sector
     * @param targetSectors Sectors to attack (defaults to all sectors 0-15)
     * @return Flow of progress updates
     */
    fun execute(
        knownKey: NestedAttack.KnownKey,
        targetSectors: List<NestedAttack.TargetSector> = (0..15).flatMap { sector ->
            listOf(
                NestedAttack.TargetSector(sector, 0), // Key A
                NestedAttack.TargetSector(sector, 1)  // Key B
            )
        }
    ): Flow<NestedAttack.Progress> {
        return nestedAttack.execute(knownKey, targetSectors)
    }

    /**
     * Quick nested attack: try to recover keys for all sectors
     * using a single known key.
     *
     * @param sector Sector with known key
     * @param key 6-byte key
     * @param keyType 0 = Key A, 1 = Key B
     * @return Flow of progress updates
     */
    fun quickAttack(
        sector: Int,
        key: ByteArray,
        keyType: Int
    ): Flow<NestedAttack.Progress> {
        val knownKey = NestedAttack.KnownKey(sector, key, keyType)
        return execute(knownKey)
    }

    /**
     * Check if the connected device/card supports nested attack.
     */
    suspend fun checkVulnerability(): Boolean {
        // Get card type from device
        bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)
        // In real implementation, parse response to get card type
        // For now, assume standard MIFARE Classic is vulnerable
        return true
    }
}
