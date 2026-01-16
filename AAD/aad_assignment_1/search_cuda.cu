#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <signal.h>
#include <cuda_runtime.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1.h"

extern "C" void save_coin_wrapper(u32_t *coin);
extern "C" void save_coin_flush(void);

#define NONCES_PER_THREAD 8

__constant__ u32_t c_static_words[16];
__constant__ u08_t c_base_nonce_digits[10];

__device__ __forceinline__ void load_base_nonce_digits(u08_t digits[10])
{
  #pragma unroll
  for(int i = 0; i < 10; ++i)
    digits[i] = c_base_nonce_digits[i];
}

__device__ __forceinline__ void add_offset_to_digits(u08_t digits[10], unsigned long long offset)
{
  unsigned long long carry = offset;
  for(int i = 0; i < 10 && carry > 0ULL; ++i)
  {
    unsigned long long val = (unsigned long long)digits[i] + (carry % 95ULL);
    carry /= 95ULL;
    if(val >= 95ULL)
    {
      digits[i] = (u08_t)(val - 95ULL);
      carry += 1ULL;
    }
    else
    {
      digits[i] = (u08_t)val;
    }
  }
}

__device__ __forceinline__ void increment_base95(u08_t digits[10])
{
  #pragma unroll
  for(int i = 0; i < 10; ++i)
  {
    u08_t val = (u08_t)(digits[i] + 1u);
    if(val >= 95u)
    {
      digits[i] = 0u;
    }
    else
    {
      digits[i] = val;
      break;
    }
  }
}

__device__ __forceinline__ void write_nonce_bytes(u08_t *coin_bytes, const u08_t digits[10])
{
  #pragma unroll
  for(int j = 0; j < 10; ++j)
    coin_bytes[(44 + j) ^ 3] = (u08_t)(digits[j] + 32u);
}

static volatile sig_atomic_t stop_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  stop_requested = 1;
}

static void nonce_to_base95_host(unsigned long long value, u08_t digits[10])
{
  for(int i = 0; i < 10; ++i)
  {
    digits[i] = (u08_t)(value % 95ULL);
    value /= 95ULL;
  }
}

__global__ void search_coins_kernel(
  unsigned long long num_coins,
  u32_t *found_coins,
  int *found_count,
  int max_found)
{
  unsigned long long thread_id = blockIdx.x * blockDim.x + threadIdx.x;
  unsigned long long first_nonce_index = thread_id * (unsigned long long)NONCES_PER_THREAD;
  if(first_nonce_index >= num_coins)
    return;

  unsigned long long current_index = first_nonce_index;

  u08_t digits[10];
  load_base_nonce_digits(digits);
  add_offset_to_digits(digits, first_nonce_index);

  u32_t coin_words[16];
  u08_t *coin_bytes = (u08_t *)coin_words;

  for(int iter = 0; iter < NONCES_PER_THREAD && current_index < num_coins; ++iter, ++current_index)
  {
    #pragma unroll
    for(int i = 0; i < 16; i++)
      coin_words[i] = c_static_words[i];

    write_nonce_bytes(coin_bytes, digits);
  
    u32_t hash[5];
# define T            u32_t
# define C(c)         (c)
# define ROTATE(x,n)  (((x) << (n)) | ((x) >> (32 - (n))))
# define DATA(i)      coin_words[(i)]
# define HASH(i)      hash[(i)]
  CUSTOM_SHA1_CODE();
# undef T
# undef C
# undef ROTATE
# undef DATA
# undef HASH
  
    if(hash[0] == 0xAAD20250u)
    {
      int pos = atomicAdd(found_count, 1);
      if(pos < max_found)
      {
        for(int i = 0; i < 16; i++)
          found_coins[pos * 16 + i] = coin_words[i];
      }
    }

    increment_base95(digits);
  }
}

