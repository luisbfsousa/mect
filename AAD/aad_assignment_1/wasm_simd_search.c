#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <wasm_simd128.h>
#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#define EXPORT EMSCRIPTEN_KEEPALIVE
#else
#define EXPORT
#endif

#include "aad_data_types.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"
#define DETI_COIN_SIGNATURE 0xAAD20250u

#define VROTL(x,n) wasm_v128_or(wasm_i32x4_shl(x,(n)), wasm_u32x4_shr(x,32-(n)))
#define F1(x,y,z) wasm_v128_or(wasm_v128_and(x,y), wasm_v128_and(wasm_v128_not(x), z))
#define F2(x,y,z) wasm_v128_xor(wasm_v128_xor(x,y), z)
#define F3(x,y,z) wasm_v128_or(wasm_v128_or(wasm_v128_and(x,y), wasm_v128_and(x,z)), wasm_v128_and(y,z))
#define F4(x,y,z) wasm_v128_xor(wasm_v128_xor(x,y), z)

static void sha1_4(const v128_t *data,v128_t *out){
  v128_t a=wasm_i32x4_splat(0x67452301u);
  v128_t b=wasm_i32x4_splat(0xEFCDAB89u);
  v128_t c=wasm_i32x4_splat(0x98BADCFEu);
  v128_t d=wasm_i32x4_splat(0x10325476u);
  v128_t e=wasm_i32x4_splat(0xC3D2E1F0u);
  v128_t w[16];
  for(int i=0;i<14;i++) w[i]=data[i]; w[14]=wasm_i32x4_splat(0); w[15]=wasm_i32x4_splat(440);
  v128_t K1=wasm_i32x4_splat(0x5A827999u), K2=wasm_i32x4_splat(0x6ED9EBA1u), K3=wasm_i32x4_splat(0x8F1BBCDCu), K4=wasm_i32x4_splat(0xCA62C1D6u);
  for(int t=0;t<80;t++){
    if(t>=16){ v128_t tmp = wasm_v128_xor(w[(t-3)&15], wasm_v128_xor(w[(t-8)&15], wasm_v128_xor(w[(t-14)&15], w[(t-16)&15]))); w[t&15]=VROTL(tmp,1);}    
    v128_t f,k; if(t<20){ f=F1(b,c,d); k=K1; } else if(t<40){ f=F2(b,c,d); k=K2; } else if(t<60){ f=F3(b,c,d); k=K3; } else { f=F4(b,c,d); k=K4; }
    v128_t tmp = wasm_i32x4_add(wasm_i32x4_add(wasm_i32x4_add(wasm_i32x4_add(VROTL(a,5), f), e), w[t&15]), k);
    e=d; d=c; c=VROTL(b,30); b=a; a=tmp;
  }
  out[0]=wasm_i32x4_add(a,wasm_i32x4_splat(0x67452301u));
  out[1]=wasm_i32x4_add(b,wasm_i32x4_splat(0xEFCDAB89u));
  out[2]=wasm_i32x4_add(c,wasm_i32x4_splat(0x98BADCFEu));
  out[3]=wasm_i32x4_add(d,wasm_i32x4_splat(0x10325476u));
  out[4]=wasm_i32x4_add(e,wasm_i32x4_splat(0xC3D2E1F0u));
}

static void nonce_hex_lane(u32_t nonce,u08_t *coin){ static const char hx[]="0123456789abcdef"; int pos; for(pos=53; pos>=40; pos--){ coin[pos^3]=hx[nonce & 0xF]; nonce >>=4; if(!nonce){ while(--pos>=40) coin[pos^3]=' '; break; }} }
static void fill_space(u64_t *seed,u08_t *coin){ for(int i=12;i<40;i++){ *seed=*seed*6364136223846793005ULL+1442695040888963407ULL; u08_t ch=(u08_t)(32+(*seed % 95)); if(ch=='\n') ch='_'; coin[i^3]=ch; }}

static u64_t g_attempts_simd=0; static u32_t g_coins_simd=0; static u32_t g_first_nonce_simd=0; static u08_t g_first_bytes_simd[55]; static u32_t g_first_len_simd=0;
static u08_t g_coins_buf_simd[8192]; static u32_t g_coins_buf_len_simd=0;
static u32_t g_mask_simd = 0xFFFFFFFFu;
static u32_t g_hex_chars_simd = 8;     
EXPORT u64_t get_attempts_simd(void){ return g_attempts_simd; }
EXPORT u32_t get_coins_found_simd(void){ return g_coins_simd; }
EXPORT u32_t get_first_coin_nonce_simd(void){ return g_first_nonce_simd; }
EXPORT u32_t get_first_coin_ptr_simd(void){ return g_first_len_simd? (u32_t)(uintptr_t)g_first_bytes_simd:0; }
EXPORT u32_t get_first_coin_length_simd(void){ return g_first_len_simd; }
EXPORT u32_t get_coins_buffer_ptr_simd(void){ return g_coins_buf_len_simd? (u32_t)(uintptr_t)g_coins_buf_simd : 0; }
EXPORT u32_t get_coins_buffer_length_simd(void){ return g_coins_buf_len_simd; }

