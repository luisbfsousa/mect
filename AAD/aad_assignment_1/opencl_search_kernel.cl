#define ROTLEFT(a,b) rotate((a), (uint)(b))

#define SHA1_ROUND1(a,b,c,d,e,w,k) \
  e += ROTLEFT(a,5) + ((b & c) | (~b & d)) + w + k; \
  b = ROTLEFT(b,30);

#define SHA1_ROUND2(a,b,c,d,e,w,k) \
  e += ROTLEFT(a,5) + (b ^ c ^ d) + w + k; \
  b = ROTLEFT(b,30);

#define SHA1_ROUND3(a,b,c,d,e,w,k) \
  e += ROTLEFT(a,5) + ((b & c) | (b & d) | (c & d)) + w + k; \
  b = ROTLEFT(b,30);

#define SHA1_ROUND4(a,b,c,d,e,w,k) \
  e += ROTLEFT(a,5) + (b ^ c ^ d) + w + k; \
  b = ROTLEFT(b,30);

inline void compute_sha1_optimized(uint *coin_words, uint *hash)
{
  uint a = 0x67452301u;
  uint b = 0xEFCDAB89u;
  uint c = 0x98BADCFEu;
  uint d = 0x10325476u;
  uint e = 0xC3D2E1F0u;
  
  uint w[16];
  
  w[0] = coin_words[0];
  w[1] = coin_words[1];
  w[2] = coin_words[2];
  w[3] = coin_words[3];
  w[4] = coin_words[4];
  w[5] = coin_words[5];
  w[6] = coin_words[6];
  w[7] = coin_words[7];
  w[8] = coin_words[8];
  w[9] = coin_words[9];
  w[10] = coin_words[10];
  w[11] = coin_words[11];
  w[12] = coin_words[12];
  w[13] = coin_words[13];
  w[14] = 0u;
  w[15] = 440u; // 55*8
  
  SHA1_ROUND1(a,b,c,d,e,w[0],0x5A827999u);
  SHA1_ROUND1(e,a,b,c,d,w[1],0x5A827999u);
  SHA1_ROUND1(d,e,a,b,c,w[2],0x5A827999u);
  SHA1_ROUND1(c,d,e,a,b,w[3],0x5A827999u);
  SHA1_ROUND1(b,c,d,e,a,w[4],0x5A827999u);
  SHA1_ROUND1(a,b,c,d,e,w[5],0x5A827999u);
  SHA1_ROUND1(e,a,b,c,d,w[6],0x5A827999u);
  SHA1_ROUND1(d,e,a,b,c,w[7],0x5A827999u);
  SHA1_ROUND1(c,d,e,a,b,w[8],0x5A827999u);
  SHA1_ROUND1(b,c,d,e,a,w[9],0x5A827999u);
  SHA1_ROUND1(a,b,c,d,e,w[10],0x5A827999u);
  SHA1_ROUND1(e,a,b,c,d,w[11],0x5A827999u);
  SHA1_ROUND1(d,e,a,b,c,w[12],0x5A827999u);
  SHA1_ROUND1(c,d,e,a,b,w[13],0x5A827999u);
  SHA1_ROUND1(b,c,d,e,a,w[14],0x5A827999u);
  SHA1_ROUND1(a,b,c,d,e,w[15],0x5A827999u);
  
  w[0] = ROTLEFT((w[13] ^ w[8] ^ w[2] ^ w[0]), 1); SHA1_ROUND1(e,a,b,c,d,w[0],0x5A827999u);
  w[1] = ROTLEFT((w[14] ^ w[9] ^ w[3] ^ w[1]), 1); SHA1_ROUND1(d,e,a,b,c,w[1],0x5A827999u);
  w[2] = ROTLEFT((w[15] ^ w[10] ^ w[4] ^ w[2]), 1); SHA1_ROUND1(c,d,e,a,b,w[2],0x5A827999u);
  w[3] = ROTLEFT((w[0] ^ w[11] ^ w[5] ^ w[3]), 1); SHA1_ROUND1(b,c,d,e,a,w[3],0x5A827999u);
  
  w[4] = ROTLEFT((w[1] ^ w[12] ^ w[6] ^ w[4]), 1); SHA1_ROUND2(a,b,c,d,e,w[4],0x6ED9EBA1u);
  w[5] = ROTLEFT((w[2] ^ w[13] ^ w[7] ^ w[5]), 1); SHA1_ROUND2(e,a,b,c,d,w[5],0x6ED9EBA1u);
  w[6] = ROTLEFT((w[3] ^ w[14] ^ w[8] ^ w[6]), 1); SHA1_ROUND2(d,e,a,b,c,w[6],0x6ED9EBA1u);
  w[7] = ROTLEFT((w[4] ^ w[15] ^ w[9] ^ w[7]), 1); SHA1_ROUND2(c,d,e,a,b,w[7],0x6ED9EBA1u);
  w[8] = ROTLEFT((w[5] ^ w[0] ^ w[10] ^ w[8]), 1); SHA1_ROUND2(b,c,d,e,a,w[8],0x6ED9EBA1u);
  w[9] = ROTLEFT((w[6] ^ w[1] ^ w[11] ^ w[9]), 1); SHA1_ROUND2(a,b,c,d,e,w[9],0x6ED9EBA1u);
  w[10] = ROTLEFT((w[7] ^ w[2] ^ w[12] ^ w[10]), 1); SHA1_ROUND2(e,a,b,c,d,w[10],0x6ED9EBA1u);
  w[11] = ROTLEFT((w[8] ^ w[3] ^ w[13] ^ w[11]), 1); SHA1_ROUND2(d,e,a,b,c,w[11],0x6ED9EBA1u);
  w[12] = ROTLEFT((w[9] ^ w[4] ^ w[14] ^ w[12]), 1); SHA1_ROUND2(c,d,e,a,b,w[12],0x6ED9EBA1u);
  w[13] = ROTLEFT((w[10] ^ w[5] ^ w[15] ^ w[13]), 1); SHA1_ROUND2(b,c,d,e,a,w[13],0x6ED9EBA1u);
  w[14] = ROTLEFT((w[11] ^ w[6] ^ w[0] ^ w[14]), 1); SHA1_ROUND2(a,b,c,d,e,w[14],0x6ED9EBA1u);
  w[15] = ROTLEFT((w[12] ^ w[7] ^ w[1] ^ w[15]), 1); SHA1_ROUND2(e,a,b,c,d,w[15],0x6ED9EBA1u);
  w[0] = ROTLEFT((w[13] ^ w[8] ^ w[2] ^ w[0]), 1); SHA1_ROUND2(d,e,a,b,c,w[0],0x6ED9EBA1u);
  w[1] = ROTLEFT((w[14] ^ w[9] ^ w[3] ^ w[1]), 1); SHA1_ROUND2(c,d,e,a,b,w[1],0x6ED9EBA1u);
  w[2] = ROTLEFT((w[15] ^ w[10] ^ w[4] ^ w[2]), 1); SHA1_ROUND2(b,c,d,e,a,w[2],0x6ED9EBA1u);
  w[3] = ROTLEFT((w[0] ^ w[11] ^ w[5] ^ w[3]), 1); SHA1_ROUND2(a,b,c,d,e,w[3],0x6ED9EBA1u);
  w[4] = ROTLEFT((w[1] ^ w[12] ^ w[6] ^ w[4]), 1); SHA1_ROUND2(e,a,b,c,d,w[4],0x6ED9EBA1u);
  w[5] = ROTLEFT((w[2] ^ w[13] ^ w[7] ^ w[5]), 1); SHA1_ROUND2(d,e,a,b,c,w[5],0x6ED9EBA1u);
  w[6] = ROTLEFT((w[3] ^ w[14] ^ w[8] ^ w[6]), 1); SHA1_ROUND2(c,d,e,a,b,w[6],0x6ED9EBA1u);
  w[7] = ROTLEFT((w[4] ^ w[15] ^ w[9] ^ w[7]), 1); SHA1_ROUND2(b,c,d,e,a,w[7],0x6ED9EBA1u);
  
  w[8] = ROTLEFT((w[5] ^ w[0] ^ w[10] ^ w[8]), 1); SHA1_ROUND3(a,b,c,d,e,w[8],0x8F1BBCDCu);
  w[9] = ROTLEFT((w[6] ^ w[1] ^ w[11] ^ w[9]), 1); SHA1_ROUND3(e,a,b,c,d,w[9],0x8F1BBCDCu);
  w[10] = ROTLEFT((w[7] ^ w[2] ^ w[12] ^ w[10]), 1); SHA1_ROUND3(d,e,a,b,c,w[10],0x8F1BBCDCu);
  w[11] = ROTLEFT((w[8] ^ w[3] ^ w[13] ^ w[11]), 1); SHA1_ROUND3(c,d,e,a,b,w[11],0x8F1BBCDCu);
  w[12] = ROTLEFT((w[9] ^ w[4] ^ w[14] ^ w[12]), 1); SHA1_ROUND3(b,c,d,e,a,w[12],0x8F1BBCDCu);
  w[13] = ROTLEFT((w[10] ^ w[5] ^ w[15] ^ w[13]), 1); SHA1_ROUND3(a,b,c,d,e,w[13],0x8F1BBCDCu);
  w[14] = ROTLEFT((w[11] ^ w[6] ^ w[0] ^ w[14]), 1); SHA1_ROUND3(e,a,b,c,d,w[14],0x8F1BBCDCu);
  w[15] = ROTLEFT((w[12] ^ w[7] ^ w[1] ^ w[15]), 1); SHA1_ROUND3(d,e,a,b,c,w[15],0x8F1BBCDCu);
  w[0] = ROTLEFT((w[13] ^ w[8] ^ w[2] ^ w[0]), 1); SHA1_ROUND3(c,d,e,a,b,w[0],0x8F1BBCDCu);
  w[1] = ROTLEFT((w[14] ^ w[9] ^ w[3] ^ w[1]), 1); SHA1_ROUND3(b,c,d,e,a,w[1],0x8F1BBCDCu);
  w[2] = ROTLEFT((w[15] ^ w[10] ^ w[4] ^ w[2]), 1); SHA1_ROUND3(a,b,c,d,e,w[2],0x8F1BBCDCu);
  w[3] = ROTLEFT((w[0] ^ w[11] ^ w[5] ^ w[3]), 1); SHA1_ROUND3(e,a,b,c,d,w[3],0x8F1BBCDCu);
  w[4] = ROTLEFT((w[1] ^ w[12] ^ w[6] ^ w[4]), 1); SHA1_ROUND3(d,e,a,b,c,w[4],0x8F1BBCDCu);
  w[5] = ROTLEFT((w[2] ^ w[13] ^ w[7] ^ w[5]), 1); SHA1_ROUND3(c,d,e,a,b,w[5],0x8F1BBCDCu);
  w[6] = ROTLEFT((w[3] ^ w[14] ^ w[8] ^ w[6]), 1); SHA1_ROUND3(b,c,d,e,a,w[6],0x8F1BBCDCu);
  w[7] = ROTLEFT((w[4] ^ w[15] ^ w[9] ^ w[7]), 1); SHA1_ROUND3(a,b,c,d,e,w[7],0x8F1BBCDCu);
  w[8] = ROTLEFT((w[5] ^ w[0] ^ w[10] ^ w[8]), 1); SHA1_ROUND3(e,a,b,c,d,w[8],0x8F1BBCDCu);
  w[9] = ROTLEFT((w[6] ^ w[1] ^ w[11] ^ w[9]), 1); SHA1_ROUND3(d,e,a,b,c,w[9],0x8F1BBCDCu);
  w[10] = ROTLEFT((w[7] ^ w[2] ^ w[12] ^ w[10]), 1); SHA1_ROUND3(c,d,e,a,b,w[10],0x8F1BBCDCu);
  w[11] = ROTLEFT((w[8] ^ w[3] ^ w[13] ^ w[11]), 1); SHA1_ROUND3(b,c,d,e,a,w[11],0x8F1BBCDCu);
  
  w[12] = ROTLEFT((w[9] ^ w[4] ^ w[14] ^ w[12]), 1); SHA1_ROUND4(a,b,c,d,e,w[12],0xCA62C1D6u);
  w[13] = ROTLEFT((w[10] ^ w[5] ^ w[15] ^ w[13]), 1); SHA1_ROUND4(e,a,b,c,d,w[13],0xCA62C1D6u);
  w[14] = ROTLEFT((w[11] ^ w[6] ^ w[0] ^ w[14]), 1); SHA1_ROUND4(d,e,a,b,c,w[14],0xCA62C1D6u);
  w[15] = ROTLEFT((w[12] ^ w[7] ^ w[1] ^ w[15]), 1); SHA1_ROUND4(c,d,e,a,b,w[15],0xCA62C1D6u);
  w[0] = ROTLEFT((w[13] ^ w[8] ^ w[2] ^ w[0]), 1); SHA1_ROUND4(b,c,d,e,a,w[0],0xCA62C1D6u);
  w[1] = ROTLEFT((w[14] ^ w[9] ^ w[3] ^ w[1]), 1); SHA1_ROUND4(a,b,c,d,e,w[1],0xCA62C1D6u);
  w[2] = ROTLEFT((w[15] ^ w[10] ^ w[4] ^ w[2]), 1); SHA1_ROUND4(e,a,b,c,d,w[2],0xCA62C1D6u);
  w[3] = ROTLEFT((w[0] ^ w[11] ^ w[5] ^ w[3]), 1); SHA1_ROUND4(d,e,a,b,c,w[3],0xCA62C1D6u);
  w[4] = ROTLEFT((w[1] ^ w[12] ^ w[6] ^ w[4]), 1); SHA1_ROUND4(c,d,e,a,b,w[4],0xCA62C1D6u);
  w[5] = ROTLEFT((w[2] ^ w[13] ^ w[7] ^ w[5]), 1); SHA1_ROUND4(b,c,d,e,a,w[5],0xCA62C1D6u);
  w[6] = ROTLEFT((w[3] ^ w[14] ^ w[8] ^ w[6]), 1); SHA1_ROUND4(a,b,c,d,e,w[6],0xCA62C1D6u);
  w[7] = ROTLEFT((w[4] ^ w[15] ^ w[9] ^ w[7]), 1); SHA1_ROUND4(e,a,b,c,d,w[7],0xCA62C1D6u);
  w[8] = ROTLEFT((w[5] ^ w[0] ^ w[10] ^ w[8]), 1); SHA1_ROUND4(d,e,a,b,c,w[8],0xCA62C1D6u);
  w[9] = ROTLEFT((w[6] ^ w[1] ^ w[11] ^ w[9]), 1); SHA1_ROUND4(c,d,e,a,b,w[9],0xCA62C1D6u);
  w[10] = ROTLEFT((w[7] ^ w[2] ^ w[12] ^ w[10]), 1); SHA1_ROUND4(b,c,d,e,a,w[10],0xCA62C1D6u);
  w[11] = ROTLEFT((w[8] ^ w[3] ^ w[13] ^ w[11]), 1); SHA1_ROUND4(a,b,c,d,e,w[11],0xCA62C1D6u);
  w[12] = ROTLEFT((w[9] ^ w[4] ^ w[14] ^ w[12]), 1); SHA1_ROUND4(e,a,b,c,d,w[12],0xCA62C1D6u);
  w[13] = ROTLEFT((w[10] ^ w[5] ^ w[15] ^ w[13]), 1); SHA1_ROUND4(d,e,a,b,c,w[13],0xCA62C1D6u);
  w[14] = ROTLEFT((w[11] ^ w[6] ^ w[0] ^ w[14]), 1); SHA1_ROUND4(c,d,e,a,b,w[14],0xCA62C1D6u);
  w[15] = ROTLEFT((w[12] ^ w[7] ^ w[1] ^ w[15]), 1); SHA1_ROUND4(b,c,d,e,a,w[15],0xCA62C1D6u);
  
  hash[0] = a + 0x67452301u;
  hash[1] = b + 0xEFCDAB89u;
  hash[2] = c + 0x98BADCFEu;
  hash[3] = d + 0x10325476u;
  hash[4] = e + 0xC3D2E1F0u;
}

