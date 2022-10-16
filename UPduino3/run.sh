#!/bin/bash
set -x
rm -fy top* 
rm -fy hardware* 

yosys -p "synth_ice40 -top top -json hardware.json" -q ../top.v
nextpnr-ice40 --up5k --package sg48 --opt-timing --pcf-allow-unconstrained --json hardware.json --asc hardware.asc --pcf upduino.pcf
icepack hardware.asc hardware.bin
iceprog -d i:0x0403:0x6014:0 hardware.bin