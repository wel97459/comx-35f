COMX-35F
============
This is a cycle-accurate COMX-35 computer that has been implemented on an FPGA, with all the hardware written in SpinalHDL.

Video
---------------
The system currently supports NTSC video.  Everything needed for PAL is available in the hardware.

For NTCS color you will need a clock source of 14.318MHz, currently I'm using a si5351 breakout board.
Both Burst and Luma use a 3 bit R2R ladder DAC.
And this get mixed the same as on the [COMIX-35][2] with a NPN transistor driving the video output.

Inputs
---------------
For a keyboard I'm using a BlackBerry Q10 PMOD, and not the best option.

There is a serial interface available for reading and writing programs to memory, which also allows for halting the CPU, and resetting the system. This interface also can press keys for type in programs. The baud rate for this interface is 57600.

The use of an Op-amp as a comparator for the input pin of the tape interface works well for the loading of programs.
I haven't tried saving out to a tape yet.

When using [EMMA][1] to generate tape audio, it is important to set the CPU frequency to 5.53125MHz. This also needs to be done for the COMIX-35. 
The pre-tone should be 2Khz.

Simulation
---------------
Also there is a simulator available that features an NTSC monitor capable of displaying the output of the video hardware. This is extremely useful for obtaining a comprehensive hardware trace of the system as it runs. However, the simulator only runs at 2 FPS.

TODOs & Problems
---------------
It appears that unintentional improvements were made to the video hardware of this implementation. As a consequence, it is possible that any code developed on the system may not function correctly on the original COMX-35 hardware. Similarly, [EMMA][1] will also run the same code without error.

[1]: <https://www.emma02.hobby-site.com/> "EMMA"
[2]: <https://github.com/schlae/comix-35> "COMIX-35"
