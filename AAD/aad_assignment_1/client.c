#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <time.h>
#include <stdint.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <omp.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_distributed.h"

#if defined(__AVX2__)
# define N_LANES 8
# define USE_AVX2 1
# define CLIENT_TYPE "SIMD+OpenMP(AVX2)"
#elif defined(__AVX__)
# define N_LANES 4
# define USE_AVX 1
# define CLIENT_TYPE "SIMD+OpenMP(AVX)"
#elif defined(__ARM_NEON)
# define N_LANES 4
# define USE_NEON 1
# define CLIENT_TYPE "SIMD+OpenMP(NEON)"
#else
# define N_LANES 1
# define CLIENT_TYPE "CPU+OpenMP"
#endif

static volatile sig_atomic_t g_stop_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  g_stop_requested = 1;
}

static int send_message(int sock, message_type_t type, const void *payload, uint32_t payload_len)
{
  message_header_t hdr;
  init_message_header(&hdr, type, payload_len);
  
  if(payload && payload_len > 0)
    hdr.checksum = simple_checksum(payload, payload_len);
  
  if(send(sock, &hdr, sizeof(hdr), 0) != sizeof(hdr))
    return -1;
  
  if(payload && payload_len > 0)
  {
    if(send(sock, payload, payload_len, 0) != (ssize_t)payload_len)
      return -1;
  }
  
  return 0;
}

static int recv_message(int sock, message_header_t *hdr, void *payload, uint32_t max_payload)
{
  ssize_t n = recv(sock, hdr, sizeof(*hdr), MSG_WAITALL);
  if(n != sizeof(*hdr))
    return -1;
  
  if(hdr->magic != PROTOCOL_MAGIC || hdr->version != DETI_PROTOCOL_VERSION)
    return -1;
  
  if(hdr->length > 0)
  {
    if(hdr->length > max_payload)
      return -1;
    
    n = recv(sock, payload, hdr->length, MSG_WAITALL);
    if(n != (ssize_t)hdr->length)
      return -1;
    
    uint32_t check = simple_checksum(payload, hdr->length);
    if(check != hdr->checksum)
      return -1;
  }
  
  return 0;
}

