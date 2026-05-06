# mfkey32v2 JNI Library for Android

This directory contains the native C implementation of the mfkey32v2 algorithm
ported from https://github.com/equipter/mfkey32v2 for use with ChameleonUltra Android app.

## Files

- `mfkey32v2.c` — Main JNI entry points
- `crypto01.c/h` — Crypto1 LFSR implementation (from Proxmark3)
- `bucketsort.c/h` — Bucket sort for state recovery
- `parity.h` — Parity calculation helpers
- `common.h` — Common definitions (minimal subset)

## Build

Integrated into Android build via CMake in `app/build.gradle`.
