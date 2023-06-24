package comx35
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._
import DSPs_SpinalHDL._
import VIS._
import spinal.lib.memory.sdram.xdr.Core

import comx35._
case class ecp5_pll() extends BlackBox {
    val io = new Bundle {
        val reset = in Bool()
        val clkin = in Bool()
        val clkout0 = out Bool()
        val locked = out Bool()
    }
noIoPrefix()
}
class Top_ECP5 extends Component {
    val io = new Bundle{
        val reset_ = in Bool()
        val clk_25Mhz = in Bool() //12Mhz CLK
        val si_14Mhz = in Bool()

        val video = out Bits(3 bits)
        val sync = out Bool()
        val burst = out Bits(3 bits)

        val scl = inout(Analog(Bool()))
        val sda = inout(Analog(Bool()))

        val sound_dac_l = out Bool()
        val sound_dac_r = out Bool()

        val serial_tx = out Bool()
        val serial_rx = in Bool()

        val led_red = out Bool()

        val tape_led = out Bool()
        val tape_out = out Bool()
        val tape_in = in Bool()
        val tape_sw = in Bool()
        val tape_motor = out Bool()

        val ps2_clk = in Bool()
        val ps2_data = in Bool()
    }
    noIoPrefix()

    //Define clock domains
    val clk14Domain = ClockDomain.internal(name = "Core14",  frequency = FixedFrequency(14.3182 MHz))
    val clk11Domain = ClockDomain.internal(name = "Core11",  frequency = FixedFrequency(11.3407 MHz))
    val clk25Domain = ClockDomain.internal(name = "Core25",  frequency = FixedFrequency(25.0000 MHz))

    //Allow clock domain crossing.
    clk14Domain.setSyncronousWith(clk11Domain)
    clk25Domain.setSyncronousWith(clk11Domain)

    //Define PLL
    val PLL = new ecp5_pll()
    //Setup signals of PLL
    PLL.io.clkin := io.si_14Mhz
    
    clk25Domain.clock := io.clk_25Mhz
    clk25Domain.reset := !io.reset_

    clk14Domain.clock := io.si_14Mhz
    clk11Domain.clock := PLL.io.clkout0

