package VIS

import spinal.core._

class CDP1869 extends Component {
    val io = new Bundle {
        val HSync_ = in Bool()
        val Display_ = in Bool()
        val AddSTB_ = in Bool()

        val N = in Bits(3 bit)
        val N3_ = out Bool()

        val TPA = in Bool()
        val TPB = in Bool()

        val MRD = in Bool()
        val MWR = in Bool()
        val Addr = in Bits(8 bit)

        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)

        val CMWR_ = out Bool()
        val CMSEL = out Bool()
        
        val CMA = out Bits(3 bits)
        val CMA3_PMA10 = out Bool()
        
        val PMWR_ = out Bool()
        val PMSEL = out Bool()

        val PMA = out Bits(10 bits)

        val Sound = out SInt(6 bits)
    }
    
    //Registers
    val UpperAddr = RegNextWhen(io.Addr, io.TPA.rise(), B"8'h00")
    val Addr16 = UpperAddr ## io.Addr

    val HMA_Reg = RegNextWhen(Addr16(10 downto 2), io.N === 7 && !io.MRD && io.TPB) init(0)
    val PMA_Reg = RegNextWhen(Addr16(10 downto 0), io.N === 6 && !io.MRD && io.TPB) init(0)
    val WN_Reg = RegNextWhen(Addr16, io.N === 5 && !io.MRD && io.TPB) init(0)
    val SN_Reg = RegNextWhen(Addr16(14 downto 0), io.N === 4 && !io.MRD && io.TPB) init(B"15'h0080")

    val RCA = Reg(UInt(5 bits)) init(0) //counter for character address
    val HMA = Reg(UInt(11 bits)) init(0) //offset counter
    val RPA = Reg(UInt(11 bits)) init(0) //counter for page address

    val ToneDiv = Reg(UInt(11 bits)) init(0) //counter for page address
    val ToneCounter = Reg(UInt(7 bits)) init(0) //counter for page address
    val Tone_FF = Reg(Bool()) init(false)

    val WN_LFSR = Reg(Bits(16 bits)) init(1)

    //Signals
    val FresVert = WN_Reg(7)
    val DoublePage = WN_Reg(6)
    val HiRes16Line = WN_Reg(5)
    val NineLine = WN_Reg(3)
    val CmemAccessMode = WN_Reg(0)

    val RCA_NEXT = RCA + 1
    val HMA_NEXT_20 = HMA + 20
    val HMA_NEXT_40 = HMA + 40

    val RCA_OUTPUT = FresVert ? RCA | RCA |>> 1

    val RCA_15 = RCA < 15 && NineLine && (HiRes16Line || !FresVert)
    val RCA_7 = RCA < 7 && NineLine
    val RCA_8 = RCA < 8 && !NineLine

    val RemapRCA = FresVert ? RCA.asBits(3 downto 0) | RCA.asBits(4 downto 1)

    val PMSEL = (UpperAddr.asUInt >= 0xf8) && io.Display_
    val CMSEL = (UpperAddr.asUInt >= 0xf4) && (UpperAddr.asUInt <= 0xf7) && io.Display_

    when(io.Display_){
        RCA := 0
        RPA := (HMA_Reg ## B"00").asUInt
        HMA := (HMA_Reg ## B"00").asUInt
    }otherwise{
        when(io.AddSTB_.fall()){
            when(RPA >= 959){
                RPA := 0
            }otherwise{
                RPA := RPA + 1
            }
        }
        when(io.HSync_.fall()){
            when(RCA_15 || RCA_7 || RCA_8){
                RCA := RCA_NEXT
                RPA := HMA
            }otherwise{
                RCA := 0
                HMA := RPA
            }
        }
    }


    when(io.TPA.rise() || io.TPB.rise() || io.TPA.fall() || io.TPB.fall()){
        ToneDiv := ToneDiv + 1
    }

    val Tone = SN_Reg(14 downto 8)
    val Tone_Off = SN_Reg(7)
    val Tone_Freq = SN_Reg(6 downto 4)
    val Tone_Amp = B"00" ## SN_Reg(3 downto 0)

    val Tone_Clk = Tone_Freq.mux(
        0 -> ToneDiv(7),
        1 -> ToneDiv(6),
        2 -> ToneDiv(5),
        3 -> ToneDiv(4),
        4 -> ToneDiv(3),
        5 -> ToneDiv(2),
        6 -> ToneDiv(1),
        7 -> ToneDiv(0)
    )

    val WN_Freq = WN_Reg(14 downto 12)
    val WN_Amp = B"00" ## WN_Reg(11 downto 8)
    val WN_Off = WN_Reg(15)

    val WN_CLK = WN_Freq.mux(
        0 -> ToneDiv(10),
        1 -> ToneDiv(9),
        2 -> ToneDiv(8),
        3 -> ToneDiv(7),
        4 -> ToneDiv(6),
        5 -> ToneDiv(5),
        6 -> ToneDiv(4),
        7 -> ToneDiv(3)
    )

    when(Tone_Clk.rise()){
        when(ToneCounter < Tone.asUInt){
            ToneCounter := ToneCounter + 1
        }otherwise{
            ToneCounter := 0
            Tone_FF := !Tone_FF
        }
    }    
    val WN_LFSR_next = WN_LFSR(14 downto 0) ## (WN_LFSR(8) ^ WN_LFSR(12))
    when(WN_CLK.rise()){
        WN_LFSR := WN_LFSR_next
    }

    val ToneOut = (Tone_FF ? (Tone_Amp.resize(6).asSInt) | (-Tone_Amp.resize(6).asSInt))
    val LFSROut = (WN_LFSR(11) ? (WN_Amp.resize(6).asSInt) | (-WN_Amp.resize(6).asSInt))

    val sound_mix = (ToneOut + LFSROut)

    //Outputs
    io.N3_ := (io.N =/= 3)
    io.DataOut := 0x00

    io.PMSEL := PMSEL
    io.PMA := io.Display_ ? ((CmemAccessMode) ? PMA_Reg(9 downto 0) | ((PMSEL) ? Addr16(9 downto 0) | 0x000)) | RPA.asBits(9 downto 0)
    io.PMWR_ := (io.Display_ & PMSEL) ? io.MWR | True

    io.CMSEL := CMSEL
    io.CMWR_ := (io.Display_ & CMSEL) ? io.MWR | True
    io.CMA := io.Display_ ? ((CMSEL) ? Addr16(2 downto 0) | 0x0) | RemapRCA(2 downto 0)

    io.CMA3_PMA10 := DoublePage ? RPA(10) | RemapRCA.asBits(3)

    io.Sound := sound_mix
}