EXPORT void set_difficulty_simd(u32_t hex_chars){
  if(hex_chars<1) hex_chars=1; if(hex_chars>8) hex_chars=8;
  g_hex_chars_simd = hex_chars;
  if(hex_chars==8) g_mask_simd = 0xFFFFFFFFu; else g_mask_simd = 0xFFFFFFFFu << ((8-hex_chars)*4);
}

EXPORT void search_coins_simd(u32_t start_nonce,u32_t iterations,const char *prefix){
  g_attempts_simd=0; g_coins_simd=0; g_first_nonce_simd=0; g_first_len_simd=0; g_coins_buf_len_simd=0;
  u32_t full = iterations/4, rem = iterations%4; int plen=0; if(prefix){ while(prefix[plen] && plen<28) plen++; }
  union { u08_t c[64]; u32_t i[16]; } coins[4];
  for(int l=0;l<4;l++){ memset(&coins[l],0,sizeof(coins[l])); const char *hdr="DETI coin 2 "; for(int i=0;i<12;i++) coins[l].c[i^3]=(u08_t)hdr[i]; u64_t seed=(u64_t)(start_nonce+l)*6364136223846793005ULL+1442695040888963407ULL; fill_space(&seed,coins[l].c); if(plen){ for(int i=0;i<plen;i++){ u08_t ch=(u08_t)prefix[i]; if(ch=='\n') ch='_'; coins[l].c[(12+i)^3]=ch; }} coins[l].c[54^3]='\n'; coins[l].c[55^3]=0x80; coins[l].i[15]=440; }
  for(u32_t blk=0; blk<full; blk++){
    u32_t base=start_nonce+blk*4; for(int lane=0; lane<4; lane++) nonce_hex_lane(base+lane, coins[lane].c);
    v128_t data[14]; for(int w=0; w<14; w++) data[w]=wasm_i32x4_make(coins[0].i[w],coins[1].i[w],coins[2].i[w],coins[3].i[w]); v128_t hv[5]; sha1_4(data,hv);
    u32_t h0_0=wasm_i32x4_extract_lane(hv[0],0); u32_t h0_1=wasm_i32x4_extract_lane(hv[0],1); u32_t h0_2=wasm_i32x4_extract_lane(hv[0],2); u32_t h0_3=wasm_i32x4_extract_lane(hv[0],3);
    u32_t diff_hex_chars = g_hex_chars_simd;
    if( (h0_0 & g_mask_simd) == (DETI_COIN_SIGNATURE & g_mask_simd) ){
      if(g_coins_simd==0){ g_first_nonce_simd=base; for(int b=0;b<55;b++) g_first_bytes_simd[b]=coins[0].c[b^3]; g_first_len_simd=55; }
      g_coins_simd++;
      if(h0_0 == DETI_COIN_SIGNATURE){ save_coin(&coins[0].i[0]); }
      if(g_coins_buf_len_simd < sizeof(g_coins_buf_simd)-70){
        g_coins_buf_simd[g_coins_buf_len_simd++]='V';
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars/10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars%10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=':';
        for(int b=0;b<55;b++){ u08_t ch=coins[0].c[b^3]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
        g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
      }
    }
    // Lane 1
    if( (h0_1 & g_mask_simd) == (DETI_COIN_SIGNATURE & g_mask_simd) ){
      if(g_coins_simd==0){ g_first_nonce_simd=base+1; for(int b=0;b<55;b++) g_first_bytes_simd[b]=coins[1].c[b^3]; g_first_len_simd=55; }
      g_coins_simd++;
      if(h0_1 == DETI_COIN_SIGNATURE){ save_coin(&coins[1].i[0]); }
      if(g_coins_buf_len_simd < sizeof(g_coins_buf_simd)-70){
        g_coins_buf_simd[g_coins_buf_len_simd++]='V';
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars/10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars%10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=':';
        for(int b=0;b<55;b++){ u08_t ch=coins[1].c[b^3]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
        g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
      }
    }
    // Lane 2
    if( (h0_2 & g_mask_simd) == (DETI_COIN_SIGNATURE & g_mask_simd) ){
      if(g_coins_simd==0){ g_first_nonce_simd=base+2; for(int b=0;b<55;b++) g_first_bytes_simd[b]=coins[2].c[b^3]; g_first_len_simd=55; }
      g_coins_simd++;
      if(h0_2 == DETI_COIN_SIGNATURE){ save_coin(&coins[2].i[0]); }
      if(g_coins_buf_len_simd < sizeof(g_coins_buf_simd)-70){
        g_coins_buf_simd[g_coins_buf_len_simd++]='V';
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars/10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars%10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=':';
        for(int b=0;b<55;b++){ u08_t ch=coins[2].c[b^3]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
        g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
      }
    }
    // Lane 3
    if( (h0_3 & g_mask_simd) == (DETI_COIN_SIGNATURE & g_mask_simd) ){
      if(g_coins_simd==0){ g_first_nonce_simd=base+3; for(int b=0;b<55;b++) g_first_bytes_simd[b]=coins[3].c[b^3]; g_first_len_simd=55; }
      g_coins_simd++;
      if(h0_3 == DETI_COIN_SIGNATURE){ save_coin(&coins[3].i[0]); }
      if(g_coins_buf_len_simd < sizeof(g_coins_buf_simd)-70){
        g_coins_buf_simd[g_coins_buf_len_simd++]='V';
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars/10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars%10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=':';
        for(int b=0;b<55;b++){ u08_t ch=coins[3].c[b^3]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
        g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
      }
    }
    g_attempts_simd += 4;
  }
  //scalar fallback (reuse lane 0)
  for(u32_t r=0; r<rem; r++){
    u32_t nonce=start_nonce+full*4+r; nonce_hex_lane(nonce, coins[0].c);
    u32_t a=0x67452301u,b=0xEFCDAB89u,c=0x98BADCFEu,d=0x10325476u,e=0xC3D2E1F0u,wbuf[16],tmp; for(int i=0;i<14;i++) wbuf[i]=coins[0].i[i]; wbuf[14]=0; wbuf[15]=440;
    for(int t=0;t<80;t++){ if(t>=16){ tmp=wbuf[(t-3)&15]^wbuf[(t-8)&15]^wbuf[(t-14)&15]^wbuf[(t-16)&15]; wbuf[t&15]=((tmp<<1)|(tmp>>31)); }
      if(t<20) tmp=((a<<5)|(a>>27))+(((b)&(c))|(~(b)&(d)))+e+wbuf[t&15]+0x5A827999u;
      else if(t<40) tmp=((a<<5)|(a>>27))+(b^c^d)+e+wbuf[t&15]+0x6ED9EBA1u;
      else if(t<60) tmp=((a<<5)|(a>>27))+(((b)&(c))|((b)&(d))|((c)&(d)))+e+wbuf[t&15]+0x8F1BBCDCu;
      else tmp=((a<<5)|(a>>27))+(b^c^d)+e+wbuf[t&15]+0xCA62C1D6u; e=d; d=c; c=((b<<30)|(b>>2)); b=a; a=tmp; }
    u32_t h0=a+0x67452301u; if( (h0 & g_mask_simd) == (DETI_COIN_SIGNATURE & g_mask_simd) ){
      if(g_coins_simd==0){ g_first_nonce_simd=nonce; for(int b=0;b<55;b++) g_first_bytes_simd[b]=coins[0].c[b^3]; g_first_len_simd=55; }
      g_coins_simd++;
      if(h0 == DETI_COIN_SIGNATURE){ save_coin(&coins[0].i[0]); }
      u32_t diff_hex_chars = g_hex_chars_simd;
      if(g_coins_buf_len_simd < sizeof(g_coins_buf_simd)-70){
        g_coins_buf_simd[g_coins_buf_len_simd++]='V';
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars/10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(diff_hex_chars%10));
        g_coins_buf_simd[g_coins_buf_len_simd++]=':';
        for(int b=0;b<55;b++){ u08_t ch=coins[0].c[b^3]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
        g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
      }
    }
    g_attempts_simd++;
  }
  if(g_coins_simd>0 && g_coins_buf_len_simd==0 && g_first_len_simd){
    g_coins_buf_simd[g_coins_buf_len_simd++]='V';
    g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(g_hex_chars_simd/10));
    g_coins_buf_simd[g_coins_buf_len_simd++]=(u08_t)('0'+(g_hex_chars_simd%10));
    g_coins_buf_simd[g_coins_buf_len_simd++]=':';
    for(u32_t b=0;b<g_first_len_simd;b++){ u08_t ch=g_first_bytes_simd[b]; if(ch=='\n') break; g_coins_buf_simd[g_coins_buf_len_simd++]=ch; }
    g_coins_buf_simd[g_coins_buf_len_simd++]='\n';
  }
  save_coin(NULL);
}
