#!/bin/bash
set -x
rm -fy Top* 
cp ../Top* ./
yosys -p "synth_ice40 -json hardware.json" -q Top.v
nextpnr-ice40 --up5k --package sg48 --opt-timing --json hardware.json --asc hardware.asc --pcf upduino.pcf
icepack hardware.asc hardware.bin
iceprog -d i:0x0403:0x6014:0 hardware.bin