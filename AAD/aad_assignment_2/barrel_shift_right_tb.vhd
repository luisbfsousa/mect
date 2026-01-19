--
-- AAD 2025/2025, barrel shift right test bench
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity barrel_shift_right_tb is
end barrel_shift_right_tb;

architecture stimulus of barrel_shift_right_tb is
  constant DATA_BITS_LOG2 : positive := 4;
  signal s_data_in  : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal s_data_out : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal s_shift    : std_logic_vector(   DATA_BITS_LOG2-1 downto 0);
  signal s_missing  : std_logic;
begin
  uut : entity work.barrel_shift_right(behavioral)
               generic map
               (
                 DATA_BITS_LOG2 => DATA_BITS_LOG2
               )
               port map
               (
                 data_in  => s_data_in,
                 data_out => s_data_out,
                 shift    => s_shift,
                 missing  => s_missing
               );
  stim : process is
  begin
    -- first round
    s_data_in <= (0 to 2**DATA_BITS_LOG2-2 => '1', others => '0');
    for_1 : for i in 0 to 2**DATA_BITS_LOG2-1 loop
      s_shift <= std_logic_vector(to_unsigned(i,DATA_BITS_LOG2));
      s_missing <= '0';
      wait for 250 ps;
      s_missing <= '1';
      wait for 250 ps;
    end loop;
    -- second round
    s_data_in <= (others => '1');
    for_2 : for i in 0 to 2**DATA_BITS_LOG2-1 loop
      s_shift <= std_logic_vector(to_unsigned(i,DATA_BITS_LOG2));
      s_missing <= '0';
      wait for 250 ps;
      s_missing <= '1';
      wait for 250 ps;
    end loop;
    -- done
    wait;
  end process;
end stimulus;
