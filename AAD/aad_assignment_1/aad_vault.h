//
// Tomás Oliveira e Silva,  September 2025
//
// Arquiteturas de Alto Desempenho 2025/2026
//
// implements a vault for all found DETI coins
//

#ifndef AAD_VAULT
#define AAD_VAULT

static void save_coin(u32_t coin[14])
{
# define VAULT_FILE_NAME  "deti_coins_v2_vault.txt"
# define MAX_SAVED_COINS  65536u
  static u08_t saved_coins[MAX_SAVED_COINS][4 + 55];
  static u32_t n_saved_coins = 0u;
  static u08_t deti_coin_v2_template[56u] =
  { // non-zero entries are mandatory, the others are arbitrary
    [ 0u] = (u08_t)'D',
    [ 1u] = (u08_t)'E',
    [ 2u] = (u08_t)'T',
    [ 3u] = (u08_t)'I',
    [ 4u] = (u08_t)' ',
    [ 5u] = (u08_t)'c',
    [ 6u] = (u08_t)'o',
    [ 7u] = (u08_t)'i',
    [ 8u] = (u08_t)'n',
    [ 9u] = (u08_t)' ',
    [10u] = (u08_t)'2',
    [11u] = (u08_t)' ',
    [54u] = (u08_t)'\n',
    [55u] = (u08_t)0x80
  };
  static int error_tolerance_count = 4; // number of errors to tolerate before bailing out
  u32_t idx,n,hash[5];
  char *reason;
  u08_t *s;

  //
  // handle a NULL argument (meaning: save all stored DETI coins) or an already full buffer
  //
  if(coin == NULL || n_saved_coins == MAX_SAVED_COINS)
  {
    if(n_saved_coins > 0u)
    {
      FILE *fp = fopen(VAULT_FILE_NAME,"a");
      if(fp == NULL                                                                                            ||
         fwrite((void *)&saved_coins[0][0],(size_t)(4 + 55),(size_t)n_saved_coins,fp) != (size_t)n_saved_coins ||
         fflush(fp) != 0                                                                                       ||
         fclose(fp) != 0)
      {
        fprintf(stderr,"save_coin(): error while updating file \"" VAULT_FILE_NAME "\"\n");
        exit(1);
      }
    }
    n_saved_coins = 0u;
  }
  if(coin == NULL)
    return;
  //
  // compute the SHA1 secure hash
  //
  sha1(coin,hash);
  //
  // make sure that the coin has the appropriate format
  //
  for(idx = 0u;idx < 56u;idx++)
    if((deti_coin_v2_template[idx] != (u08_t)0 && deti_coin_v2_template[idx] != ((u08_t *)coin)[idx ^ 3]) || (idx >= 12u && idx <= 53u && ((char *)coin)[idx ^ 3] == '\n'))
    {
      reason = "coin does not match the template";
error:
      fprintf(stderr,"save_coin(): bad DETI coin v2 format (%s)\n",reason);
      fprintf(stderr,"  coin contents\n");
      fprintf(stderr,"    idx  template      coin\n");
      fprintf(stderr,"    --- --------- ---------\n");
      for(idx = 0u;idx < 56u;idx++)
      {
        u08_t t = deti_coin_v2_template[idx];
        u08_t c = ((u08_t *)coin)[idx ^ 3];
        fprintf(stderr,"    %3u",idx);
        if(t == (u08_t)0)
          fprintf(stderr," arbitrary");
        else if(t == '\n')
          fprintf(stderr," 0x%02X '\\n'",(int)t);
        else if(t >= 32 && t <= 126)
          fprintf(stderr," 0x%02X  '%c'",(int)t,t);
        else
          fprintf(stderr," 0x%02X     ",(int)t);
        fprintf(stderr," 0x%02X",(int)c);
        if(c == '\n')
          fprintf(stderr," '\\n'");
        else if(c == '\b')
          fprintf(stderr," '\\b'");
        else if(c >= 32 && c <= 126)
          fprintf(stderr,"  '%c'",c);
        else
          fprintf(stderr,"     ");
        if((t != (u08_t)0 && t != c) || (idx >= 12u && idx <= 53u && c == '\n'))
          fprintf(stderr," error");
        fprintf(stderr,"\n");
      }
      fprintf(stderr,"    --- --------- ---------\n");
      fprintf(stderr,"  SHA1 secure hash\n");
      fprintf(stderr,"    idx      value\n");
      fprintf(stderr,"    --- ----------\n");
      for(idx = 0u;idx < 5u;idx++)
        fprintf(stderr,"      %u 0x%08X%s\n",idx,hash[idx],(idx == 0u && hash[idx] != 0xAAD20250u) ? " error" : "");
      fprintf(stderr,"    --- ----------\n\n");
      if(--error_tolerance_count < 0)
        exit(1); // too many errors, exit
      return; // ignore this coin
    }
  //
  // chech the DETI coin v2 signature
  //
  if(hash[0] != 0xAAD20250u)
  {
    reason = "bad coin signature";
    goto error;
  }
  //
  // count the number of leading zeros bits of the last 4 32-bit words of the SHA1 secure hash
  //
  for(n = 0u;n < 128u;n++)
     if((hash[1u + n / 32u] >> (31u - n % 32u)) % 2u != 0u)
       break;
  //
  // save the coin in the buffer
  // format of each line: "Vuv:" "coin_data" where u and v are ascii digits that encode, in base 10, the reported power of the coin
  //
  if(n > 99u)
    n = 99u;
  s = &saved_coins[n_saved_coins++][0];
  *s++ = (u08_t)'V';
  *s++ = (u08_t)('0' + n / 10u);
  *s++ = (u08_t)('0' + n % 10u);
  *s++ = (u08_t)':';
  for(idx = 0u;idx < 55u;idx++)
    *s++ = ((u08_t *)coin)[idx ^ 3];
# undef VAULT_FILE_NAME
# undef MAX_SAVED_COINS
}

#endif
