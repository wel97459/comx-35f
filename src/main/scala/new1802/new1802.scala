package new1802

import spinal.core._
import spinal.lib._

object CPUModes extends SpinalEnum {
    val Load, Reset, Pause, Run = newElement()
}

object CPUStates extends SpinalEnum {
    val S0_Fetch, S1_Reset, S1_Init, S1_Execute, S2_DMA, S3_INT = newElement()
}

object ALUModeEnum extends SpinalEnum {
    val BusIn, ORBus, XORBus, ANDBus, RSH, LSH, RSHR, LSHR, AddBus, AddCarryBus, SubDBus, SubDBorrowBus, SubMBus, SubMBorrowBus = newElement()
}

object RegSelEnum extends SpinalEnum {
    val PSel, NSel, XSel, Stack2, DMA0 = newElement()
}

object DataRegControlEnum extends SpinalEnum {
    val Inc, Dec, LoadLower, LoadUpper, LoadTemp, LoadJump = newElement()
}
object RegControlEnum extends SpinalEnum {
    val PLatch, XLatch, P2XLatch, Bus2XPLatch, TLatch, QLatch, IELatchL, IELatchH, InterruptLatch   = newElement()
}

object BusControlEnum extends SpinalEnum {
    val DataIn, DReg, TReg, XPReg, RLower, RUpper = newElement()
}

object CPUFlagsEnum extends SpinalEnum {
    val Idle, Branch, NOut, Read, Write, LongLoad = newElement()
}

class ALU1802() extends Component {
    val io = new Bundle {
        val Bus = in UInt(8 bit)
        val Reset = in Bool()
        val ALUMode = in Bits(ALUModeEnum.elements.length.bits) 
        val D = out UInt(8 bit)
        val DF = out Bool()
        val DLast = out UInt(8 bit) 
    }
    //Registers
    val D = Reg(UInt(8 bit)) init(0)//Data Register (Accumulator)
    val DLast = RegNext(D)//Data Register (Accumulator)

    val DF = Reg(Bool) init(False) //Data Flag (ALU Carry)
    val DFLast = RegNext(DF) //For Testing

    //IO Assignments
    io.D := D
    io.DLast := DLast
    io.DF := DF

    //Internal Wires

    //ALU Operations
    val Add = UInt(9 bit)
    val AddCarry = UInt(9 bit)
    val SubD = UInt(9 bit)
    val SubM = UInt(9 bit)
    val SubDB = UInt(9 bit)
    val SubMB = UInt(9 bit) 


    // D Register Logic / ALU Logic
    Add := io.Bus.resize(9) + D.resize(9)
    AddCarry := Add + Cat(B"8'h00", DF).asUInt
    SubD := io.Bus.resize(9) - D.resize(9)
    SubM := D.resize(9) - io.Bus.resize(9)
    SubDB := SubD - Cat(B"8'h00", !DF).asUInt
    SubMB := SubM - Cat(B"8'h00", !DF).asUInt

    when(io.Reset) {
        DF := False
        D := 0
    }elsewhen(io.ALUMode(ALUModeEnum.BusIn.position)) {
        D := io.Bus
    }elsewhen(io.ALUMode(ALUModeEnum.ORBus.position)) {
        D := io.Bus | D
    }elsewhen(io.ALUMode(ALUModeEnum.XORBus.position)) {
        D := io.Bus ^ D
    }elsewhen(io.ALUMode(ALUModeEnum.ANDBus.position)) {
        D := io.Bus & D
    }elsewhen(io.ALUMode(ALUModeEnum.RSH.position)) {
        DF := DLast.lsb
        D := D |>> 1
    }elsewhen(io.ALUMode(ALUModeEnum.RSHR.position)) {
        DF := DLast.lsb
        D := D |>> 1 | Cat(DFLast, B"7'h00").asUInt
    }elsewhen(io.ALUMode(ALUModeEnum.LSH.position)) {
        DF := DLast.msb
        D := D |<< 1
    }elsewhen(io.ALUMode(ALUModeEnum.LSHR.position)) {
        DF := DLast.msb
        D := D |<< 1 | Cat(B"7'h00", DFLast).asUInt
    }elsewhen(io.ALUMode(ALUModeEnum.AddBus.position)) {
        DF := Add.msb
        D := Add.resize(8)
    }elsewhen(io.ALUMode(ALUModeEnum.AddCarryBus.position)) {
        DF := AddCarry.msb
        D := AddCarry.resize(8)
    }elsewhen(io.ALUMode(ALUModeEnum.SubDBus.position)) {
        DF := !SubD.msb
        D := SubD.resize(8)
    }elsewhen(io.ALUMode(ALUModeEnum.SubMBus.position)) {
        DF := !SubM.msb
        D := SubM.resize(8)
    }elsewhen(io.ALUMode(ALUModeEnum.SubDBorrowBus.position)) {
        DF := !SubDB.msb
        D := SubDB.resize(8)
    }elsewhen(io.ALUMode(ALUModeEnum.SubMBorrowBus.position)) {
        DF := !SubMB.msb
        D := SubMB.resize(8)
    }
}

