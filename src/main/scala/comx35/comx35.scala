package comx35

import java.io.{BufferedReader, FileReader}
import spinal.core._
import spinal.core.sim._

import scala.util.Random
import scala.util.control._
import java.nio.file.{Files, Paths}

case class Memory(Size: Int) {
    val content = new Array[Int](Size+1);

    def write(address: Long, data: Int): Unit = {
        content(address.toInt & Size) = data & 0xff
    }

    def read(address: Long): Int = {
        content(address.toInt & Size) & 0xff
    }

    def loadBin(offset: Long, file: String): Unit = {
        val bin = Files.readAllBytes(Paths.get(file))
        for (byteId <- 0 until bin.size) {
            write(offset + byteId, bin(byteId))
        }
    }
}

class comx35_test extends Component {
    val io = new Bundle {
       val Addr16 = out Bits(16 bit)
       val DataOut = out Bits(8 bit)
       val DataIn = in Bits(8 bit)
       val MRD = out Bool()
       val MWR = out Bool()

       val PMA = out Bits(10 bits)
       val PMWR_ = out Bool() 
       val PMD_In = in Bits(8 bit)
       val PMD_Out = out Bits(8 bit)

       val CMA = out Bits(10 bits)
       val CMWR_ = out Bool() 
       val CMD_In = in Bits(8 bit)
       val CMD_Out = out Bits(8 bit)

       val Start = in Bool()
       val Test = in Bool()
    }

    //Components
    val CPU = new Spinal1802.Spinal1802()
    val vis69 = new VIS.CDP1869()
    val vis70 = new VIS.CDP1870()

    //Inputs
        CPU.io.DataIn := io.DataIn

    //Outputs
        io.DataOut := CPU.io.DataOut
        io.Addr16 := CPU.io.Addr16
        io.MRD := CPU.io.MRD
        io.MWR := CPU.io.MWR

    //Interconnects   
        vis69.io.Addr := CPU.io.Addr
        vis69.io.TPA := CPU.io.TPA
        vis69.io.TPB := CPU.io.TPB
        vis69.io.N := CPU.io.N
        vis69.io.MWR := CPU.io.MWR
        vis69.io.MRD := CPU.io.MRD
        
        vis69.io.Display_ := vis70.io.Display_
        vis69.io.AddSTB_ := vis70.io.AddSTB_
        vis69.io.HSync_ := vis70.io.HSync_
        
        io.PMA := vis69.io.PMA
        io.PMWR_ := vis69.io.PMWR_
        io.PMD_Out := CPU.io.DataOut

        io.CMA := io.PMD_In(6 downto 0) ## vis69.io.CMA(2 downto 0)
        io.CMWR_ := vis69.io.CMWR_
        io.CMD_Out := CPU.io.DataOut

        vis70.io.DataIn := CPU.io.DataOut
        vis70.io.MRD := CPU.io.MRD
        vis70.io.TPB := CPU.io.TPB
        vis70.io.N3_ := vis69.io.N3_
        vis70.io.CMSEL := vis69.io.CMSEL
        vis70.io.CDB_in := io.CMD_In(5 downto 0)
        vis70.io.CCB_in := io.CMD_In(7 downto 6)

    //Cons
        CPU.io.Wait_n := io.Start
        CPU.io.Clear_n := io.Start
        CPU.io.DMA_Out_n := True
        CPU.io.DMA_In_n := True
        CPU.io.Interrupt_n := True
        vis70.io.PalOrNTSC := False
        val rtp = True
        
    //Registers
        val NTSC_PAL_FlipFlop = RegNextWhen(False, CPU.io.Q, True) init(True)

    //Signals
        CPU.io.EF_n := True ## True ## (!NTSC_PAL_FlipFlop && rtp) ## (vis70.io.PreDisplay_)
}

object comx35_sim {
    def main(args: Array[String]) {
        SimConfig.withWave.compile{
            val dut = new comx35_test()
            dut
        }.doSim { dut =>
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)

            dut.io.Start #= false

            dut.clockDomain.waitRisingEdge()

            dut.io.Start #= true

            val ram = new Memory(0xBFFF)
            ram.loadBin(0x0000, "data/comx35.1.3.bin")
            
            val pram = new Memory(0x3FF)
            val cram = new Memory(0x3FF)
            //val trace = new TraceEmma("verification\\out.log")

            var c = 0;
            val loop = new Breaks;
            loop.breakable {
                while (true) {
                    dut.clockDomain.waitRisingEdge()

                    if (dut.io.MRD.toBoolean == false && dut.io.Addr16.toInt < 0xC000) {
                        dut.io.DataIn #= ram.read(dut.io.Addr16.toInt)
                    } else {
                        dut.io.DataIn #= 0x00
                    }

                    if (dut.io.MWR.toBoolean == false && dut.io.Addr16.toInt > 0x3fff && dut.io.Addr16.toInt < 0xC000) {
                        ram.write(dut.io.Addr16.toInt, dut.io.DataOut.toInt.toByte)
                    }

                    
                    if(dut.io.PMWR_.toBoolean == false){
                        pram.write(dut.io.PMA.toInt, dut.io.PMD_Out.toInt.toByte)
                    }
                    
                    if(dut.io.CMWR_.toBoolean == false){
                        cram.write(dut.io.CMA.toInt, dut.io.CMD_Out.toInt.toByte)
                    }
                    
                    dut.io.PMD_In #= pram.read(dut.io.PMA.toInt)
                    dut.io.CMD_In #= cram.read(dut.io.CMA.toInt)

                    c += 1
                    if(c > 999999){
                        loop.break;
                    }
                }
            }
        }
    }
}

//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object ComxSpinalConfig extends SpinalConfig(
    targetDirectory = ".",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object ComxGen {
    def main(args: Array[String]) {
        ComxSpinalConfig.generateVerilog(new comx35_test).printPruned
    }
}