    val Core11 = new ClockingArea(clk11Domain) {

        var reset = Reg(Bool) init (False)
        var rstCounter = CounterFreeRun(100)

        when(rstCounter.willOverflow){
            reset := True
        }

        val pro = new ProgrammingInterface(57600)
        io.serial_tx := pro.io.UartTX
        pro.io.UartRX := io.serial_rx
        pro.io.FlagIn := 0x00
        val keyReady = False
        pro.io.keys.ready := keyReady.fall()

        when(pro.io.FlagOut(0)){
            reset := False
            rstCounter.clear()
        }

        val ram = new Ram(15)
        ram.io.ena := True
        val areaRst = new ResetArea(!reset, false) {
            val kbd_ready = Reg(Bool()) init(False)
            val area40kHz = new SlowArea(100 kHz, true) {
                val ready = RegNext(kbd_ready) init(False)
                // val kbd = new Q10Keyboard()

                // kbd.io.i_hold := False

                // kbd.io.i_scl := io.scl
                // kbd.io.i_sda := io.sda
                // kbd.io.key_code_stream.ready := ready.rise()

                val kbd = new PS2_Keyboard_Decoder(100)
                kbd.io.clock_in := io.ps2_clk
                kbd.io.data_in := io.ps2_data
                kbd.io.keyCode.ready := ready.rise()
            }

            val a2c = new ascii2comx()
            //a2c.ascii := pro.io.keys.valid ? pro.io.keys.payload | area40kHz.kbd.io.key_code_stream.payload
            a2c.ascii := pro.io.keys.payload
            
            val areaDiv4 = new SlowArea(2) {
                val Tape_in = BufferCC(!io.tape_in, True)

                val Tape_sw_cc = BufferCC(!io.tape_sw, True)
                val Tape_sw = Debounce(1, 100 ms)
                Tape_sw.write(Tape_sw_cc.asBits)

                val Tape_Filter = Reg(Bits(8 bits)) init(0)
                val Tape_FF = Reg(Bool()) init(False)

                val Tape_stop = False
                val Tape_start = False
                val Tape_state = Reg(Bool()) init(False)

                Tape_Filter := Tape_in ## Tape_Filter(7 downto 1)
                when(Tape_Filter === B"8'hFF")
                {
                    Tape_FF := True
                }elsewhen(Tape_Filter === B"8'h00"){
                    Tape_FF := False
                }
                val comx35 = new comx35_test()
                comx35.io.Start := !pro.io.FlagOut(1)
                comx35.io.Wait := !pro.io.FlagOut(2)
                comx35.io.DataIn := 0x00
                comx35.io.Tape_in := Tape_FF 
                io.tape_out := comx35.io.Q
                io.tape_led := comx35.io.Q || Tape_FF 

                comx35.io.KBD_Latch := False
                //comx35.io.KBD_KeyCode := a2c.comx
                comx35.io.KBD_KeyCode := pro.io.keys.valid ? a2c.comx | area40kHz.kbd.io.keyCode.payload
                //comx35.io.KBD_Repeat := area40kHz.kbd.io.key_Held
                comx35.io.KBD_Repeat := False

                val keyHit = Reg(Bool()) init(False)
                when(comx35.io.KBD_Ready && comx35.io.Display_.rise()){
                    when(pro.io.keys.valid){
                        keyReady := True
                        comx35.io.KBD_Latch := True
                        kbd_ready := True
                    //}elsewhen(area40kHz.kbd.io.key_code_stream.valid){
                    }elsewhen(area40kHz.kbd.io.keyCode.valid){
                        comx35.io.KBD_Latch := True
                        kbd_ready := True
                    }
                }

                when(area40kHz.ready.rise()){
                    kbd_ready := False
                }

                val pram = new Ram(log2Up(0x3FF))
                val cram = new Ram(log2Up(0x4FF))
                pram.io.ena := True
                cram.io.ena := True

                val rom = new RamInit("./data/comx35.1.3.bin" ,log2Up(0x3FFF))

                io.sync := comx35.io.Sync

                pram.io.addra := comx35.io.PMA
                comx35.io.PMD_In := pram.io.douta
                pram.io.dina := comx35.io.PMD_Out
                pram.io.wea := comx35.io.PMWR_.rise()

                cram.io.addra := comx35.io.CMA3_PMA10 ## comx35.io.CMA
                comx35.io.CMD_In := cram.io.douta
                cram.io.dina := comx35.io.CMD_Out
                cram.io.wea := comx35.io.CMWR_.rise()

                rom.io.addra := comx35.io.Addr16(13 downto 0)
                rom.io.dina := 0
                rom.io.wea := 0
                rom.io.ena := True

                val ram_address = (comx35.io.Addr16.asUInt - 0x4000).asBits
                val wea = False

                when(!comx35.io.MRD)
                {
                    when(comx35.io.Addr16.asUInt < 0x4000)
                    {
                        when(comx35.io.N === 0){
                            //save and loads stop
                            when(comx35.io.Addr16 === 0x14e1 || comx35.io.Addr16 === 0x1047){
                                Tape_stop := True
                            }

                            //save and loads start
                            when(comx35.io.Addr16 === 0x1672 || comx35.io.Addr16 === 0x1675 || comx35.io.Addr16 ===  0x0e00 || comx35.io.Addr16 === 0x0e03){
                                Tape_start := True
                            }
                        }
                        comx35.io.DataIn := rom.io.douta
                    }elsewhen(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                        comx35.io.DataIn := ram.io.douta
                    }
                }

                when(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                    wea := comx35.io.MWR.rise()
                }

                when(!Tape_sw.value(0)){
                    Tape_state := False
                }elsewhen(Tape_sw.value(0) && Tape_stop){
                    Tape_state := True
                }elsewhen(Tape_sw.value(0) && Tape_start){
                    Tape_state := False
                }

                io.tape_motor := Tape_sw.value(0) && !Tape_state
            }
        }

        pro.io.RamInterface.DataIn := ram.io.douta
        when(pro.io.FlagOut(2))
        {
            ram.io.dina := pro.io.RamInterface.DataOut
            ram.io.wea := pro.io.RamInterface.Write
            ram.io.addra := (pro.io.RamInterface.Address.asUInt - 0x4000).asBits.resized
        }otherwise{
            ram.io.dina := areaRst.areaDiv4.comx35.io.DataOut
            ram.io.wea := areaRst.areaDiv4.wea
            ram.io.addra := areaRst.areaDiv4.ram_address.resized
        }
    }
    val Core14 = new ClockingArea(clk14Domain) {
        val Color = BufferCC(Core11.areaRst.areaDiv4.comx35.io.Color, B"000")
        val Burst = BufferCC(Core11.areaRst.areaDiv4.comx35.io.Burst, False)

        val Alive = Reg(UInt(8 bits)) init(0);
        when(Alive < U"8'hFF"){
            Alive := Alive + 1;
        }

        //0 - Black
        //1 - Green
        //2 - Blue
        //3 - Cyan
        //4 - Red
        //5 - Yellow
        //6 - Magenta
        //7 - White

        io.video := 0
        when(Color === 7 || Color === 1 || Color === 5 || Color === 3){
            io.video := 7 
        }elsewhen(Color === 4 || Color === 2 || Color === 6){
            io.video := 3 
        }

        val burst = Reg(Bits(3 bits)) init(0)

        val b = Reg(UInt(2 bits))
        b := b + 1;

        val o = U"00"
        val ob = b+o;

        when(Burst || Color === 1 || Color === 5 || Color === 6){ 
            o := 0
        }elsewhen(Color === 3){
            o := 1
        }elsewhen(Color === 4 || Color === 2){
            o := 3
        }

        when(Burst || Color === 3 || Color === 5 || Color === 4){
            when(ob === 0){
                burst := 7
            }elsewhen(ob === 1){
                burst := 3
            }elsewhen(ob === 2){
                burst := 0
            }elsewhen(ob === 3){
                burst := 3
            }
        }elsewhen(Color === 1){
            when(ob === 0){
                burst := 7 
            }elsewhen(ob === 1){
                burst := 2
            }elsewhen(ob === 2){
                burst := 0
            }elsewhen(ob === 3){
                burst := 5
            }
        }elsewhen(Color === 2 || Color === 6){
            when(ob === 0){
                burst := 2 
            }elsewhen(ob === 1){
                burst := 7
            }elsewhen(ob === 2){
                burst := 5
            }elsewhen(ob === 3){
                burst := 0
            }
        }

        io.burst := burst
    }