static void process_work(int sock, const work_assignment_t *work, int n_threads, const char *custom_string)
{
  printf("Processing work %u: nonces %lu to %lu (%lu total)\n",
         work->work_id, (unsigned long)work->start_nonce, (unsigned long)work->end_nonce,
         (unsigned long)(work->end_nonce - work->start_nonce));
  
  const char *hdr = "DETI coin 2 ";
  uint64_t range = work->end_nonce - work->start_nonce;
  uint32_t coins_found = 0;
  struct timespec ts_start;
  clock_gettime(CLOCK_MONOTONIC, &ts_start);
  double start_time = ts_start.tv_sec + ts_start.tv_nsec * 1e-9;
  
  omp_set_num_threads(n_threads);
  
  #pragma omp parallel reduction(+:coins_found)
  {
    union { u08_t c[14 * 4]; u32_t i[14]; } data[N_LANES];
    u32_t interleaved_data[14][N_LANES] __attribute__((aligned(64)));
    u32_t interleaved_hash[5][N_LANES] __attribute__((aligned(64)));
    u08_t ascii95_lut[256];
    for(int i = 0; i < 256; ++i) ascii95_lut[i] = (u08_t)((i % 95) + 32);
    
    u08_t random_space[42];
    unsigned int seed = (unsigned int)time(NULL) ^ (unsigned int)(uintptr_t)&data;
    for(int i = 0; i < 42; ++i)
    {
      seed = 3134521u * seed + 1u;
      random_space[i] = ascii95_lut[(u08_t)seed];
    }

    if (custom_string != NULL) {
        size_t len = strlen(custom_string);

        if (len > 42) len = 42; 
        memcpy(random_space, custom_string, len);
    }
    
    #pragma omp for schedule(dynamic, 1000)
    for(uint64_t batch = 0; batch < range / N_LANES; batch++)
    {
      if(g_stop_requested)
        continue;
      
      for(int lane = 0; lane < N_LANES; lane++)
      {
        uint64_t nonce = work->start_nonce + batch * N_LANES + lane;
        
        for(int k = 0; k < 12; k++)
          data[lane].c[k ^ 3] = (u08_t)hdr[k];
        
        for(int j = 0; j < 42; ++j)
          data[lane].c[(12 + j) ^ 3] = random_space[j];
        
        unsigned long long tnonce = nonce;
        for(int j = 0; j < 10; ++j)
        {
          u08_t byte_val = (u08_t)(32 + (tnonce % 95ULL));
          data[lane].c[(44 + j) ^ 3] = byte_val;
          tnonce /= 95ULL;
          if (tnonce == 0) break;
        }
        
        data[lane].c[54 ^ 3] = (u08_t)'\n';
        data[lane].c[55 ^ 3] = (u08_t)0x80;
      }
      
      for(int idx = 0; idx < 14; idx++)
        for(int lane = 0; lane < N_LANES; lane++)
          interleaved_data[idx][lane] = data[lane].i[idx];
      
#if defined(USE_AVX2)
      sha1_avx2((v8si *)&interleaved_data[0], (v8si *)&interleaved_hash[0]);
#elif defined(USE_AVX)
      sha1_avx((v4si *)&interleaved_data[0], (v4si *)&interleaved_hash[0]);
#elif defined(USE_NEON)
      sha1_neon((uint32x4_t *)&interleaved_data[0], (uint32x4_t *)&interleaved_hash[0]);
#else
      for(int lane = 0; lane < N_LANES; lane++)
        sha1(data[lane].i, &interleaved_hash[0][lane]);
#endif
      
      for(int lane = 0; lane < N_LANES; lane++)
      {
        if(interleaved_hash[0][lane] == 0xAAD20250u)
        {
          u32_t hash[5];
          for(int t = 0; t < 5; t++)
            hash[t] = interleaved_hash[t][lane];
          
          unsigned int zeros = 0u;
          for(zeros = 0u; zeros < 128u; zeros++)
            if(((hash[1u + zeros / 32u] >> (31u - zeros % 32u)) & 1u) != 0u)
              break;
          if(zeros > 99u) zeros = 99u;
          
          uint64_t found_nonce = work->start_nonce + batch * N_LANES + lane;
          
          #pragma omp critical
          {
            coin_report_t report;
            report.nonce = found_nonce;
            report.zeros = zeros;
            report.work_id = work->work_id;
            memcpy(report.coin_data, data[lane].i, sizeof(report.coin_data));
            memcpy(report.hash, hash, sizeof(report.hash));
            
            printf("FOUND COIN: nonce=%lu zeros=%u\n", (unsigned long)found_nonce, zeros);
            send_message(sock, MSG_REPORT_COIN, &report, sizeof(report));
            coins_found++;
          }
        }
      }
    }
  }
  
  struct timespec ts_end;
  clock_gettime(CLOCK_MONOTONIC, &ts_end);
  double end_time = ts_end.tv_sec + ts_end.tv_nsec * 1e-9;
  double elapsed = end_time - start_time;
  
  work_completion_t completion;
  completion.work_id = work->work_id;
  completion.nonces_tested = range;
  completion.coins_found = coins_found;
  completion.elapsed_time = elapsed;
  
  send_message(sock, MSG_WORK_COMPLETE, &completion, sizeof(completion));
  
  printf("Work %u complete: %.0f nonces/sec, %u coins\n",
         work->work_id, range / elapsed, coins_found);
}

