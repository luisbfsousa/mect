--
-- AAD 2025/2026, shift right slice for the shift right barrel shifter
--
-- too cumbersome to write a structural implementation
-- its just data movement coupled with 2 to 1 multiplexers
--

library IEEE;
use IEEE.std_logic_1164.all;

entity shift_right_slice is
  generic
  (
    DATA_BITS    : integer range 2 to 64 := 16;
    SHIFT_AMOUNT : integer range 1 to 63 :=  1
  );
  port
  (
    data_in  : in  std_logic_vector(DATA_BITS-1 downto 0); -- input data
    data_out : out std_logic_vector(DATA_BITS-1 downto 0); -- output data
    sel      : in  std_logic;                              -- '0' for no shift, '1' for shift
    missing  : in  std_logic                               -- '0' for logic shift, '1' for arithmetic shift
  );
end shift_right_slice;

architecture behavioral of shift_right_slice is
  signal s_out : std_logic_vector(DATA_BITS-1 downto 0);
begin
  assert (SHIFT_AMOUNT > 0 and SHIFT_AMOUNT < DATA_BITS) report "Bad SHIFT_AMOUNT generic value" severity failure;
  process(data_in,sel,missing) is
  begin
    if sel = '0' then
      s_out <= data_in;
    elsif missing = '0' then
      s_out <= (SHIFT_AMOUNT-1 downto 0 => '0'                 ) & data_in(DATA_BITS-1 downto SHIFT_AMOUNT);
    else
      s_out <= (SHIFT_AMOUNT-1 downto 0 => data_in(DATA_BITS-1)) & data_in(DATA_BITS-1 downto SHIFT_AMOUNT);
    end if;
  end process;
  data_out <= transport s_out after 10 ps;
end behavioral;
