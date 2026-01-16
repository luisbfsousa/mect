#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <time.h>
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

#if defined(__AVX__)
# define N_LANES 4
# define USE_AVX 1
#else
# error "No AVX support detected"
#endif

static inline void write_lane_byte(u32_t data[14][N_LANES], int lane, int offset, u08_t value)
{
  u08_t *word_bytes = (u08_t *)&data[offset / 4][lane];
  word_bytes[(offset & 3) ^ 3] = value;
}

static inline int base95_add(u08_t digits[11], unsigned int k)
{
  unsigned int carry = k;
  int last_changed = -1;
  for(int i = 0; i < 11 && carry != 0u; ++i)
  {
    unsigned int sum = (unsigned int)digits[i] + carry;
    unsigned int next_carry = 0u;
    while(sum >= 95u)
    {
      sum -= 95u;
      ++next_carry;
    }
    digits[i] = (u08_t)sum;
    carry = next_carry;
    last_changed = i;
  }
  return last_changed;
}

static inline void to_base95_11(u64_t x, u08_t out_digits[11])
{
  for(int i = 0; i < 11; ++i)
  {
    out_digits[i] = (u08_t)(x % 95u);
    x /= 95u;
  }
}

static inline void apply_nonce_digits(u32_t data[14][N_LANES], int lane, const u08_t digits[11])
{

  u08_t ascii8 = (u08_t)(digits[8] + 32u);
  u08_t ascii9 = (u08_t)(digits[9] + 32u);
  write_lane_byte(data, lane, 40, ascii8);
  write_lane_byte(data, lane, 41, ascii9);
  write_lane_byte(data, lane, 42, ascii8);
  write_lane_byte(data, lane, 43, ascii9);
  write_lane_byte(data, lane, 44, ascii9);
  write_lane_byte(data, lane, 45, ascii8);
  
  for(int d = 7; d >= 4; --d)
    write_lane_byte(data, lane, 46 + (7 - d), (u08_t)(digits[d] + 32u));
  for(int d = 3; d >= 0; --d)
    write_lane_byte(data, lane, 50 + (3 - d), (u08_t)(digits[d] + 32u));
}

static inline void refresh_nonce_digits(u32_t data[14][N_LANES], int lane, const u08_t digits[11], int max_digit)
{
  if(max_digit < 0) return;
  if(max_digit > 9) max_digit = 9;

  if(max_digit >= 0) write_lane_byte(data, lane, 53, (u08_t)(digits[0] + 32u));
  if(max_digit >= 1) write_lane_byte(data, lane, 52, (u08_t)(digits[1] + 32u));
  if(max_digit >= 2) write_lane_byte(data, lane, 51, (u08_t)(digits[2] + 32u));
  if(max_digit >= 3) write_lane_byte(data, lane, 50, (u08_t)(digits[3] + 32u));
  if(max_digit >= 4) write_lane_byte(data, lane, 49, (u08_t)(digits[4] + 32u));
  if(max_digit >= 5) write_lane_byte(data, lane, 48, (u08_t)(digits[5] + 32u));
  if(max_digit >= 6) write_lane_byte(data, lane, 47, (u08_t)(digits[6] + 32u));
  if(max_digit >= 7) write_lane_byte(data, lane, 46, (u08_t)(digits[7] + 32u));
  if(max_digit >= 8)
  {
    u08_t ascii8 = (u08_t)(digits[8] + 32u);
    write_lane_byte(data, lane, 40, ascii8);
    write_lane_byte(data, lane, 42, ascii8);
    write_lane_byte(data, lane, 45, ascii8);
  }
  if(max_digit >= 9)
  {
    u08_t ascii9 = (u08_t)(digits[9] + 32u);
    write_lane_byte(data, lane, 41, ascii9);
    write_lane_byte(data, lane, 43, ascii9);
    write_lane_byte(data, lane, 44, ascii9);
  }
}

