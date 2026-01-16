#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <time.h>
#include <omp.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

static volatile sig_atomic_t stop_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  stop_requested = 1;
}

#if defined(__AVX512F__)
# define N_LANES 16
# define USE_AVX512 1
#elif defined(__AVX2__)
# define N_LANES 8
# define USE_AVX2 1
#elif defined(__AVX__)
# define N_LANES 4
# define USE_AVX 1
#else
# define N_LANES 1
# define USE_SCALAR 1
#endif

#define BATCH_SIZE 256

static inline void to_base95_10(unsigned long long value, u08_t digits[10])
{
  for(int i = 0; i < 10; ++i)
  {
    digits[i] = (u08_t)(value % 95ULL);
    value /= 95ULL;
  }
}

static inline int base95_add(u08_t digits[10], unsigned long long delta)
{
  unsigned long long carry = delta;
  int highest_changed = -1;
  for(int i = 0; i < 10 && carry != 0u; ++i)
  {
    unsigned long long sum = (unsigned long long)digits[i] + carry;
    unsigned long long next_carry = sum / 95ULL;
    sum -= next_carry * 95ULL;
    if(digits[i] != (u08_t)sum)
      highest_changed = i;
    digits[i] = (u08_t)sum;
    carry = next_carry;
  }
  return highest_changed;
}

static inline void write_lane_byte(u32_t interleaved_data[14][N_LANES], int lane, int offset, u08_t value)
{
  u08_t *word_bytes = (u08_t *)&interleaved_data[offset / 4][lane];
  word_bytes[(offset & 3) ^ 3] = value;
}

static inline void write_nonce_bytes(u32_t interleaved_data[14][N_LANES], int lane, const u08_t digits[10], int max_digit)
{
  if(max_digit < 0) return;
  if(max_digit > 9) max_digit = 9;
  for(int j = 0; j <= max_digit; ++j)
    write_lane_byte(interleaved_data, lane, 12 + j, (u08_t)(digits[j] + 32u));
}

static inline void gather_lane_words(u32_t dst[14], u32_t interleaved_data[14][N_LANES], int lane)
{
  for(int idx = 0; idx < 14; ++idx)
    dst[idx] = interleaved_data[idx][lane];
}

