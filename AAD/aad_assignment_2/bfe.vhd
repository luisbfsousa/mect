--
-- AAD 2025/2026, data flow for the bit-field extract instruction
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity bfe is
  generic
  (
    DATA_BITS_LOG2 : integer range 2 to 6 := 4                    -- use 4 by default
  );
  port
  ( 
    dst     : out std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- 15 downto 0
    src     : in  std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- 15 downto 0
    size    : in  std_logic_vector(   DATA_BITS_LOG2-1 downto 0); --  3 downto 0
    start   : in  std_logic_vector(   DATA_BITS_LOG2-1 downto 0); --  3 downto 0
    variant : in  std_logic                                       -- '0' for .u and '1' for .s
  );
end bfe;

architecture structural of bfe is
  constant N_BITS : integer := 2**DATA_BITS_LOG2;

  -- internal signals
  signal shifted_src     : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal msfb_mask       : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal mask            : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal msb_bit         : std_logic;
  signal sign_extended   : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal zero_extended   : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
  signal msfb_mask_eq    : std_logic_vector(2**DATA_BITS_LOG2-1 downto 0);
begin
  barrel_shifter : entity work.barrel_shift_right(behavioral)
    generic map
    (
      DATA_BITS_LOG2 => DATA_BITS_LOG2
    )
    port map
    (
      data_in  => src,
      data_out => shifted_src,
      shift    => start,
      missing  => variant
    );

  mask_gen : for i in 0 to 2**DATA_BITS_LOG2-1 generate
    comp : entity work.comparator_n(structural)
      generic map
      (
        N => DATA_BITS_LOG2
      )
      port map
      (
        a  => std_logic_vector(to_unsigned(i, DATA_BITS_LOG2)),
        b  => size,
        lt => open,
        eq => msfb_mask_eq(i),
        gt => mask(i)
      );
  end generate;

  msfb_mask <= msfb_mask_eq;

  msb_bit <= or (shifted_src and msfb_mask);

  zero_extended <= shifted_src and not mask;

  sign_ext_gen : for i in 0 to 2**DATA_BITS_LOG2-1 generate
    sign_extended(i) <= shifted_src(i) when mask(i) = '0' else msb_bit;
  end generate;

  dst <= sign_extended when variant = '1' else zero_extended;

end structural;
