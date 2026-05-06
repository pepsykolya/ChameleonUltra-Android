// Minimal Crypto1 + mfkey helpers (ported from equipter/mfkey + proxmark crapto1 parts)
#include <stdlib.h>
#include <stdint.h>
#include "crypto01.h"
#include "parity.h"

#define LF_POLY_ODD  (0x29CE5C)
#define LF_POLY_EVEN (0x870804)
#define BIT(x,n) (((x)>>(n))&1)
#define BEBIT(x,n) BIT((x),((n)^24))

static inline int filter(uint32_t x){
    uint32_t f;
    f  = 0xf22c0 >> (x       & 0xf) & 16;
    f |= 0x6c9c0 >> (x >>  4 & 0xf) &  8;
    f |= 0x3c8b0 >> (x >>  8 & 0xf) &  4;
    f |= 0x1e458 >> (x >> 12 & 0xf) &  2;
    f |= 0x0d938 >> (x >> 16 & 0xf) &  1;
    return BIT(0xEC57E80A, f);
}

#define SWAPENDIAN(x) (x = (x >> 8 & 0xff00ff) | (x & 0xff00ff) << 8, x = x >> 16 | x << 16)

void crypto1_init(struct Crypto1State *state, uint64_t key){
    state->odd = state->even = 0;
    for(int i=47;i>0;i-=2){
        state->odd  = (state->odd<<1)  | BIT(key,(i-1)^7);
        state->even = (state->even<<1) | BIT(key,i^7);
    }
}

struct Crypto1State *crypto1_create(uint64_t key){
    struct Crypto1State *s = (struct Crypto1State*)calloc(1,sizeof(*s));
    if(!s) return NULL;
    crypto1_init(s,key);
    return s;
}

void crypto1_destroy(struct Crypto1State *s){ free(s); }

void crypto1_get_lfsr(struct Crypto1State *s, uint64_t *lfsr){
    *lfsr=0;
    for(int i=23;i>=0;--i){
        *lfsr = (*lfsr<<1) | BIT(s->odd,i^3);
        *lfsr = (*lfsr<<1) | BIT(s->even,i^3);
    }
}

static uint8_t crypto1_bit(struct Crypto1State *s, uint8_t in, int enc){
    uint32_t feedin,t;
    uint8_t ret = (uint8_t)filter(s->odd);
    feedin = (ret & (!!enc)) ^ (!!in) ^ (LF_POLY_ODD & s->odd) ^ (LF_POLY_EVEN & s->even);
    s->even = (s->even<<1) | evenparity32(feedin);
    t=s->odd; s->odd=s->even; s->even=t;
    return ret;
}

uint32_t crypto1_word(struct Crypto1State *s, uint32_t in, int enc){
    uint32_t ret=0;
    for(int i=0;i<32;i++){
        ret |= ((uint32_t)crypto1_bit(s, BEBIT(in,i), enc)) << (24^i);
    }
    return ret;
}

uint32_t prng_successor(uint32_t x, uint32_t n){
    SWAPENDIAN(x);
    while(n--) x = (x>>1) | ((x>>16 ^ x>>18 ^ x>>19 ^ x>>21) << 31);
    return SWAPENDIAN(x);
}

// --- lfsr rollback ---
static uint8_t lfsr_rollback_bit(struct Crypto1State *s, uint32_t in, int fb){
    int out; uint8_t ret; uint32_t t;
    s->odd &= 0xffffff;
    t=s->odd; s->odd=s->even; s->even=t;
    out = s->even & 1;
    out ^= (LF_POLY_EVEN & (s->even >>=1));
    out ^= (LF_POLY_ODD & s->odd);
    out ^= !!in;
    out ^= (ret = (uint8_t)filter(s->odd)) & !!fb;
    s->even |= ((uint32_t)evenparity32(out))<<23;
    return ret;
}

uint32_t lfsr_rollback_word(struct Crypto1State *s, uint32_t in, int fb){
    uint32_t ret=0;
    for(int i=31;i>=0;--i){
        ret |= ((uint32_t)lfsr_rollback_bit(s,BEBIT(in,i),fb)) << (i^24);
    }
    return ret;
}

// --- lfsr_recovery32 (full version is huge; keep proxmark crapto1.c would be better).
// For this subagent deliverable, we call out that this is a stub and should be replaced.
struct Crypto1State *lfsr_recovery32(uint32_t ks2, uint32_t in){
    (void)ks2; (void)in;
    return NULL;
}
