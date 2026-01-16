//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//

#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"

//
// test the reference implementation
//

static void test_sha1(int n_tests,int n_measurements)
{
  static union { u08_t c[14 * 4]; u32_t i[14]; } data; // the data as bytes and as 32-bit integers
  static union { u08_t c[ 5 * 4]; u32_t i[ 5]; } hash; // the hash as bytes and as 32-bit integers
  char command[320]; // 320 is more than enough
  char response[64]; // 64 is more than enough
  char computed[64]; // 64 is more than enough
  double hashes_per_second;
  int n,i,idx;
  u32_t sum;
  FILE *fp;

  // test
  response[40] = '\0';
  for(n = 0;n < n_tests;n++)
  {
    // create random data (55 bytes)
    for(i = 0;i < 55;i++)
      data.c[i ^ 3] = random_byte();
    // append padding (a SHA1 thing...)
    data.c[55 ^ 3] = 0x80;
    // compute its SHA1 secure hash
    sha1(&data.i[0],&hash.i[0]);
    // convert the secure hash into a string
    idx = 0;
    for(i = 0;i < 20;i++)
      idx += sprintf(&computed[idx],"%02x",(int)hash.c[i ^ 3] & 0xFF);
    if(idx >= (int)sizeof(computed))
    {
      fprintf(stderr,"computed[] is too small\n");
      exit(1);
    }
    // construct the command to test the SHA1 secure hash
    idx = sprintf(&command[0],"/bin/echo -en '"); // do not rely on the bash echo builtin command
    for(i = 0;i < 55;i++)
      idx += sprintf(&command[idx],"\\x%02x",data.c[i ^ 3]);
    idx += sprintf(&command[idx],"' | sha1sum");
    if(idx >= (int)sizeof(command))
    {
      fprintf(stderr,"command[] is too small\n");
      exit(1);
    }
    // run it and get its output
    fp = popen(command,"r");
    if(fp == NULL)
    {
      fprintf(stderr,"popen() failed\n");
      exit(1);
    }
    if(fread((void *)&response[0],sizeof(char),(size_t)40,fp) != (size_t)40)
    {
      fprintf(stderr,"fread() failed\n");
      exit(1);
    }
    pclose(fp);
    // compare them
    if(memcmp((void *)response,(void *)computed,(size_t)40) != 0)
    { // print everything
      fprintf(stderr,"sha1() failure for n=%d:\n",n);
      for(i = 0;i < 55;i++)
        fprintf(stderr,"  message[%2d] = %02x\n",i,(int)data.c[i ^ 3] & 0xFF);
      for(i = 0;i < 20;i++)
        fprintf(stderr,"  hash[%2d] = %02x\n",i,(int)hash.c[i ^ 3] % 0xFF);
      fprintf(stderr,"  sha1sum output: %s\n",response);
      fprintf(stderr,"  sha1() output:  %s\n",computed);
      for(i = 0;i < 40 && response[i] == computed[i];i++)
        ;
      fprintf(stderr,"  mismatch at %d\n",i);
      exit(1);
    }
  }
  // warmup (turbo boost...)
  for(i = n = 0;i < 1000000;i++)
    n += (int)random_byte();
  if(n == 0)
    fprintf(stderr,"sha1(): this should not be possible, n=0\n");
  // measure
  time_measurement();
  sum = 0u;
  for(n = 0;n < n_measurements;n++)
  {
    data.i[0]++;
    sha1(&data.i[0],&hash.i[0]);
    sum += hash.i[4];
  }
  time_measurement();
  if(sum == 0u)
    fprintf(stderr,"sha1(): what a coincidence, sum=0\n");
  hashes_per_second = (double)n_measurements / cpu_time_delta();
  // report
  printf("sha1() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
}


//
// test the avx implementation
//

#if defined(__AVX__)

static void test_sha1_avx(int n_tests,int n_measurements)
{
#define N_LANES 4
  static union { u08_t c[14 * 4]; u32_t i[14]; } data[N_LANES]; // the data as bytes and as 32-bit integers
  static union { u08_t c[ 5 * 4]; u32_t i[ 5]; } hash[N_LANES]; // the hash as bytes and as 32-bit integers
  static u32_t interleaved_data[14][N_LANES] __attribute__((aligned(16)));
  static u32_t interleaved_hash[5][N_LANES]  __attribute__((aligned(16)));
  double hashes_per_second;
  int n,i,lane;
  u32_t sum;

  // test
  for(n = 0;n < n_tests;n++)
  {
    // the data and the secure hash for the reference implementation
    for(lane = 0;lane < N_LANES;lane++)
    {
      // create random data (55 bytes)
      for(i = 0;i < 55;i++)
        data[lane].c[i ^ 3] = random_byte();
      // append padding (a SHA1 thing...)
      data[lane].c[55 ^ 3] = 0x80;
      // compute its SHA1 secure hash
      sha1(&data[lane].i[0],&hash[lane].i[0]);
    }
    // interleave (transpose) the data for the avx implementation
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 14;i++)
        interleaved_data[i][lane] = data[lane].i[i];
    // compute the four secure hashes in one go
    sha1_avx((v4si *)&interleaved_data[0],(v4si *)&interleaved_hash[0]);
    // test
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 5;i++)
        if(interleaved_hash[i][lane] != hash[lane].i[i])
        {
          fprintf(stderr,"sha1_avx() failure for n=%d (bad/good):\n",n);
          for(i = 0;i < 5;i++)
            for(lane = 0;lane < N_LANES;lane++)
              fprintf(stderr,"%s%08X/%08X%s",(lane == 0) ? "  " : " ",interleaved_hash[i][lane] ,hash[lane].i[i],(lane == N_LANES - 1) ? "\n" : "");
          exit(1);
        }
  }
  // measure
  time_measurement();
  sum = 0u;
  for(n = 0;n < n_measurements;n++)
  {
    interleaved_data[0][0]++;
    sha1_avx((v4si *)&interleaved_data[0],(v4si *)&interleaved_hash[0]);
    sum += interleaved_hash[4][0];
  }
  time_measurement();
  if(sum == 0u)
    fprintf(stderr,"sha1_avx(): what a coincidence, sum=0\n");
  hashes_per_second = (double)n_measurements * (double)N_LANES / cpu_time_delta();
  // report
  printf("sha1_avx() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
# undef N_LANES
}

