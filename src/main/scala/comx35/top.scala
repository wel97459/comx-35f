package comx35
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._

class top extends Component {
    val io = new Bundle{
        val reset_ = in Bool()
        val clk_12Mhz = in Bool() //12Mhz CLK
        val video = out Bool()
        val sync = out Bool()

        val spi_sck = out Bool()
        val spi_ssn = out Bool()
        val spi_mosi = out Bool()
        val spi_miso = in Bool()

        val led_red = out Bool()
    }
    noIoPrefix()

    val clk22Domain = ClockDomain.internal(name = "Core22",  frequency = FixedFrequency(22.500 MHz))

    //PLL Settings for 22.500MHz
    val PLL_CONFIG = SB_PLL40_PAD_CONFIG(
        DIVR = B"0000", DIVF = B"0111011", DIVQ = B"101", FILTER_RANGE = B"001",
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

            val ram_address = B"15'h0000"
            val ram_data_in = B"8'h00"
            val wea = False
            ram.MASKWREN := ram_address(0) ? B"1100" | B"0011"
            
            ram.DATAIN := ram_data_in ## ram_data_in
            val ram_data_out = ram_address(0) ? ram.DATAOUT(15 downto 8) | ram.DATAOUT(7 downto 0)
            ram.WREN := wea
            val areaRst = new ResetArea(!loader.io.ready, false) {
                val areaDiv4 = new SlowArea(4) {
                    val pram = new Ram(log2Up(0x3FF))
                    val cram = new Ram(log2Up(0x3FF))
                    pram.io.ena := True
                    cram.io.ena := True

                    val comx35 = new comx35_test()
                    comx35.io.Start := True
                    comx35.io.DataIn := 0x00
                    comx35.io.KBD_Latch := False
                    comx35.io.KBD_KeyCode := 0x80
                    io.video := comx35.io.Pixel
                    io.sync := comx35.io.Sync
                    ram.ADDRESS := ram_address(14 downto 1).asUInt

                    pram.io.addra := comx35.io.PMA
                    comx35.io.PMD_In := pram.io.douta
                    pram.io.dina := comx35.io.PMD_Out
                    pram.io.wea := (!comx35.io.PMWR_)

                    cram.io.addra := comx35.io.CMA
                    comx35.io.CMD_In := cram.io.douta
                    cram.io.dina := comx35.io.CMD_Out
                    cram.io.wea := (!comx35.io.CMWR_)

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
                    when(!comx35.io.MWR)
                    {
                        when(comx35.io.Addr16.asUInt >= 0x4000 && comx35.io.Addr16.asUInt < 0xC000){
                            wea := True
                            ram_data_in := comx35.io.DataOut
                        }
                    }
                    io.led_red := !comx35.io.led
                }

            }

        }
    }
}

object TopVerilog {
    def main(args: Array[String]) {
        SpinalVerilog(new top()).printPruned()
    }
}