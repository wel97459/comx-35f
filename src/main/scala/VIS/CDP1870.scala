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

        val CCB_in = in Bits(3 bits)
        val CCB_out = in Bits(3 bits)

        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)

        val N3_ = in Bool()
        val TPB = in Bool()
        val MRD = in Bool()

        val CMSEL = in Bool() 
        val PalOrNTSC = in Bool()
        val CompSync_ = out Bool()
        val Pixel = out Bool()
        val Color = out Bits(3 bits)
    }

    //Registers
        val CMD_Reg = RegNextWhen(io.DataIn, !io.N3_ && !io.MRD && io.TPB) init(0x10)
 
        val FresHorz = CMD_Reg(7)
        val COLB = CMD_Reg(6 downto 5).asUInt
        val DispOff_Next = CMD_Reg(4)
        val CFC = CMD_Reg(3)
        val BKG = CMD_Reg(2 downto 0)
        
        val VerticalCounter = Reg(UInt(9 bits)) init(0)
        val HorizontalCounter = Reg(UInt(6 bits)) init(59)
        val TimingCounter = Reg(UInt(4 bits)) init(0)
        
        val DispOff = RegNextWhen(DispOff_Next, (VerticalCounter === 0).rise()) init(True)

        val PixelShifter = Reg(Bits(6 bits))
        val Color = Reg(Bits(3 bits))

    //Signals
        val VSync_NTSC = VerticalCounter >= 258 && VerticalCounter <= 262
        val VSync_PAL = VerticalCounter >=  308
        val VSync = io.PalOrNTSC ? VSync_PAL | VSync_NTSC

        val VDisplay_NTSC = VerticalCounter >= 36 && VerticalCounter < 228
        val VDisplay_PAL = VerticalCounter >= 44 && VerticalCounter < 260
        val VDisplay = io.PalOrNTSC ? VDisplay_PAL | VDisplay_NTSC

        val VPreDisplay_NTSC = VerticalCounter >= 35 && VerticalCounter < 228
        val VPreDisplay_PAL = VerticalCounter >= 43 && VerticalCounter < 260
        val VPreDisplay = io.PalOrNTSC ? VPreDisplay_PAL | VPreDisplay_NTSC

        val VReset_NTSC = VerticalCounter === 262
        val VReset_PAL = VerticalCounter === 312
        val VReset = io.PalOrNTSC ? VReset_PAL | VReset_NTSC

        val HSync = HorizontalCounter >= 56 && HorizontalCounter <= 59
        val Burst = HorizontalCounter >= 1 && HorizontalCounter <= 4

        val HorizontalBlanking = HorizontalCounter <= 5 || HorizontalCounter >= 54
        val VerticalBlanking = VerticalCounter <= 10 || VerticalCounter >= 252

        val HDisplay = HorizontalCounter >= 10 && HorizontalCounter < 50

        val DotClk6 = TimingCounter === 0 || TimingCounter === 6
        val DotClk12 = TimingCounter === 0

        val DotClk = FresHorz ? DotClk6 | DotClk12

        val PixelClk = FresHorz ? True | !TimingCounter(0)
        
        val AddSTB_ = (HDisplay && !DispOff && VDisplay) ? !DotClk | True

        val ColorOut = COLB.mux(
            0 -> (Color(0) ## Color(1) ## Color(2)),
            1 -> (Color(0) ## Color(2) ## Color(1)),
            2 -> (Color(2) ## Color(0) ## Color(1)),
            3 -> (Color(2) ## Color(0) ## Color(1))
        )

    //Latches

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

        when(PixelClk){
            when(!AddSTB_){
                PixelShifter := io.CDB_in
                Color := io.CCB_in
            }otherwise{
                PixelShifter := PixelShifter |<< 1
            }
        }

    //Outputs
    io.Pixel := PixelShifter(5)
    io.Color := PixelShifter(5) ? ColorOut | BKG

    io.CompSync_ := !(HSync ^ VSync)
    io.HSync_ := !HSync
    io.Display_ := !DispOff ?  !VDisplay | True
    io.PreDisplay_ := !DispOff ? !VPreDisplay | True

    io.AddSTB_ := AddSTB_
    io.DataOut := 0x00
    io.CPUCLK := TimingCounter(0)
}
