package Cards

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import java.io._
import scala.util.control.Breaks

class FDC_Card extends Component {
  val io = new Bundle {
    val Addr16 = in Bits (16 bit)
    val DataIn = in Bits (8 bit)
    val DataOut = out Bits (8 bit)
    val MRD = in Bool ()
    val MWR = in Bool ()
    val TPB = in Bool ()
    val N = in Bits (3 bit)
    val Q = in Bool ()
    val EF4_ = out Bool ()
    val ExtRom = out Bool ()
    val FDCRom = new Bundle {
      val DataIn = in Bits (8 bit)
      val Addr = out Bits (13 bit)
    }
    val Disk = new Bundle {
      // Disk byte interface — the host (simulation) supplies one byte on a read
      // request, and accepts one byte on a write request.
      val SeekReq = out Bool () // FDC needs to seek to a track (restore/seek)
      val ReadReq = out Bool () // FDC needs a byte (read sector/track)
      val Valid = in Bool () // host has supplied a byte
      val DataIn = in Bits (8 bit) // byte supplied by the host
      val WriteReq = out Bool () // FDC has a byte to store
      val DataOut = out Bits (8 bit) // byte to store
      // Disk addressing (valid while DiskReadReq/DiskWriteReq is asserted) so the
      // host can map the byte to its offset in the raw sector image.
      val Track = out Bits (8 bits) // current track
      val Sector = out Bits (8 bits) // current sector
      val Side = out Bool () // current head/side (select-register bit 5)
      val Byte = out UInt (7 bits) // byte index within the sector
    }
  }

  // ---- register select latch (Q=1 write) ----
  val F3_Latch = Reg(Bits(8 bits)) init (0)
  val F5_addr = F3_Latch(1 downto 0)

  // ---- WD1770 registers ----
  val FDC_Command = Reg(UInt(8 bits)) init (0)
  val FDC_Track = Reg(Bits(8 bits)) init (0)
  val FDC_Sector = Reg(Bits(8 bits)) init (0)
  val FDC_Data = Reg(Bits(8 bits)) init (0)

  // Side/head select: latched from select-register bit 5 when bit 4 (0x10) is set.
  val FDC_Side = Reg(Bool()) init (False)

  // ---- status register (WD1770 bit order) ----
  // bit 0 Busy, 1 DRQ, 2 LostData/TRACK0(type-I), 3 CRC, 4 RNF, 5 WriteFault, 6 WriteProtect, 7 NotReady
  // Reset value 0x04: head is at track 0 (matches Emma 02 resetFdc: status_=4).
  val FDC_Status = Reg(Bits(8 bits)) init (0x04)
  val FDC_INTRQ = Reg(Bool()) init (False)

  // ---- byte counter for sector transfers ----
  val ByteCount = Reg(UInt(7 bits)) init (0)

  // ---- command timing ----
  // Real WD1770 takes ~90 clock cycles after a Read/Write Sector command
  // before the first DRQ (head settle / sector find). DOS polls status and
  // expects BUSY (bit 0) during this window.
  val DrqDelay = Reg(UInt(8 bits)) init (0)
  // Multiple-sector mode (command 0x9x/0xBx): keep reading until the last
  // sector of the track instead of stopping after one.
  val MultiSector = Reg(Bool()) init (False)

  // ---- bus decode (matches the existing Q-gated port-2 interface) ----
  // NOTE: on the 1802, OUT pulses MWR low; INP leaves MRD HIGH (a memory read
  // of the addressed port). So a register READ is MRD high with N==2.
  val regWrite = !io.MRD && io.TPB && io.N === 2 // OUT 2 (write)
  // INP 2: MRD stays HIGH for the whole input instruction and MWR pulses low
  // for TWO cycles (SC5-SC6) while the CPU stores the byte into M(RX). The
  // register mux must therefore be active for both cycles - NOT gated on TPB
  // (which only pulses at SC6), otherwise the SC5 store captures the card's
  // default 0x00 instead of the register value.
  val regRead = io.MRD && !io.MWR && io.N === 2
  val selWrite = regWrite && io.Q // Q=1: select register
  val cmdWrite = regWrite && !io.Q && F5_addr === 0
  val trkWrite = regWrite && !io.Q && F5_addr === 1
  val secWrite = regWrite && !io.Q && F5_addr === 2
  val datWrite = regWrite && !io.Q && F5_addr === 3
  val datRead = regRead && !io.Q && F5_addr === 3
  val statRead = regRead && !io.Q && F5_addr === 0

  // edge detection (single-cycle pulses for the FSM)
  // cmdWriteRise fires the cycle the command byte is on the bus; the FSM must
  // wait one more cycle for FDC_Command to latch, so it uses cmdLatched.

