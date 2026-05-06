//-----------------------------------------------------------------------------
// Parity functions for mfkey32v2 Android port
// Based on Proxmark3 parity.h - adapted for Android NDK
//-----------------------------------------------------------------------------

#ifndef __PARITY_H
#define __PARITY_H

#include "common.h"

static const uint8_t g_odd_byte_parity[256] = {
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
    1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1
};

#define ODD_PARITY8(x)   { g_odd_byte_parity[x] }
#define EVEN_PARITY8(x)  { !g_odd_byte_parity[x] }

static inline uint8_t oddparity8(const uint8_t x) {
    return g_odd_byte_parity[x];
}

static inline uint8_t evenparity8(const uint8_t x) {
    return !g_odd_byte_parity[x];
}

static inline uint8_t evenparity16(uint16_t x) {
    x ^= x >> 8;
    return EVEN_PARITY8(x);
}

static inline uint8_t oddparity16(uint16_t x) {
    x ^= x >> 8;
    return ODD_PARITY8(x);
}

static inline uint8_t evenparity32(uint32_t x) {
    x ^= x >> 16;
    x ^= x >> 8;
    return EVEN_PARITY8(x);
}

static inline uint8_t oddparity32(uint32_t x) {
    x ^= x >> 16;
    x ^= x >> 8;
    return ODD_PARITY8(x);
}

#endif /* __PARITY_H */
