clean:
	rm -f a.out bfe_tb.vhd work-*.cf *.vcd

.PHONY:	comparator_n.vcd
comparator_n.vcd:
	rm -f work-*.cf
	ghdl -i --std=08 comparator_n.vhd comparator_n_tb.vhd
	ghdl -m --std=08 comparator_n_tb
	ghdl -r --std=08 comparator_n_tb --stop-time=1000ps --vcd=comparator_n.vcd
	@echo use \"gtkwave comparator_n.vcd\" to see the simulation waveforms

.PHONY:	barrel_shift_right.vcd
barrel_shift_right.vcd:
	rm -f work-*.cf
	ghdl -i --std=08 shift_right_slice.vhd barrel_shift_right.vhd barrel_shift_right_tb.vhd
	ghdl -m --std=08 barrel_shift_right_tb
	ghdl -r --std=08 barrel_shift_right_tb --stop-time=16000ps --vcd=barrel_shift_right.vcd
	@echo use \"gtkwave barrel_shift_right.vcd\" to see the simulation waveforms

.PHONY:	bfe.vcd
bfe.vcd:	bfe_tb.vhd
	rm -f work-*.cf
	ghdl -i --std=08 shift_right_slice.vhd barrel_shift_right.vhd comparator_n.vhd bfe.vhd bfe_tb.vhd
	ghdl -m --std=08 bfe_tb
	ghdl -r --std=08 bfe_tb --stop-time=1000000ps --vcd=bfe.vcd
	@echo use \"gtkwave bfe.vcd\" to see the simulation waveforms

bfe_tb.vhd:	bfe_tb.c
	rm -f bfe_tb.vhd
	cc -Wall bfe_tb.c
	./a.out >bfe_tb.vhd
	rm -f a.out