  val cmdWriteRise = cmdWrite.rise()
  val cmdLatched = RegNext(cmdWriteRise) init (False)
  val datReadLatched = RegNext(datRead) init (False)
  val datWriteRise = datWrite
  val datReadRise = datReadLatched && io.MWR.rise()

  // ---- register interface ----
  io.DataOut := 0
  io.ExtRom := False
  io.FDCRom.Addr := 0
  // EF4 = DRQ. Emma 02's ef1770() returns drq_ directly (no inversion in the
  // COMX config), i.e. DRQ asserted -> EF4 reads HIGH (1). DOS polls this
  // with B4/BN4 while waiting for each sector byte.
  io.EF4_ := FDC_Status(1) // EF4 = DRQ (active-high)

  when(selWrite) { F3_Latch := io.DataIn }
  when(selWrite && io.DataIn(4)) {
    FDC_Side := io.DataIn(5)
  } // drive/side update
  when(cmdWrite) { FDC_Command := io.DataIn.asUInt }
  when(trkWrite) { FDC_Track := io.DataIn }
  when(secWrite) { FDC_Sector := io.DataIn }
  when(datWrite) { FDC_Data := io.DataIn }

  when(regRead) {
    when(io.Q) {
      io.DataOut := B"7'h00" ## FDC_INTRQ
    } otherwise {
      switch(F5_addr) {
        is(0) { io.DataOut := FDC_Status }
        is(1) { io.DataOut := FDC_Track }
        is(2) { io.DataOut := FDC_Sector }
        is(3) { io.DataOut := FDC_Data }
      }
    }
  }

  // ExtRom / FDCRom decode — mirrors Emma 02's COMX FDC config:
  //   <copy start="0xdd0" end="0xddf" slot="0">0xc000</copy>
  //   slot 0 ROM at 0xC000-0xDFFF (fdc.bin)
  // Emma 02 maps the copy as readMemDebug(address + 0xc000); the slot window
  // is based at 0xC000, so the fdc.bin offset equals the host address itself
  // (0x0DD0 -> fdc.bin[0xDD0], whose content is CALL 0xC002).
  // NOTE: the <copy start="0x1000"> directive in the XML belongs to the
  // expansion/EPROM board config (it targets the 0xE000 expansion.bin ROM),
  // NOT the FDC card - do not decode it here.
  // NOTE: this decode must NOT fire during a port-2 INP (regRead): the CPU
  // address bus holds a port address then, which can alias into a ROM window
  // and clobber io_DataOut. regRead is handled above and takes priority.
  when(regRead) {
    // keep whatever io.DataOut the register mux drove
  } elsewhen (!io.MRD && io.Addr16.asUInt >= 0x0dd0 && io.Addr16.asUInt <= 0x0ddf) {
    io.ExtRom := True
    io.DataOut := io.FDCRom.DataIn
    io.FDCRom.Addr := io.Addr16(
      12 downto 0
    ) // 0x0DD0 +0xC000 = 0xCDD0 → fdc.bin[0xDD0]
  } elsewhen (!io.MRD && io.Addr16.asUInt >= 0xc000 && io.Addr16.asUInt <= 0xdfff) {
    io.ExtRom := True
    io.DataOut := io.FDCRom.DataIn
    io.FDCRom.Addr := (io.Addr16.asUInt - 0xc000)
      .asBits(12 downto 0) // full 8K window
  }

  // INTRQ clears on status read and on a new command
  when(statRead) { FDC_INTRQ := False }
  when(cmdWrite) { FDC_INTRQ := False }

