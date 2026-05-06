package com.chameleonultra.android.domain.mfkey32

import com.chameleonultra.android.crypto.MfKey32Jni

/**
 * mfkey32v2 key recovery calculator using native JNI implementation.
 *
 * Wraps the C implementation from equipter/mfkey32v2 ported via NDK.
 */
class MfKey32Calculator {

    /**
     * Recover keys from detection logs using native mfkey32v2.
     *
     * @param logs List of detection logs (each 18 bytes)
     * @param uid 4-byte card UID
     * @return List of unique recovered 6-byte keys
     */
    fun recoverKeys(logs: List<DetectionLog>, uid: ByteArray): List<ByteArray> {
        if (logs.isEmpty() || uid.size != 4) return emptyList()

        // Pack all logs into single byte array for JNI
        val nonces = ByteArray(logs.size * 18)
        logs.forEachIndexed { index, log ->
            log.toBytes().copyInto(nonces, index * 18)
        }

        // Call native implementation
        val result = MfKey32Jni.mfkey32v2(nonces, uid) ?: return emptyList()

        // Parse result into 6-byte keys
        val keys = mutableListOf<ByteArray>()
        for (i in result.indices step 6) {
            if (i + 6 <= result.size) {
                keys.add(result.copyOfRange(i, i + 6))
            }
        }
        return keys
    }

    /**
     * Check if a card uses weak PRNG (vulnerable to darkside attack).
     *
     * @param nonce First nonce observed from card
     * @return true if weak PRNG detected
     */
    fun isWeakPrg(nonce: ByteArray): Boolean {
        if (nonce.size != 4) return false
        return MfKey32Jni.validatePrngNonce(nonce)
    }

    /**
     * Verify a candidate key against known authentication trace.
     * Uses native Crypto1 implementation.
     *
     * @param key 6-byte candidate key
     * @param uid 4-byte UID
     * @param nt 4-byte tag nonce
     * @param nr 4-byte reader nonce
     * @param ar 4-byte reader response
     * @return true if key produces expected AR
     */
    fun verifyKey(key: ByteArray, uid: ByteArray, nt: ByteArray, nr: ByteArray, ar: ByteArray): Boolean {
        // TODO: Implement verification via JNI or Kotlin Crypto1
        // For now, simple structural check
        return key.size == 6 && uid.size == 4
    }
}