#endif


//
// test the avx2 implementation
//

#if defined(__AVX2__)

static void test_sha1_avx2(int n_tests,int n_measurements)
{
#define N_LANES 8
  static union { u08_t c[14 * 4]; u32_t i[14]; } data[N_LANES]; // the data as bytes and as 32-bit integers
  static union { u08_t c[ 5 * 4]; u32_t i[ 5]; } hash[N_LANES]; // the hash as bytes and as 32-bit integers
  static u32_t interleaved_data[14][N_LANES] __attribute__((aligned(32)));
  static u32_t interleaved_hash[5][N_LANES]  __attribute__((aligned(32)));
  double hashes_per_second;
  int n,i,lane;
  u32_t sum;

  // test
  for(n = 0;n < n_tests;n++)
  {
    // the data and the secure hash for the reference implementation
    for(lane = 0;lane < N_LANES;lane++)
    {
      // create random data (55 bytes)
      for(i = 0;i < 55;i++)
        data[lane].c[i ^ 3] = random_byte();
      // append padding (a SHA1 thing...)
      data[lane].c[55 ^ 3] = 0x80;
      // compute its SHA1 secure hash
      sha1(&data[lane].i[0],&hash[lane].i[0]);
    }
    // interleave (transpose) the data for the avx2 implementation
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 14;i++)
        interleaved_data[i][lane] = data[lane].i[i];
    // compute the eight secure hashes in one go
    sha1_avx2((v8si *)&interleaved_data[0],(v8si *)&interleaved_hash[0]);
    // test
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 5;i++)
        if(interleaved_hash[i][lane] != hash[lane].i[i])
        {
          fprintf(stderr,"sha1_avx2() failure for n=%d (bad/good):\n",n);
          for(i = 0;i < 5;i++)
            for(lane = 0;lane < N_LANES;lane++)
              fprintf(stderr,"%s%08X/%08X%s",(lane == 0) ? "  " : " ",interleaved_hash[i][lane] ,hash[lane].i[i],(lane == N_LANES - 1) ? "\n" : "");
          exit(1);
        }
  }
  // measure
  time_measurement();
  sum = 0u;
  for(n = 0;n < n_measurements;n++)
  {
    interleaved_data[0][0]++;
    sha1_avx2((v8si *)&interleaved_data[0],(v8si *)&interleaved_hash[0]);
    sum += interleaved_hash[4][0];
  }
  time_measurement();
  if(sum == 0u)
    fprintf(stderr,"sha1_avx2(): what a coincidence, sum=0\n");
  hashes_per_second = (double)n_measurements * (double)N_LANES / cpu_time_delta();
  // report
  printf("sha1_avx2() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
# undef N_LANES
}

#endif


//
// test the avx512f implementation
//

#if defined(__AVX512F__)

