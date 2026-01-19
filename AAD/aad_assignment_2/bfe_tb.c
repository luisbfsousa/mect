//
// AAD 2025/2026, bfe testbench generator
//

#include <stdio.h>

//
// missing bits behaviour:
//   == 0 -> zero extend
//   != 0 -> sign extend
//
#ifndef missing_bits_behaviour
# define missing_bits_behaviour  0
#endif

//
// number of bits of the registers (16 bits in the assignment)
//
#ifndef n_bits_log2
# define n_bits_log2  4
#endif
#define n_bits  (1 << n_bits_log2)

static const char *to_binary(unsigned long n,int bits)
{
  static char buffer[8][80];
  static int idx = 0;
  char *s;

  s = &buffer[idx][79];
  idx = (idx + 1) & 7;
  *s = '\0';
  for(int i = 0;i < bits;i++)
  {
    *--s = (char)((int)'0' + (int)(n & 1ul));
    n >>= 1;
  }
  return (const char *)s;
}

// static void do_one(unsigned long src,int size,int start)
// {
//   unsigned long dst0,dst1;

// #if missing_bits_behaviour == 0
//   // zero extend the n_bits register
//   src = src << (64 - n_bits);
//   src = src >> (64 - n_bits);
// #else
//   // sign-extend the n_bits register
//   src = src << (64 - n_bits);
//   src = (unsigned long)((signed long)src >> (64 - n_bits));
// #endif
//   // extract and zero extend the bit-field
//   dst0 = src >> start;
//   dst0 = dst0 << (63 - size);
//   dst0 = dst0 >> (63 - size);
//   // extract and sign extend the bit-field
//   dst1 = src >> start;
//   dst1 = dst1 << (63 - size);
//   dst1 = (unsigned long)((signed long)dst1 >> (63 - size));
//   // output test data
//   printf(
//     "    --- new test case"                                                       "\n"
//     "    s_src      <= \"%s\";"                                                   "\n"
//     "    s_size     <= \"%s\"; -- %d"                                             "\n"
//     "    s_start    <= \"%s\"; -- %d"                                             "\n"
//     "    s_variant  <= '0';"                                                      "\n"
//     "    s_expected <= \"%s\";"                                                   "\n"
//     "    wait for 499 ps;"                                                        "\n"
//     "    assert s_dst = s_expected report \"bad dst\" severity note;"             "\n"
//     "    wait for 1 ps;"                                                          "\n"
//     "    s_variant  <= '1';"                                                      "\n"
//     "    s_expected <= \"%s\";"                                                   "\n"
//     "    wait for 499 ps;"                                                        "\n"
//     "    assert s_dst = s_expected report \"bad dst\" severity note;"             "\n"
//     "    wait for 1 ps;"                                                          "\n",
//     to_binary(src,n_bits),
//     to_binary(size,n_bits_log2),size,
//     to_binary(start,n_bits_log2),start,
//     to_binary(dst0,n_bits),
//     to_binary(dst1,n_bits)
//   );
// }

static void do_one(unsigned long src,int size,int start)
{
  unsigned long dst0, dst1;
  unsigned long src_u, src_s;

  // Preparar versão com Zero-Extend
  src_u = src << (64 - n_bits);
  src_u = src_u >> (64 - n_bits);

  // Preparar versão com Sign-Extend (para variante)
  src_s = src << (64 - n_bits);
  src_s = (unsigned long)((signed long)src_s >> (64 - n_bits));

  // Variante .u (usa src_u - missing bits a 0)
  dst0 = src_u >> start;
  dst0 = dst0 << (63 - size);
  dst0 = dst0 >> (63 - size);

  // Variante .s (usa src_s - missing bits = MSB)
  dst1 = src_s >> start;
  dst1 = dst1 << (63 - size);
  dst1 = (unsigned long)((signed long)dst1 >> (63 - size));

  // output test data
  printf(
    "    --- new test case"                                                       "\n"
    "    s_src      <= \"%s\";"                                                   "\n"
    "    s_size     <= \"%s\"; -- %d"                                             "\n"
    "    s_start    <= \"%s\"; -- %d"                                             "\n"
    "    s_variant  <= '0';"                                                      "\n"
    "    s_expected <= \"%s\";"                                                   "\n"
    "    wait for 499 ps;"                                                        "\n"
    "    assert s_dst = s_expected report \"bad dst\" severity note;"             "\n"
    "    wait for 1 ps;"                                                          "\n"
    "    s_variant  <= '1';"                                                      "\n"
    "    s_expected <= \"%s\";"                                                   "\n"
    "    wait for 499 ps;"                                                        "\n"
    "    assert s_dst = s_expected report \"bad dst\" severity note;"             "\n"
    "    wait for 1 ps;"                                                          "\n",
    to_binary(src,n_bits),
    to_binary(size,n_bits_log2),size,
    to_binary(start,n_bits_log2),start,
    to_binary(dst0,n_bits),
    to_binary(dst1,n_bits)
  );
}

int main(void)
{
  printf(
    "--"                                                                          "\n"
    "-- AAD 2025/2026, bfe testbench"                                             "\n"
    "--"                                                                          "\n"
    ""                                                                            "\n"
    "library IEEE;"                                                               "\n"
    "use IEEE.std_logic_1164.all;"                                                "\n"
    "use IEEE.numeric_std.all;"                                                   "\n"
    ""                                                                            "\n"
    "entity bfe_tb is"                                                            "\n"
    "end bfe_tb;"                                                                 "\n"
    ""                                                                            "\n"
    "architecture stimulus of bfe_tb is"                                          "\n"
    "  -- constants"                                                              "\n"
    "  constant DATA_BITS_LOG2 : positive := 4;"                                  "\n"
    "  -- the interface signals"                                                  "\n"
    "  signal s_dst            : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);" "\n"
    "  signal s_src            : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);" "\n"
    "  signal s_size           : std_logic_vector(   DATA_BITS_LOG2-1 downto 0);" "\n"
    "  signal s_start          : std_logic_vector(   DATA_BITS_LOG2-1 downto 0);" "\n"
    "  signal s_variant        : std_logic;"                                      "\n"
    "  -- internal testbench siganls (to verify the correctness of the uut)"      "\n"
    "  signal s_expected       : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);" "\n"
    "begin"                                                                       "\n"
    "  uut : entity work.bfe(structural)"                                         "\n"
    "               generic map"                                                  "\n"
    "               ("                                                            "\n"
    "                 DATA_BITS_LOG2 => DATA_BITS_LOG2"                           "\n"
    "               )"                                                            "\n"
    "               port map"                                                     "\n"
    "               ("                                                            "\n"
    "                 dst     => s_dst,"                                          "\n"
    "                 src     => s_src,"                                          "\n"
    "                 size    => s_size,"                                         "\n"
    "                 start   => s_start,"                                        "\n"
    "                 variant => s_variant"                                       "\n"
    "               );"                                                           "\n"
    "  stim : process is"                                                         "\n"
    "  begin"                                                                     "\n"
  );
  do_one(0b0110100101100101ul, 5, 3);
  do_one(0b0110100101100101ul, 4,13);
  do_one(0b1111111111111111ul, 5, 3);
  do_one(0b1111111111111111ul, 4,13);
  printf(
    "    -- wait for ever"                                                        "\n"
    "    wait;"                                                                   "\n"
    "  end process;"                                                              "\n"
    "end stimulus;"                                                               "\n"
  );
  return 0;
}
