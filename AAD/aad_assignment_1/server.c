#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <time.h>
#include <pthread.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

#include "aad_data_types.h"
#include "aad_utilities.h"
#include "aad_sha1_cpu.h"
#include "aad_distributed.h"
#include "aad_vault.h"

typedef struct {
  uint64_t next_nonce;
  uint64_t total_nonces_assigned;
  uint64_t total_nonces_completed;
  uint32_t total_coins_found;
  uint32_t next_work_id;
  int n_clients_connected;
  int n_clients_active;
  pthread_mutex_t state_lock;
  int running;
} server_state_t;

static server_state_t g_state;
static volatile sig_atomic_t g_shutdown_requested = 0;

static void handle_sigint(int sig)
{
  (void)sig;
  g_shutdown_requested = 1;
  g_state.running = 0;
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
  
  if(hdr->magic != PROTOCOL_MAGIC)
  {
    fprintf(stderr, "Invalid magic: 0x%x\n", hdr->magic);
    return -1;
  }
  
  if(hdr->version != DETI_PROTOCOL_VERSION)
  {
    fprintf(stderr, "Protocol version mismatch: %u\n", hdr->version);
    return -1;
  }
  
  if(hdr->length > 0)
  {
    if(hdr->length > max_payload)
    {
      fprintf(stderr, "Payload too large: %u > %u\n", hdr->length, max_payload);
      return -1;
    }
    
    n = recv(sock, payload, hdr->length, MSG_WAITALL);
    if(n != (ssize_t)hdr->length)
      return -1;
    
    uint32_t check = simple_checksum(payload, hdr->length);
    if(check != hdr->checksum)
    {
      fprintf(stderr, "Checksum mismatch: 0x%x != 0x%x\n", check, hdr->checksum);
      return -1;
    }
  }
  
  return 0;
}

static void *client_handler(void *arg)
{
  int client_sock = *(int *)arg;
  free(arg);
  
  char client_addr[64];
  struct sockaddr_in addr;
  socklen_t addr_len = sizeof(addr);
  getpeername(client_sock, (struct sockaddr *)&addr, &addr_len);
  snprintf(client_addr, sizeof(client_addr), "%s:%d", 
           inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));
  
  printf("[%s] Client connected\n", client_addr);
  
  pthread_mutex_lock(&g_state.state_lock);
  g_state.n_clients_connected++;
  pthread_mutex_unlock(&g_state.state_lock);
  
  message_header_t hdr;
  client_info_t client_info;
  
  if(recv_message(client_sock, &hdr, &client_info, sizeof(client_info)) < 0 ||
     hdr.type != MSG_CLIENT_HELLO)
  {
    fprintf(stderr, "[%s] Failed to receive CLIENT_HELLO\n", client_addr);
    close(client_sock);
    goto cleanup;
  }
  
  printf("[%s] Client: %s, type: %s\n", client_addr, 
         client_info.hostname, client_info.client_type);
  
  if(send_message(client_sock, MSG_SERVER_HELLO, NULL, 0) < 0)
  {
    fprintf(stderr, "[%s] Failed to send SERVER_HELLO\n", client_addr);
    close(client_sock);
    goto cleanup;
  }
  
  char buffer[8192];
  while(g_state.running)
  {
    if(recv_message(client_sock, &hdr, buffer, sizeof(buffer)) < 0)
    {
      fprintf(stderr, "[%s] Connection lost\n", client_addr);
      break;
    }
    
    switch(hdr.type)
    {
      case MSG_REQUEST_WORK:
      {
        work_assignment_t work;
        
        pthread_mutex_lock(&g_state.state_lock);
        work.work_id = g_state.next_work_id++;
        work.start_nonce = g_state.next_nonce;
        work.end_nonce = g_state.next_nonce + WORK_RANGE_SIZE;
        work.priority = 0;
        g_state.next_nonce = work.end_nonce;
        g_state.total_nonces_assigned += WORK_RANGE_SIZE;
        pthread_mutex_unlock(&g_state.state_lock);
        
        printf("[%s] Assigned work %u: nonces %lu-%lu\n", 
               client_addr, work.work_id, (unsigned long)work.start_nonce, (unsigned long)work.end_nonce);
        
        if(send_message(client_sock, MSG_WORK_ASSIGNMENT, &work, sizeof(work)) < 0)
        {
          fprintf(stderr, "[%s] Failed to send work assignment\n", client_addr);
          goto done;
        }
        break;
      }
      
      case MSG_REPORT_COIN:
      {
        coin_report_t *report = (coin_report_t *)buffer;
        
        printf("[%s] *** COIN FOUND *** work_id=%u nonce=%lu zeros=%u\n",
               client_addr, report->work_id, (unsigned long)report->nonce, report->zeros);
        
        printf("    coin: \"");
        for(int b = 0; b < 55; b++)
        {
          unsigned char ch = ((unsigned char *)report->coin_data)[b ^ 3];
          if(ch >= 32 && ch <= 126) putchar((int)ch); else putchar('?');
        }
        printf("\"\n");
        
        printf("    sha1: ");
        for(int h = 0; h < 20; h++)
          printf("%02x", ((unsigned char *)report->hash)[h ^ 3]);
        printf("\n");
        
        pthread_mutex_lock(&g_state.state_lock);
        save_coin(report->coin_data);
        save_coin(NULL);
        g_state.total_coins_found++;
        pthread_mutex_unlock(&g_state.state_lock);
        
        break;
      }
      
      case MSG_WORK_COMPLETE:
      {
        work_completion_t *completion = (work_completion_t *)buffer;
        
        pthread_mutex_lock(&g_state.state_lock);
        g_state.total_nonces_completed += completion->nonces_tested;
        pthread_mutex_unlock(&g_state.state_lock);
        
        printf("[%s] Work %u complete: %lu nonces in %.2fs (%.0f nonces/sec), %u coins\n",
               client_addr, completion->work_id, (unsigned long)completion->nonces_tested,
               completion->elapsed_time, 
               (double)completion->nonces_tested / completion->elapsed_time,
               completion->coins_found);
        break;
      }
      
      case MSG_PING:
        send_message(client_sock, MSG_PONG, NULL, 0);
        break;
      
      default:
        fprintf(stderr, "[%s] Unknown message type: %u\n", client_addr, hdr.type);
        break;
    }
  }
  
