package VIS

import spinal.core._

class CDP1870 extends Component{
    val io = new Bundle{
        val HSync_ = out Bool()
        val Display_ = out Bool()
        val PreDisplay_ = out Bool()
        val AddSTB_ = out Bool()
        val CPUCLK = out Bool()

        val CDB_in = in Bits(6 bits)
        val CDB_out = in Bits(6 bits)

        val CCB_in = in Bits(2 bits)
        val CCB_out = in Bits(2 bits)

        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)

        val N3_ = in Bool()
        val TPB = in Bool()
        val MRD = in Bool()

        val CMSEL = in Bool() 
        val PalOrNTSC = in Bool()
    }

    //Registers
        val CMD_Reg = RegNextWhen(io.DataIn, !io.N3_ && io.TPB, Bits(8 bits)) init(0x80)
        val FresHorz = CMD_Reg(7)
        val COLB = CMD_Reg(6 downto 5)
        val DispOff_Next = CMD_Reg(4)
        val CFC = CMD_Reg(3)
        val BKG = CMD_Reg(2 downto 0)
        
        val VerticalCounter = Reg(UInt(9 bits)) init(0)
        val HorizontalCounter = Reg(UInt(6 bits)) init(59)
        val TimingCounter = Reg(UInt(4 bits)) init(0)

    //Signals
        val VSync_NTSC = VerticalCounter >= 258 && VerticalCounter <= 262
        val VSync_PAL = VerticalCounter >=  308
        val VSync = io.PalOrNTSC ? VSync_PAL | VSync_NTSC

        val VDisplay_NTSC = VerticalCounter >= 36 && VerticalCounter <= 228
        val VDisplay_PAL = VerticalCounter >= 44 && VerticalCounter <= 260
        val VDisplay = io.PalOrNTSC ? VDisplay_PAL | VDisplay_NTSC

        val VPreDisplay_NTSC = VerticalCounter >= 35 && VerticalCounter <= 228
        val VPreDisplay_PAL = VerticalCounter >= 43 && VerticalCounter <= 260
        val VPreDisplay = io.PalOrNTSC ? VPreDisplay_PAL | VPreDisplay_NTSC

        val VReset_NTSC = VerticalCounter === 262
        val VReset_PAL = VerticalCounter === 312
        val VReset = io.PalOrNTSC ? VReset_PAL | VReset_NTSC

        val HSync = HorizontalCounter >= 56 && HorizontalCounter <= 59
        val Burst = HorizontalCounter >= 1 && HorizontalCounter <= 4

        val HDisplay = HorizontalCounter >= 10 && HorizontalCounter <= 49

        val DotClk6 = TimingCounter === 0 || TimingCounter === 6
        val DotClk12 = TimingCounter === 0
        val DotClk = FresHorz ? DotClk6 | DotClk12

        val DispOff = RegNextWhen(DispOff_Next, (VerticalCounter === 0).rise(), Bool()) init(False)

        //Outputs
        io.HSync_ := True
        io.Display_ := DispOff ? VDisplay | True
        io.PreDisplay_ := DispOff ? VPreDisplay | True
        io.AddSTB_ := (HDisplay && !DispOff && VDisplay) ? DotClk | True
        io.DataOut := 0x00
        io.CPUCLK := TimingCounter(0)

    when(TimingCounter === 11){
        TimingCounter := 0
    }otherwise{
        TimingCounter := TimingCounter + 1
    }

    when(DotClk6){
        when(HorizontalCounter === 59){
            HorizontalCounter := 0
            when(VReset){
                VerticalCounter := 0
            }otherwise{
                VerticalCounter := VerticalCounter + 1
            }
        }otherwise{
            HorizontalCounter := HorizontalCounter + 1
        }
    }
}
