--
-- AAD 2025/2026, comparator testbench
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity comparator_n_tb is
end comparator_n_tb;

architecture stimulus of comparator_n_tb is
  -- constants
  constant N : positive := 4;
  -- the interface signals
  signal s_a  : std_logic_vector(N-1 downto 0);
  signal s_b  : std_logic_vector(N-1 downto 0);
  signal s_lt : std_logic;
  signal s_eq : std_logic;
  signal s_gt : std_logic;
begin
  uut : entity work.comparator_n(behavioral)
               generic map
               (
                 N => N
               )
               port map
               (
                 a  => s_a,
                 b  => s_b,
                 lt => s_lt,
                 eq => s_eq,
                 gt => s_gt
               );
  stim : process is
  begin
    s_a <= "0010";
    s_b <= "0010";
    wait for 200 ps;
    s_a <= "1011";
    s_b <= "0100";
    wait for 200 ps;
    s_a <= "0101";
    s_b <= "1010";
    wait for 200 ps;
    s_a <= "0111";
    s_b <= "0111";
    wait for 200 ps;
    s_a <= "1111";
    s_b <= "1110";
    wait for 200 ps;
    wait;
  end process;
end stimulus;