__kernel void search_coins_kernel(
    ulong base_nonce,
    ulong num_coins,
    __constant uint *static_words,
    __global uint *found_coins,
    __global int *found_count,
    int max_found)
{
  ulong idx = get_global_id(0);
  if(idx >= num_coins) return;
  
  ulong nonce = base_nonce + idx;
  
  uint coin_words[14];
  uchar *coin_bytes = (uchar *)coin_words;

  coin_words[0] = static_words[0];
  coin_words[1] = static_words[1];
  coin_words[2] = static_words[2];
  coin_words[3] = static_words[3];
  coin_words[4] = static_words[4];
  coin_words[5] = static_words[5];
  coin_words[6] = static_words[6];
  coin_words[7] = static_words[7];
  coin_words[8] = static_words[8];
  coin_words[9] = static_words[9];
  coin_words[10] = static_words[10];
  coin_words[11] = static_words[11];
  coin_words[12] = static_words[12];
  coin_words[13] = static_words[13];
  
  ulong temp_nonce = nonce;
  
  #pragma unroll
  for(int j = 0; j < 10; j++)
  {
    uchar digit = (uchar)(temp_nonce % 95UL);
    coin_bytes[(44 + j) ^ 0x3] = digit + 32;
    temp_nonce /= 95UL;
  }
  
  uint hash[5];
  compute_sha1_optimized(coin_words, hash);
  
  if(hash[0] != 0xAAD20250u)
    return;
  
  int pos = atomic_add(found_count, 1);
  if(pos < max_found)
  {
    __global uint *dest = &found_coins[pos * 14];
    
    dest[0] = coin_words[0];
    dest[1] = coin_words[1];
    dest[2] = coin_words[2];
    dest[3] = coin_words[3];
    dest[4] = coin_words[4];
    dest[5] = coin_words[5];
    dest[6] = coin_words[6];
    dest[7] = coin_words[7];
    dest[8] = coin_words[8];
    dest[9] = coin_words[9];
    dest[10] = coin_words[10];
    dest[11] = coin_words[11];
    dest[12] = coin_words[12];
    dest[13] = coin_words[13];
  }
}