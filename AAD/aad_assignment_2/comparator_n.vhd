--
-- AAD 2025/2026, n-bit comparator
--
-- for extra credit, implement this also using chains of unsigned comparators:
--
-- unsigned comparator stage (one per bit)
--   in   a_bit   b_bit   old_lt   old_eq   old_gt
--   out                  new_lt   new_eq   new_gt
-- logic, start from the least significant bit with old_lt=old_gt=0 and old_eq=1
--   if a_bit=b_bit (no change, keep the earlier result)
--     new_lt=old_lt   new_eq=old_eq   new_gt=old_gt
--   else if a_bit=1 (a is greater because a_bit=1 and b_bit=0)
--     new_lt=0        new_eq=0        new_gt=1
--   else  (a is smaller because a_bit=0 and b_bit=1)
--     new_lt=1        new_eq=0        new_gt=0
-- use a transport delay of 5 ps per stage
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity comparator_n is
  generic
  (
    N : positive
  );
  port
  (
    a  : in  std_logic_vector(N-1 downto 0);
    b  : in  std_logic_vector(N-1 downto 0);
    lt : out std_logic; -- '1' if a<b, '0' otherwise
    eq : out std_logic; -- '1' if a=b, '0' otherwise
    gt : out std_logic  -- '1' if a>b, '0' otherwise
  );
end comparator_n;

architecture structural of comparator_n is
  signal c_lt : std_logic_vector(0 to N);
  signal c_eq : std_logic_vector(0 to N);
  signal c_gt : std_logic_vector(0 to N);
begin
  c_lt(0) <= '0';
  c_eq(0) <= '1';
  c_gt(0) <= '0';
  chain_gen: for i in 0 to N-1 generate
    process(a(i), b(i), c_lt(i), c_eq(i), c_gt(i))
    begin
      if a(i) = b(i) then
        c_lt(i+1) <= c_lt(i);
        c_eq(i+1) <= c_eq(i);
        c_gt(i+1) <= c_gt(i);
      elsif a(i) = '1' then 
        c_lt(i+1) <= '0';
        c_eq(i+1) <= '0';
        c_gt(i+1) <= '1';
      else 
        c_lt(i+1) <= '1';
        c_eq(i+1) <= '0';
        c_gt(i+1) <= '0';
      end if;
    end process;
  end generate;

  lt <= transport c_lt(N) after 5 ps * N;
  eq <= transport c_eq(N) after 5 ps * N;
  gt <= transport c_gt(N) after 5 ps * N;

end structural;