done:
  send_message(client_sock, MSG_SHUTDOWN, NULL, 0);
  close(client_sock);
  
cleanup:
  pthread_mutex_lock(&g_state.state_lock);
  g_state.n_clients_connected--;
  pthread_mutex_unlock(&g_state.state_lock);
  
  printf("[%s] Client disconnected\n", client_addr);
  return NULL;
}

static void *status_reporter(void *arg)
{
  (void)arg;
  
  while(g_state.running)
  {
    sleep(10);
    
    pthread_mutex_lock(&g_state.state_lock);
    printf("\n=== Server Status ===\n");
    printf("Clients connected: %d\n", g_state.n_clients_connected);
    printf("Next nonce: %lu\n", (unsigned long)g_state.next_nonce);
    printf("Total assigned: %lu\n", (unsigned long)g_state.total_nonces_assigned);
    printf("Total completed: %lu\n", (unsigned long)g_state.total_nonces_completed);
    printf("Total coins found: %u\n", g_state.total_coins_found);
    printf("=====================\n\n");
    pthread_mutex_unlock(&g_state.state_lock);
  }
  
  return NULL;
}

int main(int argc, char **argv)
{
  int port = DETI_DEFAULT_PORT;
  uint64_t start_nonce = 0;
  
  if(argc > 1)
    port = atoi(argv[1]);
  if(argc > 2)
    start_nonce = strtoull(argv[2], NULL, 10);
  
  printf("DETI Coin Search Server\n");
  printf("=======================\n");
  printf("Port: %d\n", port);
  printf("Starting nonce: %lu\n", (unsigned long)start_nonce);
  printf("\n");
  
  memset(&g_state, 0, sizeof(g_state));
  g_state.next_nonce = start_nonce;
  g_state.running = 1;
  pthread_mutex_init(&g_state.state_lock, NULL);
  
  signal(SIGINT, handle_sigint);
  signal(SIGPIPE, SIG_IGN);
  
  int listen_sock = socket(AF_INET, SOCK_STREAM, 0);
  if(listen_sock < 0)
  {
    perror("socket");
    return 1;
  }
  
  int opt = 1;
  setsockopt(listen_sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  
  struct sockaddr_in addr;
  memset(&addr, 0, sizeof(addr));
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(port);
  
  if(bind(listen_sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
  {
    perror("bind");
    close(listen_sock);
    return 1;
  }
  
  if(listen(listen_sock, 10) < 0)
  {
    perror("listen");
    close(listen_sock);
    return 1;
  }
  
  printf("Server listening on port %d\n", port);
  printf("Waiting for clients...\n\n");
  
  pthread_t status_thread;
  pthread_create(&status_thread, NULL, status_reporter, NULL);
  
  while(g_state.running)
  {
    struct sockaddr_in client_addr;
    socklen_t client_len = sizeof(client_addr);
    
    int client_sock = accept(listen_sock, (struct sockaddr *)&client_addr, &client_len);
    if(client_sock < 0)
    {
      if(errno == EINTR)
        continue;
      perror("accept");
      break;
    }
    
    pthread_t thread;
    int *sock_ptr = malloc(sizeof(int));
    *sock_ptr = client_sock;
    pthread_create(&thread, NULL, client_handler, sock_ptr);
    pthread_detach(thread);
  }
  
  printf("\nShutdown requested, waiting for clients to disconnect...\n");
  
  for(int i = 0; i < 50 && g_state.n_clients_connected > 0; i++)
  {
    usleep(100000);
  }
  
  close(listen_sock);
  pthread_mutex_destroy(&g_state.state_lock);
  
  printf("\nFinal statistics:\n");
  printf("Total nonces assigned: %lu\n", (unsigned long)g_state.total_nonces_assigned);
  printf("Total nonces completed: %lu\n", (unsigned long)g_state.total_nonces_completed);
  printf("Total coins found: %u\n", g_state.total_coins_found);
  
  return 0;
}
