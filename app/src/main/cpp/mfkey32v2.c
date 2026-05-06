//-----------------------------------------------------------------------------
// JNI wrapper for mfkey32v2 algorithm
// Exposes: mfkey32v2, nested_recover, darkside_recover
//-----------------------------------------------------------------------------
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "crypto01.h"

#define TAG "MfKey32Jni"

// Helper: convert jbyteArray to uint32_t (big-endian from bytes)
static uint32_t bytes_to_uint32(const uint8_t *bytes) {
    return ((uint32_t)bytes[0] << 24) | ((uint32_t)bytes[1] << 16) |
           ((uint32_t)bytes[2] << 8) | (uint32_t)bytes[3];
}

// Helper: convert uint32_t to bytes (big-endian)
static void uint32_to_bytes(uint32_t value, uint8_t *bytes) {
    bytes[0] = (value >> 24) & 0xFF;
    bytes[1] = (value >> 16) & 0xFF;
    bytes[2] = (value >> 8) & 0xFF;
    bytes[3] = value & 0xFF;
}

// Helper: convert uint64_t to bytes (big-endian, 6 bytes for MIFARE key)
static void uint64_to_bytes(uint64_t value, uint8_t *bytes) {
    bytes[0] = (value >> 40) & 0xFF;
    bytes[1] = (value >> 32) & 0xFF;
    bytes[2] = (value >> 24) & 0xFF;
    bytes[3] = (value >> 16) & 0xFF;
    bytes[4] = (value >> 8) & 0xFF;
    bytes[5] = value & 0xFF;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_mfkey32v2(
        JNIEnv *env,
        jclass clazz,
        jbyteArray nonces,
        jbyteArray uidBytes) {

    // Get UID
    jsize uidLen = (*env)->GetArrayLength(env, uidBytes);
    if (uidLen != 4) {
        return NULL; // Invalid UID length
    }
    jbyte *uidRaw = (*env)->GetByteArrayElements(env, uidBytes, NULL);
    uint32_t uid = bytes_to_uint32((const uint8_t *)uidRaw);
    (*env)->ReleaseByteArrayElements(env, uidBytes, uidRaw, JNI_ABORT);

    // Get nonces array
    jsize noncesLen = (*env)->GetArrayLength(env, nonces);
    if (noncesLen % 18 != 0) {
        return NULL; // Each nonce record is 18 bytes
    }
    jbyte *noncesRaw = (*env)->GetByteArrayElements(env, nonces, NULL);

    int numRecords = noncesLen / 18;
    uint64_t *foundKeys = calloc(numRecords * 2, sizeof(uint64_t));
    int keyCount = 0;

    for (int i = 0; i < numRecords; i++) {
        const uint8_t *record = (const uint8_t *)noncesRaw + i * 18;

        // Parse detection log record
        uint32_t nt0 = bytes_to_uint32(record + 0);
        uint32_t nr0_enc = bytes_to_uint32(record + 4);
        uint32_t ar0_enc = bytes_to_uint32(record + 8);
        uint32_t nt1 = bytes_to_uint32(record + 12);
        uint32_t nr1_enc = bytes_to_uint32(record + 16);
        // ar1_enc would be at offset 20, but we only have 18 bytes per record
        // For single sector detection, we use the same auth twice
        uint32_t ar1_enc = ar0_enc; // Simplified - real implementation needs proper parsing

        uint32_t p64 = prng_successor(nt0, 64);
        uint32_t p64b = prng_successor(nt1, 64);

        struct Crypto1State *s, *t;
        s = lfsr_recovery32(ar0_enc ^ p64, 0);

        for (t = s; t->odd | t->even; ++t) {
            lfsr_rollback_word(t, 0, 0);
            lfsr_rollback_word(t, nr0_enc, 1);
            lfsr_rollback_word(t, uid ^ nt0, 0);

            uint64_t key;
            crypto1_get_lfsr(t, &key);

            crypto1_word(t, uid ^ nt1, 0);
            crypto1_word(t, nr1_enc, 1);
            if (ar1_enc == (crypto1_word(t, 0, 0) ^ p64b)) {
                // Check if key already found
                int duplicate = 0;
                for (int k = 0; k < keyCount; k++) {
                    if (foundKeys[k] == key) {
                        duplicate = 1;
                        break;
                    }
                }
                if (!duplicate) {
                    foundKeys[keyCount++] = key;
                }
                break;
            }
        }
        free(s);
    }

    (*env)->ReleaseByteArrayElements(env, nonces, noncesRaw, JNI_ABORT);

    // Return keys as array of 6-byte key values
    jbyteArray result = (*env)->NewByteArray(env, keyCount * 6);
    if (result && keyCount > 0) {
        jbyte *resultBytes = (*env)->GetByteArrayElements(env, result, NULL);
        for (int i = 0; i < keyCount; i++) {
            uint64_to_bytes(foundKeys[i], (uint8_t *)resultBytes + i * 6);
        }
        (*env)->ReleaseByteArrayElements(env, result, resultBytes, 0);
    }

    free(foundKeys);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_nestedRecover(
        JNIEnv *env,
        jclass clazz,
        jbyteArray nonces,
        jbyteArray uidBytes,
        jint targetSector,
        jint targetKeyType) {

    // TODO: Implement nested attack
    // This requires collecting encrypted nonces from the target sector
    // using a known key from another sector

    // Placeholder: return empty array
    jbyteArray result = (*env)->NewByteArray(env, 0);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_darksideRecover(
        JNIEnv *env,
        jclass clazz,
        jbyteArray params,
        jbyteArray uidBytes) {

    // TODO: Implement darkside attack
    // This requires specific darkside parameters (uid, nt, par, ks, etc.)

    // Placeholder: return empty array
    jbyteArray result = (*env)->NewByteArray(env, 0);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_validatePrngNonce(
        JNIEnv *env,
        jclass clazz,
        jbyteArray nonceBytes) {

    jsize len = (*env)->GetArrayLength(env, nonceBytes);
    if (len != 4) return JNI_FALSE;

    jbyte *nonceRaw = (*env)->GetByteArrayElements(env, nonceBytes, NULL);
    uint32_t nonce = bytes_to_uint32((const uint8_t *)nonceRaw);
    (*env)->ReleaseByteArrayElements(env, nonceBytes, nonceRaw, JNI_ABORT);

    return validate_prng_nonce(nonce) ? JNI_TRUE : JNI_FALSE;
}
