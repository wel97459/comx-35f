package VIS
import spinal.core._

class CDP1871 extends Component
{
    val io = new Bundle {
        val Latch = in Bool()
        val KeyCode = in Bits(8 bits)
        val Ready = out Bool()

        val TPB = in Bool()
        val MRD_ = in Bool()
        val N3_ = in Bool()

        val DA_ = out Bool()
        val RPT_ = out Bool()
        val DataOut = out Bits(8 bits)
        val KBD_SEL = out Bool()
    }

    //Registers 
        val ready = Reg(Bool()) init(True)
        val keycode = Reg(Bits(8 bits)) init(0x00)
        val da = Reg(Bool()) init(False)
        val rpt = False

    //Signals
        val kbd_sel = !io.N3_ & io.MRD_

    //Latches

        when(ready && io.Latch)
        {
            keycode := io.KeyCode
            ready := False
            da := True
        }

        when(kbd_sel)
        {
            da := False
            ready := True
        }
        
    //Outputs
    io.DataOut := kbd_sel ? keycode | 0x00
    io.DA_ := !da
    io.RPT_ := !rpt
    io.KBD_SEL := kbd_sel
    io.Ready := ready
}