class Registers() extends Component {
    val io = new Bundle
    {
        val AddrLatch = in Bool()
        val RLatch = in Bool()
        val EFLatch = in Bool()

        val Reset = in Bool()

        val RegSel = in Bits(RegSelEnum.elements.length.bits)
        val DataRegControl = in Bits(DataRegControlEnum.elements.length.bits)
        val RegControl = in Bits(RegControlEnum.elements.length.bits)

        val N = in UInt(4 bits)
        val EF_n = in Bits(4 bits)

        val P = out UInt(4 bits)
        val X = out UInt(4 bits)
        val T = out UInt(8 bits)
        val IE = out Bool()
        val Q = out Bool()

        val EF = out Bits(4 bits)
        val Bus = in UInt(8 bits)

        val R = out UInt(16 bits)
        val Addr16 = out UInt(16 bits)
    }
    
    //Register
    val TmpUpper = Reg(UInt(8 bits))
    val R = Vec(Reg(UInt(16 bits)), 16)
    
    val P = Reg(UInt(4 bits))
    val X = Reg(UInt(4 bits))
    val T = RegNextWhen(X @@ P, io.RegControl(RegControlEnum.TLatch.position))
    val Q = RegNextWhen(io.N(0), io.RegControl(RegControlEnum.QLatch.position))
    val IE = Reg(Bool()) init(True)
    val EF = RegNextWhen(~io.EF_n, io.EFLatch)
    //Internal Wires
    val RSel = U"4'h0"
    val RWrite = U"16'h0000"

    val A = R(RSel)

    val Addr = RegNextWhen(A, io.AddrLatch) init(0)//Current Address Register

    //IO Assignments
    io.R := A
    io.Addr16 := io.AddrLatch ? A | Addr
    io.P := P
    io.X := X
    io.T := T
    io.Q := Q
    io.IE := IE
    io.EF := EF
    //Register Array Selection Logic
    when(io.Reset){
        RSel := 0
    } elsewhen (io.RegSel(RegSelEnum.PSel.position)) {
        RSel := io.P
    } elsewhen(io.RegSel(RegSelEnum.NSel.position)) {
        RSel := io.N
    } elsewhen (io.RegSel(RegSelEnum.XSel.position)) {
        RSel := io.X
    } elsewhen (io.RegSel(RegSelEnum.Stack2.position)) {
        RSel := 2
    } elsewhen (io.RegSel(RegSelEnum.DMA0.position)) {
        RSel := 0
    }

    //Register Array Operation Logic
    when(io.DataRegControl(DataRegControlEnum.LoadJump.position)){
        RWrite := TmpUpper @@ io.Bus
    } elsewhen(io.DataRegControl(DataRegControlEnum.Inc.position)){
        RWrite := A + 1
    } elsewhen (io.DataRegControl(DataRegControlEnum.Dec.position)) {
        RWrite := A - 1
    } elsewhen (io.DataRegControl(DataRegControlEnum.LoadUpper.position)) {
        RWrite := io.Bus @@ A(7 downto 0)
    } elsewhen (io.DataRegControl(DataRegControlEnum.LoadLower.position)) {
        RWrite := A(15 downto 8) @@ io.Bus
    }

    when(io.Reset){
        R(0) := U"16'h0000"
    } elsewhen(io.RLatch) {
        R(RSel) := RWrite
    }

    when(io.RLatch && io.DataRegControl(DataRegControlEnum.LoadTemp.position)) {
        TmpUpper := io.Bus
    }

    when(io.RegControl(RegControlEnum.Bus2XPLatch.position)){
        P := io.Bus(3 downto 0)
    } elsewhen(io.RegControl(RegControlEnum.PLatch.position)) {
        P := io.N
    } elsewhen(io.RegControl(RegControlEnum.InterruptLatch.position)){
        P := 1
    }

    when(io.RegControl(RegControlEnum.Bus2XPLatch.position)) {
        X := io.Bus(7 downto 4)
    } elsewhen(io.RegControl(RegControlEnum.P2XLatch.position)) {
        X := P
    } elsewhen(io.RegControl(RegControlEnum.XLatch.position)) {
        X := io.N
    } elsewhen(io.RegControl(RegControlEnum.InterruptLatch.position)){
        X := 2
    }

