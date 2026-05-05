package com.chameleonultra.android.domain.mfkey32

/**
 * mfkey32v2 key recovery calculator for MIFARE Classic.
 *
 * Based on the C implementation by equipter (https://github.com/equipter/mfkey)
 * and the Python implementation in the ChameleonUltra CLI.
 *
 * The mfkey32 attack recovers a MIFARE Classic key from a single
 * authentication attempt where the reader nonce (NR) is known.
 *
 * Algorithm overview:
 * 1. From detection log: UID, NT, NR, AR (or AR_ENC)
 * 2. If AR is encrypted (AR_ENC), decrypt using known key fragment
 * 3. Roll back the Crypto1 state from AR to find the key
 * 4. Verify the recovered key against the authentication trace
 */
class MfKey32Calculator {

    companion object {
        // MIFARE Classic PRNG constants
        private const val POLY = 0x1D // Primitive polynomial for LFSR
    }

    /**
     * Attempt to recover a key from a single detection log.
     *
     * @param log Detection log with UID, NT, NR, AR
     * @return Recovered 6-byte key as ByteArray, or null if recovery failed
     */
    fun recoverKey(log: DetectionLog): ByteArray? {
        return recoverKey(
            uid = log.uid,
            nt = log.nt,
            nr = log.nr,
            ar = log.ar,
            isEncrypted = log.isEncrypted
        )
    }

    /**
     * Recover key from raw authentication trace values.
     *
     * @param uid 32-bit card UID
     * @param nt 32-bit tag nonce
     * @param nr 32-bit reader nonce
     * @param ar 32-bit reader response (AR or AR_ENC)
     * @param isEncrypted true if AR is encrypted
     * @return 6-byte key as ByteArray, or null
     */
    fun recoverKey(
        uid: Int,
        nt: Int,
        nr: Int,
        ar: Int,
        isEncrypted: Boolean = false
    ): ByteArray? {
        // If AR is encrypted, we need to handle it differently
        // For now, implement the basic mfkey32v2 with known AR
        val decryptedAr = if (isEncrypted) {
            // AR_ENC needs to be decrypted first
            // This requires partial key knowledge or brute force
            // For basic implementation, assume AR is already decrypted
            return null // TODO: implement AR_ENC decryption
        } else {
            ar
        }

        // Try to recover key using the mfkey32v2 algorithm
        val key = tryRecoverKey(uid, nt, nr, decryptedAr)
        return key
    }

    /**
     * Core mfkey32v2 key recovery.
     *
     * The algorithm works by:
     * 1. Computing the Crypto1 state after authentication
     * 2. Rolling back the LFSR to find the initial key state
     * 3. Extracting the 48-bit key from the LFSR state
     */
    private fun tryRecoverKey(uid: Int, nt: Int, nr: Int, ar: Int): ByteArray? {
        // Compute the internal state after the authentication
        // uid ^ nt gives the initial PRNG input
        val initialState = uid xor nt

        // Try to find the key by testing possible LFSR states
        // This is a simplified version - full implementation would
        // use the complete mfkey32v2 algorithm with proper state rollback

        val possibleKeys = mutableListOf<ByteArray>()

        // The actual mfkey32v2 algorithm is complex and involves:
        // 1. Crypto1 state reconstruction from (uid, nt, nr, ar)
        // 2. LFSR rollback to find key bits
        // 3. Key verification

        // For a production implementation, we would port the C algorithm
        // from equipter's mfkey or use a native library

        // Simplified brute-force for demonstration
        // In reality, this should use the proper algorithm
        val key = bruteForceKey(uid, nt, nr, ar)
        if (key != null) {
            possibleKeys.add(key)
        }

        return possibleKeys.firstOrNull()
    }

    /**
     * Brute-force key recovery (fallback for demonstration).
     * In production, this should be replaced with the proper mfkey32v2 algorithm.
     */
    private fun bruteForceKey(uid: Int, nt: Int, nr: Int, ar: Int): ByteArray? {
        // This is a placeholder for the actual algorithm
        // The real implementation would:
        // 1. Build the Crypto1 state from the authentication trace
        // 2. Use the LFSR properties to recover key bits
        // 3. Verify the recovered key

        // For now, return null to indicate we need the full algorithm
        // The full mfkey32v2 implementation requires significant crypto code
        return null
    }

    /**
     * Verify a recovered key against the authentication trace.
     *
     * @param key 6-byte candidate key
     * @param uid 32-bit UID
     * @param nt 32-bit tag nonce
     * @param nr 32-bit reader nonce
     * @param ar 32-bit reader response
     * @return true if the key produces the expected AR
     */
    fun verifyKey(key: ByteArray, uid: Int, nt: Int, nr: Int, ar: Int): Boolean {
        if (key.size != 6) return false

        // Compute expected AR using the key
        // This requires implementing the MIFARE Classic authentication
        // algorithm (Crypto1) with the given key

        // For now, this is a placeholder
        // Full implementation would:
        // 1. Initialize Crypto1 with the key
        // 2. Compute the authentication response
        // 3. Compare with the observed AR

        return false // TODO: implement full verification
    }

    /**
     * Convert a 48-bit integer to a 6-byte key array.
     */
    private fun int48ToKey(value: Long): ByteArray {
        return byteArrayOf(
            ((value shr 40) and 0xFF).toByte(),
            ((value shr 32) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    /**
     * Convert a 6-byte key to a 48-bit integer.
     */
    private fun keyToInt48(key: ByteArray): Long {
        if (key.size != 6) return 0
        return ((key[0].toLong() and 0xFF) shl 40) or
                ((key[1].toLong() and 0xFF) shl 32) or
                ((key[2].toLong() and 0xFF) shl 24) or
                ((key[3].toLong() and 0xFF) shl 16) or
                ((key[4].toLong() and 0xFF) shl 8) or
                (key[5].toLong() and 0xFF)
    }
}
