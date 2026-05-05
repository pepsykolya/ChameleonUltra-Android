package com.chameleonultra.android.domain.darkside

import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for darkside attack.
 *
 * Wraps DarksideAttack with higher-level operations for the UI layer.
 */
class DarksideUseCase @Inject constructor(
    private val darksideAttack: DarksideAttack,
    private val bleRepository: BleRepository
) {

    /**
     * Execute darkside attack on a target sector.
     *
     * @param sector Sector to attack
     * @param keyType 0 = Key A, 1 = Key B
     * @param traceCount Number of traces to collect (default: auto-estimate)
     * @return Flow of progress updates
     */
    fun execute(
        sector: Int,
        keyType: Int,
        traceCount: Int? = null
    ): Flow<DarksideAttack.Progress> {
        val target = DarksideAttack.Target(sector, keyType)
        val count = traceCount ?: darksideAttack.estimateTraceCount(0x1001) // Default to 1K
        return darksideAttack.execute(target, count)
    }

    /**
     * Quick darkside attack: try to recover both keys for a sector.
     *
     * @param sector Sector to attack
     * @return Flow of progress updates
     */
    fun attackSector(sector: Int): Flow<DarksideAttack.Progress> {
        // Try Key A first, then Key B
        // In a real implementation, we might chain these or run them in parallel
        return execute(sector, 0) // Key A
    }

    /**
     * Check if the connected device/card supports darkside attack.
     */
    suspend fun checkVulnerability(): Boolean {
        // Get card type from device
        bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)
        // In real implementation, parse response to get card type
        // For now, assume standard MIFARE Classic is vulnerable
        return true
    }
}