    when(io.RegControl(RegControlEnum.IELatchL.position)) {
        IE := False
    } elsewhen(io.RegControl(RegControlEnum.IELatchH.position)) {
        IE := True
    }
} 

class Bus() extends Component {
    val io = new Bundle
    {
        val Reset = in Bool()

        val DataIn = in UInt(8 bits)
        val D = in UInt(8 bits)
        val T = in UInt(8 bits)
        val X = in UInt(4 bits)
        val P = in UInt(4 bits)
        val Reg = in UInt(16 bits)

        val BusControl = in Bits(BusControlEnum.elements.length.bits)

        val Bus = out UInt(8 bits)
    }
    val XP = RegNext(io.X @@ io.P) //Delay X and P
    //Data Bus Logic
    when(io.BusControl(BusControlEnum.DataIn.position)){
        io.Bus := io.DataIn
    } elsewhen(io.BusControl(BusControlEnum.DReg.position)){
        io.Bus := io.D
    } elsewhen(io.BusControl(BusControlEnum.TReg.position)){
        io.Bus := io.T
    } elsewhen(io.BusControl(BusControlEnum.XPReg.position)){
        io.Bus := XP
    } elsewhen(io.BusControl(BusControlEnum.RLower.position)){
        io.Bus := io.Reg(7 downto 0)
    } elsewhen(io.BusControl(BusControlEnum.RUpper.position)){
        io.Bus := io.Reg(15 downto 8)
    } otherwise(io.Bus := 0)
}

class OpDecoder() extends Component {
    val io = new Bundle {
        val Bus = in UInt(8 bits)

        val OpLatch = in Bool()

        val D = in UInt(8 bits)
        val DF = in Bool()
        val EF = in Bits(4 bits)

        val CPUFlags = out Bits(CPUFlagsEnum.elements.length.bits)

        val ALUMode = out Bits(ALUModeEnum.elements.length.bits)

        val RegSel = out Bits(RegSelEnum.elements.length.bits)
        val DataRegControl = out Bits(DataRegControlEnum.elements.length.bits)
        val RegControl = out Bits(RegControlEnum.elements.length.bits)

        val BusControl = out Bits(BusControlEnum.elements.length.bits)

        val I = out UInt(4 bits)
        val N = out UInt(4 bits)

        val P = in UInt(4 bits)
        val X = in UInt(4 bits)
        val T = in UInt(8 bits)
        val Q = in Bool()
        val IE = in Bool()
    }
    //Register
    val OP = RegNextWhen(io.Bus, io.OpLatch) //Holds Low-Order Instruction Digit

    //Internal Assignments
    val N = OP(3 downto 0)//Holds Low-Order Instruction Digit

    val Nor = N.orR

    val N0 = N(0)
    val N1 = N(1)
    val N2 = N(2)
    val N3 = N(3)

    val Nx0 = !Nor
    val Nx1 = N === 0x1
    val Nx2 = N === 0x2
    val Nx3 = N === 0x3
    val Nx4 = N === 0x4
    val Nx5 = N === 0x5
    val Nx6 = N === 0x6
    val Nx7 = N === 0x7
    val Nx8 = N === 0x8
    val Nx9 = N === 0x9
    val NxA = N === 0xA
    val NxB = N === 0xB
    val NxC = N === 0xC
    val NxD = N === 0xD
    val NxE = N === 0xE
    val NxF = N === 0xF

    val I = OP(7 downto 4)//Holds High-Order Instruction Digit
    val Ix3 = I === 0x3
    val IxC = I === 0xC

    val EF = io.EF(N(1 downto 0))
    val Continue = (Nx1 || Nx2 || Nx3 || Nx9 || NxA || NxB)
    //IO Assignments
    io.N := N
    io.I := I

    io.ALUMode := 0
    io.RegSel := 0
    io.DataRegControl := 0
    io.BusControl := 0
    io.RegControl := 0

    io.CPUFlags(CPUFlagsEnum.Idle.position) := False
    io.CPUFlags(CPUFlagsEnum.elements.length-1 downto 2) := 0