int main(int argc, char **argv)
{
  unsigned long long n_batches = 0ULL;
  const char *custom_string = NULL;
  int custom_string_len = 0;

  for(int i = 1; i < argc; ++i)
  {
    if(argv[i][0] == '-' && argv[i][1] == 's' && i + 1 < argc)
    {
      custom_string = argv[i + 1];
      custom_string_len = (int)strlen(custom_string);
      if(custom_string_len > 32)
        custom_string_len = 32;
      ++i;
    }
    else if(argv[i][0] != '-')
    {
      n_batches = strtoull(argv[i], NULL, 10);
    }
  }

  (void)signal(SIGINT, handle_sigint);

  const char *hdr = "DETI coin 2 ";

  time_measurement();
  double total_elapsed_time = 0.0;
  unsigned long long total_iterations = 0ULL;
  unsigned long long last_report_iter = 0ULL;
  unsigned long long global_batches = 0ULL;
  unsigned long long coins_found = 0ULL;

  #pragma omp parallel
  {
    const int tid = omp_get_thread_num();
    const int nth = omp_get_num_threads();

    u32_t interleaved_data[BATCH_SIZE][14][N_LANES] __attribute__((aligned(64)));
    u32_t interleaved_hash[BATCH_SIZE][5][N_LANES] __attribute__((aligned(64)));

    u08_t ascii95_lut[256];
    for(int i = 0; i < 256; ++i)
      ascii95_lut[i] = (u08_t)((i % 95) + 32);

    u08_t static_tail[N_LANES][32];
    for(int lane = 0; lane < N_LANES; ++lane)
    {
      for(int j = 0; j < 32; ++j)
        static_tail[lane][j] = ascii95_lut[random_byte()];
      
      if(custom_string != NULL)
      {
        for(int j = 0; j < custom_string_len; ++j)
          static_tail[lane][j] = (u08_t)custom_string[j];
      }
    }

    unsigned long long thread_seed = (unsigned long long)time(NULL) ^ (0x9E3779B97F4A7C15ULL * (unsigned long long)(tid + 1));
    unsigned long long base_nonce = ((thread_seed & 0xFFFFFFFFULL) << 32) | ((thread_seed >> 32) & 0xFFFFFFFFULL);
    base_nonce = base_nonce + (unsigned long long)tid * 0x100000000ULL;

    for(int batch_idx = 0; batch_idx < BATCH_SIZE; ++batch_idx)
    {
      for(int idx = 0; idx < 14; ++idx)
        for(int lane = 0; lane < N_LANES; ++lane)
          interleaved_data[batch_idx][idx][lane] = 0u;

      for(int lane = 0; lane < N_LANES; ++lane)
      {
        for(int k = 0; k < 12; ++k)
          write_lane_byte(interleaved_data[batch_idx], lane, k, (u08_t)hdr[k]);

        if(custom_string != NULL)
        {
          for(int j = 0; j < custom_string_len && j < 32; ++j)
            write_lane_byte(interleaved_data[batch_idx], lane, 12 + j, (u08_t)custom_string[j]);
          for(int j = custom_string_len; j < 32; ++j)
            write_lane_byte(interleaved_data[batch_idx], lane, 12 + j, static_tail[lane][j]);
        }
        else
        {
          for(int j = 0; j < 32; ++j)
            write_lane_byte(interleaved_data[batch_idx], lane, 12 + j, static_tail[lane][j]);
        }

        write_lane_byte(interleaved_data[batch_idx], lane, 54, (u08_t)'\n');
        write_lane_byte(interleaved_data[batch_idx], lane, 55, (u08_t)0x80);
      }
    }

    u08_t lane_digits[N_LANES][10];
    for(int lane = 0; lane < N_LANES; ++lane)
    {
      unsigned long long lane_nonce = base_nonce + (unsigned long long)lane;
      to_base95_10(lane_nonce, lane_digits[lane]);
    }

    const unsigned long long stride = (unsigned long long)N_LANES * (unsigned long long)nth;
    unsigned long long batches_done = 0ULL;
    unsigned long long local_coins_found = 0ULL;

    unsigned long long report_interval = 0x1FFFFFFULL;

    while(!stop_requested && (n_batches == 0ULL || batches_done < n_batches))
    {
      for(int batch_idx = 0; batch_idx < BATCH_SIZE; ++batch_idx)
      {
        for(int lane = 0; lane < N_LANES; ++lane)
        {
          for(int j = 0; j < 10; ++j)
            write_lane_byte(interleaved_data[batch_idx], lane, 44 + j, (u08_t)(lane_digits[lane][j] + 32u));
          base95_add(lane_digits[lane], stride);
        }
      }

      for(int batch_idx = 0; batch_idx < BATCH_SIZE; ++batch_idx)
      {
        #if defined(USE_AVX512)
          sha1_avx512f((v16si *)&interleaved_data[batch_idx][0], (v16si *)&interleaved_hash[batch_idx][0]);
        #elif defined(USE_AVX2)
          sha1_avx2((v8si *)&interleaved_data[batch_idx][0], (v8si *)&interleaved_hash[batch_idx][0]);
        #elif defined(USE_AVX)
          sha1_avx((v4si *)&interleaved_data[batch_idx][0], (v4si *)&interleaved_hash[batch_idx][0]);
        #else
          for(int lane = 0; lane < N_LANES; ++lane)
          {
            u32_t lane_words[14];
            u32_t htmp[5];
            gather_lane_words(lane_words, interleaved_data[batch_idx], lane);
            sha1(lane_words, htmp);
            for(int t = 0; t < 5; ++t)
              interleaved_hash[batch_idx][t][lane] = htmp[t];
          }
        #endif
      }

      for(int batch_idx = 0; batch_idx < BATCH_SIZE; ++batch_idx)
      {
        for(int lane = 0; lane < N_LANES; ++lane)
        {
          u32_t h0 = interleaved_hash[batch_idx][0][lane];
          if(__builtin_expect(h0 == 0xAAD20250u, 0))
          {
            u32_t hash[5];
            for(int t = 0; t < 5; ++t)
              hash[t] = interleaved_hash[batch_idx][t][lane];
            
            unsigned int zeros = __builtin_clz(hash[1]);
            if((hash[1] & ((1u << (31u - zeros)) - 1u)) == 0u)
            {
              for(unsigned int word = 2; word < 5 && zeros < 128u; ++word)
              {
                if(hash[word] == 0u)
                  zeros += 32u;
                else
                {
                  zeros += __builtin_clz(hash[word]);
                  break;
                }
              }
            }
            if(zeros > 99u) zeros = 99u;

            unsigned long long found_nonce = base_nonce + (unsigned long long)(batch_idx * stride) + (unsigned long long)lane - (unsigned long long)(BATCH_SIZE * stride);

            u32_t coin_words[14];
            gather_lane_words(coin_words, interleaved_data[batch_idx], lane);

            #pragma omp critical(aad_vault)
            {
              save_coin(coin_words);
              coins_found++;
            }

            local_coins_found++;

            #pragma omp critical(console)
            printf("Found DETI coin (OPT): tid=%d nonce=%llu zeros=%u\n", tid, found_nonce, zeros);
          }
        }
      }

      base_nonce += stride * BATCH_SIZE;
      batches_done += BATCH_SIZE;
      
      #pragma omp atomic
      global_batches += BATCH_SIZE;

      #pragma omp master
      {
        unsigned long long batches_snapshot;
        #pragma omp atomic read
        batches_snapshot = global_batches;
        unsigned long long current_total = batches_snapshot * (unsigned long long)N_LANES;
        
        if((current_total & report_interval) == 0ULL && current_total != total_iterations)
        {
          total_iterations = current_total;
          time_measurement();
          double delta = wall_time_delta();
          total_elapsed_time += delta;
          double fps = (delta > 0.0) ? (double)(total_iterations - last_report_iter) / delta : 0.0;
          last_report_iter = total_iterations;
          
          fprintf(stderr, "Speed: %.2f MH/s (%.2f M/min) | Nonce: %llx | Coins: %llu\n", 
                  fps / 1000000.0, 
                  (fps * 60.0) / 1000000.0, 
                  base_nonce,
                  coins_found);
        }
      }
    }
  }

  save_coin(NULL);
  
  time_measurement();
  double final_time = wall_time_delta();
  total_elapsed_time += final_time;
  
  unsigned long long final_total_hashes = global_batches * (unsigned long long)N_LANES;
  double avg_hashes_per_sec = (total_elapsed_time > 0.0) ? (double)final_total_hashes / total_elapsed_time : 0.0;
  double avg_hashes_per_min = avg_hashes_per_sec * 60.0;
  double hashes_per_coin = (coins_found > 0ULL) ? (double)final_total_hashes / (double)coins_found : 0.0;
  
  printf("\n========================================\n");
  printf("Final Summary:\n");
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
  
  return 0;
}