int main(int argc,char **argv)
{
  unsigned long long n_batches = 0ULL;
  const char *static_override = NULL;
  
  for(int argi = 1; argi < argc; ++argi)
  {
    if(strcmp(argv[argi], "-s") == 0 && (argi + 1) < argc)
    {
      static_override = argv[++argi];
    }
    else if(n_batches == 0ULL)
    {
      n_batches = strtoull(argv[argi], NULL, 10);
    }
  }

  (void)signal(SIGINT,handle_sigint);

  u32_t interleaved_data[14][N_LANES] __attribute__((aligned(64)));
  u32_t interleaved_hash[5][N_LANES] __attribute__((aligned(64)));
  
  unsigned long long base_nonce = 0ULL;
  unsigned long long batches_done = 0ULL;
  unsigned long long total_iterations = 0ULL;
  unsigned long long last_report_iter = 0ULL;
  double total_elapsed_time = 0.0;
  unsigned long long coins_found = 0ULL;

  srand((unsigned int)time(NULL));
  base_nonce = ((unsigned long long)rand() << 32) | (unsigned long long)rand();
  time_measurement();

  const u32_t fixed_header[3] = {
    0x44455449u,
    0x20636f69u,
    0x6e203220u 
  };
  
  for(int lane = 0; lane < N_LANES; ++lane)
  {
    for(int idx = 0; idx < 14; ++idx)
      interleaved_data[idx][lane] = 0u;
    interleaved_data[0][lane] = fixed_header[0];
    interleaved_data[1][lane] = fixed_header[1];
    interleaved_data[2][lane] = fixed_header[2];
    write_lane_byte(interleaved_data, lane, 54, (u08_t)'\n');
    write_lane_byte(interleaved_data, lane, 55, (u08_t)0x80u);
  }

  u08_t static_bytes[28];
  int override_len = (static_override != NULL) ? (int)strlen(static_override) : 0;
  if(override_len > 28)
    override_len = 28;
  
  for(int i = 0; i < 28; ++i)
  {
    if(i < override_len)
    {
      unsigned char ch = (unsigned char)static_override[i];
      static_bytes[i] = (ch >= 32 && ch <= 126) ? (u08_t)ch : (u08_t)' ';
    }
    else
    {
      static_bytes[i] = (u08_t)(32 + (random_byte() % 95));
    }
  }

  for(int lane = 0; lane < N_LANES; ++lane)
    for(int j = 0; j < 28; ++j)
      write_lane_byte(interleaved_data, lane, 12 + j, static_bytes[j]);

  u08_t lane_digits[N_LANES][11];
  for(int lane = 0; lane < N_LANES; ++lane)
  {
    to_base95_11(base_nonce + (unsigned long long)lane, lane_digits[lane]);
    apply_nonce_digits(interleaved_data, lane, lane_digits[lane]);
  }

  while((n_batches == 0ULL || batches_done < n_batches) && !stop_requested)
  {
#if defined(USE_AVX)
    sha1_avx((v4si *)&interleaved_data[0],(v4si *)&interleaved_hash[0]);
#endif

    for(int lane = 0; lane < N_LANES; ++lane)
    {
      if(interleaved_hash[0][lane] == 0xAAD20250u)
      {
        u32_t hash[5];
        for(int t = 0; t < 5; ++t)
          hash[t] = interleaved_hash[t][lane];
        
        unsigned int zeros = 0u;
        for(zeros = 0u; zeros < 128u; ++zeros)
          if(((hash[1u + zeros / 32u] >> (31u - zeros % 32u)) & 1u) != 0u)
            break;
        if(zeros > 99u) zeros = 99u;
        
        unsigned long long found_nonce = base_nonce + (unsigned long long)lane;
        printf("Found DETI coin (SIMD): nonce=%llu zeros=%u\n",found_nonce,zeros);
        
        u32_t coin_data[14];
        for(int i = 0; i < 14; ++i)
          coin_data[i] = interleaved_data[i][lane];
        
        printf("coin: \"");
        u08_t *coin_bytes = (u08_t *)coin_data;
        for(int b = 0; b < 55; ++b)
        {
          unsigned char ch = coin_bytes[b ^ 3];
          if(ch >= 32 && ch <= 126) putchar((int)ch); else putchar('?');
        }
        printf("\"\n");
        
        printf("sha1: ");
        for(int h = 0; h < 20; ++h) printf("%02x", ((unsigned char *)hash)[h ^ 3]);
        printf("\n");
        
        save_coin(coin_data);
        coins_found++;
      }
    }

    for(int lane = 0; lane < N_LANES; ++lane)
    {
      int changed = base95_add(lane_digits[lane], (unsigned int)N_LANES);
      refresh_nonce_digits(interleaved_data, lane, lane_digits[lane], changed);
    }

    base_nonce += (unsigned long long)N_LANES;
    ++batches_done;
    total_iterations += (unsigned long long)N_LANES;

    if((total_iterations & 0xFFFFFFULL) == 0ULL && total_iterations != last_report_iter)
    {
      time_measurement();
      double delta_time = wall_time_delta();
      total_elapsed_time += delta_time;
      double fps = (delta_time > 0.0) ? (double)(total_iterations - last_report_iter) / delta_time : 0.0;
      last_report_iter = total_iterations;

      fprintf(stderr, "Speed: %.2f MH/s (%.2f M/min) | Nonce: %llx\n",
              fps / 1000000.0,
              (fps * 60.0) / 1000000.0,
              base_nonce);
    }
  }

  save_coin(NULL);

  time_measurement();
  double final_time = wall_time_delta();
  total_elapsed_time += final_time;

  unsigned long long final_total_hashes = total_iterations;
  double avg_hashes_per_sec = (total_elapsed_time > 0.0) ? (double)final_total_hashes / total_elapsed_time : 0.0;
  double avg_hashes_per_min = avg_hashes_per_sec * 60.0;
  double hashes_per_coin = (coins_found > 0ULL) ? (double)final_total_hashes / (double)coins_found : 0.0;

  printf("\n");
  printf("========================================\n");
  printf("Final Summary (SIMD):\n");
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
