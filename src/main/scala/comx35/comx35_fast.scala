package comx35

import spinal.core._
import Cards._

// CPU-only variant of the COMX-35 for "fast" (emulated-video) simulation.
//
// The VIS (CDP1869 timing/counters + CDP1870 pixel shifter) is removed and
// replaced by a software model in sim_fast.cpp. That model:
//   - decodes the VIS register writes (N=4..7 OUT) and pram/cram writes off the bus,
//   - renders pram/cram directly to an RGB framebuffer,
//   - drives PreDisplay_ (CPU EF1 + interrupt) to keep frame sync.
//
// The CPU, FDC and keyboard remain in hardware. The CPU is clocked directly by
// the top-level clk (no ClockEnableArea on the video dot clock).
class comx35_fast() extends Component {
  val io = new Bundle {
    // CPU bus (software observes)
    val Addr16 = out Bits (16 bit)
    val DataOut = out Bits (8 bit)
    val MRD = out Bool ()
    val MWR = out Bool ()
    val N = out Bits (3 bit)
    val TPA = out Bool ()
    val TPB = out Bool ()
    val Q = out Bool ()

    // CPU inputs (software drives)
    val DataIn = in Bits (8 bit)
    val Start = in Bool () // -> Clear_n
    val Wait = in Bool () // -> Wait_n
    val Tape_in = in Bool ()

    // Keyboard (CDP1871)
    val KBD_Latch = in Bool ()
    val KBD_Repeat = in Bool ()
    val KBD_KeyCode = in Bits (8 bits)
    val KBD_Ready = out Bool ()
    val KBD_SEL = out Bool ()
    val KBD_DataOut = out Bits (8 bits)

    // FDC
    val ExtRom = out Bool ()
    val Card_DataOut = out Bits (8 bits)
    val FDCRom = new Bundle {
      val DataIn = in Bits (8 bit)
      val Addr = out Bits (13 bit)
    }

    // FDC disk byte interface (served by software)
    val Disk = new Bundle {
      val ReadReq = out Bool ()
      val DataIn = in Bits (8 bits)
      val DataInValid = in Bool ()
      val WriteReq = out Bool ()
      val DataOut = out Bits (8 bits)
      val Track = out Bits (8 bits)
      val Sector = out Bits (8 bits)
      val Side = out Bool ()
      val Byte = out UInt (7 bits)
    }

    // Software-driven timing (replaces the VIS)
    val PreDisplay_ = in Bool ()
  }

  // Components
  val kbd71 = new VIS.CDP1871()
  val fdc = new FDC_Card()
  val CPU = new Spinal1802.Spinal1802()

  // Registers (kept from comx35_test)
  val NTSC_PAL_FlipFlop = RegNextWhen(False, CPU.io.Q, True) init (True)
  val INT_FF = Reg(Bool()) init (True)

  // N3_ was produced by vis69 (N =/= 3); recompute it locally for the keyboard.
  val N3_ = CPU.io.N =/= 3

  // Keyboard
  kbd71.io.TPB := CPU.io.TPB
  kbd71.io.MRD_ := CPU.io.MRD
  kbd71.io.N3_ := N3_
  kbd71.io.KeyCode := io.KBD_KeyCode
  kbd71.io.Latch := io.KBD_Latch
  kbd71.io.Repeat := io.KBD_Repeat

  // FDC
  fdc.io.Addr16 := CPU.io.Addr16
  fdc.io.DataIn := CPU.io.DataOut
  io.Card_DataOut := fdc.io.DataOut
  fdc.io.MRD := CPU.io.MRD
  fdc.io.MWR := CPU.io.MWR
  fdc.io.TPB := CPU.io.TPB
  fdc.io.N := CPU.io.N
  fdc.io.Q := CPU.io.Q
  io.ExtRom := fdc.io.ExtRom
  io.FDCRom.Addr := fdc.io.FDCRom.Addr
  fdc.io.FDCRom.DataIn := io.FDCRom.DataIn
  io.Disk.ReadReq := fdc.io.Disk.ReadReq
  fdc.io.Disk.DataIn := io.Disk.DataIn
  fdc.io.Disk.DataInValid := io.Disk.DataInValid
  io.Disk.WriteReq := fdc.io.Disk.WriteReq
  io.Disk.DataOut := fdc.io.Disk.DataOut
  io.Disk.Track := fdc.io.Disk.Track
  io.Disk.Sector := fdc.io.Disk.Sector
  io.Disk.Side := fdc.io.Disk.Side
  io.Disk.Byte := fdc.io.Disk.Byte

  // CPU control
  CPU.io.Wait_n := io.Wait
  CPU.io.Clear_n := io.Start
  CPU.io.EF_n := (fdc.io.EF4_) ## kbd71.io.DA_ ## (!NTSC_PAL_FlipFlop && kbd71.io.RPT_) ## io.PreDisplay_
  CPU.io.Interrupt_n := INT_FF
  CPU.io.DMA_Out_n := True
  CPU.io.DMA_In_n := True
  CPU.io.DataIn := io.DataIn

  // Interrupt latch (kept from comx35_test; PreDisplay_ now comes from software)
  when(io.PreDisplay_.rise()) {
    INT_FF := NTSC_PAL_FlipFlop
  } elsewhen ((CPU.io.SC === 3 && CPU.io.TPA) && (!NTSC_PAL_FlipFlop)) {
    INT_FF := True
  }

  // Outputs
  io.DataOut := CPU.io.DataOut
  io.Addr16 := CPU.io.Addr16
  io.MRD := CPU.io.MRD
  io.MWR := CPU.io.MWR
  io.N := CPU.io.N
  io.TPA := CPU.io.TPA
  io.TPB := CPU.io.TPB
  io.Q := CPU.io.Q
  io.KBD_Ready := kbd71.io.Ready
  io.KBD_SEL := kbd71.io.KBD_SEL
  io.KBD_DataOut := kbd71.io.DataOut
}

object ComxFastGen {
  def main(args: Array[String]) {
    ComxSpinalConfig.generateVerilog(new comx35_fast())
  }
}