static void test_sha1_avx512f(int n_tests,int n_measurements)
{
#define N_LANES 16
  static union { u08_t c[14 * 4]; u32_t i[14]; } data[N_LANES]; // the data as bytes and as 32-bit integers
  static union { u08_t c[ 5 * 4]; u32_t i[ 5]; } hash[N_LANES]; // the hash as bytes and as 32-bit integers
  static u32_t interleaved_data[14][N_LANES] __attribute__((aligned(64)));
  static u32_t interleaved_hash[5][N_LANES]  __attribute__((aligned(64)));
  double hashes_per_second;
  int n,i,lane;
  u32_t sum;

  // test
  for(n = 0;n < n_tests;n++)
  {
    // the data and the secure hash for the reference implementation
    for(lane = 0;lane < N_LANES;lane++)
    {
      // create random data (55 bytes)
      for(i = 0;i < 55;i++)
        data[lane].c[i ^ 3] = random_byte();
      // append padding (a SHA1 thing...)
      data[lane].c[55 ^ 3] = 0x80;
      // compute its SHA1 secure hash
      sha1(&data[lane].i[0],&hash[lane].i[0]);
    }
    // interleave (transpose) the data for the avx512f implementation
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 14;i++)
        interleaved_data[i][lane] = data[lane].i[i];
    // compute the sixteen secure hashes in one go
    sha1_avx512f((v16si *)&interleaved_data[0],(v16si *)&interleaved_hash[0]);
    // test
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 5;i++)
        if(interleaved_hash[i][lane] != hash[lane].i[i])
        {
          fprintf(stderr,"sha1_avx512f() failure for n=%d (bad/good):\n",n);
          for(i = 0;i < 5;i++)
            for(lane = 0;lane < N_LANES;lane++)
              fprintf(stderr,"%s%08X/%08X%s",(lane == 0) ? "  " : " ",interleaved_hash[i][lane] ,hash[lane].i[i],(lane == N_LANES - 1) ? "\n" : "");
          exit(1);
        }
  }
  // measure
  time_measurement();
  sum = 0u;
  for(n = 0;n < n_measurements;n++)
  {
    interleaved_data[0][0]++;
    sha1_avx512f((v16si *)&interleaved_data[0],(v16si *)&interleaved_hash[0]);
    sum += interleaved_hash[4][0];
  }
  time_measurement();
  if(sum == 0u)
    fprintf(stderr,"sha1_avx512f(): what a coincidence, sum=0\n");
  hashes_per_second = (double)n_measurements * (double)N_LANES / cpu_time_delta();
  // report
  printf("sha1_avx512f() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
# undef N_LANES
}

#endif


//
// test the neon implementation
//

#if defined(__ARM_NEON)

static void test_sha1_neon(int n_tests,int n_measurements)
{
#define N_LANES 4
  static union { u08_t c[14 * 4]; u32_t i[14]; } data[N_LANES]; // the data as bytes and as 32-bit integers
  static union { u08_t c[ 5 * 4]; u32_t i[ 5]; } hash[N_LANES]; // the hash as bytes and as 32-bit integers
  static u32_t interleaved_data[14][N_LANES] __attribute__((aligned(16)));
  static u32_t interleaved_hash[5][N_LANES]  __attribute__((aligned(16)));
  double hashes_per_second;
  int n,i,lane;
  u32_t sum;

  // test
  for(n = 0;n < n_tests;n++)
  {
    // the data and the secure hash for the reference implementation
    for(lane = 0;lane < N_LANES;lane++)
    {
      // create random data (55 bytes)
      for(i = 0;i < 55;i++)
        data[lane].c[i ^ 3] = random_byte();
      // append padding (a SHA1 thing...)
      data[lane].c[55 ^ 3] = 0x80;
      // compute its SHA1 secure hash
      sha1(&data[lane].i[0],&hash[lane].i[0]);
    }
    // interleave (transpose) the data for the neon implementation
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 14;i++)
        interleaved_data[i][lane] = data[lane].i[i];
    // compute the four secure hashes in one go
    sha1_neon((uint32x4_t *)&interleaved_data[0],(uint32x4_t *)&interleaved_hash[0]);
    // test
    for(lane = 0;lane < N_LANES;lane++)
      for(i = 0;i < 5;i++)
        if(interleaved_hash[i][lane] != hash[lane].i[i])
        {
          fprintf(stderr,"sha1_neon() failure for n=%d (bad/good):\n",n);
          for(i = 0;i < 5;i++)
            for(lane = 0;lane < N_LANES;lane++)
              fprintf(stderr,"%s%08X/%08X%s",(lane == 0) ? "  " : " ",interleaved_hash[i][lane] ,hash[lane].i[i],(lane == N_LANES - 1) ? "\n" : "");
          exit(1);
        }
  }
  // measure
  time_measurement();
  sum = 0u;
  for(n = 0;n < n_measurements;n++)
  {
    interleaved_data[0][0]++;
    sha1_neon((uint32x4_t *)&interleaved_data[0],(uint32x4_t *)&interleaved_hash[0]);
    sum += interleaved_hash[4][0];
  }
  time_measurement();
  if(sum == 0u)
    fprintf(stderr,"sha1_neon(): what a coincidence, sum=0\n");
  hashes_per_second = (double)n_measurements * (double)N_LANES / cpu_time_delta();
  // report
  printf("sha1_neon() passed (%d test%s, %.0f secure hashes per second)\n",n_tests,(n_tests == 1) ? "" : "s",hashes_per_second);
# undef N_LANES
}

#endif


//
// main program
//

int main(void)
{
  int n_tests = 1000;
  int n_measurements = 10000000;

  test_sha1(n_tests,n_measurements);
#if defined(__AVX__)
  test_sha1_avx(n_tests,n_measurements);
#endif
#if defined(__AVX2__)
  test_sha1_avx2(n_tests,n_measurements);
#endif
#if defined(__AVX512F__)
  test_sha1_avx512f(n_tests,n_measurements);
#endif
#if defined(__ARM_NEON)
  test_sha1_neon(n_tests,n_measurements);
#endif
  return 0;
}
