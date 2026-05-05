package com.chameleonultra.android.domain.card

/**
 * MIFARE Classic card data model.
 *
 * @param uid 4-byte card UID
 * @param sak Select Acknowledge byte
 * @param atqa Answer To Request A bytes
 * @param type Card type (1K, 4K, etc.)
 * @param sectors List of sectors with their data
 */
data class MifareCard(
    val uid: ByteArray,
    val sak: Byte,
    val atqa: ByteArray,
    val type: CardType,
    val sectors: List<Sector>
) {
    enum class CardType {
        MIFARE_1K,    // 16 sectors, 4 blocks each
        MIFARE_2K,    // 32 sectors, 4 blocks each
        MIFARE_4K,    // 40 sectors (32x4 + 8x16)
        MIFARE_MINI,  // 5 sectors, 4 blocks each
        UNKNOWN
    }

    /**
     * Get UID as hex string.
     */
    fun uidHex(): String = uid.joinToString(" ") { "%02X".format(it) }

    /**
     * Get ATQA as hex string.
     */
    fun atqaHex(): String = atqa.joinToString(" ") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MifareCard
        return uid.contentEquals(other.uid) &&
                sak == other.sak &&
                atqa.contentEquals(other.atqa) &&
                type == other.type &&
                sectors == other.sectors
    }

    override fun hashCode(): Int {
        var result = uid.contentHashCode()
        result = 31 * result + sak.hashCode()
        result = 31 * result + atqa.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + sectors.hashCode()
        return result
    }
}

/**
 * A single sector of a MIFARE Classic card.
 *
 * @param number Sector number (0-15 for 1K)
 * @param blocks List of blocks in this sector
 * @param keyA Key A (6 bytes), null if unknown
 * @param keyB Key B (6 bytes), null if unknown
 * @param accessBits Access conditions (4 bytes)
 */
data class Sector(
    val number: Int,
    val blocks: List<Block>,
    val keyA: ByteArray?,
    val keyB: ByteArray?,
    val accessBits: ByteArray
) {
    /**
     * Check if both keys are known.
     */
    fun hasBothKeys(): Boolean = keyA != null && keyB != null

    /**
     * Check if at least one key is known.
     */
    fun hasAnyKey(): Boolean = keyA != null || keyB != null

    fun keyAHex(): String = keyA?.joinToString(" ") { "%02X".format(it) } ?: "????????????"
    fun keyBHex(): String = keyB?.joinToString(" ") { "%02X".format(it) } ?: "????????????"
    fun accessBitsHex(): String = accessBits.joinToString(" ") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Sector
        return number == other.number &&
                blocks == other.blocks &&
                (keyA?.contentEquals(other.keyA ?: byteArrayOf()) ?: (other.keyA == null)) &&
                (keyB?.contentEquals(other.keyB ?: byteArrayOf()) ?: (other.keyB == null)) &&
                accessBits.contentEquals(other.accessBits)
    }

    override fun hashCode(): Int {
        var result = number
        result = 31 * result + blocks.hashCode()
        result = 31 * result + (keyA?.contentHashCode() ?: 0)
        result = 31 * result + (keyB?.contentHashCode() ?: 0)
        result = 31 * result + accessBits.contentHashCode()
        return result
    }
}

/**
 * A single block (16 bytes) of a MIFARE Classic card.
 *
 * @param number Block number within the sector (0-3 for standard sectors)
 * @param data 16 bytes of block data
 */
data class Block(
    val number: Int,
    val data: ByteArray
) {
    /**
     * Get data as hex string.
     */
    fun dataHex(): String = data.joinToString(" ") { "%02X".format(it) }

    /**
     * Try to interpret data as ASCII string.
     */
    fun asAscii(): String = data.map { if (it in 32..126) it.toChar() else '.' }.joinToString("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Block
        return number == other.number && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = number
        result = 31 * result + data.contentHashCode()
        return result
    }
}
