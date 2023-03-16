package comx35
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._
import VIS._
import spinal.lib.memory.sdram.xdr.Core

case class ecp5_pll() extends BlackBox {
    val io = new Bundle {
        val clkin = in Bool()
        val clkout0 = out Bool()
        val clkout1 = out Bool()
        val locked = out Bool()
    }
noIoPrefix()
}

class Top_ECP5 extends Component {
    val io = new Bundle{
        val reset_ = in Bool()
        val clk_25Mhz = in Bool() //12Mhz CLK
        val clk_14Mhz = in Bool()
        val phyrst_ = out Bool()

        val video = out Bits(2 bits)
        val sync = out Bool()
        val burst = out Bits(3 bits)

        val scl = inout(Analog(Bool()))
        val sda = inout(Analog(Bool()))

        //val pwm_sound = out Bool()
        val led_red = out Bool()
    }
    noIoPrefix()
    io.phyrst_ := True

    //Define clock domains
    val clk14Domain = ClockDomain.internal(name = "Core14",  frequency = FixedFrequency(14.2857 MHz))
    val clk11Domain = ClockDomain.internal(name = "Core11",  frequency = FixedFrequency(11.4286 MHz))
    val clk25Domain = ClockDomain.internal(name = "Core25",  frequency = FixedFrequency(25.0000 MHz))

    //Allow clock domain crossing.
    clk14Domain.setSyncronousWith(clk11Domain)

    //Define PLL
    val PLL = new ecp5_pll()
    //Setup signals of PLL
    PLL.io.clkin := io.clk_25Mhz

    clk25Domain.clock := io.clk_25Mhz
    clk25Domain.reset := !io.reset_

    val Core25 = new ClockingArea(clk25Domain) {
        var reset = Reg(Bool) init (False)
        var rstCounter = CounterFreeRun(8750000)
        when(!PLL.io.locked){
            rstCounter.clear()
        }elsewhen(rstCounter.willOverflow){
            reset := True
        }
    }

    //Connect the PLL output of 17.625Mhz to the 17.625MHz clock domain
    clk14Domain.clock := io.clk_14Mhz
    clk14Domain.reset := !Core25.reset

    //Connect the internal oscillator output to the 48MHz clock domain
    clk11Domain.clock := PLL.io.clkout1
    clk11Domain.reset := !Core25.reset

    val Core11 = new ClockingArea(clk11Domain) {
        val kbd_ready = Reg(Bool()) init(False)
        val area40kHz = new SlowArea(50 kHz, true) {
            val ready = RegNext(kbd_ready) init(False)
            val kbd = new Q10Keyboard()
            val si = new Si5351("./data/si5351.bin")

            kbd.io.i_hold := !si.io.o_done

            kbd.io.i_scl := io.scl
            si.io.i_scl := io.scl
            when(!kbd.io.o_scl_write || !si.io.o_scl_write){
                io.scl := False
            }

            kbd.io.i_sda := io.sda
            si.io.i_sda := io.sda
            when(!kbd.io.o_sda_write || !si.io.o_sda_write){
                io.sda := False
            }

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

            io.video := comx35.io.Pixel ? B"11" | B"00"
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
        val HSync_ = BufferCC(Core11.areaDiv4.comx35.io.HSync_, False)

        val b = Reg(UInt(2 bits))
        when(HSync_){
            b := b + 1;
        }otherwise{
            b := 0;
        }

        val burst = Reg(Bits(3 bits))


        when(Burst || (Color =/= 0 && Color =/= 7)){
            when(b === 0){
                burst := B"110"
            }elsewhen(b === 1){
                burst := B"011"
            }elsewhen(b === 2){
                burst := B"000"
            }elsewhen(b === 3){
                burst := B"011"
            }
        }
        io.burst := burst

        val pwm = new LedGlow(25)
        io.led_red := !pwm.io.led
    }
}

object Top_ECP5_Verilog extends App {
  Config_ECP5.spinal.generateVerilog(new Top_ECP5())
}