  // ---- disk interface defaults ----
  io.Disk.ReadReq := False
  io.Disk.WriteReq := False
  io.Disk.DataOut := FDC_Data
  io.Disk.Track := FDC_Track
  io.Disk.Sector := FDC_Sector
  io.Disk.Side := FDC_Side
  io.Disk.Byte := ByteCount
  // restore/seek commands request a seek
  io.Disk.SeekReq := False
  // ---- FDC command state machine ----
  val fsm = new StateMachine {
    val Wait4CMD: State = new State with EntryPoint {
      whenIsActive {
        FDC_Status(0) := False // Busy
        FDC_Status(1) := False // DRQ
        // NOTE: bit 2 is NOT refreshed here. On a real WD1770 the
        // completion status persists until the next command; Wait4CMD
        // rewriting the TRACK0/LostData bit made every post-command
        // status read report type-II "Lost Data" (error 10 in DOS).
        when(cmdLatched) {
          switch(FDC_Command(7 downto 4)) {
            is(0x0) { goto(Restore_Busy) }
            is(0x1) { goto(Seek_Busy) }
            is(0x8, 0x9) {
              ByteCount := 0
              MultiSector := FDC_Command(4)
              DrqDelay := 90
              goto(ReadSector_Find)
            }
            is(0xa, 0xb) {
              ByteCount := 0
              MultiSector := FDC_Command(4)
              DrqDelay := 90
              goto(WriteSector_DRQ)
            }
            is(0xd) { goto(ForceInt) }
            default { goto(Wait4CMD) }
          }
        }
      }
    }

    // ---- Restore: seek to track 0.  Our FDC does not model head stepping, so
    // the drive is always physically at its current track; a restore therefore
    // completes immediately (matching Emma 02's "if already at track 0 -> endCommand"
    // fast path).  DOS's driver issues the restore and reads status ~1 ms later,
    // expecting TRACK0 to already be reported - a 30 ms step delay here makes DOS
    // retry and abort with error 104.
    val Restore_Busy: State = new State {
      whenIsActive {
        FDC_Track := 0
        FDC_INTRQ := True
        FDC_Status := 0x04 // endCommand(4): TRACK0 flag only
        io.Disk.SeekReq := True
        goto(Wait4CMD)
      }
    }

    // ---- Seek: move to track in data register (instant - no stepping modeled) ----
    val Seek_Busy: State = new State {
      whenIsActive {
        FDC_Track := FDC_Data
        FDC_INTRQ := True
        FDC_Status := (FDC_Data === 0) ? B"8'x04" | B"8'x00"
        io.Disk.SeekReq := True
        goto(Wait4CMD)
      }
    }

    // ---- Force Interrupt: clear INTRQ, drop busy ----
    val ForceInt: State = new State {
      whenIsActive {
        FDC_INTRQ := False
        FDC_Status(0) := False
        goto(Wait4CMD)
      }
    }

    // ---- Read Sector (0x8/0x9): 128-byte DRQ handshake ----

    // Find phase: BUSY for ~90 cycles before the first byte is available
    // (models head settle / sector find on a real drive).
    val ReadSector_Find: State = new State {
      whenIsActive {
        FDC_Status := 0x01 // Busy, all else clear
        when(DrqDelay =/= 0) {
          DrqDelay := DrqDelay - 1
        } otherwise {
          goto(ReadSector_Req)
        }
      }
    }

    val ReadSector_Req: State = new State {
      whenIsActive {
        FDC_Status(0) := True // Busy
        io.Disk.ReadReq := True // request a byte from the host
        when(io.Disk.Valid) {
          FDC_Data := io.Disk.DataIn
          goto(ReadSector_DRQ)
        }
      }
    }

    val ReadSector_DRQ: State = new State {
      whenIsActive {
        FDC_Status(0) := True // Busy
        FDC_Status(1) := True // DRQ
        when(datReadRise) {
          FDC_Status(1) := False // CPU consumed the byte
          ByteCount := ByteCount + 1
          when(ByteCount === 127) {
            goto(ReadSector_Done)
          } otherwise {
            goto(ReadSector_Req)
          }
        }
      }
    }

    val ReadSector_Done: State = new State {
      whenIsActive {
        FDC_INTRQ := True
        // Type-II completion status: NO error flags on success.
        // (In type-II status bit 2 = LostData, NOT Track0 - returning
        // 0x04 here made DOS report error 10.) Matches Emma 02's
        // endCommand(status_ & 0xfe) with no errors accumulated.
        FDC_Status := 0x00
        // Multiple-sector mode: advance to the next sector (1..15 wrap
        // to 0 per Emma 02: sector_++ while sector_ < numberOfSectors-1)
        when(MultiSector && FDC_Sector.asUInt < 15) {
          FDC_Sector := (FDC_Sector.asUInt + 1).asBits
          ByteCount := 0
          DrqDelay := 90
          goto(ReadSector_Find)
        } otherwise {
          goto(Wait4CMD)
        }
      }
    }

    // ---- Write Sector (0xA/0xB): 128-byte DRQ handshake ----
    val WriteSector_DRQ: State = new State {
      whenIsActive {
        FDC_Status(0) := True // Busy
        FDC_Status(1) := True // DRQ (ask for first byte)
        when(datWriteRise) {
          FDC_Status(1) := False // CPU supplied a byte
          goto(WriteSector_Send)
        }
      }
    }

    val WriteSector_Send: State = new State {
      whenIsActive {
        FDC_Status(0) := True
        io.Disk.WriteReq := True
        io.Disk.DataOut := FDC_Data
        ByteCount := ByteCount + 1
        when(ByteCount === 127) {
          goto(WriteSector_Done)
        } otherwise {
          goto(WriteSector_DRQ)
        }
      }
    }

    val WriteSector_Done: State = new State {
      whenIsActive {
        // Type-II completion: no error flags on success (see ReadSector_Done)
        FDC_Status := 0x00
        FDC_INTRQ := True
        goto(Wait4CMD)
      }
    }
  }
}
