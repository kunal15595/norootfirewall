#ifndef LWIP_1_4_1_SRC_INCLUDE_CC_H_
#define LWIP_1_4_1_SRC_INCLUDE_CC_H_

#include <stdint.h>
#include <sys/endian.h>
#include <stdlib.h>
#include <stdio.h>
#include "log.h"

typedef uint8_t u8_t;
typedef uint16_t u16_t;
typedef uint32_t u32_t;
typedef int8_t s8_t;
typedef int16_t s16_t;
typedef int32_t s32_t;

typedef intptr_t mem_ptr_t;

#define U16_F "hu"
#define S16_F "d"
#define X16_F "hx"
#define U32_F "u"
#define S32_F "d"
#define X32_F "x"
#define SZT_F "uz"

#if BYTE_ORDER == LITTLE_ENDIAN
#define LWIP_PLATFORM_BYTESWAP 1
#define LWIP_PLATFORM_HTONS(x) ( (((u16_t)(x))>>8) | (((x)&0xFF)<<8) )
#define LWIP_PLATFORM_HTONL(x) ( (((u32_t)(x))>>24) | (((x)&0xFF0000)>>8) \
| (((x)&0xFF00)<<8) | (((x)&0xFF)<<24) )
#endif

#define LWIP_CHKSUM_ALGORITHM 2

/* Compiler hints for packing structures */
#define PACK_STRUCT_FIELD(x) x
#define PACK_STRUCT_STRUCT __attribute__((packed))
#define PACK_STRUCT_BEGIN
#define PACK_STRUCT_END

/* Platform-specific diagnostic output */
#define LWIP_PLATFORM_DIAG(x)	do {printf x;} while(0)

#define LWIP_PLATFORM_ASSERT(x) do {LOGE("Assertion \"%s\" failed", x); \
	                                fflush(NULL); abort();} while(0)

#endif
