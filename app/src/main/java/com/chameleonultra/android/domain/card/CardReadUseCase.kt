package com.chameleonultra.android.domain.card

import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.usecase.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for reading a MIFARE Classic card.
 *
 * Flow:
 * 1. Scan for card (HF14A_SCAN)
 * 2. Read all sectors using known keys or default keys
 * 3. Return structured card data
 */
class CardReadUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Progress state for card reading.
     */
    sealed class Progress {
        data object Scanning : Progress()
        data class ReadingSector(val sector: Int, val total: Int) : Progress()
        data class Authenticating(val sector: Int, val keyType: String) : Progress()
        data class Success(val card: MifareCard) : Progress()
        data class Error(val message: String) : Progress()
    }

    /**
     * Default MIFARE Classic keys to try.
     */
    companion object {
        val DEFAULT_KEYS = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
            byteArrayOf(0x4D.toByte(), 0x49.toByte(), 0x46.toByte(), 0x41.toByte(), 0x52.toByte(), 0x45.toByte()), // "MIFARE"
        )
    }

    /**
     * Read a MIFARE Classic card.
     *
     * @param knownKeys Optional list of known keys to try first
     * @return Flow of progress updates with final card data
     */
    fun execute(knownKeys: List<ByteArray> = emptyList()): Flow<Progress> = flow {
        try {
            emit(Progress.Scanning)

            // Step 1: Scan for card
            bleRepository.sendCommand(ChameleonCommands.HF14A_SCAN)
            // In real implementation, parse response to get UID, SAK, ATQA

            // Placeholder card data
            val uid = byteArrayOf(0x12, 0x34, 0x56, 0x78)
            val sak = 0x08.toByte()
            val atqa = byteArrayOf(0x04, 0x00)

            // Step 2: Read all sectors
            val sectors = mutableListOf<Sector>()
            val totalSectors = 16 // MIFARE 1K

            for (sectorNum in 0 until totalSectors) {
                emit(Progress.ReadingSector(sectorNum, totalSectors))

                val sector = readSector(sectorNum, knownKeys)
                sectors.add(sector)
            }

            val card = MifareCard(
                uid = uid,
                sak = sak,
                atqa = atqa,
                type = MifareCard.CardType.MIFARE_1K,
                sectors = sectors
            )

            emit(Progress.Success(card))

        } catch (e: Exception) {
            emit(Progress.Error("Card read failed: ${e.message}"))
        }
    }

    /**
     * Read a single sector using available keys.
     */
    private suspend fun readSector(sectorNum: Int, knownKeys: List<ByteArray>): Sector {
        val blocks = mutableListOf<Block>()
        var keyA: ByteArray? = null
        var keyB: ByteArray? = null
        val accessBits = byteArrayOf(0xFF.toByte(), 0x07.toByte(), 0x80.toByte(), 0x69.toByte())

        // Try to authenticate and read blocks
        // For standard sectors (0-31), there are 4 blocks
        // For large sectors (32-39), there are 16 blocks
        val blocksPerSector = if (sectorNum < 32) 4 else 16

        for (blockNum in 0 until blocksPerSector) {
            val blockData = readBlock(sectorNum, blockNum, knownKeys)
            blocks.add(Block(blockNum, blockData))
        }

        // The last block (trailer) contains keys and access bits
        val trailerBlock = blocks.lastOrNull()
        if (trailerBlock != null && trailerBlock.data.size == 16) {
            // Extract keys from trailer block
            keyA = trailerBlock.data.copyOfRange(0, 6)
            keyB = trailerBlock.data.copyOfRange(10, 16)
            accessBits = trailerBlock.data.copyOfRange(6, 10)
        }

        return Sector(
            number = sectorNum,
            blocks = blocks,
            keyA = keyA,
            keyB = keyB,
            accessBits = accessBits
        )
    }

    /**
     * Read a single block using available keys.
     */
    private suspend fun readBlock(sectorNum: Int, blockNum: Int, knownKeys: List<ByteArray>): ByteArray {
        // Try each known key
        val allKeys = knownKeys + DEFAULT_KEYS

        for (key in allKeys) {
            // Send read command with key
            val data = byteArrayOf(
                sectorNum.toByte(),
                blockNum.toByte(),
                0x00, // Key A
                *key
            )
            bleRepository.sendCommand(ChameleonCommands.MF1_READ_ONE_BLOCK, data = data)
            // In real implementation, check response and return data
        }

        // Return empty block if all keys failed
        return ByteArray(16) { 0x00.toByte() }
    }
}
