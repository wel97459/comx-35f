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
    }

    //Outputs
    io.N3_ := (io.N =/= 3)
    io.DataOut := 0x00

    io.CMWR_ := True
    io.CMA3_PMA10 := False
    io.CMA := 0x0

    io.PMWR_ := True
    io.PMA := 0x000

    
    //Registers
    val UpperAddr = RegNextWhen(io.Addr, io.TPA.rise(), B"8'h00")
    val Addr16 = UpperAddr ## io.Addr

    val HMA_Reg = RegNextWhen(Addr16(10 downto 2), io.N === 7 && io.TPB, B"9'h000")
    val PMA_Reg = RegNextWhen(Addr16(10 downto 0), io.N === 6 && io.TPB, B"11'h000")
    val WN_Reg = RegNextWhen(Addr16(7 downto 0), io.N === 5 && io.TPB, B"8'h00")
    
    val RCA = Reg(UInt(5 bits)) init(0) //counter for character address
    val HMA = Reg(UInt(11 bits)) init(0) //offset counter
    val RPA = Reg(UInt(11 bits)) init(0) //counter for page address

    //Signals
    val FresVert = WN_Reg(7)
    val DoublePage = WN_Reg(6)
    val HiRes16Line = WN_Reg(5)
    val NineLine = WN_Reg(3)
    val CmemAccessMode = WN_Reg(0)

    val RCA_NEXT = RCA + 1
    val HMA_NEXT = HMA + 20

    val RCA_OUTPUT = FresVert ? RCA | RCA |>> 1

    val RCA_15 = RCA < 15 && NineLine && (HiRes16Line || !FresVert)
    val RCA_7 = RCA < 7 && NineLine
    val RCA_8 = RCA < 8 && !NineLine

    io.PMSEL := (UpperAddr.asUInt >= 0xf8)
    io.CMSEL := (UpperAddr.asUInt >= 0xf4) && (UpperAddr.asUInt <= 0xf7)

    when(io.Display_){
        RCA := 0
        RPA := (HMA_Reg ## B"00").asUInt
        HMA := (HMA_Reg ## B"00").asUInt
    }otherwise{
        when(io.AddSTB_.fall()){
            RPA := RPA + 1
        }
        when(io.HSync_.fall()){
            when(RCA_15 || RCA_7 || RCA_8){
                RCA := RCA_NEXT
                RPA := HMA
            }otherwise{
                RCA := 0
                HMA := HMA_NEXT
            }
        }
    }
}
