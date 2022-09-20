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
        content(address.toInt & 0xFFFF) = data & 0xff
    }

    def read(address: Long): Int = {
        content(address.toInt & 0xFFFF) & 0xff
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
       val Start = in Bool()
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

        vis69.io.Display_ := vis70.io.Display_
        vis69.io.AddSTB_ := vis70.io.AddSTB_
        vis69.io.HSync_ := vis70.io.HSync_
        
        vis70.io.DataIn := CPU.io.DataOut
        vis70.io.MRD := CPU.io.MRD
        vis70.io.TPB := CPU.io.TPB
        vis70.io.N3_ := vis69.io.N3_
        vis70.io.CMSEL := vis69.io.CMSEL

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

                    c += 1
                    if(c > 999999){
                        loop.break;
                    }
                }
            }
        }
    }
}
