#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include "aad_data_types.h"
#include "aad_sha1_cpu.h"
#include "aad_vault.h"

void save_coin_wrapper(u32_t *coin)
{
  save_coin(coin);
}

void save_coin_flush(void)
{
  save_coin(NULL);
}