int main(int argc, char **argv)
{
  const char *server_host = "localhost";
  int server_port = DETI_DEFAULT_PORT;
  int n_threads = omp_get_max_threads();
  const char *custom_string = NULL;

  int pos_arg_index = 0;
  for (int i = 1; i < argc; i++) {
    if (strcmp(argv[i], "-s") == 0) {
      if (i + 1 < argc) {
        custom_string = argv[i+1];
        i++;
      } else {
        fprintf(stderr, "Error: -s requires a string argument\n");
        return 1;
      }
    } else {
      if (pos_arg_index == 0) server_host = argv[i];
      else if (pos_arg_index == 1) server_port = atoi(argv[i]);
      else if (pos_arg_index == 2) n_threads = atoi(argv[i]);
      pos_arg_index++;
    }
  }
  
  printf("DETI Coin Search Client (%s)\n", CLIENT_TYPE);
  printf("==============================\n");
  printf("Server: %s:%d\n", server_host, server_port);
  printf("Threads: %d\n", n_threads);
  printf("SIMD lanes: %d\n", N_LANES);
  if(custom_string) printf("Custom String: \"%s\"\n", custom_string);
  printf("\n");
  
  signal(SIGINT, handle_sigint);
  signal(SIGPIPE, SIG_IGN);
  
  int sock = socket(AF_INET, SOCK_STREAM, 0);
  if(sock < 0)
  {
    perror("socket");
    return 1;
  }
  
  struct hostent *he = gethostbyname(server_host);
  if(!he)
  {
    fprintf(stderr, "Unknown host: %s\n", server_host);
    return 1;
  }
  
  struct sockaddr_in addr;
  memset(&addr, 0, sizeof(addr));
  addr.sin_family = AF_INET;
  addr.sin_port = htons(server_port);
  memcpy(&addr.sin_addr, he->h_addr_list[0], he->h_length);
  
  printf("Connecting to %s:%d...\n", server_host, server_port);
  if(connect(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
  {
    perror("connect");
    close(sock);
    return 1;
  }
  
  printf("Connected!\n\n");
  
  client_info_t client_info;
  memset(&client_info, 0, sizeof(client_info));
  gethostname(client_info.hostname, sizeof(client_info.hostname));
  strncpy(client_info.client_type, CLIENT_TYPE, sizeof(client_info.client_type) - 1);
  client_info.capabilities = n_threads;
  client_info.version = DETI_PROTOCOL_VERSION;
  
  if(send_message(sock, MSG_CLIENT_HELLO, &client_info, sizeof(client_info)) < 0)
  {
    fprintf(stderr, "Failed to send CLIENT_HELLO\n");
    close(sock);
    return 1;
  }
  
  message_header_t hdr;
  char buffer[4096];
  if(recv_message(sock, &hdr, buffer, sizeof(buffer)) < 0 || hdr.type != MSG_SERVER_HELLO)
  {
    fprintf(stderr, "Failed to receive SERVER_HELLO\n");
    close(sock);
    return 1;
  }
  
  printf("Handshake complete, requesting work...\n\n");
  
  while(!g_stop_requested)
  {
    if(send_message(sock, MSG_REQUEST_WORK, NULL, 0) < 0)
    {
      fprintf(stderr, "Failed to request work\n");
      break;
    }
    
    if(recv_message(sock, &hdr, buffer, sizeof(buffer)) < 0)
    {
      fprintf(stderr, "Connection lost\n");
      break;
    }
    
    if(hdr.type == MSG_SHUTDOWN || hdr.type == MSG_NO_WORK)
    {
      printf("Server shutting down or no work available\n");
      break;
    }
    
    if(hdr.type != MSG_WORK_ASSIGNMENT)
    {
      fprintf(stderr, "Unexpected message type: %u\n", hdr.type);
      break;
    }
    
    work_assignment_t *work = (work_assignment_t *)buffer;
    process_work(sock, work, n_threads, custom_string);
  }
  
  printf("\nDisconnecting...\n");
  close(sock);
  
  return 0;
}