    //Check for a branch conditions
    when(Nx0) {
        io.CPUFlags(CPUFlagsEnum.Branch.position) := True
    }elsewhen(Nx1 || (IxC && (Nx5 || Nx1))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (io.Q)
    }elsewhen(Nx2 || (IxC && (Nx6 || Nx2))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (io.D === 0x0)
    }elsewhen(Nx3 || (IxC && (Nx7 || Nx3))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (io.DF)
    }elsewhen(Nx9 || (IxC && (NxD || Nx9))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (!io.Q)
    }elsewhen(NxA || (IxC && (NxE || NxA))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (io.D =/= 0x0)
    }elsewhen(NxB || (IxC && (NxF || NxB))){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (!io.DF)
    }elsewhen(IxC && NxC){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (io.IE)
    }elsewhen(Ix3 && (Nx4 || Nx5 || Nx6 || Nx7)){
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (EF)
    }elsewhen(Ix3 && (NxC || NxD || NxE || NxF)) {
        io.CPUFlags(CPUFlagsEnum.Branch.position) := (!EF)
    } otherwise{
        io.CPUFlags(CPUFlagsEnum.Branch.position) := False
    }

    //Logic
    switch(I) {
        is(0x0) {
            io.CPUFlags(CPUFlagsEnum.Read.position) := True
            io.BusControl(BusControlEnum.DataIn.position) := True

            //IDLE
            io.CPUFlags(CPUFlagsEnum.Idle.position) := !Nor
            io.RegSel(RegSelEnum.DMA0.position) := !Nor

            //LOAD VIA N
            io.RegSel(RegSelEnum.NSel.position) := Nor
            io.ALUMode(ALUModeEnum.BusIn.position) := Nor
        }
        is(0x1) { //INCREMENT REG N
            io.RegSel(RegSelEnum.NSel.position) := True
            io.DataRegControl(DataRegControlEnum.Inc.position) := True
        }
        is(0x2) { //DECREMENT REG N
            io.RegSel(RegSelEnum.NSel.position) := True
            io.DataRegControl(DataRegControlEnum.Dec.position) := True
        }
        is(0x3) { //INCREMENT REG X
            io.CPUFlags(CPUFlagsEnum.Read.position) := True
            io.BusControl(BusControlEnum.DataIn.position) := True
            io.RegSel(RegSelEnum.PSel.position) := True
            io.DataRegControl(DataRegControlEnum.LoadLower.position) := io.CPUFlags(CPUFlagsEnum.Branch.position)
            io.DataRegControl(DataRegControlEnum.Inc.position) := !io.CPUFlags(CPUFlagsEnum.Branch.position)
        }
        is(0x4){ //LOAD ADVANCE N
            io.CPUFlags(CPUFlagsEnum.Read.position) := True
            io.BusControl(BusControlEnum.DataIn.position) := True
            io.RegSel(RegSelEnum.NSel.position) := True
            io.DataRegControl(DataRegControlEnum.Inc.position) := True
            io.ALUMode(ALUModeEnum.BusIn.position) := True
        }
        is(0x5){
            io.CPUFlags(CPUFlagsEnum.Write.position) := True
            io.BusControl(BusControlEnum.DReg.position) := True
            io.RegSel(RegSelEnum.NSel.position) := True
        }
        is(0x6){
            io.RegSel(RegSelEnum.XSel.position) := True
            io.CPUFlags(CPUFlagsEnum.NOut.position) := True
            io.BusControl(BusControlEnum.DataIn.position) := True

            //INPUT N
            io.CPUFlags(CPUFlagsEnum.Write.position) := N3
            io.ALUMode(ALUModeEnum.BusIn.position) := N3

            //INCREMENT REG X
            //OUTPUT N
            io.CPUFlags(CPUFlagsEnum.Read.position) := !N3
            io.DataRegControl(DataRegControlEnum.Inc.position) := !N3
        }
        is(0x7) {
            // Ret / Dis / LOAD VIA X AND ADVANCE / STORE VIA X AND DECREMENT
            io.RegSel(RegSelEnum.XSel.position) := !N3 || Nx8
            io.DataRegControl(DataRegControlEnum.Inc.position) := Nx0 || Nx1 || Nx2 || NxC || NxD || NxF
            io.RegControl(RegControlEnum.Bus2XPLatch.position) := Nx0 || Nx1
            io.RegControl(RegControlEnum.IELatchH.position) := Nx0
            io.RegControl(RegControlEnum.IELatchL.position) := Nx1
            // LOAD VIA X AND ADVANCE
            io.CPUFlags(CPUFlagsEnum.Read.position) := Nx0 || Nx1 || Nx2 || Nx4 || Nx5 || Nx7
            io.BusControl(BusControlEnum.DataIn.position) := Nx0 || Nx1 || Nx2 || Nx4 || Nx5 || Nx7
            io.ALUMode(ALUModeEnum.BusIn.position) := Nx2
            // STORE VIA X AND DECREMENT
            io.CPUFlags(CPUFlagsEnum.Write.position) := Nx3 || Nx8 || Nx9
            io.BusControl(BusControlEnum.DReg.position) := Nx3
            //ADD WITH CARRY
            io.ALUMode(ALUModeEnum.AddCarryBus.position) := Nx4 || NxC
            //SUBTRACT D WITH BORROW
            io.ALUMode(ALUModeEnum.SubDBorrowBus.position) := Nx5 || NxD
            // RING SHIFT RIGHT
            io.ALUMode(ALUModeEnum.RSHR.position) := Nx6
            // SUBTRACT MEMORY WITH BORROW
            io.ALUMode(ALUModeEnum.SubMBorrowBus.position) := Nx7 || NxF
            // SAV
            io.BusControl(BusControlEnum.TReg.position) := Nx8
            // Mark / STORE VIA X AND DECREMENT
            io.RegSel(RegSelEnum.Stack2.position) := Nx9
            io.DataRegControl(DataRegControlEnum.Dec.position) := Nx9 || Nx3
            io.BusControl(BusControlEnum.XPReg.position) := Nx9
            io.RegControl(RegControlEnum.P2XLatch.position) := Nx9
            io.RegControl(RegControlEnum.TLatch.position) := Nx9
            // SET/RESET Q
            io.RegControl(RegControlEnum.QLatch.position) := NxA || NxB
            //ADD WITH CARRY, IMMEDIATE
            io.RegSel(RegSelEnum.PSel.position) := NxC || NxD || NxF
            //RING SHIFT LEFT
            io.ALUMode(ALUModeEnum.LSHR.position) := NxE
        }
        is(0x8) {//GET LOW REG N
            io.BusControl(BusControlEnum.RLower.position) := True
            io.RegSel(RegSelEnum.NSel.position) := True
            io.ALUMode(ALUModeEnum.BusIn.position) := True
        }
        is(0x9) { //PUT LOW REG N
            io.BusControl(BusControlEnum.RUpper.position) := True
            io.RegSel(RegSelEnum.NSel.position) := True
            io.ALUMode(ALUModeEnum.BusIn.position) := True
        }
        is(0xA) {//GET HIGH REG N
            io.RegSel(RegSelEnum.NSel.position) := True
            io.BusControl(BusControlEnum.DReg.position) := True
            io.DataRegControl(DataRegControlEnum.LoadLower.position) := True
        }
        is(0xB) { //PUT HIGH REG N
            io.RegSel(RegSelEnum.NSel.position) := True
            io.BusControl(BusControlEnum.DReg.position) := True
            io.DataRegControl(DataRegControlEnum.LoadUpper.position) := True
        }
        is(0xC){
            io.CPUFlags(CPUFlagsEnum.Read.position) := True
            io.CPUFlags(CPUFlagsEnum.LongLoad.position) := True
            io.BusControl(BusControlEnum.DataIn.position) := True
            io.RegSel(RegSelEnum.PSel.position) := True
            io.DataRegControl(DataRegControlEnum.LoadTemp.position) := io.CPUFlags(CPUFlagsEnum.Branch.position)
            io.DataRegControl(DataRegControlEnum.LoadJump.position) := io.CPUFlags(CPUFlagsEnum.Branch.position)
            io.DataRegControl(DataRegControlEnum.Inc.position) := io.CPUFlags(CPUFlagsEnum.Branch.position) || Continue
        }
        is(0xD){
            io.RegControl(RegControlEnum.PLatch.position) := True
        }
        is(0xE){
            io.RegControl(RegControlEnum.XLatch.position) := True
        }
        is(0xF){
            io.CPUFlags(CPUFlagsEnum.Read.position) := !Nx6 && !NxE
            io.RegSel(RegSelEnum.PSel.position) := N3
            io.RegSel(RegSelEnum.XSel.position) := !N3
            io.DataRegControl(DataRegControlEnum.Inc.position) := N3 && !NxE 
            io.ALUMode(ALUModeEnum.BusIn.position) := Nx0 || Nx8
            io.BusControl(BusControlEnum.DataIn.position) := !Nx6 && !NxE

            io.ALUMode(ALUModeEnum.ORBus.position) := Nx1 || Nx9
            io.ALUMode(ALUModeEnum.ANDBus.position) := Nx2 || NxA
            io.ALUMode(ALUModeEnum.XORBus.position) := Nx3 || NxB
            io.ALUMode(ALUModeEnum.AddBus.position) := Nx4 || NxC
            io.ALUMode(ALUModeEnum.SubDBus.position) := Nx5 || NxD
            io.ALUMode(ALUModeEnum.RSH.position) := Nx6
            io.ALUMode(ALUModeEnum.LSH.position) := NxE
            io.ALUMode(ALUModeEnum.SubMBus.position) := Nx7 || NxF
        }
    }
}
// Hardware definition
class new1802() extends Component 
{
    val io = new Bundle 
    {
        val Wait_n = in Bool()
        val Clear_n = in Bool()
        val DMA_In_n = in Bool()
        val DMA_Out_n = in Bool()
        val Interrupt_n = in Bool()
        val EF_n = in Bits (4 bit)

        val Q = out Bool()
        val SC = out Bits (2 bit)
        val N = out Bits (3 bit)
        val TPA = out Bool()
        val TPB = out Bool()

        val MRD = out Bool()
        val MWR = out Bool()
        val Addr16 = out Bits(16 bit)
        val Addr = out Bits(8 bit)

        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)
    }

    //External

    //Internal
    val StateCounter = Counter(8)

    val Mode = Reg(CPUModes())
    val CPUState = Reg(CPUStates()) init(CPUStates.S1_Reset)
    val CPUStateNext = CPUStates()

    val LongExec = Reg(Bool())
    val LongExecNext = False
    val PowerUp = Reg(Bool()) init(False)

    //Internal Wires
    val Reset = StateCounter === 0 && CPUState === CPUStates.S1_Reset

    val Seq = new Bundle 
    {
        val FETCH = Bits(8 bits)
        val INIT = Bits(8 bits)
        val RESET = Bits(8 bits)
        val EXEC = Bits(8 bits)
        val INT = Bits(8 bits)
        val DMA = Bits(8 bits)
    }

    //Components
    val BusControl = new Bus()

    val Decoder = new OpDecoder()
    Decoder.io.Bus := BusControl.io.Bus 
    Decoder.io.OpLatch := Seq.RESET(0) || Seq.FETCH(5)

    val ALU = new ALU1802()
    ALU.io.Bus := BusControl.io.Bus
    ALU.io.Reset := Reset

    val Registers = new Registers()
    Registers.io.Reset := Reset
    Registers.io.Bus := BusControl.io.Bus
    Registers.io.N := Decoder.io.N
    Registers.io.EF_n := io.EF_n

    Registers.io.AddrLatch := Seq.EXEC(0) || Seq.FETCH(0) || Seq.INT(0) || Seq.DMA(0)
    Registers.io.RLatch := Seq.FETCH(1) || Seq.DMA(1) || (Decoder.io.DataRegControl.orR && Seq.EXEC(5))
    Registers.io.EFLatch := Seq.EXEC(1) 

    val PReg = (CPUState === CPUStates.S0_Fetch || Seq.EXEC(7) || Seq.INT(7)) && !Seq.FETCH(7)
    val EXEC_Sel = CPUState === CPUStates.S1_Execute || Seq.FETCH(7)

    Registers.io.RegSel(RegSelEnum.PSel.position) := PReg || (Decoder.io.RegSel(RegSelEnum.PSel.position) && EXEC_Sel)
    Registers.io.RegSel(RegSelEnum.NSel.position) := (Decoder.io.RegSel(RegSelEnum.NSel.position) && EXEC_Sel) 
    Registers.io.RegSel(RegSelEnum.XSel.position) := (Decoder.io.RegSel(RegSelEnum.XSel.position) && EXEC_Sel)
    Registers.io.RegSel(RegSelEnum.Stack2.position) := (Decoder.io.RegSel(RegSelEnum.Stack2.position) && EXEC_Sel)
    Registers.io.RegSel(RegSelEnum.DMA0.position) := CPUState === CPUStates.S2_DMA

    Registers.io.DataRegControl(DataRegControlEnum.Inc.position) := CPUState === CPUStates.S2_DMA || CPUState === CPUStates.S0_Fetch || Decoder.io.DataRegControl(DataRegControlEnum.Inc.position)
    Registers.io.DataRegControl(DataRegControlEnum.Dec.position) := Decoder.io.DataRegControl(DataRegControlEnum.Dec.position)
    Registers.io.DataRegControl(DataRegControlEnum.LoadUpper.position) := Decoder.io.DataRegControl(DataRegControlEnum.LoadUpper.position)
    Registers.io.DataRegControl(DataRegControlEnum.LoadLower.position) := Decoder.io.DataRegControl(DataRegControlEnum.LoadLower.position)
    Registers.io.DataRegControl(DataRegControlEnum.LoadTemp.position) :=  Decoder.io.DataRegControl(DataRegControlEnum.LoadTemp.position) && EXEC_Sel
    Registers.io.DataRegControl(DataRegControlEnum.LoadJump.position) := Decoder.io.DataRegControl(DataRegControlEnum.LoadJump.position) && LongExec

    Registers.io.RegControl(RegControlEnum.PLatch.position) := Seq.INIT(0) || (Decoder.io.RegControl(RegControlEnum.PLatch.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.XLatch.position) := Seq.INIT(0) || (Decoder.io.RegControl(RegControlEnum.XLatch.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.P2XLatch.position) := (Decoder.io.RegControl(RegControlEnum.P2XLatch.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.Bus2XPLatch.position) := (Decoder.io.RegControl(RegControlEnum.Bus2XPLatch.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.TLatch.position) := Seq.INIT(1) || Seq.INT(1) || (Decoder.io.RegControl(RegControlEnum.TLatch.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.QLatch.position)  := Seq.INIT(0) || (Decoder.io.RegControl(RegControlEnum.QLatch.position) && Seq.EXEC(5))

    Registers.io.RegControl(RegControlEnum.IELatchL.position) := Seq.INT(0) || (Decoder.io.RegControl(RegControlEnum.IELatchL.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.IELatchH.position) := Seq.INIT(0) || (Decoder.io.RegControl(RegControlEnum.IELatchH.position) && Seq.EXEC(5))
    Registers.io.RegControl(RegControlEnum.InterruptLatch.position) := Seq.INT(2) || (Decoder.io.RegControl(RegControlEnum.IELatchH.position) && Seq.EXEC(5))

    val BusEnable = Seq.EXEC(4) || Seq.EXEC(5) || Seq.EXEC(6) || Seq.EXEC(7)
    BusControl.io.BusControl(BusControlEnum.DReg.position) := BusEnable && Decoder.io.BusControl(BusControlEnum.DReg.position)
    BusControl.io.BusControl(BusControlEnum.DataIn.position)  := CPUState === CPUStates.S2_DMA || CPUState === CPUStates.S0_Fetch || CPUState === CPUStates.S2_DMA || (CPUState === CPUStates.S1_Execute && Decoder.io.BusControl(BusControlEnum.DataIn.position))
    BusControl.io.BusControl(BusControlEnum.XPReg.position)  := BusEnable && Decoder.io.BusControl(BusControlEnum.XPReg.position)
    BusControl.io.BusControl(BusControlEnum.RLower.position)  := BusEnable && Decoder.io.BusControl(BusControlEnum.RLower.position)
    BusControl.io.BusControl(BusControlEnum.RUpper.position)  := BusEnable && Decoder.io.BusControl(BusControlEnum.RUpper.position)
    BusControl.io.BusControl(BusControlEnum.TReg.position)  := BusEnable && Decoder.io.BusControl(BusControlEnum.TReg.position)

    BusControl.io.DataIn := io.DataIn.asUInt
    BusControl.io.D := ALU.io.D
    BusControl.io.P := Registers.io.P
    BusControl.io.X := Registers.io.X
    BusControl.io.T := Registers.io.T

    BusControl.io.Reg := Registers.io.R

    ALU.io.ALUMode := Seq.EXEC(5) ? Decoder.io.ALUMode | 0

    Decoder.io.D := ALU.io.D
    Decoder.io.DF := ALU.io.DF
    Decoder.io.EF := Registers.io.EF
    Decoder.io.P := Registers.io.P
    Decoder.io.X := Registers.io.X
    Decoder.io.T := Registers.io.T
    Decoder.io.Q := Registers.io.Q
    Decoder.io.IE := Registers.io.IE

    //IO Assignments
    io.Q := Registers.io.Q
    io.SC := (CPUState === CPUStates.S2_DMA || CPUState === CPUStates.S3_INT) ## (CPUState === CPUStates.S1_Execute || CPUState === CPUStates.S1_Init || CPUState === CPUStates.S3_INT) 
    io.N := (Decoder.io.CPUFlags(CPUFlagsEnum.NOut.position) && CPUState === CPUStates.S1_Execute) ? Decoder.io.N(2 downto 0).asBits | 0
    io.TPA := False
    io.TPB := False
    io.MRD := True
    io.MWR := True
    io.DataOut := BusControl.io.Bus.asBits
    io.Addr16 := Registers.io.Addr16.asBits

    when(StateCounter <= 2) {
        io.Addr := Registers.io.Addr16(15 downto 8).asBits
    } otherwise(io.Addr := Registers.io.Addr16(7 downto 0).asBits)

    //Internal Assignments
    val StateDemux = (B"8'h01" |<< StateCounter)
    Seq.RESET := (CPUState === CPUStates.S1_Reset) ? StateDemux | (B"8'h00")
    Seq.INIT := (CPUState === CPUStates.S1_Init) ? StateDemux | (B"8'h00")
    Seq.EXEC := (CPUState === CPUStates.S1_Execute) ? StateDemux | (B"8'h00")
    Seq.FETCH := (CPUState === CPUStates.S0_Fetch) ? StateDemux | (B"8'h00")
    Seq.DMA := (CPUState === CPUStates.S2_DMA) ? StateDemux | (B"8'h00")
    Seq.INT := (CPUState === CPUStates.S3_INT) ? StateDemux | (B"8'h00")

    //Counter Control
    when(Mode =/= CPUModes.Pause) {
        StateCounter.increment()
    }

    //Mode Logic
    when(!io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Load
    } elsewhen ((!io.Clear_n && io.Wait_n) || !PowerUp) {
        Mode := CPUModes.Reset
    } elsewhen (io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Pause
    } otherwise (Mode := CPUModes.Run)

    //Memory Read Control Lines
    when(StateCounter >= 3) {
        when (CPUState === CPUStates.S0_Fetch || (CPUState === CPUStates.S2_DMA && !io.DMA_Out_n) || (CPUState === CPUStates.S1_Execute && Decoder.io.CPUFlags(CPUFlagsEnum.Read.position))) {
            io.MRD := False
        }
    }

    //Memory Write Control Lines
    when(StateCounter >= 5) {
        when ((CPUState === CPUStates.S2_DMA && !io.DMA_In_n) || (CPUState === CPUStates.S1_Execute && Decoder.io.CPUFlags(CPUFlagsEnum.Write.position))){
            io.MWR := False
        }
    }

    //TPA & TPB Logic
    when(StateCounter === 1 && CPUState =/= CPUStates.S1_Reset) {
        io.TPA := True
    }

    when(StateCounter === 6 && CPUState =/= CPUStates.S1_Reset) {
        io.TPB := True
    }

    //CPU State Logic
    when(Mode === CPUModes.Reset){
        CPUStateNext := CPUStates.S1_Reset
    }elsewhen(CPUState === CPUStates.S1_Reset) {
        CPUStateNext := CPUStates.S1_Init
    }elsewhen(CPUState === CPUStates.S1_Init) {
        when(io.DMA_In_n || io.DMA_Out_n){
            CPUStateNext := CPUStates.S0_Fetch
        } otherwise {
            CPUStateNext := CPUStates.S2_DMA
        }
    }elsewhen(CPUState === CPUStates.S0_Fetch || ((Decoder.io.CPUFlags(CPUFlagsEnum.Idle.position) && (!io.DMA_In_n || !io.DMA_Out_n || !io.Interrupt_n)))) {
        CPUStateNext :=  CPUStates.S1_Execute
    }elsewhen(CPUState === CPUStates.S1_Execute) {
        when(!io.Interrupt_n && Registers.io.IE) {
            CPUStateNext := CPUStates.S3_INT
        } elsewhen(!io.DMA_In_n || !io.DMA_Out_n){
            CPUStateNext := CPUStates.S2_DMA
        } elsewhen(Decoder.io.CPUFlags(CPUFlagsEnum.LongLoad.position) && !LongExec) {
            LongExecNext := True
            CPUStateNext :=  CPUStates.S1_Execute
        } otherwise {
            CPUStateNext := CPUStates.S0_Fetch
        }
    }elsewhen(CPUState === CPUStates.S3_INT) {
        when(!io.DMA_In_n || !io.DMA_Out_n) {
            CPUStateNext := CPUStates.S2_DMA
        } otherwise {
            CPUStateNext := CPUStates.S0_Fetch
        }
    }elsewhen(CPUState === CPUStates.S2_DMA) {
        when(!io.DMA_In_n || !io.DMA_Out_n) {
            CPUStateNext := CPUStates.S2_DMA
        } elsewhen(!io.Interrupt_n && Registers.io.IE) {
            CPUStateNext := CPUStates.S3_INT
        } otherwise {
            CPUStateNext := CPUStates.S0_Fetch
        }
    }otherwise{
        CPUStateNext := CPUStates.S1_Reset
    } 

    when(StateCounter === 7) {
        CPUState := CPUStateNext
        LongExec := LongExecNext
        PowerUp := True
    }
}

object new1802SpinalConfig extends SpinalConfig(
    targetDirectory = ".",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object new1802Gen {
    def main(args: Array[String]) {
        new1802SpinalConfig.generateVerilog(new new1802).printPruned
    }
}