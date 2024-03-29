package Cards

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.core.sim._

import java.io._
import scala.util.control.Breaks

class FDC_Card extends Component
{
    val io = new Bundle {
        val Addr16 = in Bits(16 bit)
        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)
        val MRD = in Bool()
        val MWR = in Bool()
        val TPB = in Bool()
        val N = in Bits(3 bit)
        val Q = in Bool()
        val EF4_ = out Bool()
        val ExtRom = out Bool()
        val FDCRom = new Bundle {
            val DataIn = in Bits(8 bit)
            val Addr = out Bits(12 bit)
        }
    }

    val F3_Latch = Reg(Bits(8 bits)) init(0)
    val F5_addr = F3_Latch(1 downto 0)

    val FDC_S0_Busy = Reg(Bool()) init(False)
    val FDC_S1_DRQ = Reg(Bool()) init(False)
    val FDC_S2_LostData = Reg(Bool()) init(False)
    val FDC_S3_CRC_Error = Reg(Bool()) init(False)
    val FDC_S4_RNF = Reg(Bool()) init(False)
    val FDC_S5_WriteFault = Reg(Bool()) init(False)
    val FDC_S6_WriteProtect = Reg(Bool()) init(False)
    val FDC_S7_NotReady = Reg(Bool()) init(False)

    val FDC_INTRQ = Reg(Bool()) init(False)

    val FDC_Status = FDC_S7_NotReady ## 
                     FDC_S6_WriteProtect ## 
                     FDC_S5_WriteFault ## 
                     FDC_S4_RNF ## 
                     FDC_S3_CRC_Error ##
                     FDC_S2_LostData ##
                     FDC_S1_DRQ ##
                     FDC_S0_Busy

    val FDC_Command = Reg(UInt(8 bits)) init(0)
    val FDC_Track = Reg(Bits(8 bits)) init(0)
    val FDC_Sector = Reg(Bits(8 bits)) init(0)
    val FDC_Data = Reg(Bits(8 bits)) init(0)

    val FDC_Read_Status = False
    val FDC_Read_Track = False
    val FDC_Read_Sector = False
    val FDC_Read_Data = False
    
    val FDC_Write_Status = False
    val FDC_Write_Track = False
    val FDC_Write_Sector = False
    val FDC_Write_Data = False
    
    val FDC_Command_Loaded = False
    val FDC_CMD_Loaded = RegNext(FDC_Command_Loaded)

    val FDC_Data_Loaded = RegNext(Bool()) init(False)

    val FDC_Command_Type = FDC_Command(7 downto 4)

    val IndexPulseCounter = CounterFreeRun(100000)
    val IndexPulseCount = Counter(10)

    when(IndexPulseCounter.willOverflowIfInc)
    {
        IndexPulseCount.increment()
    }

    io.DataOut := 0
    io.ExtRom := False
    io.FDCRom.Addr := 0
    io.EF4_ := !(FDC_S1_DRQ && !F3_Latch(4)) 

    when(!io.MRD && io.TPB && io.N === 2){
        when(io.Q){
            F3_Latch := io.DataIn
        }elsewhen(!io.Q){
            when(F5_addr === 0){
                FDC_Command := io.DataIn.asUInt
                FDC_Command_Loaded := True
            }elsewhen(F5_addr === 1){
                FDC_Track := io.DataIn
                FDC_Write_Track := True
            }elsewhen(F5_addr === 2){
                FDC_Sector := io.DataIn
                FDC_Write_Sector := True
            }elsewhen(F5_addr === 3){
                FDC_Data := io.DataIn
                FDC_Data_Loaded := True
                FDC_Write_Data := True
            }
        }
    }elsewhen(!io.MRD && io.Addr16.asUInt >= 0x0D00 && io.Addr16.asUInt <= 0x0DFF){
        io.ExtRom := True
        io.DataOut := io.FDCRom.DataIn
        io.FDCRom.Addr := (io.Addr16.asUInt - 0xC87).asBits(11 downto 0)
    }elsewhen(!io.MRD && io.Addr16.asUInt >= 0xC000 && io.Addr16.asUInt <= 0xDFFF){
        io.ExtRom := True
        io.DataOut := io.FDCRom.DataIn
        io.FDCRom.Addr := (io.Addr16.asUInt - 0xC000).asBits(11 downto 0)
    }

    when(io.MRD){
        when(io.Q && io.N === 2){
            io.DataOut := B"7'h00" ## FDC_INTRQ
        }elsewhen(!io.Q && io.N === 2){
            when(F5_addr === 0){
                io.DataOut := FDC_Status
                FDC_Read_Status := True
            }elsewhen(F5_addr === 1){
                io.DataOut := FDC_Track
                FDC_Read_Track := True
            }elsewhen(F5_addr === 2){
                io.DataOut := FDC_Sector
                FDC_Read_Sector := True
            }elsewhen(F5_addr === 3){
                io.DataOut := FDC_Data
                FDC_Read_Data := True
            }
        }
    }
    val fsm = new StateMachine
	{
        val StartingUp: State = new State with EntryPoint 
        {
            whenIsActive{
                FDC_S7_NotReady := False
                FDC_S6_WriteProtect := False
                FDC_S5_WriteFault := False
                FDC_S4_RNF := False
                FDC_S3_CRC_Error := False
                FDC_S2_LostData := False
                FDC_S1_DRQ := False
                FDC_S0_Busy := False

                FDC_Data_Loaded := False
                FDC_INTRQ := False
                goto(Wait4CMD)
            }
        }

        val Wait4CMD: State = new State
        {
            whenIsActive
            {
                FDC_S0_Busy := False
                when(FDC_CMD_Loaded.rise())
                {
                    IndexPulseCounter.clear()
                    IndexPulseCount.clear()
                    when(FDC_Command === 0xF4){
                        FDC_Data_Loaded := False
                        goto(Write_Track_Start)
                    }
                }
            }
        }

        val Write_Track_Start: State = new State
        {
            whenIsActive
            {
                FDC_S0_Busy := True
                FDC_S1_DRQ := True
                FDC_S2_LostData := False
                FDC_S4_RNF := False
                FDC_S5_WriteFault := False
                FDC_S6_WriteProtect := False

                FDC_INTRQ := False
                when(IndexPulseCounter.willOverflowIfInc && IndexPulseCount > 1 && !FDC_Data_Loaded){
                    goto(Write_Track_Failed)
                }
            }
        }

        val Write_Track_Failed: State = new State
        {
            whenIsActive
            {
                FDC_S2_LostData := True
                FDC_INTRQ := True
                goto(Wait4CMD)
            }
        }
    }
}
