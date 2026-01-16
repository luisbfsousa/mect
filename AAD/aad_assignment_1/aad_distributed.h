#ifndef AAD_DISTRIBUTED_H
#define AAD_DISTRIBUTED_H

#include <stdint.h>
#include "aad_data_types.h"


#define DETI_DEFAULT_PORT 9876
#define DETI_PROTOCOL_VERSION 1
#define WORK_RANGE_SIZE 100000000ULL

typedef enum {
  MSG_CLIENT_HELLO = 1,
  MSG_SERVER_HELLO = 2,
  MSG_REQUEST_WORK = 3,
  MSG_WORK_ASSIGNMENT = 4,
  MSG_REPORT_COIN = 5,
  MSG_WORK_COMPLETE = 6,
  MSG_NO_WORK = 7,
  MSG_SHUTDOWN = 8,
  MSG_PING = 9,
  MSG_PONG = 10
} message_type_t;

typedef struct {
  char hostname[64];
  char client_type[32];
  uint32_t capabilities;
  uint32_t version;
} client_info_t;

typedef struct {
  uint64_t start_nonce;
  uint64_t end_nonce;
  uint32_t priority;
  uint32_t work_id;
} work_assignment_t;

typedef struct {
  uint64_t nonce;
  uint32_t zeros;
  uint32_t work_id;
  u32_t coin_data[14];
  u32_t hash[5];
} coin_report_t;

typedef struct {
  uint32_t work_id;
  uint64_t nonces_tested;
  uint32_t coins_found;
  double elapsed_time;
} work_completion_t;

typedef struct {
  uint32_t magic;
  uint16_t version;
  uint16_t type;
  uint32_t length;
  uint32_t checksum;
} message_header_t;

#define PROTOCOL_MAGIC 0xDEA1C01Eu

static inline uint32_t simple_checksum(const void *data, size_t len)
{
  uint32_t sum = 0;
  const uint8_t *p = (const uint8_t *)data;
  for(size_t i = 0; i < len; i++)
    sum = (sum << 1) ^ p[i];
  return sum;
}

static inline void init_message_header(message_header_t *hdr, message_type_t type, uint32_t payload_len)
{
  hdr->magic = PROTOCOL_MAGIC;
  hdr->version = DETI_PROTOCOL_VERSION;
  hdr->type = (uint16_t)type;
  hdr->length = payload_len;
  hdr->checksum = 0;
}

#endif
