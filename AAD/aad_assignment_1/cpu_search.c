#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <signal.h>
#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

#define DETI_COIN_SIGNATURE 0xAAD20250u

static volatile sig_atomic_t stop_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  stop_requested = 1;
}

int main(int argc, char **argv)
{
  union { 
    u08_t c[64]; 
    u32_t i[16]; 
  } coin;
  
  u32_t hash[5];
  unsigned long long nonce = 0ULL;

  for(int i=0; i<16; i++) coin.i[i] = 0;

  const char *static_override = NULL;
  for(int argi = 1; argi < argc; ++argi)
  {
    if(strcmp(argv[argi], "-s") == 0 && (argi + 1) < argc)
    {
      static_override = argv[++argi];
    }
  }

  (void)signal(SIGINT, handle_sigint);

  srand((unsigned int)time(NULL));
  
  nonce = 0;
  
  const char *hdr = "DETI coin 2 ";
  for(int k = 0; k < 12; k++)
    coin.c[k ^ 3] = (u08_t)hdr[k];

  const int static_start = 12;
  const int static_end = 53;
  const int static_len = static_end - static_start + 1;
  int override_len = (static_override != NULL) ? (int)strlen(static_override) : 0;
  if(override_len > static_len)
    override_len = static_len;

  for(int offset = 0; offset < static_len; ++offset)
  {
    int pos = static_start + offset;
    unsigned char value;
    if(offset < override_len)
    {
      value = (unsigned char)static_override[offset];
      if(value < 32 || value > 126)
        value = (unsigned char)' ';
    }
    else
    {
      value = (unsigned char)(32 + (rand() % 95));
    }
    coin.c[pos ^ 3] = (u08_t)value;
  }

  coin.c[54 ^ 3] = (u08_t)'\n';
  coin.c[55 ^ 3] = (u08_t)0x80;
  
  coin.i[15] = 440; 

  time_measurement();
  double total_elapsed_time = 0.0;
  unsigned long long iter = 0ULL;
  unsigned long long last_report_iter = 0ULL;
  unsigned long long coins_found = 0ULL;

  fprintf(stderr, "Iniciando procura CPU...\n");

  while(!stop_requested)
  {
    unsigned long long temp_n = nonce;
    coin.c[53 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[52 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[51 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[50 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[49 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[48 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[47 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[46 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[45 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[44 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[43 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[42 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[41 ^ 3] = (u08_t)(32 + (temp_n & 0x1F)); temp_n >>= 5;
    coin.c[40 ^ 3] = (u08_t)(32 + (temp_n & 0x1F));

    sha1(&coin.i[0], hash);

    //aad20250
    if(hash[0] == DETI_COIN_SIGNATURE)
    {
      printf("Found DETI coin: nonce=%llu\n", nonce);
      printf("Coin Content: \"");
      for(int b = 0; b < 55; b++) {
        unsigned char ch = ((unsigned char *)coin.c)[b ^ 3];
        putchar((ch >= 32 && ch <= 126) ? ch : '.');
      }
      printf("\"\n");

      save_coin(&coin.i[0]);
      coins_found++;
    }

    iter++;
    nonce++;

    if((iter & 0xFFFFFF) == 0) 
    {
      time_measurement();
      double delta = wall_time_delta();
      total_elapsed_time += delta;
      double fps = (double)(iter - last_report_iter) / delta;
      last_report_iter = iter;
      
      fprintf(stderr, "Speed: %.2f MH/s (%.2f M/min) | Nonce: %llx\n", 
              fps / 1000000.0, 
              (fps * 60.0) / 1000000.0, 
              nonce);
    }
  }

  save_coin(NULL);

  time_measurement();
  double final_time = wall_time_delta();
  total_elapsed_time += final_time;

  unsigned long long final_total_hashes = iter;
  double avg_hashes_per_sec = (total_elapsed_time > 0.0) ? (double)final_total_hashes / total_elapsed_time : 0.0;
  double avg_hashes_per_min = avg_hashes_per_sec * 60.0;
  double hashes_per_coin = (coins_found > 0ULL) ? (double)final_total_hashes / (double)coins_found : 0.0;

  printf("\n");
  printf("========================================\n");
  printf("Final Summary (CPU):\n");
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