package comx35
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import Config_ECP5_CLI5v6._
import scala.util.control.Breaks
import scala.collection.mutable.ArrayBuffer

class PWM_Sound extends Component{
    val io = new Bundle {
        val i_func = in SInt(16 bits)
        val o_DAC = out Bool()
    }

    val this_bit = Reg(Bool()) init(False)
    val DAC_acc_1st = Reg(UInt(20 bits)) init(0)
    val DAC_acc_2nd = Reg(UInt(20 bits)) init(0)
    val i_func_extended = io.i_func.resize(19)

    io.o_DAC := this_bit

    when(this_bit){
        DAC_acc_1st := DAC_acc_1st + i_func_extended.asUInt - (U"20'h8000");//2**15
        DAC_acc_2nd := DAC_acc_2nd + DAC_acc_1st     - (U"20'h8000");//2**15
    }otherwise{
        DAC_acc_1st := DAC_acc_1st + i_func_extended.asUInt + (U"20'h8000");
        DAC_acc_2nd := DAC_acc_2nd + DAC_acc_1st + (U"20'h8000");
    }

    this_bit := !DAC_acc_2nd(19);
}


class DSA_Two(idw:Int = 16, osr:Int = 6) extends Component
{

    val io = new Bundle {
        val i_data = in SInt(idw.toInt bits)
        val dout = out Bool()
    }

    val mid_val = (1<<idw-1) | (1<<osr+2)
    println("mid_val:" + mid_val)

    val max_val = mid_val.intoSInt.resize(dw_tot)
    val min_val = -mid_val.intoSInt.resize(dw_tot)

    var dw_ext = 2
    var dw_tot = idw + dw_ext
    
    val dout_r = Reg(Bool()) init(False)
    val dac_dout = Reg(Bool()) init(False)
    
    val DAC_acc_1st = Reg(SInt(dw_tot bits)) init(0)

    val dac_val = (dout_r) ? (min_val) | (max_val)

    val in_ext = io.i_data.resize(dw_ext+idw)
    val delta_s0_c0 = in_ext.resize(dw_tot) + dac_val.resize(dw_tot)

    val delta_s0_c1 = DAC_acc_1st.resize(dw_tot) + delta_s0_c0.resize(dw_tot)

    DAC_acc_1st := delta_s0_c1;

    dout_r := delta_s0_c1(dw_tot-1);
    dac_dout	:= ~dout_r;

    // //---2nd---//
    // var dw_tot2 = dw_tot + osr;
    // val DAC_acc_2nd = Reg(SInt(dw_tot2 bits)) init(0)

	// val max_val2 = mid_val.intoSInt.resize(dw_tot2)
	// val min_val2 = -mid_val.intoSInt.resize(dw_tot2)
	// val dac_val2 = (dout_r) ? min_val2 | max_val2

    // val in_ext2 = delta_s0_c1.resize(osr+idw)
	// val	delta_s1_c0 = in_ext2.resize(dw_tot2) + dac_val2.resize(dw_tot2);
	// val	delta_s1_c1 = DAC_acc_2nd.resize(dw_tot2) + delta_s1_c0.resize(dw_tot2);

    // DAC_acc_2nd := delta_s1_c1;

    // dout_r		:= delta_s1_c1(dw_tot2-1);
    // dac_dout	:= ~dout_r;

    // io.dout_2nd := dac_dout
}

class Darkknight_DAC() extends Component
{
/**
 * @file sigma_delta_dac_lowpass_first_order.sv
 *
 * @author Alex Lao <lao.alex.512@gmail.com>
 * @date   2023
 *
 * @brief First Order Delta Sigma DAC
 */

    val io = new Bundle {
        val dac_in = in Bits(16 bits)
        val dac_out = out Bool()
    }

    // SECTION: Signals

    val dac_accumulator = Reg(UInt( 18 bits)) init(0)

    // SECTION: Logic

    val dac_in_extended = io.dac_in(15) ## io.dac_in(15) ## io.dac_in;
    when (io.dac_out){
        dac_accumulator := dac_accumulator + dac_in_extended.asUInt - (1<<15);
    }otherwise{
        dac_accumulator := dac_accumulator + dac_in_extended.asUInt + (1<<15);
    }

    io.dac_out := !dac_accumulator(17);
}

class Comb(idw:Int=8, odw:Int=9, g:Int=1) extends Component
{
    val io = new Bundle {
        val i_div = in Bool()
        val i_data = in SInt(idw bits)
        val o_data = out SInt(odw bits)
    }

    val data_reg = Vec(Reg(SInt(idw bits)) init(0), g+1)
    when(io.i_div){
        data_reg(0) := io.i_data
        for(i <- 1 to g){
            data_reg(i) := data_reg(i-1)
        }
    }
    io.o_data := (io.i_data - data_reg(g-1)).resize(odw)
}

class Integrator(idw:Int=8, odw:Int=9) extends Component
{
    val io = new Bundle {
        val i_data = in SInt(idw bits)
        val o_data = out SInt(odw bits)
    }

