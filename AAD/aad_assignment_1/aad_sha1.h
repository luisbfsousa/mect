//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//
// template for the computation of the SHA1 secure hash
//


//
// compute the SHA1 secure hash of a custom message with exactly 55 bytes
//
// the general SHA1 secure hash algorithm ingests data in chunks of 64 bytes; at the end there must
// be one byte of padding, with value 0x80, zero or more bytes of zeros, also for padding, appended
// until the last chunk has 56 bytes, and a final 8 byte integer holding the number of bits of the
// entire message
//
// by restricting the data to have 55 bytes or less the algorithm can be simplified, and only one
// chunk is needed; that is what is done below
//

#ifndef AAD_SHA1
#define AAD_SHA1


//
// number of threads in each CUDA block
//
// we place this here to simplify things (aad_sha1_cuda_kernel.cu includes this file...)
//
#define RECOMENDED_CUDA_BLOCK_SIZE  128


//
// each custom message has exactly 55 bytes, and must be followed by an additional byte with the
// value 0x80
// these 55+1=56 bytes must be stored in a 32-bit integer array with 14 elements as illustrated in
// the test code below; the secure hash has to be interpreted in the same way --- don't blame the
// teacher for this; that is how the SHA1 secure hash is described in the 3174 request for comments
// (https://datatracker.ietf.org/doc/html/rfc3174)
//
// the SHA1 secure hash of the 55 bytes message is computed using a macro called CUSTOM_SHA1_CODE
// it must be customized using the following additional macros:
//   T           --- the data type
//   C(c)        --- how to expand the constant c
//   ROTATE(x,n) --- how to rotate x left by n bits
//   DATA(idx)   --- how to access the data at index idx, 0 <= idx <= 13
//   HASH(idx)   --- how to access the hash at index idx, 0 <= idx <= 4
// see aad_sha1_cpu.h for examples
//
// each custom message is stored in the locations
//   DATA(0), DATA(1), ..., DATA(13)
// each SHA1 secure hash is stored in the locations
//   HASH(0), HASH(1), ..., HASH(4)
//

//
// first group of 20 iterations (0 <= t <= 19)
//
#define SHA1_F1(x,y,z)  ((x & y) | (~x & z))
#define SHA1_K1         0x5A827999u

//
// second group of 20 iterations (20 <= t <= 39)
//
#define SHA1_F2(x,y,z)  (x ^ y ^ z)
#define SHA1_K2         0x6ED9EBA1u

//
// third group of 20 iterations (40 <= t <= 59)
//
#define SHA1_F3(x,y,z)  ((x & y) | (x & z) | (y & z))
#define SHA1_K3         0x8F1BBCDCu

//
// fourth group of 20 iterations (60 <= t <= 79)
//
#define SHA1_F4(x,y,z)  (x ^ y ^ z)
#define SHA1_K4         0xCA62C1D6u

//
// data mixing function
//
#define SHA1_D(t)                                                                            \
  do                                                                                         \
  {                                                                                          \
    T tmp = w[((t) - 3) & 15] ^ w[((t) - 8) & 15] ^ w[((t) - 14) & 15] ^ w[((t) - 16) & 15]; \
    w[(t) & 15] = ROTATE(tmp,1);                                                             \
  }                                                                                          \
  while(0)

//
// state mixing function
//
#define SHA1_S(F,t,K)                                                                        \
  do                                                                                         \
  {                                                                                          \
    T tmp = ROTATE(a,5) + F(b,c,d) + e + w[(t) & 15] + C(K);                                 \
    e = d;                                                                                   \
    d = c;                                                                                   \
    c = ROTATE(b,30);                                                                        \
    b = a;                                                                                   \
    a = tmp;                                                                                 \
  }                                                                                          \
  while(0)

