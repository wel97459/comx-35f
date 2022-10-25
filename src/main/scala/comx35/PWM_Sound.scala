package comx35

import spinal.core._

class PWM_Sound extends Component{
    val io = new Bundle {
        val Sound = in Bits(5 bits)
        val PWM = out Bool()
    }

    val PWM_Counter = Reg(UInt(6 bits))
    PWM_Counter := PWM_Counter + 1
    io.PWM := PWM_Counter < (B"0" ## io.Sound).asUInt
}
