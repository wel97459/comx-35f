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

class ascii2comx extends Component
{   
    val ascii = in Bits(8 bits)
    val comx = out Bits(8 bits)

    val keyboardCode_ = B"8'h00" 

    when(ascii.asUInt  === '\r'){keyboardCode_ := 0x80
    }elsewhen(ascii.asUInt  === '\n'){keyboardCode_ := 0x80
    }elsewhen(ascii.asUInt  === '@'){keyboardCode_ := 0x20
    }elsewhen(ascii.asUInt  === '#'){keyboardCode_ := 0x23
    }elsewhen(ascii.asUInt  === '\''){ keyboardCode_ := 0x27
    }elsewhen(ascii.asUInt  === '['){keyboardCode_ := 0x28
    }elsewhen(ascii.asUInt  === ']'){keyboardCode_ := 0x29
    }elsewhen(ascii.asUInt  === ':'){keyboardCode_ := 0x2a
    }elsewhen(ascii.asUInt  === ';'){keyboardCode_ := 0x2b
    }elsewhen(ascii.asUInt  === '<'){keyboardCode_ := 0x2c
    }elsewhen(ascii.asUInt  === '='){keyboardCode_ := 0x2d
    }elsewhen(ascii.asUInt  === '`'){keyboardCode_ := 0x2d//=
    }elsewhen(ascii.asUInt  === '>'){keyboardCode_ := 0x2e
    }elsewhen(ascii.asUInt  === '\\'){keyboardCode_ := 0x2f
    }elsewhen(ascii.asUInt  === '.'){keyboardCode_ := 0x3a
    }elsewhen(ascii.asUInt  === ','){keyboardCode_ := 0x3b
    }elsewhen(ascii.asUInt  === '('){keyboardCode_ := 0x3c
    }elsewhen(ascii.asUInt  === '^'){keyboardCode_ := 0x3d
    }elsewhen(ascii.asUInt  === '~'){keyboardCode_ := 0x3d //^
    }elsewhen(ascii.asUInt  === ')'){keyboardCode_ := 0x3e
    }elsewhen(ascii.asUInt  === '_'){keyboardCode_ := 0x3f
    }elsewhen(ascii.asUInt  === '?'){keyboardCode_ := 0x40
    }elsewhen(ascii.asUInt  === '+'){keyboardCode_ := 0x5b
    }elsewhen(ascii.asUInt  === '-'){keyboardCode_ := 0x5c
    }elsewhen(ascii.asUInt  === '*'){keyboardCode_ := 0x5d
    }elsewhen(ascii.asUInt  === '/'){keyboardCode_ := 0x5e
    }elsewhen(ascii.asUInt  === ' '){keyboardCode_ := 0x5f
    }elsewhen(ascii.asUInt  === '|'){keyboardCode_ := 0x1B
    }elsewhen(ascii.asUInt  === '\b'){keyboardCode_ := 0x86
    }elsewhen(ascii.asUInt  === '\t'){keyboardCode_ := 0x81
    }elsewhen(ascii.asUInt < 0x90) {keyboardCode_ := ascii
    }elsewhen(ascii.asUInt >= 0x90) {keyboardCode_ := ascii & 0x7f}

    comx := keyboardCode_.asBits
}