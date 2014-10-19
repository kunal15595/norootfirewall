#ifndef __CC_H__
#define __CC_H__

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/endian.h>
#include <android/log>
// TODO: #include <compiler.h>
// This file is used in the Google's implementation

/* Data types */
typedef uint8_t u8_t;
typedef uint16_t u16_t;
typedef uint32_t u32_t;

typedef int8_t s8_t;
typedef int16_t s16_t;
typedef int32_t s32_t;

typedef intptr_t mem_ptr_t;

#define MAIN_LOG_TAG "wIP"


/* Printf formatters for data types */
#define U16_F "hu"
#define S16_F "d"
#define X16_F "hx"
#define U32_F "u"
#define S32_F "d"
#define X32_F "x"
#define SZT_F "uz"


/* Endianness */
#if BYTE_ORDER == LITTLE_ENDIAN
    #define LWIP_PLATFORM_BYTESWAP 1
    #define LWIP_PLATFORM_HTONS(x) ( (((u16_t)(x))>>8) | (((x)&0xFF)<<8) )
    #define LWIP_PLATFORM_HTONL(x) ( (((u32_t)(x))>>24) | (((x)&0xFF0000)>>8) \ 
                                   | (((x)&0xFF00)<<8) | (((x)&0xFF)<<24) )
#endif


/* Computing checksums */
#define LWIP_CHKSUM_ALGORITHM 2

/* Platform specific diagnostic output */
#define LWIP_PLATFORM_DIAG(x) do {} while (0)
#define LWIP_PLATFORM_ASSERT(x) do {} while (0)

/* Structure packing */
#define PACK_STRUCT_STRUCT __PACKED


#endif /* __CC_H__ */