//
// the CUSTOM_SHA1_CODE macro, for a little-endian processor
//
// everything is loop unrolled to make sure all indices are static integers, so the compiler
// has no excuse to produce sub-optimal code (the w[16] array can even become 16 separate
// integer variables, the CUDA compiler actually does this)
//
#define CUSTOM_SHA1_CODE()                                                                  \
  do                                                                                        \
  {                                                                                         \
    /* local variables */                                                                   \
    T a,b,c,d,e,w[16];                                                                      \
    /* initial state */                                                                     \
    a = C(0x67452301u);                                                                     \
    b = C(0xEFCDAB89u);                                                                     \
    c = C(0x98BADCFEu);                                                                     \
    d = C(0x10325476u);                                                                     \
    e = C(0xC3D2E1F0u);                                                                     \
    /* copy data to the internal buffer */                                                  \
    w[ 0] = DATA( 0);                                                                       \
    w[ 1] = DATA( 1);                                                                       \
    w[ 2] = DATA( 2);                                                                       \
    w[ 3] = DATA( 3);                                                                       \
    w[ 4] = DATA( 4);                                                                       \
    w[ 5] = DATA( 5);                                                                       \
    w[ 6] = DATA( 6);                                                                       \
    w[ 7] = DATA( 7);                                                                       \
    w[ 8] = DATA( 8);                                                                       \
    w[ 9] = DATA( 9);                                                                       \
    w[10] = DATA(10);                                                                       \
    w[11] = DATA(11);                                                                       \
    w[12] = DATA(12);                                                                       \
    w[13] = DATA(13); /* WARNING: DATA(13) & 0xFF must be 0x80 (SHA1 padding) */            \
    w[14] = C(0);                                                                           \
    w[15] = C(440); /* the message has 55*8 bits */                                         \
    /* first group of 20 iterations (0 <= t <= 19) */                                       \
                SHA1_S(SHA1_F1, 0,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 1,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 2,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 3,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 4,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 5,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 6,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 7,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 8,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1, 9,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,10,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,11,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,12,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,13,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,14,SHA1_K1);                                                 \
                SHA1_S(SHA1_F1,15,SHA1_K1);                                                 \
    SHA1_D(16); SHA1_S(SHA1_F1,16,SHA1_K1);                                                 \
    SHA1_D(17); SHA1_S(SHA1_F1,17,SHA1_K1);                                                 \
    SHA1_D(18); SHA1_S(SHA1_F1,18,SHA1_K1);                                                 \
    SHA1_D(19); SHA1_S(SHA1_F1,19,SHA1_K1);                                                 \
    /* second group of 20 iterations (20 <= t <= 39) */                                     \
    SHA1_D(20); SHA1_S(SHA1_F2,20,SHA1_K2);                                                 \
    SHA1_D(21); SHA1_S(SHA1_F2,21,SHA1_K2);                                                 \
    SHA1_D(22); SHA1_S(SHA1_F2,22,SHA1_K2);                                                 \
    SHA1_D(23); SHA1_S(SHA1_F2,23,SHA1_K2);                                                 \
    SHA1_D(24); SHA1_S(SHA1_F2,24,SHA1_K2);                                                 \
    SHA1_D(25); SHA1_S(SHA1_F2,25,SHA1_K2);                                                 \
    SHA1_D(26); SHA1_S(SHA1_F2,26,SHA1_K2);                                                 \
    SHA1_D(27); SHA1_S(SHA1_F2,27,SHA1_K2);                                                 \
    SHA1_D(28); SHA1_S(SHA1_F2,28,SHA1_K2);                                                 \
    SHA1_D(29); SHA1_S(SHA1_F2,29,SHA1_K2);                                                 \
    SHA1_D(30); SHA1_S(SHA1_F2,30,SHA1_K2);                                                 \
    SHA1_D(31); SHA1_S(SHA1_F2,31,SHA1_K2);                                                 \
    SHA1_D(32); SHA1_S(SHA1_F2,32,SHA1_K2);                                                 \
    SHA1_D(33); SHA1_S(SHA1_F2,33,SHA1_K2);                                                 \
    SHA1_D(34); SHA1_S(SHA1_F2,34,SHA1_K2);                                                 \
    SHA1_D(35); SHA1_S(SHA1_F2,35,SHA1_K2);                                                 \
    SHA1_D(36); SHA1_S(SHA1_F2,36,SHA1_K2);                                                 \
    SHA1_D(37); SHA1_S(SHA1_F2,37,SHA1_K2);                                                 \
    SHA1_D(38); SHA1_S(SHA1_F2,38,SHA1_K2);                                                 \
    SHA1_D(39); SHA1_S(SHA1_F2,39,SHA1_K2);                                                 \
    /* third group of 20 iterations (40 <= t <= 59) */                                      \
    SHA1_D(40); SHA1_S(SHA1_F3,40,SHA1_K3);                                                 \
    SHA1_D(41); SHA1_S(SHA1_F3,41,SHA1_K3);                                                 \
    SHA1_D(42); SHA1_S(SHA1_F3,42,SHA1_K3);                                                 \
    SHA1_D(43); SHA1_S(SHA1_F3,43,SHA1_K3);                                                 \
    SHA1_D(44); SHA1_S(SHA1_F3,44,SHA1_K3);                                                 \
    SHA1_D(45); SHA1_S(SHA1_F3,45,SHA1_K3);                                                 \
    SHA1_D(46); SHA1_S(SHA1_F3,46,SHA1_K3);                                                 \
    SHA1_D(47); SHA1_S(SHA1_F3,47,SHA1_K3);                                                 \
    SHA1_D(48); SHA1_S(SHA1_F3,48,SHA1_K3);                                                 \
    SHA1_D(49); SHA1_S(SHA1_F3,49,SHA1_K3);                                                 \
    SHA1_D(50); SHA1_S(SHA1_F3,50,SHA1_K3);                                                 \
    SHA1_D(51); SHA1_S(SHA1_F3,51,SHA1_K3);                                                 \
    SHA1_D(52); SHA1_S(SHA1_F3,52,SHA1_K3);                                                 \
    SHA1_D(53); SHA1_S(SHA1_F3,53,SHA1_K3);                                                 \
    SHA1_D(54); SHA1_S(SHA1_F3,54,SHA1_K3);                                                 \
    SHA1_D(55); SHA1_S(SHA1_F3,55,SHA1_K3);                                                 \
    SHA1_D(56); SHA1_S(SHA1_F3,56,SHA1_K3);                                                 \
    SHA1_D(57); SHA1_S(SHA1_F3,57,SHA1_K3);                                                 \
    SHA1_D(58); SHA1_S(SHA1_F3,58,SHA1_K3);                                                 \
    SHA1_D(59); SHA1_S(SHA1_F3,59,SHA1_K3);                                                 \
    /* fourth group of 20 iterations (60 <= t <= 79) */                                     \
    SHA1_D(60); SHA1_S(SHA1_F4,60,SHA1_K4);                                                 \
    SHA1_D(61); SHA1_S(SHA1_F4,61,SHA1_K4);                                                 \
    SHA1_D(62); SHA1_S(SHA1_F4,62,SHA1_K4);                                                 \
    SHA1_D(63); SHA1_S(SHA1_F4,63,SHA1_K4);                                                 \
    SHA1_D(64); SHA1_S(SHA1_F4,64,SHA1_K4);                                                 \
    SHA1_D(65); SHA1_S(SHA1_F4,65,SHA1_K4);                                                 \
    SHA1_D(66); SHA1_S(SHA1_F4,66,SHA1_K4);                                                 \
    SHA1_D(67); SHA1_S(SHA1_F4,67,SHA1_K4);                                                 \
    SHA1_D(68); SHA1_S(SHA1_F4,68,SHA1_K4);                                                 \
    SHA1_D(69); SHA1_S(SHA1_F4,69,SHA1_K4);                                                 \
    SHA1_D(70); SHA1_S(SHA1_F4,70,SHA1_K4);                                                 \
    SHA1_D(71); SHA1_S(SHA1_F4,71,SHA1_K4);                                                 \
    SHA1_D(72); SHA1_S(SHA1_F4,72,SHA1_K4);                                                 \
    SHA1_D(73); SHA1_S(SHA1_F4,73,SHA1_K4);                                                 \
    SHA1_D(74); SHA1_S(SHA1_F4,74,SHA1_K4);                                                 \
    SHA1_D(75); SHA1_S(SHA1_F4,75,SHA1_K4);                                                 \
    SHA1_D(76); SHA1_S(SHA1_F4,76,SHA1_K4);                                                 \
    SHA1_D(77); SHA1_S(SHA1_F4,77,SHA1_K4);                                                 \
    SHA1_D(78); SHA1_S(SHA1_F4,78,SHA1_K4);                                                 \
    SHA1_D(79); SHA1_S(SHA1_F4,79,SHA1_K4);                                                 \
    /* update state (in this special case, finish) */                                       \
    HASH(0) = a + C(0x67452301u);                                                           \
    HASH(1) = b + C(0xEFCDAB89u);                                                           \
    HASH(2) = c + C(0x98BADCFEu);                                                           \
    HASH(3) = d + C(0x10325476u);                                                           \
    HASH(4) = e + C(0xC3D2E1F0u);                                                           \
  }                                                                                         \
  while(0)


//
// the end!
//

#endif