    val Core25 = new ClockingArea(clk25Domain) {
        //val kbd_sda_b = BufferCC(Core11.areaRst.area40kHz.kbd.io.o_sda_write, True)
        //val kbd_scl_b = BufferCC(Core11.areaRst.area40kHz.kbd.io.o_scl_write, True)
        val Alive_b = BufferCC(Core14.Alive, U"00000000")
        val rstSi = False
        val SkipOSC = Reg(Bool()) init(False)
        val area40kHz = new SlowArea(50 kHz, true) {
            val si = new Si5351("./data/si5351_14.318.bin")
            si.io.i_scl := io.scl
            si.io.i_sda := io.sda
            si.io.i_skip := !SkipOSC

            val pllRst = Reg(Bool()) init(False)
            var rstPllCounter = CounterFreeRun(25)
            PLL.io.reset := !pllRst
            when(!si.io.o_done){
                pllRst := False
                rstPllCounter.clear()
            }
            si.io.i_prog := False
            when(Alive_b === U"8'hff" && rstPllCounter.willOverflow){
                pllRst := True
            }elsewhen(Alive_b < U"8'hff" && rstPllCounter.willOverflow){
                si.io.i_prog := True
                SkipOSC := True
            }
        }

        //when(!kbd_sda_b || !area40kHz.si.io.o_sda_write){
        when(!area40kHz.si.io.o_sda_write){
            io.sda := False
        }
        //when(!kbd_scl_b || !area40kHz.si.io.o_scl_write){
        when(!area40kHz.si.io.o_scl_write){
            io.scl := False
        } 

        var reset = Reg(Bool()) init (False)
        var rstCounter = CounterFreeRun(2500000)
        when(!PLL.io.locked){
            rstCounter.clear()
            reset := False
        }elsewhen(rstCounter.willOverflow){
            reset := True
            SkipOSC := False
        }

        val pwm = new LedGlow(25)
        io.led_red := !(pwm.io.led || PLL.io.locked)

        val Q = BufferCC(Core11.areaRst.areaDiv4.comx35.io.Q, False)
        val soundBuff = BufferCC(Core11.areaRst.areaDiv4.comx35.io.Sound, S"000000")
        val dac = new Delta_Sigma_DAC_SOrder(16)
        val cic = new  CIC_Interpolation(8,4,8,1)
        val downs = new DownSampler(8,8)
        downs.io.i_data := soundBuff.resize(8) + (io.tape_led ? S"000001" | S"000000");
        cic.io.i_div := downs.io.div
        cic.io.i_data := downs.io.o_data
        dac.io.dac_in := cic.io.o_data.resize(16).asUInt
        io.sound_dac_l := dac.io.dac_out
        io.sound_dac_r := dac.io.dac_out
    }

    clk14Domain.reset := !Core25.area40kHz.si.io.o_done
    clk11Domain.reset := !Core25.reset
}

object Top_ECP5_CL8_Verilog extends App {
  Config_ECP5_CLV8.spinal.generateVerilog(new Top_ECP5())
}

object Top_ECP5_CLI5v6_Verilog extends App {
  Config_ECP5_CLI5v6.spinal.generateVerilog(new Top_ECP5())
}