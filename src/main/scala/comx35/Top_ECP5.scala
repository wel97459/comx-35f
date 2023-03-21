package comx35
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._
import VIS._
import spinal.lib.memory.sdram.xdr.Core

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
        val J9_P1 = in Bool()
        val phyrst_ = out Bool()
        val video = out Bits(2 bits)
        val sync = out Bool()
        val burst = out Bits(3 bits)

        val scl = inout(Analog(Bool()))
        val sda = inout(Analog(Bool()))

        val Dial = in Bits(4 bits)
        //val pwm_sound = out Bool()
        val led_red = out Bool()
    }
    noIoPrefix()
    io.phyrst_ := True

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
    PLL.io.clkin := io.J9_P1
    
    clk25Domain.clock := io.clk_25Mhz
    clk25Domain.reset := !io.reset_

    clk14Domain.clock := io.J9_P1
    clk11Domain.clock := PLL.io.clkout0

    val Core11 = new ClockingArea(clk11Domain) {
        val Dial = BufferCC(io.Dial, B"0000")
        val kbd_ready = Reg(Bool()) init(False)
        val area40kHz = new SlowArea(50 kHz, true) {
            val ready = RegNext(kbd_ready) init(False)
            val kbd = new Q10Keyboard()

            kbd.io.i_hold := False

            kbd.io.i_scl := io.scl
            kbd.io.i_sda := io.sda
            kbd.io.key_code_stream.ready := ready.rise()
        }

        val a2c = new ascii2comx()
        a2c.ascii := area40kHz.kbd.io.key_code_stream.payload
        
        val areaDiv4 = new SlowArea(2) {
            val Tape_in = False
            val Tape_Filter = Reg(Bits(8 bits)) init(0)
            val Tape_FF = Reg(Bool()) init(False)
            Tape_Filter := Tape_in ## Tape_Filter(7 downto 1)
            when(Tape_Filter === B"8'hFF")
            {
                Tape_FF := True
            }elsewhen(Tape_Filter === B"8'h00"){
                Tape_FF := False
            }
            val comx35 = new comx35_test()
            comx35.io.Start := True
            comx35.io.DataIn := 0x00
            comx35.io.Tape_in := Tape_FF
            //io.lcd_sdo := Tape_FF ^ comx35.io.Q

            comx35.io.KBD_Latch := False
            comx35.io.KBD_KeyCode := a2c.comx

            val keyHit = Reg(Bool()) init(False)
            when(comx35.io.KBD_Ready && comx35.io.Display_.rise()){
                when(area40kHz.kbd.io.key_code_stream.valid){
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
            val ram = new Ram(log2Up(0x4FFF))

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

            ram.io.addra := comx35.io.Addr16(14 downto 0)
            ram.io.wea := False
            ram.io.dina := comx35.io.DataOut
            ram.io.ena := True

            when(!comx35.io.MRD)
            {
                when(comx35.io.Addr16.asUInt < 0x4000)
                {
                    comx35.io.DataIn := rom.io.douta
                }elsewhen(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                    comx35.io.DataIn := ram.io.douta
                }elsewhen(comx35.io.Addr16.asUInt === 0xC000){
                    comx35.io.DataIn := B"0000" ## ~Dial
                }
            }

            when(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                ram.io.wea := comx35.io.MWR.rise()
            }

            val pwm = new PWM_Sound()
            pwm.io.Sound := (comx35.io.Sound | ((comx35.io.Q ^ Tape_in) ? B"1111" | B"0000")) ## B"0"
            //io.pwm_sound := pwm.io.PWM
            //io.led_red := !pwm.io.PWM
        }
    }
    val Core14 = new ClockingArea(clk14Domain) {
        val Color = BufferCC(Core11.areaDiv4.comx35.io.Color, B"000")
        val Burst = BufferCC(Core11.areaDiv4.comx35.io.Burst, False)

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
            io.video := 3 
        }elsewhen(Color === 4 || Color === 2 || Color === 6){
            io.video := 2 
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
        val kbd_sda_b = BufferCC(Core11.area40kHz.kbd.io.o_sda_write, True)
        val kbd_scl_b = BufferCC(Core11.area40kHz.kbd.io.o_scl_write, True)
        val Alive_b = BufferCC(Core14.Alive, U"00000000")
        val rstSi = False
        val SkipOSC = Reg(Bool()) init(False)
        val area40kHz = new SlowArea(50 kHz, true) {
            val si = new Si5351("./data/si5351.bin")
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

        when(!kbd_sda_b || !area40kHz.si.io.o_sda_write){
            io.sda := False
        }
        when(!kbd_scl_b || !area40kHz.si.io.o_scl_write){
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
    }
    clk14Domain.reset := !Core25.area40kHz.si.io.o_done
    clk11Domain.reset := !Core25.reset
}

object Top_ECP5_Verilog extends App {
  Config_ECP5.spinal.generateVerilog(new Top_ECP5())
}