int main(int argc, char **argv)
{
  unsigned long long total_batches = 0ULL;
  char *static_string = NULL;
  
  for(int i = 1; i < argc; i++)
  {
    if(strcmp(argv[i], "-s") == 0 && i + 1 < argc)
    {
      static_string = argv[++i];
    }
    else if(argv[i][0] != '-')
    {
      total_batches = strtoull(argv[i], NULL, 10);
    }
  }
  
  (void)signal(SIGINT, handle_sigint);
  
  const int threads_per_block = 256;
  const unsigned long long coins_per_batch = 32 * 1024 * 1024;
  const int max_found_per_batch = 1024;
  
  unsigned long long base_nonce = 0ULL;
  unsigned long long batches_done = 0ULL;
  unsigned long long total_iterations = 0ULL;
  unsigned long long last_report_iter = 0ULL;
  double total_elapsed_time = 0.0;
  unsigned long long coins_found = 0ULL;

  srand((unsigned int)time(NULL));

  cudaEvent_t start, stop;
  cudaEventCreate(&start);
  cudaEventCreate(&stop);
  
  base_nonce = ((unsigned long long)rand() << 32) | (unsigned long long)rand();
  
  time_measurement();
  
  u32_t h_coin_template[16];
  for(int i = 0; i < 16; i++)
    h_coin_template[i] = 0;

  u08_t *template_bytes = (u08_t *)h_coin_template;
  const char *hdr = "DETI coin 2 ";
  for(int k = 0; k < 12; k++)
    template_bytes[k ^ 3] = (u08_t)hdr[k];

  for(int k = 12; k < 44; k++)
    template_bytes[k ^ 3] = (u08_t)(32 + (rand() % 95));

  if(static_string != NULL)
  {
    int len = strlen(static_string);
    
    for(int k = 12; k < 44 && k - 12 < len; k++)
      template_bytes[k ^ 3] = (u08_t)static_string[k - 12];
  }

  template_bytes[54 ^ 3] = (u08_t)'\n';
  template_bytes[55 ^ 3] = (u08_t)0x80;
  h_coin_template[15] = 440;

  cudaMemcpyToSymbol(c_static_words, h_coin_template, sizeof(h_coin_template), 0, cudaMemcpyHostToDevice);
  
  u32_t *d_found_coins;
  int *d_found_count;
  cudaMalloc(&d_found_coins, max_found_per_batch * 16 * sizeof(u32_t));
  cudaMalloc(&d_found_count, sizeof(int));

  u32_t *h_found_coins = (u32_t*)malloc(max_found_per_batch * 16 * sizeof(u32_t));
  int h_found_count = 0;
  
  printf("Starting CUDA DETI coin search...\n");
  printf("Threads per block: %d\n", threads_per_block);
  printf("Coins per batch: %llu\n", coins_per_batch);
  
  while((total_batches == 0ULL || batches_done < total_batches) && !stop_requested)
  {
    h_found_count = 0;
    cudaMemcpy(d_found_count, &h_found_count, sizeof(int), cudaMemcpyHostToDevice);

    u08_t h_base_digits[10];
    nonce_to_base95_host(base_nonce, h_base_digits);
    cudaMemcpyToSymbol(c_base_nonce_digits, h_base_digits, sizeof(h_base_digits), 0, cudaMemcpyHostToDevice);
    
    unsigned long long threads_needed = (coins_per_batch + NONCES_PER_THREAD - 1ULL) / (unsigned long long)NONCES_PER_THREAD;
    int num_blocks = (int)((threads_needed + threads_per_block - 1ULL) / threads_per_block);
    
    cudaEventRecord(start);
    search_coins_kernel<<<num_blocks, threads_per_block>>>(
      coins_per_batch, d_found_coins, d_found_count, max_found_per_batch);
    cudaEventRecord(stop);
    cudaEventSynchronize(stop);

    float milliseconds = 0;
    cudaEventElapsedTime(&milliseconds, start, stop);
    
    cudaDeviceSynchronize();
    
    cudaError_t err = cudaGetLastError();
    if(err != cudaSuccess)
    {
      fprintf(stderr, "CUDA error: %s\n", cudaGetErrorString(err));
      break;
    }
    
    cudaMemcpy(&h_found_count, d_found_count, sizeof(int), cudaMemcpyDeviceToHost);
    
    if(h_found_count > 0)
    {
      int coins_to_process = (h_found_count > max_found_per_batch) ? max_found_per_batch : h_found_count;
      cudaMemcpy(h_found_coins, d_found_coins, 
                 coins_to_process * 16 * sizeof(u32_t), cudaMemcpyDeviceToHost);
      
      for(int i = 0; i < coins_to_process; i++)
      {
        printf("Found DETI coin! \n");
        save_coin_wrapper(&h_found_coins[i * 16]);
      }

      coins_found += (unsigned long long)coins_to_process;
    }
    
    base_nonce += coins_per_batch;
    batches_done++;
    total_iterations += coins_per_batch;
    
    if((total_iterations & 0xFFFFFB00000000ULL) != ((total_iterations - coins_per_batch) & 0xFFFFFB00000000ULL))
    {
      time_measurement();
      double delta = wall_time_delta();
      total_elapsed_time += delta;
      double fps = (double)(total_iterations - last_report_iter) / delta;
      last_report_iter = total_iterations;
      
      fprintf(stderr, "Speed: %.2f MH/s (%.2f M/min) | Nonce: %llx\n", 
              fps / 1000000.0, 
              (fps * 60.0) / 1000000.0, 
              base_nonce);
    }
  }

  cudaEventDestroy(start);
  cudaEventDestroy(stop);
  cudaFree(d_found_coins);
  cudaFree(d_found_count);
  free(h_found_coins);
  
  save_coin_flush();

  time_measurement();
  double final_time = wall_time_delta();
  total_elapsed_time += final_time;

  unsigned long long final_total_hashes = total_iterations;
  double avg_hashes_per_sec = (total_elapsed_time > 0.0) ? (double)final_total_hashes / total_elapsed_time : 0.0;
  double avg_hashes_per_min = avg_hashes_per_sec * 60.0;
  double hashes_per_coin = (coins_found > 0ULL) ? (double)final_total_hashes / (double)coins_found : 0.0;

  printf("\n");
  printf("========================================\n");
  printf("Final Summary (CUDA):\n");
  printf("========================================\n");
  printf("Total coins found:    %llu\n", coins_found);
  printf("Total hashes:         %llu\n", final_total_hashes);
  printf("Total time:           %.2f seconds\n", total_elapsed_time);
  printf("Average speed:        %.2f MH/s\n", avg_hashes_per_sec / 1000000.0);
  printf("Average speed:        %.2f M/min\n", avg_hashes_per_min / 1000000.0);
  if(coins_found > 0ULL)
    printf("Hashes per coin:      %.2f\n", hashes_per_coin);
  else
    printf("Hashes per coin:      N/A (no coins found)\n");
  printf("========================================\n");

  printf("CUDA search completed.\n");
  return 0;
}
