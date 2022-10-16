package comx35

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._
import spinal.lib.fsm._
import spinal.core.sim._
import scala.util.control.Breaks

class Loader extends Component {
    val io = new Bundle{
        val spi_sck = out Bool()
        val spi_ssn = out Bool()
        val spi_mosi = out Bool()
        val spi_miso = in Bool()

        val ram_data_in = in Bits(8 bits)
        val ram_data_out = out Bits(8 bits)
        val address = in Bits(15 bits)
        val wea = in Bool()
        val ready = out Bool()
    } 

    val ready = False
    io.ready := ready

    val address = Reg(UInt(15 bits)) init(0)
    val address_next = address + 1
    val data_in = Reg(Bits(8 bits))   
    val wea = False

    val ram = new SB_SPRAM256KA()
    ram.POWEROFF := True //We are not turning off the power
    ram.SLEEP := False
    ram.STANDBY := False
    ram.CHIPSELECT := True

    val ram_address = ready ? io.address | address.asBits
    val ram_data_in = ready ? io.ram_data_in | data_in
    ram.MASKWREN := ram_address(0) ? B"1100" | B"0011"
    ram.ADDRESS := ram_address(14 downto 1).asUInt
    ram.DATAIN := ram_data_in ## ram_data_in
    val ram_data_out = ram_address(0) ? ram.DATAOUT(15 downto 8) | ram.DATAOUT(7 downto 0)
    ram.WREN := ready ? io.wea | wea
    io.ram_data_out := ram_data_out

    val SPI = new SPI_Flash()
    SPI.io.SPI.CS <> io.spi_ssn 
    SPI.io.SPI.SCLK <> io.spi_sck
    SPI.io.SPI.MOSI <> io.spi_mosi
    SPI.io.SPI.MISO <> io.spi_miso

    val wake = Reg(Bool) init(False)
    SPI.io.wake := wake
    SPI.io.output.ready := False

    SPI.io.address := 0x020000
    val startRead = Reg(Bool) init(False)
    SPI.io.startRead := startRead

    val fsm = new StateMachine {
        val Init: State = new StateDelay(100) with EntryPoint {
            whenCompleted {
                wake := True
                goto(StartRead)
            }
        }
        val StartRead: State = new State {
            whenIsActive {
                startRead := True
                goto(LoadRam)
            }
        }
        val LoadRam: State = new State {
            whenIsActive {
                when(SPI.io.output.valid){
                    data_in := SPI.io.output.payload
                    SPI.io.output.ready := True 
                    goto(IncrementAddress)
                }
            }
        }
        val IncrementAddress: State = new State {
            whenIsActive {
                address := address_next
                wea := True
                when(address_next < 0x4000)
                {
                    goto(LoadRam)
                }otherwise{
                    goto(Done)
                }
            }
        }
        val Done: State = new State {
            whenIsActive {
                startRead := False
                wake := False
                ready := True
            }
        }
    }
}

object Loader_sim {
    def main(args: Array[String]) {
        SimConfig.withWave.addIncludeDir("/home/winston/Projects/SpinalHDL/fcomx-35").compile{
            val dut = new Loader()
            dut
        }.doSim { dut =>
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)

            dut.clockDomain.waitRisingEdge()
            //val trace = new TraceEmma("verification\\out.log")

            var c = 0;
            val loop = new Breaks;
            loop.breakable {
                while (true) {
                    dut.clockDomain.waitRisingEdge()
                    c += 1
                    if(c > 999){
                        loop.break;
                    }
                }
            }
        }
    }
}