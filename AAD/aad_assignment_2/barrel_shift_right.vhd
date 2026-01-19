--
-- AAD 2025/2026, barrel shift right
--
-- too cumbersome to write a full structural implementation
-- its just conditional shifts by 1, 2, 4, ...
-- this could have been done in VHDL using the srl and sra operators, but where is the fun in doing that?
--

library IEEE;
use IEEE.std_logic_1164.all;

entity barrel_shift_right is
  generic
  (
    DATA_BITS_LOG2 : integer range 1 to 6 := 4
  );
  port
  (
    data_in  : in  std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- input data
    data_out : out std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- output data
    shift    : in  std_logic_vector(   DATA_BITS_LOG2-1 downto 0); -- right shift amount
    missing  : in  std_logic                                       -- '0' for logic shift, '1' for arithmetic shift
  );
end barrel_shift_right;

architecture behavioral of barrel_shift_right is
  type internal_data_t is array(0 to DATA_BITS_LOG2) of std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal s_internal_data : internal_data_t;
begin
  s_internal_data(0) <= data_in;
  data_out <= s_internal_data(DATA_BITS_LOG2);
  for_i : for i in 0 to DATA_BITS_LOG2-1 generate
            slice : entity work.shift_right_slice(behavioral)
              generic map
              (
                DATA_BITS    => 2**DATA_BITS_LOG2,
                SHIFT_AMOUNT => 2**i
              )
              port map
              (
                data_in  => s_internal_data(i),
                data_out => s_internal_data(i+1),
                sel      => shift(i),
                missing  => missing
              );
          end generate;
end behavioral;
