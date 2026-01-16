//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//
// integer data types
//

#ifndef AAD_DATA_TYPES
#define AAD_DATA_TYPES

//
// scalar data types (for a typical 64-bit processor)
//

typedef   signed  char s08_t;  //  8-bit signed integer
typedef unsigned  char u08_t;  //  8-bit unsigned integer
typedef   signed short s16_t;  // 16-bit signed integer
typedef unsigned short u16_t;  // 16-bit unsigned integer
typedef   signed   int s32_t;  // 32-bit signed integer
typedef unsigned   int u32_t;  // 32-bit unsigned integer
typedef   signed  long s64_t;  // 64-bit signed integer
typedef unsigned  long u64_t;  // 64-bit unsigned integer


//
// vector data types (this probably will only work on the gcc compiler)
//

#if defined(__AVX__)
typedef int v4si  __attribute__((vector_size(16))) __attribute__((aligned(16)));
#endif
#if defined(__AVX2__)
typedef int v8si  __attribute__((vector_size(32))) __attribute__((aligned(32)));
#endif
#if defined(__AVX512F__)
typedef int v16si __attribute__((vector_size(64))) __attribute__((aligned(64)));
#endif
#if defined(__ARM_NEON)
# include <arm_neon.h>
#endif


//
// the end!
//

#endif
