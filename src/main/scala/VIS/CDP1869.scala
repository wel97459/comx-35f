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
    io.CMSEL := False
    io.CMA3_PMA10 := False
    io.CMA := 0x0

    io.PMWR_ := True
    io.PMSEL := False
    io.PMA := 0x000

    
    //Registers
    val UpperAddr = RegNextWhen(io.Addr, io.TPA.rise(), Bits(8 bits))
    val Addr16 = UpperAddr ## io.Addr

    val HMA_Reg = RegNextWhen(Addr16(10 downto 2), io.N === 7 && io.TPB, Bits(9 bits))
    val PMA_Reg = RegNextWhen(Addr16(10 downto 0), io.N === 6 && io.TPB, Bits(11 bits))
    val WN_Reg = RegNextWhen(Addr16(7 downto 0), io.N === 5 && io.TPB, Bits(8 bits))

    val RCA = Reg(Bits(4 bits)) init(0) //counter for character address
    val RPA = Reg(Bits(11 bits)) init(0) //counter for page address

    //Signals
    val FresVert = WN_Reg(7)
    val DoublePage = WN_Reg(6)
    val HiRes16Line = WN_Reg(5)
    val NineLine = WN_Reg(3)
    val CmemAccessMode = WN_Reg(0)
 

}
