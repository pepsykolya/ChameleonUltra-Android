package com.chameleonultra.android.domain.mfkey32

/**
 * Detection log entry from ChameleonUltra MF1 detection mode.
 *
 * The device collects authentication attempts when in detection mode
 * and returns them in this structured format.
 *
 * @param uid 4-byte card UID (little-endian from device)
 * @param nt 4-byte tag nonce (NT)
 * @param nr 4-byte reader nonce (NR)
 * @param ar 4-byte reader response (AR)
 * @param sector MIFARE Classic sector number (0-15 for 1K)
 * @param keyType 0 = Key A, 1 = Key B
 * @param isEncrypted Whether AR is encrypted (AR_ENC vs AR)
 */
data class DetectionLog(
    val uid: Int,        // 32-bit UID
    val nt: Int,         // 32-bit tag nonce
    val nr: Int,         // 32-bit reader nonce
    val ar: Int,         // 32-bit reader response (may be AR_ENC)
    val sector: Int,
    val keyType: Int,    // 0 = KeyA, 1 = KeyB
    val isEncrypted: Boolean = false
) {
    companion object {
        /**
         * Parse detection log from ChameleonUltra raw bytes.
         *
         * Format (from MF1_GET_DETECTION_LOG):
         * [UID: 4 LE][NT: 4 LE][NR: 4 LE][AR: 4 LE][Sector: 1][KeyType: 1]
         * Total: 18 bytes per entry
         */
        fun fromBytes(bytes: ByteArray, offset: Int = 0): DetectionLog? {
            if (bytes.size < offset + 18) return null

            val uid = bytes.toInt32LE(offset)
            val nt = bytes.toInt32LE(offset + 4)
            val nr = bytes.toInt32LE(offset + 8)
            val ar = bytes.toInt32LE(offset + 12)
            val sector = bytes[offset + 16].toInt() and 0xFF
            val keyType = bytes[offset + 17].toInt() and 0xFF

            return DetectionLog(
                uid = uid,
                nt = nt,
                nr = nr,
                ar = ar,
                sector = sector,
                keyType = keyType
            )
        }

        /**
         * Parse multiple detection logs from a byte array.
         */
        fun parseAll(bytes: ByteArray): List<DetectionLog> {
            val logs = mutableListOf<DetectionLog>()
            var offset = 0
            while (offset + 18 <= bytes.size) {
                val log = fromBytes(bytes, offset)
                if (log != null) {
                    logs.add(log)
                }
                offset += 18
            }
            return logs
        }

        private fun ByteArray.toInt32LE(offset: Int): Int {
            return (this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8) or
                    ((this[offset + 2].toInt() and 0xFF) shl 16) or
                    ((this[offset + 3].toInt() and 0xFF) shl 24)
        }
    }

    /**
     * Get UID as hex string
     */
    fun uidHex(): String = uid.toHex(8)

    /**
     * Get NT as hex string
     */
    fun ntHex(): String = nt.toHex(8)

    /**
     * Get NR as hex string
     */
    fun nrHex(): String = nr.toHex(8)

    /**
     * Get AR as hex string
     */
    fun arHex(): String = ar.toHex(8)

    private fun Int.toHex(digits: Int): String {
        return "0x${toString(16).uppercase().padStart(digits, '0')}"
    }
}
