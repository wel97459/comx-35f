TARGET=Top_ice40
OBJS+=../src/main/scala/comx35/$(TARGET).scala
OBJS+=../src/main/scala/comx35/Config_ice40.scala
HW_FILES=./gen/*.v # TODO: add support to vhdl somehow
TOP_FILE=./gen/$(TARGET).v
PIN_BINDING_FILE=upduino.pcf

all: prog

# generate hardware code from spinal code
#$(TOP_FILE): $(OBJS)
#	sbt "runMain comx35.$(TARGET)_Verilog"

# synthetize rtl (generates a json file)
$(TARGET).json: $(TOP_FILE)
	yosys -p "synth_ice40 -top $(TARGET) -json $@" -q $(HW_FILES)

# transform synthetized rtl + pin mapping into fpga config file
$(TARGET)_out.config: $(TARGET).json $(PIN_BINDING_FILE)
	nextpnr-ice40 --up5k --package sg48 --opt-timing  --timing-allow-fail --pcf-allow-unconstrained --json $< --asc $@ --pcf $(PIN_BINDING_FILE)

# generate bitstream from config file
$(TARGET).bit: $(TARGET)_out.config
	icepack $< $@

# upload bitstream to fpga
prog: $(TARGET).bit
	iceprog -d i:0x0403:0x6014:0 $(TARGET).bit

clean:
	rm -f *.svf *.bit *.config *.json *.ys -r target gen project .bsp
