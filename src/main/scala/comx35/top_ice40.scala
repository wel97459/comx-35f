package comx35
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._
import VIS._

class top_ice40 extends Component {
    val io = new Bundle{
        val reset_ = in Bool()
        val clk_12Mhz = in Bool() //12Mhz CLK
        val video = out Bool()
        val sync = out Bool()

        val spi_sck = out Bool()
        val spi_ssn = out Bool()
        val spi_mosi = out Bool()
        val spi_miso = in Bool()

        val lcd_sck = in Bool()
        val lcd_sdo = out Bool()

        val scl = inout(Analog(Bool()))
        val sda = inout(Analog(Bool()))

        val pwm_sound = out Bool()
        val led_red = out Bool()
    }
    noIoPrefix()

    val clk22Domain = ClockDomain.internal(name = "Core22",  frequency = FixedFrequency(44.25 MHz))

    //PLL Settings for 22.500MHz
    val PLL_CONFIG = SB_PLL40_PAD_CONFIG(
        DIVR = B"0000", DIVF = B"0111010", DIVQ = B"100", FILTER_RANGE = B"001",
        FEEDBACK_PATH = "SIMPLE", PLLOUT_SELECT = "GENCLK", 
        DELAY_ADJUSTMENT_MODE_FEEDBACK = "FIXED", DELAY_ADJUSTMENT_MODE_RELATIVE = "FIXED", //NO DELAY
        FDA_FEEDBACK = B"0000", FDA_RELATIVE = B"0000", SHIFTREG_DIV_MODE = B"0", ENABLE_ICEGATE = False //NOT USED
    ) 

    //Define PLL
    val PLL = new SB_PLL40_CORE(PLL_CONFIG)
    //Setup signals of PLL
    PLL.BYPASS := False
    PLL.RESETB := True
    PLL.REFERENCECLK := io.clk_12Mhz

    //Connect the PLL output of 22.500Mhz to the 22.500MHz clock domain
    clk22Domain.clock := PLL.PLLOUTGLOBAL
    clk22Domain.reset := !io.reset_
    val Core22 = new ClockingArea(clk22Domain) {
        var reset = Reg(Bool) init (False)
        var rstCounter = CounterFreeRun(100)

        when(rstCounter.willOverflow){
            reset := True
        }

        val areaRst = new ResetArea(!reset, false) {
            val kbd_ready = Reg(Bool()) init(False)
            val area40kHz = new SlowArea(50 kHz) {
                val ready = RegNext(kbd_ready) init(False)
                val kbd = new Q10Keyboard()

                kbd.io.i_scl := io.scl
                when(!kbd.io.o_scl_write){
                    io.scl := False
                }

                kbd.io.i_sda := io.sda
                when(!kbd.io.o_sda_write){
                    io.sda := False
                }

                kbd.io.key_code_stream.ready := ready.rise()
            }

            var loader = new Loader()
            loader.io.spi_miso := io.spi_miso
            io.spi_mosi := loader.io.spi_mosi
            io.spi_sck := loader.io.spi_sck
            io.spi_ssn := loader.io.spi_ssn
              
            loader.io.ram_data_in := 0x00
            loader.io.wea := False

            val ram = new SB_SPRAM256KA()
            ram.POWEROFF := True //We are not turning off the power
            ram.SLEEP := False
            ram.STANDBY := False
            ram.CHIPSELECT := True

            val a2c = new ascii2comx()
            a2c.ascii := area40kHz.kbd.io.key_code_stream.payload
            
            val areaRst = new ResetArea(!loader.io.ready, false) {
                val areaDiv4 = new SlowArea(8) {
                    val Tape_in = BufferCC(io.lcd_sck)
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
                    io.lcd_sdo := Tape_FF ^ comx35.io.Q

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

                    val ram_address = comx35.io.Addr16(14 downto 0)
                    val ram_data_in = B"8'h00"
                    val wea = False
                    
                    ram.DATAIN := ram_data_in ## ram_data_in
                    ram.WREN := wea
                    val ram_data_out = ram_address(0) ? ram.DATAOUT(15 downto 8) | ram.DATAOUT(7 downto 0)
                    ram.MASKWREN := ram_address(0) ? B"1100" | B"0011"
                    ram.ADDRESS := ram_address(14 downto 1).asUInt

                    io.video := comx35.io.Pixel
                    io.sync := comx35.io.Sync

                    pram.io.addra := comx35.io.PMA
                    comx35.io.PMD_In := pram.io.douta
                    pram.io.dina := comx35.io.PMD_Out
                    pram.io.wea := comx35.io.PMWR_.rise()

                    cram.io.addra := comx35.io.CMA3_PMA10 ## comx35.io.CMA
                    comx35.io.CMD_In := cram.io.douta
                    cram.io.dina := comx35.io.CMD_Out
                    cram.io.wea := comx35.io.CMWR_.rise()

                    loader.io.address := comx35.io.Addr16(14 downto 0)
                    when(!comx35.io.MRD)
                    {
                        when(comx35.io.Addr16.asUInt < 0x4000)
                        {
                            comx35.io.DataIn := loader.io.ram_data_out
                        }elsewhen(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                            comx35.io.DataIn := ram_data_out
                        }
                    }

                    when(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                        wea := comx35.io.MWR.rise()
                        ram_data_in := comx35.io.DataOut
                    }
                    val pwm = new PWM_Sound()
                    pwm.io.Sound := (comx35.io.Sound | ((comx35.io.Q ^ Tape_in) ? B"1111" | B"0000")) ## B"0"
                    io.pwm_sound := pwm.io.PWM
                    io.led_red := !pwm.io.PWM
                }
            }
        }
    }
}

object Top_ICE40_Verilog {
    def main(args: Array[String]) {
        SpinalVerilog(new top_ice40()).printPruned()
    }
}