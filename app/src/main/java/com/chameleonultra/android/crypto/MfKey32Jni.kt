package com.chameleonultra.android.crypto

/**
 * JNI bridge to native mfkey32v2 implementation.
 *
 * Loads the native library and exposes crypto1 functions for MIFARE Classic
 * key recovery.
 */
object MfKey32Jni {

    init {
        System.loadLibrary("mfkey32v2")
    }

    /**
     * Recover MIFARE Classic keys using mfkey32v2 attack.
     *
     * @param nonces Detection log records (18 bytes each):
     *               [nt0:4][nr0_enc:4][ar0_enc:4][nt1:4][nr1_enc:2]
     * @param uid 4-byte card UID
     * @return Array of recovered keys (6 bytes each), or null if error
     */
    @JvmStatic
    external fun mfkey32v2(nonces: ByteArray, uid: ByteArray): ByteArray?

    /**
     * Recover key using nested attack.
     *
     * @param nonces Encrypted nonces collected from target sector
     * @param uid 4-byte card UID
     * @param targetSector Target sector number (0-15 for 1K)
     * @param targetKeyType 0=KeyA, 1=KeyB
     * @return Recovered key (6 bytes), or empty array
     */
    @JvmStatic
    external fun nestedRecover(
        nonces: ByteArray,
        uid: ByteArray,
        targetSector: Int,
        targetKeyType: Int
    ): ByteArray

    /**
     * Recover key using darkside attack.
     *
     * @param params Darkside parameters (format TBD)
     * @param uid 4-byte card UID
     * @return Recovered key (6 bytes), or empty array
     */
    @JvmStatic
    external fun darksideRecover(params: ByteArray, uid: ByteArray): ByteArray

    /**
     * Check if a nonce is from weak PRNG (vulnerable to darkside).
     *
     * @param nonce 4-byte nonce
     * @return true if weak PRNG detected
     */
    @JvmStatic
    external fun validatePrngNonce(nonce: ByteArray): Boolean
}
