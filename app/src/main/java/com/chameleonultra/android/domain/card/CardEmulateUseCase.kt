package com.chameleonultra.android.domain.card

import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for emulating a MIFARE Classic card in a ChameleonUltra slot.
 *
 * Flow:
 * 1. Select target slot
 * 2. Set tag type to MIFARE Classic
 * 3. Load card data (UID, SAK, ATQA, sector data)
 * 4. Enable emulation
 */
class CardEmulateUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Progress state for emulation setup.
     */
    sealed class Progress {
        data class SelectingSlot(val slot: Int) : Progress()
        data object SettingTagType : Progress()
        data class LoadingData(val sector: Int, val total: Int) : Progress()
        data object EnablingEmulation : Progress()
        data class Success(val slot: Int) : Progress()
        data class Error(val message: String) : Progress()
    }

    /**
     * Emulate a card in a ChameleonUltra slot.
     *
     * @param card Card data to emulate
     * @param slot Slot number (0-7 for ChameleonUltra)
     * @return Flow of progress updates
     */
    fun execute(card: MifareCard, slot: Int): Flow<Progress> = flow {
        try {
            // Step 1: Select slot
            emit(Progress.SelectingSlot(slot))
            bleRepository.sendCommand(
                ChameleonCommands.SET_SLOT_ACTIVATED,
                data = byteArrayOf(slot.toByte())
            )

            // Step 2: Set tag type
            emit(Progress.SettingTagType)
            val tagType = when (card.type) {
                MifareCard.CardType.MIFARE_1K -> ChameleonCommands.TagType.MIFARE_1024
                MifareCard.CardType.MIFARE_2K -> ChameleonCommands.TagType.MIFARE_2048
                MifareCard.CardType.MIFARE_4K -> ChameleonCommands.TagType.MIFARE_4096
                MifareCard.CardType.MIFARE_MINI -> ChameleonCommands.TagType.MIFARE_MINI
                else -> ChameleonCommands.TagType.MIFARE_1024
            }
            bleRepository.sendCommand(
                ChameleonCommands.SET_SLOT_TAG_TYPE,
                data = byteArrayOf(
                    (tagType shr 8).toByte(),
                    (tagType and 0xFF).toByte()
                )
            )

            // Step 3: Set anticollision data (UID, SAK, ATQA)
            val anticollisionData = card.uid + byteArrayOf(card.sak) + card.atqa
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_ANTICOLLISION_DATA,
                data = anticollisionData
            )

            // Step 4: Load sector data
            card.sectors.forEachIndexed { index, sector ->
                emit(Progress.LoadingData(index, card.sectors.size))
                loadSector(sector, index)
            }

            // Step 5: Enable emulation
            emit(Progress.EnablingEmulation)
            bleRepository.sendCommand(
                ChameleonCommands.TAG_EMULATION_START,
                data = byteArrayOf()
            )

            emit(Progress.Success(slot))

        } catch (e: Exception) {
            emit(Progress.Error("Emulation setup failed: ${e.message}"))
        }
    }

    /**
     * Stop emulation.
     */
    suspend fun stopEmulation() {
        bleRepository.sendCommand(ChameleonCommands.TAG_EMULATION_STOP)
    }

    /**
     * Load a single sector into the emulator.
     */
    private suspend fun loadSector(sector: Sector, sectorNum: Int) {
        // Load block data
        sector.blocks.forEachIndexed { blockNum, block ->
            val blockIndex = sectorNum * 4 + blockNum // Simplified for 1K
            val data = byteArrayOf(
                blockIndex.toByte(),
                *block.data
            )
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_BLOCK_DATA,
                data = data
            )
        }

        // Load keys if available
        if (sector.keyA != null) {
            val keyData = byteArrayOf(
                sectorNum.toByte(),
                0x00, // Key A
                *sector.keyA
            )
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_SECTOR_DATA,
                data = keyData
            )
        }

        if (sector.keyB != null) {
            val keyData = byteArrayOf(
                sectorNum.toByte(),
                0x01, // Key B
                *sector.keyB
            )
            bleRepository.sendCommand(
                ChameleonCommands.MF1_SET_SECTOR_DATA,
                data = keyData
            )
        }
    }
}
