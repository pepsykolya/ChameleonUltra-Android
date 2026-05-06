#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <android/log.h>

#include "crypto01.h"

int mfkey32v2_recover(uint32_t uid, uint32_t nt0, uint32_t nr0, uint32_t ar0,
                      uint32_t nt1, uint32_t nr1, uint32_t ar1,
                      uint64_t *out_key);

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "mfkey32jni", __VA_ARGS__)

static void key64_to_bytes(uint64_t key, uint8_t out6[6]){
    out6[0] = (key >> 40) & 0xFF;
    out6[1] = (key >> 32) & 0xFF;
    out6[2] = (key >> 24) & 0xFF;
    out6[3] = (key >> 16) & 0xFF;
    out6[4] = (key >> 8) & 0xFF;
    out6[5] = (key) & 0xFF;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_mfkey32v2(JNIEnv* env, jobject thiz,
                                                           jint uid,
                                                           jint nt0, jint nr0, jint ar0,
                                                           jint nt1, jint nr1, jint ar1){
    (void)thiz;
    uint64_t key;
    int ok = mfkey32v2_recover((uint32_t)uid,(uint32_t)nt0,(uint32_t)nr0,(uint32_t)ar0,
                              (uint32_t)nt1,(uint32_t)nr1,(uint32_t)ar1,
                              &key);
    if(!ok) return NULL;

    uint8_t out6[6];
    key64_to_bytes(key,out6);
    jbyteArray arr = (*env)->NewByteArray(env,6);
    (*env)->SetByteArrayRegion(env,arr,0,6,(jbyte*)out6);
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_nestedRecover(JNIEnv* env, jobject thiz,
                                                                jbyteArray nonces,
                                                                jint uid,
                                                                jint targetSector,
                                                                jint targetKeyType){
    (void)env;(void)thiz;(void)nonces;(void)uid;(void)targetSector;(void)targetKeyType;
    // TODO: implement using proxmark mfnested logic; requires more device-side data.
    LOGE("nestedRecover not implemented");
    return NULL;
}

JNIEXPORT jbyteArray JNICALL
Java_com_chameleonultra_android_crypto_MfKey32Jni_darksideRecover(JNIEnv* env, jobject thiz,
                                                                  jbyteArray params,
                                                                  jint uid){
    (void)env;(void)thiz;(void)params;(void)uid;
    // TODO: implement using proxmark darkside nonce2key + mfCheckKeys replacement.
    LOGE("darksideRecover not implemented");
    return NULL;
}
