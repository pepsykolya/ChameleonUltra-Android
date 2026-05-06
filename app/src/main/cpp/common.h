//-----------------------------------------------------------------------------
// Minimal common.h for mfkey32v2 Android port
// Based on Proxmark3 common.h - stripped down for NDK build
//-----------------------------------------------------------------------------

#ifndef __COMMON_H
#define __COMMON_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#define PACKED __attribute__((packed))

#ifndef MIN
# define MIN(a, b) (((a) < (b)) ? (a) : (b))
#endif

#ifndef MAX
# define MAX(a, b) (((a) > (b)) ? (a) : (b))
#endif

#ifndef ABS
# define ABS(a) ( ((a)<0) ? -(a) : (a) )
#endif

#ifndef ROTR
# define ROTR(x,n) (((uintmax_t)(x) >> (n)) | ((uintmax_t)(x) << ((sizeof(x) * 8) - (n))))
#endif

#ifndef PM3_ROTL
# define PM3_ROTL(x,n) (((uintmax_t)(x) << (n)) | ((uintmax_t)(x) >> ((sizeof(x) * 8) - (n))))
#endif

// endian change for 32bit
#ifndef BSWAP_32
# define BSWAP_32(x) \
    ((((x) & 0xff000000) >> 24) | (((x) & 0x00ff0000) >>  8) | \
     (((x) & 0x0000ff00) <<  8) | (((x) & 0x000000ff) << 24))
#endif

// convert 4 bytes to U32 in big endian
#ifndef BYTES2UINT32_BE
# define BYTES2UINT32_BE(x) ((x[0] << 24) | (x[1] << 16) | (x[2] << 8) | (x[3]))
#endif

#define EVEN 0
#define ODD  1

// Nibble logic
#ifndef NIBBLE_HIGH
# define NIBBLE_HIGH(b) ( ((b) & 0xF0) >> 4 )
#endif

#ifndef NIBBLE_LOW
# define NIBBLE_LOW(b)  ((b) & 0x0F )
#endif

#ifndef CRUMB
# define CRUMB(b,p)    (((b & (0x3 << p) ) >> p ) & 0xF)
#endif

#ifndef SWAP_NIBBLE
# define SWAP_NIBBLE(b)  ( (NIBBLE_LOW(b)<< 4) | NIBBLE_HIGH(b))
#endif

// bit stream operations
#define TEST_BIT(data, i) (*(data + (i / 8)) >> (7 - (i % 8))) & 1
#define SET_BIT(data, i)   *(data + (i / 8)) |= (1 << (7 - (i % 8)))
#define CLEAR_BIT(data, i) *(data + (i / 8)) &= ~(1 << (7 - (i % 8)))
#define FLIP_BIT(data, i)  *(data + (i / 8)) ^= (1 << (7 - (i % 8)))

#endif