    val data_out = Reg(SInt(odw bits)) init(0)

    data_out := (data_out + io.i_data)
    io.o_data := data_out.resize(odw)
}

class Downsampler(dw:Int=8, r:Int=4) extends Component
{
    val io = new Bundle {
        val i_data = in SInt(dw bits)
        val o_data = out SInt(dw bits)
        val div = out Bool()
    }

    val counter =  Counter(r-1)
    val data_out = Reg(SInt(dw bits)) init(0)

    counter.increment()
    io.div := counter.willOverflowIfInc
    when(counter.willOverflowIfInc){
        data_out := io.i_data 
    }
    io.o_data := data_out
}

class CIC_Interpolation(DataWidth:Int=8, ChainLength:Int = 4, Ratio:Int=4,  DifferentialDelay:Int = 1) extends Component
{
    var odw = DataWidth+log2Up(Math.pow(Ratio, ChainLength).toInt / Ratio)+1
    var udw = DataWidth + ChainLength
    println("odw:"+ odw + "  udw:"+ udw)
    val io = new Bundle {
        val i_data = in SInt(DataWidth bits)
        val o_data = out SInt(odw bits)
        val i_div = in Bool()
    }

    val Combs = new ArrayBuffer[Comb]()
    var Comb_odw = 0;
    for(i <- 0 to ChainLength-1){
        if(i == ChainLength-1){
            Comb_odw = DataWidth+i;
        }else{
            Comb_odw = DataWidth+i+1;
        }
        Combs += new Comb(DataWidth+i, Comb_odw, DifferentialDelay)
        if(i!=0){
            Combs(i).io.i_data := Combs(i-1).io.o_data.resized
        }else{
            Combs(i).io.i_data := io.i_data
        }
        Combs(i).io.i_div := io.i_div
    }

    val upsample = (io.i_div) ? Combs(ChainLength-1).io.o_data | 0

    val Integrators = new ArrayBuffer[Integrator]()
    
    var Int_odw = 0;
    var Int_idw = 0;
    for(i <- 0 to ChainLength-1){
        if(i == 0){
            Int_idw = DataWidth+ChainLength-1
        }else{
            Int_idw = Int_odw
        }
        Int_odw = DataWidth+log2Up((Math.pow(2, ChainLength - i).toInt * Math.pow(Ratio, i+1).toInt) / Ratio)

        Integrators += new Integrator(Int_idw, Int_odw)
        if(i==0){
            Integrators(i).io.i_data := upsample 
        } else {
            Integrators(i).io.i_data := Integrators(i-1).io.o_data
        }
    }

    io.o_data := Integrators(ChainLength-1).io.o_data
}

class CIC_Interpolation_Test(DataWidthIn:Int=8, DataWidthOut:Int=8) extends Component
{
    val io = new Bundle {
        val i_data = in SInt(DataWidthIn bits)
        val o_data = out SInt(DataWidthOut bits)
        val tone = in SInt(32 bits)
        val sound_dac_p = out Bool()
        val sound_dac_n = out Bool()
    }

    val dac = new Darkknight_DAC()

    val cic = new  CIC_Interpolation(DataWidthIn,4,8,2)
    val downs = new Downsampler(DataWidthIn, 8)

    downs.io.i_data := io.i_data.resize(DataWidthIn);
    cic.io.i_div := downs.io.div
    cic.io.i_data := downs.io.o_data
    dac.io.dac_in := cic.io.o_data.resize(16).asBits

    io.o_data := cic.io.o_data.resize(DataWidthOut)
    io.sound_dac_p := dac.io.dac_out
    io.sound_dac_n := !dac.io.dac_out
}

object CIC_Interpolation_sim {
    def main(args: Array[String]) {
        SimConfig.withFstWave.compile{
            val dut = new CIC_Interpolation_Test(8,16)
            dut
        }.doSim { dut =>
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)
            dut.io.i_data #= 0
            var c = 0;
            var cc = 0;
            var t = 0.0
            var s = 0.1
            var ss = 0.1

            var tone = 100000
            dut.io.tone #= tone
            val loop = new Breaks;
            loop.breakable {
                while (true) {
                    dut.clockDomain.waitRisingEdge()

                    t = c.toFloat / 25000000.0
                    s = Math.sin((2 * Math.PI * tone.toFloat * t))*127

                    // if(c % tone == 0){
                    //     if(tone>1000){
                    //         tone -= 1000
                    //         dut.io.tone #= tone
                    //     }
                    // }

                    // if(c > cc){
                    //     if(s>0){
                    //         s = -(1<<2)
                    //     }else{
                    //         s = (1<<2)
                    //     }
                    //     cc = c + (25000000/tone)
                    // }

                    dut.io.i_data #= (s).toInt
  
                    c += 1
                    if(c > 99999){
                        loop.break;
                    }
                }
            }
        }
    }
}