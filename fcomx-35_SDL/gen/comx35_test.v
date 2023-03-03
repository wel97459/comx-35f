// Generator : SpinalHDL v1.7.1    git head : 0444bb76ab1d6e19f0ec46bc03c4769776deb7d5
// Component : comx35_test
// Git hash  : 445cde4c686f22076d303b83a3f0b9aea0fc7699

`timescale 1ns/1ps

module comx35_test (
  output     [15:0]   io_Addr16,
  output     [7:0]    io_DataOut,
  input      [7:0]    io_DataIn,
  output              io_MRD,
  output              io_MWR,
  output     [9:0]    io_PMA,
  output              io_PMWR_,
  input      [7:0]    io_PMD_In,
  output     [7:0]    io_PMD_Out,
  output     [9:0]    io_CMA,
  output              io_CMWR_,
  input      [7:0]    io_CMD_In,
  output     [7:0]    io_CMD_Out,
  output              io_CMA3_PMA10,
  input               io_Start,
  output              io_HSync_,
  output              io_VSync_,
  output              io_Display_,
  output     [2:0]    io_Color,
  output              io_Pixel,
  output              io_Sync,
  input               io_KBD_Latch,
  input      [7:0]    io_KBD_KeyCode,
  output              io_KBD_Ready,
  output              io_Q,
  input               io_Tape_in,
  output     [3:0]    io_Sound,
  input               clk,
  input               reset
);

  wire       [7:0]    vis69_io_DataIn;
  wire       [5:0]    vis70_io_CDB_in;
  wire       [5:0]    vis70_io_CDB_out;
  wire       [2:0]    vis70_io_CCB_in;
  wire       [2:0]    vis70_io_CCB_out;
  wire       [3:0]    clockedArea_CPU_io_EF_n;
  wire                vis69_io_N3_;
  wire       [7:0]    vis69_io_DataOut;
  wire                vis69_io_CMWR_;
  wire                vis69_io_CMSEL;
  wire       [2:0]    vis69_io_CMA;
  wire                vis69_io_CMA3_PMA10;
  wire                vis69_io_PMWR_;
  wire                vis69_io_PMSEL;
  wire       [9:0]    vis69_io_PMA;
  wire       [3:0]    vis69_io_Sound;
  wire                vis70_io_HSync_;
  wire                vis70_io_VSync_;
  wire                vis70_io_Display_;
  wire                vis70_io_PreDisplay_;
  wire                vis70_io_AddSTB_;
  wire                vis70_io_CPUCLK;
  wire       [7:0]    vis70_io_DataOut;
  wire                vis70_io_CompSync_;
  wire                vis70_io_Pixel;
  wire       [2:0]    vis70_io_Color;
  wire                vis70_io_Burst;
  wire                kbd71_io_Ready;
  wire                kbd71_io_DA_;
  wire                kbd71_io_RPT_;
  wire       [7:0]    kbd71_io_DataOut;
  wire                kbd71_io_KBD_SEL;
  wire                clockedArea_CPU_io_Q;
  wire       [1:0]    clockedArea_CPU_io_SC;
  wire       [2:0]    clockedArea_CPU_io_N;
  wire                clockedArea_CPU_io_TPA;
  wire                clockedArea_CPU_io_TPB;
  wire                clockedArea_CPU_io_MRD;
  wire                clockedArea_CPU_io_MWR;
  wire       [7:0]    clockedArea_CPU_io_Addr;
  wire       [15:0]   clockedArea_CPU_io_Addr16;
  wire       [7:0]    clockedArea_CPU_io_DataOut;
  wire                clockedArea_newClockEnable;
  reg                 NTSC_PAL_FlipFlop;
  reg                 INT_FF;
  wire       [2:0]    DataInSel;
  reg                 vis70_io_PreDisplay__regNext;
  wire                when_comx35_l128;
  wire                when_comx35_l130;
  reg        [7:0]    _zz_io_DataIn;

  CDP1869 vis69 (
    .io_HSync_     (vis70_io_HSync_             ), //i
    .io_Display_   (vis70_io_Display_           ), //i
    .io_AddSTB_    (vis70_io_AddSTB_            ), //i
    .io_N          (clockedArea_CPU_io_N[2:0]   ), //i
    .io_N3_        (vis69_io_N3_                ), //o
    .io_TPA        (clockedArea_CPU_io_TPA      ), //i
    .io_TPB        (clockedArea_CPU_io_TPB      ), //i
    .io_MRD        (clockedArea_CPU_io_MRD      ), //i
    .io_MWR        (clockedArea_CPU_io_MWR      ), //i
    .io_Addr       (clockedArea_CPU_io_Addr[7:0]), //i
    .io_DataIn     (vis69_io_DataIn[7:0]        ), //i
    .io_DataOut    (vis69_io_DataOut[7:0]       ), //o
    .io_CMWR_      (vis69_io_CMWR_              ), //o
    .io_CMSEL      (vis69_io_CMSEL              ), //o
    .io_CMA        (vis69_io_CMA[2:0]           ), //o
    .io_CMA3_PMA10 (vis69_io_CMA3_PMA10         ), //o
    .io_PMWR_      (vis69_io_PMWR_              ), //o
    .io_PMSEL      (vis69_io_PMSEL              ), //o
    .io_PMA        (vis69_io_PMA[9:0]           ), //o
    .io_Sound      (vis69_io_Sound[3:0]         ), //o
    .clk           (clk                         ), //i
    .reset         (reset                       )  //i
  );
  CDP1870 vis70 (
    .io_HSync_      (vis70_io_HSync_                ), //o
    .io_VSync_      (vis70_io_VSync_                ), //o
    .io_Display_    (vis70_io_Display_              ), //o
    .io_PreDisplay_ (vis70_io_PreDisplay_           ), //o
    .io_AddSTB_     (vis70_io_AddSTB_               ), //o
    .io_CPUCLK      (vis70_io_CPUCLK                ), //o
    .io_CDB_in      (vis70_io_CDB_in[5:0]           ), //i
    .io_CDB_out     (vis70_io_CDB_out[5:0]          ), //i
    .io_CCB_in      (vis70_io_CCB_in[2:0]           ), //i
    .io_CCB_out     (vis70_io_CCB_out[2:0]          ), //i
    .io_DataIn      (clockedArea_CPU_io_DataOut[7:0]), //i
    .io_DataOut     (vis70_io_DataOut[7:0]          ), //o
    .io_N3_         (vis69_io_N3_                   ), //i
    .io_TPB         (clockedArea_CPU_io_TPB         ), //i
    .io_MRD         (clockedArea_CPU_io_MRD         ), //i
    .io_CMSEL       (vis69_io_CMSEL                 ), //i
    .io_PalOrNTSC   (1'b0                           ), //i
    .io_CompSync_   (vis70_io_CompSync_             ), //o
    .io_Pixel       (vis70_io_Pixel                 ), //o
    .io_Color       (vis70_io_Color[2:0]            ), //o
    .io_Burst       (vis70_io_Burst                 ), //o
    .clk            (clk                            ), //i
    .reset          (reset                          )  //i
  );
  CDP1871 kbd71 (
    .io_Latch   (io_KBD_Latch          ), //i
    .io_KeyCode (io_KBD_KeyCode[7:0]   ), //i
    .io_Ready   (kbd71_io_Ready        ), //o
    .io_TPB     (clockedArea_CPU_io_TPB), //i
    .io_MRD_    (clockedArea_CPU_io_MRD), //i
    .io_N3_     (vis69_io_N3_          ), //i
    .io_DA_     (kbd71_io_DA_          ), //o
    .io_RPT_    (kbd71_io_RPT_         ), //o
    .io_DataOut (kbd71_io_DataOut[7:0] ), //o
    .io_KBD_SEL (kbd71_io_KBD_SEL      ), //o
    .clk        (clk                   ), //i
    .reset      (reset                 )  //i
  );
  Spinal1802 clockedArea_CPU (
    .io_Wait_n                  (io_Start                       ), //i
    .io_Clear_n                 (io_Start                       ), //i
    .io_DMA_In_n                (1'b1                           ), //i
    .io_DMA_Out_n               (1'b1                           ), //i
    .io_Interrupt_n             (INT_FF                         ), //i
    .io_EF_n                    (clockedArea_CPU_io_EF_n[3:0]   ), //i
    .io_Q                       (clockedArea_CPU_io_Q           ), //o
    .io_SC                      (clockedArea_CPU_io_SC[1:0]     ), //o
    .io_N                       (clockedArea_CPU_io_N[2:0]      ), //o
    .io_TPA                     (clockedArea_CPU_io_TPA         ), //o
    .io_TPB                     (clockedArea_CPU_io_TPB         ), //o
    .io_MRD                     (clockedArea_CPU_io_MRD         ), //o
    .io_MWR                     (clockedArea_CPU_io_MWR         ), //o
    .io_Addr                    (clockedArea_CPU_io_Addr[7:0]   ), //o
    .io_Addr16                  (clockedArea_CPU_io_Addr16[15:0]), //o
    .io_DataIn                  (_zz_io_DataIn[7:0]             ), //i
    .io_DataOut                 (clockedArea_CPU_io_DataOut[7:0]), //o
    .clk                        (clk                            ), //i
    .reset_1                    (reset                          ), //i
    .clockedArea_newClockEnable (clockedArea_newClockEnable     )  //i
  );
  assign clockedArea_newClockEnable = (1'b1 && vis70_io_CPUCLK);
  assign DataInSel = {{kbd71_io_KBD_SEL,vis69_io_CMSEL},vis69_io_PMSEL};
  assign io_PMA = vis69_io_PMA;
  assign io_PMWR_ = vis69_io_PMWR_;
  assign io_PMD_Out = clockedArea_CPU_io_DataOut;
  assign io_CMA = {io_PMD_In[6 : 0],vis69_io_CMA[2 : 0]};
  assign io_CMWR_ = vis69_io_CMWR_;
  assign io_CMD_Out = clockedArea_CPU_io_DataOut;
  assign vis70_io_CDB_in = io_CMD_In[5 : 0];
  assign vis70_io_CCB_in = {io_PMD_In[7],io_CMD_In[7 : 6]};
  assign clockedArea_CPU_io_EF_n = {{{io_Tape_in,kbd71_io_DA_},((! NTSC_PAL_FlipFlop) && kbd71_io_RPT_)},vis70_io_PreDisplay_};
  assign when_comx35_l128 = (vis70_io_PreDisplay_ && (! vis70_io_PreDisplay__regNext));
  assign when_comx35_l130 = (((clockedArea_CPU_io_SC == 2'b11) && clockedArea_CPU_io_TPA) && (! NTSC_PAL_FlipFlop));
  always @(*) begin
    case(DataInSel)
      3'b001 : begin
        _zz_io_DataIn = io_PMD_In;
      end
      3'b010 : begin
        _zz_io_DataIn = io_CMD_In;
      end
      3'b100 : begin
        _zz_io_DataIn = kbd71_io_DataOut;
      end
      default : begin
        _zz_io_DataIn = io_DataIn;
      end
    endcase
  end

  assign io_DataOut = clockedArea_CPU_io_DataOut;
  assign io_Addr16 = clockedArea_CPU_io_Addr16;
  assign io_MRD = clockedArea_CPU_io_MRD;
  assign io_MWR = clockedArea_CPU_io_MWR;
  assign io_HSync_ = vis70_io_HSync_;
  assign io_VSync_ = vis70_io_VSync_;
  assign io_Display_ = vis70_io_Display_;
  assign io_Pixel = vis70_io_Pixel;
  assign io_Color = vis70_io_Color;
  assign io_Sync = vis70_io_CompSync_;
  assign io_KBD_Ready = kbd71_io_Ready;
  assign io_Q = clockedArea_CPU_io_Q;
  assign io_CMA3_PMA10 = vis69_io_CMA3_PMA10;
  assign io_Sound = vis69_io_Sound;
  always @(posedge clk) begin
    if(reset) begin
      NTSC_PAL_FlipFlop <= 1'b1;
      NTSC_PAL_FlipFlop <= 1'b1;
      INT_FF <= 1'b1;
    end else begin
      if(clockedArea_CPU_io_Q) begin
        NTSC_PAL_FlipFlop <= 1'b0;
      end
      if(when_comx35_l128) begin
        INT_FF <= NTSC_PAL_FlipFlop;
      end else begin
        if(when_comx35_l130) begin
          INT_FF <= 1'b1;
        end
      end
    end
  end

  always @(posedge clk) begin
    vis70_io_PreDisplay__regNext <= vis70_io_PreDisplay_;
  end


endmodule

module Spinal1802 (
  input               io_Wait_n,
  input               io_Clear_n,
  input               io_DMA_In_n,
  input               io_DMA_Out_n,
  input               io_Interrupt_n,
  input      [3:0]    io_EF_n,
  output              io_Q,
  output     [1:0]    io_SC,
  output     [2:0]    io_N,
  output              io_TPA,
  output              io_TPB,
  output              io_MRD,
  output              io_MWR,
  output reg [7:0]    io_Addr,
  output     [15:0]   io_Addr16,
  input      [7:0]    io_DataIn,
  output     [7:0]    io_DataOut,
  input               clk,
  input               reset_1,
  input               clockedArea_newClockEnable
);
  localparam CPUModes_Load = 2'd0;
  localparam CPUModes_Reset = 2'd1;
  localparam CPUModes_Pause = 2'd2;
  localparam CPUModes_Run = 2'd3;
  localparam RegSelectModes_PSel = 3'd0;
  localparam RegSelectModes_NSel = 3'd1;
  localparam RegSelectModes_XSel = 3'd2;
  localparam RegSelectModes_DMA0 = 3'd3;
  localparam RegSelectModes_Stack2 = 3'd4;
  localparam RegOperationModes_None = 4'd0;
  localparam RegOperationModes_Inc = 4'd1;
  localparam RegOperationModes_Dec = 4'd2;
  localparam RegOperationModes_LoadUpper = 4'd3;
  localparam RegOperationModes_LoadLower = 4'd4;
  localparam RegOperationModes_UpperOnBus = 4'd5;
  localparam RegOperationModes_LowerOnBus = 4'd6;
  localparam RegOperationModes_LoadTemp = 4'd7;
  localparam RegOperationModes_LoadJump = 4'd8;
  localparam ExecuteModes_None = 4'd0;
  localparam ExecuteModes_Load = 4'd1;
  localparam ExecuteModes_LoadDec = 4'd2;
  localparam ExecuteModes_LoadNoInc = 4'd3;
  localparam ExecuteModes_Write = 4'd4;
  localparam ExecuteModes_WriteDec = 4'd5;
  localparam ExecuteModes_WriteNoInc = 4'd6;
  localparam ExecuteModes_LongLoad = 4'd7;
  localparam ExecuteModes_LongContinue = 4'd8;
  localparam ExecuteModes_DMA_In = 4'd9;
  localparam ExecuteModes_DMA_Out = 4'd10;
  localparam BusControlModes_DataIn = 3'd0;
  localparam BusControlModes_DReg = 3'd1;
  localparam BusControlModes_TReg = 3'd2;
  localparam BusControlModes_PXReg = 3'd3;
  localparam BusControlModes_RLower = 3'd4;
  localparam BusControlModes_RUpper = 3'd5;
  localparam DRegControlModes_None = 4'd0;
  localparam DRegControlModes_BusIn = 4'd1;
  localparam DRegControlModes_ALU_OR = 4'd2;
  localparam DRegControlModes_ALU_XOR = 4'd3;
  localparam DRegControlModes_ALU_AND = 4'd4;
  localparam DRegControlModes_ALU_RSH = 4'd5;
  localparam DRegControlModes_ALU_LSH = 4'd6;
  localparam DRegControlModes_ALU_RSHR = 4'd7;
  localparam DRegControlModes_ALU_LSHR = 4'd8;
  localparam DRegControlModes_ALU_Add = 4'd9;
  localparam DRegControlModes_ALU_AddCarry = 4'd10;
  localparam DRegControlModes_ALU_SubD = 4'd11;
  localparam DRegControlModes_ALU_SubDBorrow = 4'd12;
  localparam DRegControlModes_ALU_SubM = 4'd13;
  localparam DRegControlModes_ALU_SubMBorrow = 4'd14;
  localparam CoreFMS_enumDef_BOOT = 3'd0;
  localparam CoreFMS_enumDef_S1_Reset = 3'd1;
  localparam CoreFMS_enumDef_S1_Init = 3'd2;
  localparam CoreFMS_enumDef_S0_Fetch = 3'd3;
  localparam CoreFMS_enumDef_S1_Execute = 3'd4;
  localparam CoreFMS_enumDef_S2_DMA = 3'd5;
  localparam CoreFMS_enumDef_S3_INT = 3'd6;

  wire       [2:0]    _zz_StateCounter_valueNext;
  wire       [0:0]    _zz_StateCounter_valueNext_1;
  reg        [15:0]   _zz__zz_A;
  wire       [3:0]    _zz_when_Spinal1802_l189;
  wire       [3:0]    _zz_when_Spinal1802_l189_1;
  wire       [8:0]    _zz_ALU_Add;
  wire       [8:0]    _zz_ALU_Add_1;
  wire       [8:0]    _zz_ALU_SubD;
  wire       [8:0]    _zz_ALU_SubD_1;
  wire       [8:0]    _zz_ALU_SubM;
  wire       [8:0]    _zz_ALU_SubM_1;
  wire       [7:0]    _zz_D;
  wire       [7:0]    _zz_D_1;
  reg        [1:0]    SC;
  reg                 Q;
  reg                 TPA;
  reg                 TPB;
  reg                 MRD;
  reg                 MWR;
  reg                 StateCounter_willIncrement;
  reg                 StateCounter_willClear;
  reg        [2:0]    StateCounter_valueNext;
  reg        [2:0]    StateCounter_value;
  wire                StateCounter_willOverflowIfInc;
  wire                StateCounter_willOverflow;
  reg                 StartCounting;
  reg        [1:0]    Mode;
  reg        [2:0]    RegSelMode;
  reg        [3:0]    RegOpMode;
  reg        [3:0]    ExeMode;
  reg        [2:0]    BusControl;
  reg        [3:0]    DRegControl;
  reg        [15:0]   Addr;
  reg        [15:0]   Addr16;
  reg        [7:0]    D;
  reg        [7:0]    Dlast;
  reg        [2:0]    outN;
  reg        [3:0]    N;
  reg        [3:0]    I;
  reg        [3:0]    P;
  reg        [3:0]    X;
  reg        [7:0]    T;
  reg        [3:0]    EF;
  reg                 IE;
  reg                 DF;
  reg                 DFLast;
  reg        [7:0]    OP;
  reg                 Idle;
  reg                 Reset;
  reg                 Branch;
  wire                Skip;
  reg        [7:0]    TmpUpper;
  wire       [8:0]    ALU_Add;
  wire       [8:0]    ALU_AddCarry;
  wire       [8:0]    ALU_SubD;
  wire       [8:0]    ALU_SubM;
  wire       [8:0]    ALU_SubDB;
  wire       [8:0]    ALU_SubMB;
  reg        [7:0]    Bus_1;
  wire       [15:0]   A;
  reg        [3:0]    RSel;
  reg        [15:0]   R_0;
  reg        [15:0]   R_1;
  reg        [15:0]   R_2;
  reg        [15:0]   R_3;
  reg        [15:0]   R_4;
  reg        [15:0]   R_5;
  reg        [15:0]   R_6;
  reg        [15:0]   R_7;
  reg        [15:0]   R_8;
  reg        [15:0]   R_9;
  reg        [15:0]   R_10;
  reg        [15:0]   R_11;
  reg        [15:0]   R_12;
  reg        [15:0]   R_13;
  reg        [15:0]   R_14;
  reg        [15:0]   R_15;
  wire       [15:0]   _zz_A;
  wire       [15:0]   _zz_1;
  wire                _zz_2;
  wire                _zz_3;
  wire                _zz_4;
  wire                _zz_5;
  wire                _zz_6;
  wire                _zz_7;
  wire                _zz_8;
  wire                _zz_9;
  wire                _zz_10;
  wire                _zz_11;
  wire                _zz_12;
  wire                _zz_13;
  wire                _zz_14;
  wire                _zz_15;
  wire                _zz_16;
  wire                _zz_17;
  wire                when_Spinal1802_l124;
  wire                when_Spinal1802_l129;
  wire                when_Spinal1802_l131;
  wire                when_Spinal1802_l133;
  wire                when_Spinal1802_l138;
  wire                when_Spinal1802_l142;
  wire                when_Spinal1802_l147;
  wire                when_Spinal1802_l149;
  wire                when_Spinal1802_l151;
  wire                when_Spinal1802_l153;
  wire       [15:0]   _zz_R_0;
  wire       [15:0]   _zz_R_0_1;
  wire       [15:0]   _zz_R_0_2;
  wire       [15:0]   _zz_R_0_3;
  wire       [15:0]   _zz_R_0_4;
  wire                when_Spinal1802_l160;
  wire                when_Spinal1802_l162;
  wire                when_Spinal1802_l164;
  wire                when_Spinal1802_l166;
  wire                when_Spinal1802_l168;
  wire                when_Spinal1802_l170;
  wire                when_Spinal1802_l177;
  wire                when_Spinal1802_l181;
  wire                when_Spinal1802_l186;
  wire                when_Spinal1802_l187;
  wire                when_Spinal1802_l189;
  wire                when_Spinal1802_l199;
  wire                when_Spinal1802_l203;
  wire                when_Spinal1802_l219;
  wire                when_Spinal1802_l221;
  wire                when_Spinal1802_l223;
  wire                when_Spinal1802_l225;
  wire                when_Spinal1802_l227;
  wire                when_Spinal1802_l230;
  wire                when_Spinal1802_l233;
  wire                when_Spinal1802_l236;
  wire                when_Spinal1802_l239;
  wire                when_Spinal1802_l242;
  wire                when_Spinal1802_l245;
  wire                when_Spinal1802_l248;
  wire                when_Spinal1802_l251;
  wire                when_Spinal1802_l254;
  wire                when_Spinal1802_l260;
  wire                when_Spinal1802_l262;
  wire                when_Spinal1802_l264;
  wire                when_Spinal1802_l266;
  wire                when_Spinal1802_l268;
  wire                when_Spinal1802_l270;
  wire                when_Spinal1802_l275;
  wire                when_Spinal1802_l277;
  wire                when_Spinal1802_l279;
  wire                when_Spinal1802_l281;
  wire                when_Spinal1802_l283;
  wire                when_Spinal1802_l285;
  wire                when_Spinal1802_l287;
  wire                when_Spinal1802_l289;
  wire                when_Spinal1802_l291;
  wire                when_Spinal1802_l293;
  wire                CoreFMS_wantExit;
  reg                 CoreFMS_wantStart;
  wire                CoreFMS_wantKill;
  reg        [3:0]    _zz_when_State_l238;
  reg        [2:0]    CoreFMS_stateReg;
  reg        [2:0]    CoreFMS_stateNext;
  wire                when_Spinal1802_l304;
  wire                when_State_l238;
  wire                when_Spinal1802_l333;
  wire                when_Spinal1802_l344;
  wire                when_Spinal1802_l349;
  wire                when_Spinal1802_l352;
  wire                when_Spinal1802_l355;
  wire                when_Spinal1802_l360;
  wire                when_Spinal1802_l363;
  wire                when_Spinal1802_l367;
  wire                when_Spinal1802_l391;
  wire                when_Spinal1802_l393;
  wire                when_Spinal1802_l398;
  wire                when_Spinal1802_l401;
  wire                when_Spinal1802_l404;
  wire                when_Spinal1802_l407;
  wire                when_Spinal1802_l410;
  wire                when_Spinal1802_l414;
  wire                when_Spinal1802_l434;
  wire                when_Spinal1802_l437;
  wire                when_Spinal1802_l443;
  wire                when_Spinal1802_l456;
  wire                when_Spinal1802_l458;
  wire                when_Spinal1802_l460;
  wire                when_Spinal1802_l464;
  wire                when_Spinal1802_l469;
  wire                when_Spinal1802_l473;
  wire                when_Spinal1802_l479;
  wire                when_Spinal1802_l481;
  wire                when_Spinal1802_l488;
  wire                when_Spinal1802_l491;
  wire                when_Spinal1802_l506;
  wire                when_Spinal1802_l508;
  wire                when_Spinal1802_l513;
  wire                when_Spinal1802_l517;
  wire                when_Spinal1802_l519;
  wire                when_Spinal1802_l521;
  wire                when_Spinal1802_l523;
  wire                when_Spinal1802_l525;
  wire                when_Spinal1802_l527;
  wire                when_Spinal1802_l529;
  wire                when_Spinal1802_l531;
  wire                when_Spinal1802_l554;
  wire                when_Spinal1802_l555;
  wire                when_Spinal1802_l558;
  wire                when_Spinal1802_l572;
  wire                when_Spinal1802_l574;
  wire                when_Spinal1802_l576;
  wire                when_Spinal1802_l578;
  wire                when_Spinal1802_l580;
  wire                when_Spinal1802_l582;
  wire                when_Spinal1802_l584;
  wire                when_Spinal1802_l586;
  wire                when_Spinal1802_l588;
  wire                when_Spinal1802_l590;
  wire                when_Spinal1802_l597;
  wire                when_Spinal1802_l598;
  wire                when_Spinal1802_l602;
  wire                when_Spinal1802_l604;
  wire                when_Spinal1802_l606;
  wire                when_Spinal1802_l608;
  wire                when_Spinal1802_l621;
  wire                when_Spinal1802_l625;
  wire                when_Spinal1802_l639;
  wire                when_Spinal1802_l629;
  wire                when_Spinal1802_l633;
  wire                when_Spinal1802_l636;
  wire                when_Spinal1802_l651;
  wire                when_Spinal1802_l656;
  wire                when_Spinal1802_l660;
  wire                when_Spinal1802_l663;
  wire                when_Spinal1802_l666;
  wire                when_Spinal1802_l668;
  wire                when_Spinal1802_l684;
  wire                when_Spinal1802_l688;
  wire                when_Spinal1802_l693;
  wire                when_Spinal1802_l697;
  wire                when_Spinal1802_l700;
  wire                when_StateMachine_l249;
  `ifndef SYNTHESIS
  reg [39:0] Mode_string;
  reg [47:0] RegSelMode_string;
  reg [79:0] RegOpMode_string;
  reg [95:0] ExeMode_string;
  reg [47:0] BusControl_string;
  reg [111:0] DRegControl_string;
  reg [79:0] CoreFMS_stateReg_string;
  reg [79:0] CoreFMS_stateNext_string;
  `endif


  assign _zz_StateCounter_valueNext_1 = StateCounter_willIncrement;
  assign _zz_StateCounter_valueNext = {2'd0, _zz_StateCounter_valueNext_1};
  assign _zz_ALU_Add = {1'd0, Bus_1};
  assign _zz_ALU_Add_1 = {1'd0, D};
  assign _zz_ALU_SubD = {1'd0, Bus_1};
  assign _zz_ALU_SubD_1 = {1'd0, D};
  assign _zz_ALU_SubM = {1'd0, D};
  assign _zz_ALU_SubM_1 = {1'd0, Bus_1};
  assign _zz_D = (D >>> 1);
  assign _zz_D_1 = (D <<< 1);
  assign _zz_when_Spinal1802_l189 = ExecuteModes_Load;
  assign _zz_when_Spinal1802_l189_1 = ExecuteModes_LongLoad;
  always @(*) begin
    case(RSel)
      4'b0000 : _zz__zz_A = R_0;
      4'b0001 : _zz__zz_A = R_1;
      4'b0010 : _zz__zz_A = R_2;
      4'b0011 : _zz__zz_A = R_3;
      4'b0100 : _zz__zz_A = R_4;
      4'b0101 : _zz__zz_A = R_5;
      4'b0110 : _zz__zz_A = R_6;
      4'b0111 : _zz__zz_A = R_7;
      4'b1000 : _zz__zz_A = R_8;
      4'b1001 : _zz__zz_A = R_9;
      4'b1010 : _zz__zz_A = R_10;
      4'b1011 : _zz__zz_A = R_11;
      4'b1100 : _zz__zz_A = R_12;
      4'b1101 : _zz__zz_A = R_13;
      4'b1110 : _zz__zz_A = R_14;
      default : _zz__zz_A = R_15;
    endcase
  end

  `ifndef SYNTHESIS
  always @(*) begin
    case(Mode)
      CPUModes_Load : Mode_string = "Load ";
      CPUModes_Reset : Mode_string = "Reset";
      CPUModes_Pause : Mode_string = "Pause";
      CPUModes_Run : Mode_string = "Run  ";
      default : Mode_string = "?????";
    endcase
  end
  always @(*) begin
    case(RegSelMode)
      RegSelectModes_PSel : RegSelMode_string = "PSel  ";
      RegSelectModes_NSel : RegSelMode_string = "NSel  ";
      RegSelectModes_XSel : RegSelMode_string = "XSel  ";
      RegSelectModes_DMA0 : RegSelMode_string = "DMA0  ";
      RegSelectModes_Stack2 : RegSelMode_string = "Stack2";
      default : RegSelMode_string = "??????";
    endcase
  end
  always @(*) begin
    case(RegOpMode)
      RegOperationModes_None : RegOpMode_string = "None      ";
      RegOperationModes_Inc : RegOpMode_string = "Inc       ";
      RegOperationModes_Dec : RegOpMode_string = "Dec       ";
      RegOperationModes_LoadUpper : RegOpMode_string = "LoadUpper ";
      RegOperationModes_LoadLower : RegOpMode_string = "LoadLower ";
      RegOperationModes_UpperOnBus : RegOpMode_string = "UpperOnBus";
      RegOperationModes_LowerOnBus : RegOpMode_string = "LowerOnBus";
      RegOperationModes_LoadTemp : RegOpMode_string = "LoadTemp  ";
      RegOperationModes_LoadJump : RegOpMode_string = "LoadJump  ";
      default : RegOpMode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(ExeMode)
      ExecuteModes_None : ExeMode_string = "None        ";
      ExecuteModes_Load : ExeMode_string = "Load        ";
      ExecuteModes_LoadDec : ExeMode_string = "LoadDec     ";
      ExecuteModes_LoadNoInc : ExeMode_string = "LoadNoInc   ";
      ExecuteModes_Write : ExeMode_string = "Write       ";
      ExecuteModes_WriteDec : ExeMode_string = "WriteDec    ";
      ExecuteModes_WriteNoInc : ExeMode_string = "WriteNoInc  ";
      ExecuteModes_LongLoad : ExeMode_string = "LongLoad    ";
      ExecuteModes_LongContinue : ExeMode_string = "LongContinue";
      ExecuteModes_DMA_In : ExeMode_string = "DMA_In      ";
      ExecuteModes_DMA_Out : ExeMode_string = "DMA_Out     ";
      default : ExeMode_string = "????????????";
    endcase
  end
  always @(*) begin
    case(BusControl)
      BusControlModes_DataIn : BusControl_string = "DataIn";
      BusControlModes_DReg : BusControl_string = "DReg  ";
      BusControlModes_TReg : BusControl_string = "TReg  ";
      BusControlModes_PXReg : BusControl_string = "PXReg ";
      BusControlModes_RLower : BusControl_string = "RLower";
      BusControlModes_RUpper : BusControl_string = "RUpper";
      default : BusControl_string = "??????";
    endcase
  end
  always @(*) begin
    case(DRegControl)
      DRegControlModes_None : DRegControl_string = "None          ";
      DRegControlModes_BusIn : DRegControl_string = "BusIn         ";
      DRegControlModes_ALU_OR : DRegControl_string = "ALU_OR        ";
      DRegControlModes_ALU_XOR : DRegControl_string = "ALU_XOR       ";
      DRegControlModes_ALU_AND : DRegControl_string = "ALU_AND       ";
      DRegControlModes_ALU_RSH : DRegControl_string = "ALU_RSH       ";
      DRegControlModes_ALU_LSH : DRegControl_string = "ALU_LSH       ";
      DRegControlModes_ALU_RSHR : DRegControl_string = "ALU_RSHR      ";
      DRegControlModes_ALU_LSHR : DRegControl_string = "ALU_LSHR      ";
      DRegControlModes_ALU_Add : DRegControl_string = "ALU_Add       ";
      DRegControlModes_ALU_AddCarry : DRegControl_string = "ALU_AddCarry  ";
      DRegControlModes_ALU_SubD : DRegControl_string = "ALU_SubD      ";
      DRegControlModes_ALU_SubDBorrow : DRegControl_string = "ALU_SubDBorrow";
      DRegControlModes_ALU_SubM : DRegControl_string = "ALU_SubM      ";
      DRegControlModes_ALU_SubMBorrow : DRegControl_string = "ALU_SubMBorrow";
      default : DRegControl_string = "??????????????";
    endcase
  end
  always @(*) begin
    case(CoreFMS_stateReg)
      CoreFMS_enumDef_BOOT : CoreFMS_stateReg_string = "BOOT      ";
      CoreFMS_enumDef_S1_Reset : CoreFMS_stateReg_string = "S1_Reset  ";
      CoreFMS_enumDef_S1_Init : CoreFMS_stateReg_string = "S1_Init   ";
      CoreFMS_enumDef_S0_Fetch : CoreFMS_stateReg_string = "S0_Fetch  ";
      CoreFMS_enumDef_S1_Execute : CoreFMS_stateReg_string = "S1_Execute";
      CoreFMS_enumDef_S2_DMA : CoreFMS_stateReg_string = "S2_DMA    ";
      CoreFMS_enumDef_S3_INT : CoreFMS_stateReg_string = "S3_INT    ";
      default : CoreFMS_stateReg_string = "??????????";
    endcase
  end
  always @(*) begin
    case(CoreFMS_stateNext)
      CoreFMS_enumDef_BOOT : CoreFMS_stateNext_string = "BOOT      ";
      CoreFMS_enumDef_S1_Reset : CoreFMS_stateNext_string = "S1_Reset  ";
      CoreFMS_enumDef_S1_Init : CoreFMS_stateNext_string = "S1_Init   ";
      CoreFMS_enumDef_S0_Fetch : CoreFMS_stateNext_string = "S0_Fetch  ";
      CoreFMS_enumDef_S1_Execute : CoreFMS_stateNext_string = "S1_Execute";
      CoreFMS_enumDef_S2_DMA : CoreFMS_stateNext_string = "S2_DMA    ";
      CoreFMS_enumDef_S3_INT : CoreFMS_stateNext_string = "S3_INT    ";
      default : CoreFMS_stateNext_string = "??????????";
    endcase
  end
  `endif

  always @(*) begin
    StateCounter_willIncrement = 1'b0;
    if(when_Spinal1802_l124) begin
      StateCounter_willIncrement = 1'b1;
    end
  end

  always @(*) begin
    StateCounter_willClear = 1'b0;
    case(CoreFMS_stateReg)
      CoreFMS_enumDef_S1_Reset : begin
      end
      CoreFMS_enumDef_S1_Init : begin
        StateCounter_willClear = 1'b1;
      end
      CoreFMS_enumDef_S0_Fetch : begin
      end
      CoreFMS_enumDef_S1_Execute : begin
      end
      CoreFMS_enumDef_S2_DMA : begin
      end
      CoreFMS_enumDef_S3_INT : begin
      end
      default : begin
      end
    endcase
  end

  assign StateCounter_willOverflowIfInc = (StateCounter_value == 3'b111);
  assign StateCounter_willOverflow = (StateCounter_willOverflowIfInc && StateCounter_willIncrement);
  always @(*) begin
    StateCounter_valueNext = (StateCounter_value + _zz_StateCounter_valueNext);
    if(StateCounter_willClear) begin
      StateCounter_valueNext = 3'b000;
    end
  end

  always @(*) begin
    Reset = 1'b0;
    case(CoreFMS_stateReg)
      CoreFMS_enumDef_S1_Reset : begin
      end
      CoreFMS_enumDef_S1_Init : begin
        Reset = 1'b1;
      end
      CoreFMS_enumDef_S0_Fetch : begin
        Reset = 1'b0;
      end
      CoreFMS_enumDef_S1_Execute : begin
        Reset = 1'b0;
      end
      CoreFMS_enumDef_S2_DMA : begin
      end
      CoreFMS_enumDef_S3_INT : begin
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    Branch = 1'b0;
    if(when_Spinal1802_l275) begin
      Branch = 1'b1;
    end else begin
      if(when_Spinal1802_l277) begin
        Branch = (Q == 1'b1);
      end else begin
        if(when_Spinal1802_l279) begin
          Branch = (D == 8'h0);
        end else begin
          if(when_Spinal1802_l281) begin
            Branch = (DF == 1'b1);
          end else begin
            if(when_Spinal1802_l283) begin
              Branch = (Q == 1'b0);
            end else begin
              if(when_Spinal1802_l285) begin
                Branch = (D != 8'h0);
              end else begin
                if(when_Spinal1802_l287) begin
                  Branch = (DF == 1'b0);
                end else begin
                  if(when_Spinal1802_l289) begin
                    Branch = (IE == 1'b0);
                  end else begin
                    if(when_Spinal1802_l291) begin
                      Branch = (EF[N[1 : 0]] == 1'b1);
                    end else begin
                      if(when_Spinal1802_l293) begin
                        Branch = (EF[N[1 : 0]] == 1'b0);
                      end else begin
                        Branch = 1'b0;
                      end
                    end
                  end
                end
              end
            end
          end
        end
      end
    end
  end

  assign Skip = (((((((((N == 4'b0100) || (N == 4'b0101)) || (N == 4'b0110)) || (N == 4'b0111)) || (N == 4'b1000)) || (N == 4'b1100)) || (N == 4'b1101)) || (N == 4'b1110)) || (N == 4'b1111));
  assign _zz_A = _zz__zz_A;
  assign _zz_1 = ({15'd0,1'b1} <<< RSel);
  assign _zz_2 = _zz_1[0];
  assign _zz_3 = _zz_1[1];
  assign _zz_4 = _zz_1[2];
  assign _zz_5 = _zz_1[3];
  assign _zz_6 = _zz_1[4];
  assign _zz_7 = _zz_1[5];
  assign _zz_8 = _zz_1[6];
  assign _zz_9 = _zz_1[7];
  assign _zz_10 = _zz_1[8];
  assign _zz_11 = _zz_1[9];
  assign _zz_12 = _zz_1[10];
  assign _zz_13 = _zz_1[11];
  assign _zz_14 = _zz_1[12];
  assign _zz_15 = _zz_1[13];
  assign _zz_16 = _zz_1[14];
  assign _zz_17 = _zz_1[15];
  assign A = _zz_A;
  assign io_Q = Q;
  assign io_SC = SC;
  assign io_N = outN;
  assign io_TPA = TPA;
  assign io_TPB = TPB;
  assign io_MRD = MRD;
  assign io_MWR = MWR;
  assign io_DataOut = Bus_1;
  assign io_Addr16 = Addr16;
  assign when_Spinal1802_l124 = (StartCounting && (Mode != CPUModes_Pause));
  assign when_Spinal1802_l129 = ((! io_Clear_n) && (! io_Wait_n));
  assign when_Spinal1802_l131 = ((! io_Clear_n) && io_Wait_n);
  assign when_Spinal1802_l133 = (io_Clear_n && (! io_Wait_n));
  assign when_Spinal1802_l138 = ((StateCounter_value == 3'b001) && (Mode != CPUModes_Reset));
  assign when_Spinal1802_l142 = ((StateCounter_value == 3'b110) && (Mode != CPUModes_Reset));
  assign when_Spinal1802_l147 = (RegSelMode == RegSelectModes_NSel);
  always @(*) begin
    if(when_Spinal1802_l147) begin
      RSel = N;
    end else begin
      if(when_Spinal1802_l149) begin
        RSel = X;
      end else begin
        if(when_Spinal1802_l151) begin
          RSel = 4'b0010;
        end else begin
          if(when_Spinal1802_l153) begin
            RSel = 4'b0000;
          end else begin
            RSel = P;
          end
        end
      end
    end
  end

  assign when_Spinal1802_l149 = (RegSelMode == RegSelectModes_XSel);
  assign when_Spinal1802_l151 = (RegSelMode == RegSelectModes_Stack2);
  assign when_Spinal1802_l153 = (RegSelMode == RegSelectModes_DMA0);
  assign _zz_R_0 = (A + 16'h0001);
  assign _zz_R_0_1 = (A - 16'h0001);
  assign _zz_R_0_2 = {Bus_1,_zz_A[7 : 0]};
  assign _zz_R_0_3 = {_zz_A[15 : 8],Bus_1};
  assign _zz_R_0_4 = {TmpUpper,Bus_1};
  assign when_Spinal1802_l160 = (RegOpMode == RegOperationModes_Inc);
  assign when_Spinal1802_l162 = (RegOpMode == RegOperationModes_Dec);
  assign when_Spinal1802_l164 = (RegOpMode == RegOperationModes_LoadUpper);
  assign when_Spinal1802_l166 = (RegOpMode == RegOperationModes_LoadLower);
  assign when_Spinal1802_l168 = (RegOpMode == RegOperationModes_LoadTemp);
  assign when_Spinal1802_l170 = (RegOpMode == RegOperationModes_LoadJump);
  assign when_Spinal1802_l177 = (StateCounter_value == 3'b000);
  assign when_Spinal1802_l181 = ((3'b001 <= StateCounter_value) && (StateCounter_value <= 3'b010));
  always @(*) begin
    if(when_Spinal1802_l181) begin
      io_Addr = Addr[15 : 8];
    end else begin
      io_Addr = Addr[7 : 0];
    end
  end

  assign when_Spinal1802_l186 = (3'b011 <= StateCounter_value);
  assign when_Spinal1802_l187 = (SC == 2'b00);
  assign when_Spinal1802_l189 = (((SC == 2'b01) || (SC == 2'b10)) && (((((((ExeMode == _zz_when_Spinal1802_l189) || (ExeMode == _zz_when_Spinal1802_l189_1)) || (ExeMode == ExecuteModes_LoadDec)) || (ExeMode == ExecuteModes_LoadNoInc)) || (ExeMode == ExecuteModes_LongLoad)) || (ExeMode == ExecuteModes_LongContinue)) || (ExeMode == ExecuteModes_DMA_Out)));
  assign when_Spinal1802_l199 = ((3'b101 <= StateCounter_value) && (StateCounter_value < 3'b111));
  assign when_Spinal1802_l203 = (((SC == 2'b01) || (SC == 2'b10)) && ((((ExeMode == ExecuteModes_Write) || (ExeMode == ExecuteModes_WriteDec)) || (ExeMode == ExecuteModes_WriteNoInc)) || (ExeMode == ExecuteModes_DMA_In)));
  assign ALU_Add = (_zz_ALU_Add + _zz_ALU_Add_1);
  assign ALU_AddCarry = (ALU_Add + {8'h0,DF});
  assign ALU_SubD = (_zz_ALU_SubD - _zz_ALU_SubD_1);
  assign ALU_SubM = (_zz_ALU_SubM - _zz_ALU_SubM_1);
  assign ALU_SubDB = (ALU_SubD - {8'h0,(! DF)});
  assign ALU_SubMB = (ALU_SubM - {8'h0,(! DF)});
  assign when_Spinal1802_l219 = (DRegControl == DRegControlModes_BusIn);
  assign when_Spinal1802_l221 = (DRegControl == DRegControlModes_ALU_OR);
  assign when_Spinal1802_l223 = (DRegControl == DRegControlModes_ALU_XOR);
  assign when_Spinal1802_l225 = (DRegControl == DRegControlModes_ALU_AND);
  assign when_Spinal1802_l227 = (DRegControl == DRegControlModes_ALU_RSH);
  assign when_Spinal1802_l230 = (DRegControl == DRegControlModes_ALU_RSHR);
  assign when_Spinal1802_l233 = (DRegControl == DRegControlModes_ALU_LSH);
  assign when_Spinal1802_l236 = (DRegControl == DRegControlModes_ALU_LSHR);
  assign when_Spinal1802_l239 = (DRegControl == DRegControlModes_ALU_Add);
  assign when_Spinal1802_l242 = (DRegControl == DRegControlModes_ALU_AddCarry);
  assign when_Spinal1802_l245 = (DRegControl == DRegControlModes_ALU_SubD);
  assign when_Spinal1802_l248 = (DRegControl == DRegControlModes_ALU_SubM);
  assign when_Spinal1802_l251 = (DRegControl == DRegControlModes_ALU_SubDBorrow);
  assign when_Spinal1802_l254 = (DRegControl == DRegControlModes_ALU_SubMBorrow);
  assign when_Spinal1802_l260 = (BusControl == BusControlModes_DataIn);
  always @(*) begin
    if(when_Spinal1802_l260) begin
      Bus_1 = io_DataIn;
    end else begin
      if(when_Spinal1802_l262) begin
        Bus_1 = D;
      end else begin
        if(when_Spinal1802_l264) begin
          Bus_1 = T;
        end else begin
          if(when_Spinal1802_l266) begin
            Bus_1 = {X,P};
          end else begin
            if(when_Spinal1802_l268) begin
              Bus_1 = A[7 : 0];
            end else begin
              if(when_Spinal1802_l270) begin
                Bus_1 = A[15 : 8];
              end else begin
                Bus_1 = 8'h0;
              end
            end
          end
        end
      end
    end
  end

  assign when_Spinal1802_l262 = (BusControl == BusControlModes_DReg);
  assign when_Spinal1802_l264 = (BusControl == BusControlModes_TReg);
  assign when_Spinal1802_l266 = (BusControl == BusControlModes_PXReg);
  assign when_Spinal1802_l268 = (BusControl == BusControlModes_RLower);
  assign when_Spinal1802_l270 = (BusControl == BusControlModes_RUpper);
  assign when_Spinal1802_l275 = ((N == 4'b0000) || (OP == 8'hc4));
  assign when_Spinal1802_l277 = (((N == 4'b0001) || (OP == 8'hc5)) || (OP == 8'hc1));
  assign when_Spinal1802_l279 = (((N == 4'b0010) || (OP == 8'hc6)) || (OP == 8'hc2));
  assign when_Spinal1802_l281 = (((N == 4'b0011) || (OP == 8'hc7)) || (OP == 8'hc3));
  assign when_Spinal1802_l283 = (((N == 4'b1001) || (OP == 8'hcd)) || (OP == 8'hc9));
  assign when_Spinal1802_l285 = (((N == 4'b1010) || (OP == 8'hce)) || (OP == 8'hca));
  assign when_Spinal1802_l287 = (((N == 4'b1011) || (OP == 8'hcf)) || (OP == 8'hcb));
  assign when_Spinal1802_l289 = (OP == 8'hcc);
  assign when_Spinal1802_l291 = ((I == 4'b0011) && ((((N == 4'b0100) || (N == 4'b0101)) || (N == 4'b0110)) || (N == 4'b0111)));
  assign when_Spinal1802_l293 = ((I == 4'b0011) && ((((N == 4'b1100) || (N == 4'b1101)) || (N == 4'b1110)) || (N == 4'b1111)));
  assign CoreFMS_wantExit = 1'b0;
  always @(*) begin
    CoreFMS_wantStart = 1'b0;
    case(CoreFMS_stateReg)
      CoreFMS_enumDef_S1_Reset : begin
      end
      CoreFMS_enumDef_S1_Init : begin
      end
      CoreFMS_enumDef_S0_Fetch : begin
      end
      CoreFMS_enumDef_S1_Execute : begin
      end
      CoreFMS_enumDef_S2_DMA : begin
      end
      CoreFMS_enumDef_S3_INT : begin
      end
      default : begin
        CoreFMS_wantStart = 1'b1;
      end
    endcase
  end

  assign CoreFMS_wantKill = 1'b0;
  always @(*) begin
    CoreFMS_stateNext = CoreFMS_stateReg;
    case(CoreFMS_stateReg)
      CoreFMS_enumDef_S1_Reset : begin
        if(when_Spinal1802_l304) begin
          CoreFMS_stateNext = CoreFMS_enumDef_S1_Init;
        end
      end
      CoreFMS_enumDef_S1_Init : begin
        if(when_State_l238) begin
          if(when_Spinal1802_l333) begin
            CoreFMS_stateNext = CoreFMS_enumDef_S1_Execute;
          end else begin
            CoreFMS_stateNext = CoreFMS_enumDef_S0_Fetch;
          end
        end
      end
      CoreFMS_enumDef_S0_Fetch : begin
        if(when_Spinal1802_l443) begin
          CoreFMS_stateNext = CoreFMS_enumDef_S1_Reset;
        end else begin
          if(StateCounter_willOverflow) begin
            CoreFMS_stateNext = CoreFMS_enumDef_S1_Execute;
          end
        end
      end
      CoreFMS_enumDef_S1_Execute : begin
        if(when_Spinal1802_l621) begin
          CoreFMS_stateNext = CoreFMS_enumDef_S1_Reset;
        end else begin
          if(StateCounter_willOverflow) begin
            if(when_Spinal1802_l625) begin
              CoreFMS_stateNext = CoreFMS_enumDef_S2_DMA;
            end else begin
              if(when_Spinal1802_l629) begin
                CoreFMS_stateNext = CoreFMS_enumDef_S2_DMA;
              end else begin
                if(when_Spinal1802_l633) begin
                  CoreFMS_stateNext = CoreFMS_enumDef_S3_INT;
                end else begin
                  if(!when_Spinal1802_l636) begin
                    if(when_Spinal1802_l639) begin
                      CoreFMS_stateNext = CoreFMS_enumDef_S0_Fetch;
                    end
                  end
                end
              end
            end
          end
        end
      end
      CoreFMS_enumDef_S2_DMA : begin
        if(when_Spinal1802_l663) begin
          CoreFMS_stateNext = CoreFMS_enumDef_S1_Reset;
        end else begin
          if(StateCounter_willOverflow) begin
            if(when_Spinal1802_l666) begin
              if(when_Spinal1802_l668) begin
                CoreFMS_stateNext = CoreFMS_enumDef_S1_Execute;
              end else begin
                CoreFMS_stateNext = CoreFMS_enumDef_S0_Fetch;
              end
            end
          end
        end
      end
      CoreFMS_enumDef_S3_INT : begin
        if(when_Spinal1802_l693) begin
          CoreFMS_stateNext = CoreFMS_enumDef_S1_Reset;
        end else begin
          if(StateCounter_willOverflow) begin
            if(when_Spinal1802_l697) begin
              CoreFMS_stateNext = CoreFMS_enumDef_S2_DMA;
            end else begin
              if(when_Spinal1802_l700) begin
                CoreFMS_stateNext = CoreFMS_enumDef_S2_DMA;
              end else begin
                CoreFMS_stateNext = CoreFMS_enumDef_S0_Fetch;
              end
            end
          end
        end
      end
      default : begin
      end
    endcase
    if(CoreFMS_wantStart) begin
      CoreFMS_stateNext = CoreFMS_enumDef_S1_Reset;
    end
    if(CoreFMS_wantKill) begin
      CoreFMS_stateNext = CoreFMS_enumDef_BOOT;
    end
  end

  assign when_Spinal1802_l304 = (Mode != CPUModes_Reset);
  assign when_State_l238 = (_zz_when_State_l238 <= 4'b0001);
  assign when_Spinal1802_l333 = (Mode == CPUModes_Load);
  assign when_Spinal1802_l344 = (StateCounter_value == 3'b000);
  assign when_Spinal1802_l349 = (StateCounter_value == 3'b001);
  assign when_Spinal1802_l352 = (StateCounter_value == 3'b010);
  assign when_Spinal1802_l355 = (StateCounter_value == 3'b110);
  assign when_Spinal1802_l360 = (StateCounter_value == 3'b111);
  assign when_Spinal1802_l363 = (N == 4'b0000);
  assign when_Spinal1802_l367 = (4'b0001 <= N);
  assign when_Spinal1802_l391 = ((4'b0000 < N) && (N <= 4'b0111));
  assign when_Spinal1802_l393 = (4'b1001 <= N);
  assign when_Spinal1802_l398 = (((N == 4'b0000) || (N == 4'b0001)) || (N == 4'b0010));
  assign when_Spinal1802_l401 = (N == 4'b0011);
  assign when_Spinal1802_l404 = (((N == 4'b0100) || (N == 4'b0101)) || (N == 4'b0111));
  assign when_Spinal1802_l407 = (N == 4'b1000);
  assign when_Spinal1802_l410 = (N == 4'b1001);
  assign when_Spinal1802_l414 = (((N == 4'b1100) || (N == 4'b1101)) || (N == 4'b1111));
  assign when_Spinal1802_l434 = ((N <= 4'b0101) || (N == 4'b0111));
  assign when_Spinal1802_l437 = (((4'b1000 <= N) && (N <= 4'b1101)) || (N == 4'b1111));
  assign when_Spinal1802_l443 = (Mode == CPUModes_Reset);
  assign when_Spinal1802_l456 = (StateCounter_value == 3'b001);
  assign when_Spinal1802_l458 = (((ExeMode == ExecuteModes_Load) || (ExeMode == ExecuteModes_Write)) || (ExeMode == ExecuteModes_LongLoad));
  assign when_Spinal1802_l460 = (((ExeMode == ExecuteModes_LoadDec) || (ExeMode == ExecuteModes_WriteDec)) || (ExeMode == ExecuteModes_LongContinue));
  assign when_Spinal1802_l464 = ((I == 4'b0110) && (4'b0000 < N));
  assign when_Spinal1802_l469 = (StateCounter_value == 3'b010);
  assign when_Spinal1802_l473 = (StateCounter_value == 3'b100);
  assign when_Spinal1802_l479 = ((N == 4'b1000) || (N == 4'b1001));
  assign when_Spinal1802_l481 = (N == 4'b0011);
  assign when_Spinal1802_l488 = (StateCounter_value == 3'b101);
  assign when_Spinal1802_l491 = (N != 4'b0000);
  assign when_Spinal1802_l506 = (N == 4'b0000);
  assign when_Spinal1802_l508 = (4'b1001 <= N);
  assign when_Spinal1802_l513 = ((N == 4'b0000) || (N == 4'b0001));
  assign when_Spinal1802_l517 = (N == 4'b0010);
  assign when_Spinal1802_l519 = ((N == 4'b0100) || (N == 4'b1100));
  assign when_Spinal1802_l521 = ((N == 4'b0101) || (N == 4'b1101));
  assign when_Spinal1802_l523 = (N == 4'b0110);
  assign when_Spinal1802_l525 = ((N == 4'b0111) || (N == 4'b1111));
  assign when_Spinal1802_l527 = (N == 4'b1010);
  assign when_Spinal1802_l529 = (N == 4'b1011);
  assign when_Spinal1802_l531 = (N == 4'b1110);
  assign when_Spinal1802_l554 = (ExeMode == ExecuteModes_Load);
  assign when_Spinal1802_l555 = (Branch && (! Skip));
  assign when_Spinal1802_l558 = (((ExeMode == ExecuteModes_LongLoad) && Branch) && (! Skip));
  assign when_Spinal1802_l572 = (N == 4'b0000);
  assign when_Spinal1802_l574 = ((N == 4'b0001) || (N == 4'b1001));
  assign when_Spinal1802_l576 = ((N == 4'b0010) || (N == 4'b1010));
  assign when_Spinal1802_l578 = ((N == 4'b0011) || (N == 4'b1011));
  assign when_Spinal1802_l580 = ((N == 4'b0100) || (N == 4'b1100));
  assign when_Spinal1802_l582 = ((N == 4'b0101) || (N == 4'b1101));
  assign when_Spinal1802_l584 = (N == 4'b0110);
  assign when_Spinal1802_l586 = ((N == 4'b0111) || (N == 4'b1111));
  assign when_Spinal1802_l588 = (N == 4'b1000);
  assign when_Spinal1802_l590 = (N == 4'b1110);
  assign when_Spinal1802_l597 = (StateCounter_value == 3'b110);
  assign when_Spinal1802_l598 = ((I == 4'b0111) && (N == 4'b1001));
  assign when_Spinal1802_l602 = ((ExeMode == ExecuteModes_LongLoad) || (ExeMode == ExecuteModes_LongContinue));
  assign when_Spinal1802_l604 = ((I == 4'b1100) && ((RegOpMode == RegOperationModes_LoadTemp) || (Skip && (! Branch))));
  assign when_Spinal1802_l606 = (((I == 4'b1100) && Skip) && Branch);
  assign when_Spinal1802_l608 = (((I == 4'b1100) && (! Skip)) && (! Branch));
  assign when_Spinal1802_l621 = (Mode == CPUModes_Reset);
  assign when_Spinal1802_l625 = (! io_DMA_In_n);
  assign when_Spinal1802_l639 = ((! ((ExeMode == ExecuteModes_LongLoad) || (ExeMode == ExecuteModes_LongContinue))) && (! Idle));
  assign when_Spinal1802_l629 = (! io_DMA_Out_n);
  assign when_Spinal1802_l633 = (((! io_Interrupt_n) && IE) && (! ((ExeMode == ExecuteModes_LongLoad) || (ExeMode == ExecuteModes_LongContinue))));
  assign when_Spinal1802_l636 = (Mode == CPUModes_Load);
  assign when_Spinal1802_l651 = (StateCounter_value == 3'b000);
  assign when_Spinal1802_l656 = (StateCounter_value == 3'b001);
  assign when_Spinal1802_l660 = (StateCounter_value == 3'b010);
  assign when_Spinal1802_l663 = (Mode == CPUModes_Reset);
  assign when_Spinal1802_l666 = (io_DMA_In_n && io_DMA_Out_n);
  assign when_Spinal1802_l668 = (Mode == CPUModes_Load);
  assign when_Spinal1802_l684 = (StateCounter_value == 3'b010);
  assign when_Spinal1802_l688 = (StateCounter_value == 3'b011);
  assign when_Spinal1802_l693 = (Mode == CPUModes_Reset);
  assign when_Spinal1802_l697 = (! io_DMA_In_n);
  assign when_Spinal1802_l700 = (! io_DMA_Out_n);
  assign when_StateMachine_l249 = ((! (CoreFMS_stateReg == CoreFMS_enumDef_S1_Init)) && (CoreFMS_stateNext == CoreFMS_enumDef_S1_Init));
  always @(posedge clk) begin
    if(clockedArea_newClockEnable) begin
      if(reset_1) begin
      Q <= 1'b0;
      TPA <= 1'b0;
      TPB <= 1'b0;
      MRD <= 1'b1;
      MWR <= 1'b1;
      StateCounter_value <= 3'b000;
      StartCounting <= 1'b0;
      Addr <= 16'h0;
      Addr16 <= 16'h0;
      D <= 8'h0;
      outN <= 3'b000;
      T <= 8'h0;
      IE <= 1'b1;
      DF <= 1'b0;
      Idle <= 1'b0;
      CoreFMS_stateReg <= CoreFMS_enumDef_BOOT;
      end else begin
        StateCounter_value <= StateCounter_valueNext;
        Addr16 <= Addr;
        if(when_Spinal1802_l138) begin
          TPA <= 1'b1;
        end else begin
          TPA <= 1'b0;
        end
        if(when_Spinal1802_l142) begin
          TPB <= 1'b1;
        end else begin
          TPB <= 1'b0;
        end
        if(Reset) begin
          Addr <= 16'h0;
        end else begin
          if(when_Spinal1802_l177) begin
            Addr <= A;
          end
        end
        if(when_Spinal1802_l186) begin
          if(when_Spinal1802_l187) begin
            MRD <= 1'b0;
          end else begin
            if(when_Spinal1802_l189) begin
              MRD <= 1'b0;
            end
          end
        end else begin
          MRD <= 1'b1;
        end
        if(when_Spinal1802_l199) begin
          if(when_Spinal1802_l203) begin
            MWR <= 1'b0;
          end
        end else begin
          MWR <= 1'b1;
        end
        if(Reset) begin
          DF <= 1'b0;
          D <= 8'h0;
        end else begin
          if(when_Spinal1802_l219) begin
            D <= Bus_1;
          end else begin
            if(when_Spinal1802_l221) begin
              D <= (Bus_1 | D);
            end else begin
              if(when_Spinal1802_l223) begin
                D <= (Bus_1 ^ D);
              end else begin
                if(when_Spinal1802_l225) begin
                  D <= (Bus_1 & D);
                end else begin
                  if(when_Spinal1802_l227) begin
                    DF <= Dlast[0];
                    D <= (D >>> 1);
                  end else begin
                    if(when_Spinal1802_l230) begin
                      DF <= Dlast[0];
                      D <= (_zz_D | {DFLast,7'h0});
                    end else begin
                      if(when_Spinal1802_l233) begin
                        DF <= Dlast[7];
                        D <= (D <<< 1);
                      end else begin
                        if(when_Spinal1802_l236) begin
                          DF <= Dlast[7];
                          D <= (_zz_D_1 | {7'h0,DFLast});
                        end else begin
                          if(when_Spinal1802_l239) begin
                            DF <= ALU_Add[8];
                            D <= ALU_Add[7:0];
                          end else begin
                            if(when_Spinal1802_l242) begin
                              DF <= ALU_AddCarry[8];
                              D <= ALU_AddCarry[7:0];
                            end else begin
                              if(when_Spinal1802_l245) begin
                                DF <= (! ALU_SubD[8]);
                                D <= ALU_SubD[7:0];
                              end else begin
                                if(when_Spinal1802_l248) begin
                                  DF <= (! ALU_SubM[8]);
                                  D <= ALU_SubM[7:0];
                                end else begin
                                  if(when_Spinal1802_l251) begin
                                    DF <= (! ALU_SubDB[8]);
                                    D <= ALU_SubDB[7:0];
                                  end else begin
                                    if(when_Spinal1802_l254) begin
                                      DF <= (! ALU_SubMB[8]);
                                      D <= ALU_SubMB[7:0];
                                    end
                                  end
                                end
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
            end
          end
        end
        CoreFMS_stateReg <= CoreFMS_stateNext;
        case(CoreFMS_stateReg)
          CoreFMS_enumDef_S1_Reset : begin
          end
          CoreFMS_enumDef_S1_Init : begin
            Idle <= 1'b0;
            IE <= 1'b1;
            outN <= 3'b000;
            T <= 8'h0;
            Q <= 1'b0;
            if(when_State_l238) begin
              StartCounting <= 1'b1;
            end
          end
          CoreFMS_enumDef_S0_Fetch : begin
            if(when_Spinal1802_l360) begin
              case(I)
                4'b0000 : begin
                  if(when_Spinal1802_l363) begin
                    Idle <= 1'b1;
                  end
                end
                4'b0111 : begin
                  if(!when_Spinal1802_l398) begin
                    if(!when_Spinal1802_l401) begin
                      if(!when_Spinal1802_l404) begin
                        if(!when_Spinal1802_l407) begin
                          if(when_Spinal1802_l410) begin
                            T <= {X,P};
                          end
                        end
                      end
                    end
                  end
                end
                default : begin
                end
              endcase
            end
          end
          CoreFMS_enumDef_S1_Execute : begin
            if(when_Spinal1802_l456) begin
              if(when_Spinal1802_l464) begin
                outN <= N[2:0];
              end
            end
            if(when_Spinal1802_l488) begin
              case(I)
                4'b0111 : begin
                  if(when_Spinal1802_l513) begin
                    IE <= (! N[0]);
                  end else begin
                    if(!when_Spinal1802_l517) begin
                      if(!when_Spinal1802_l519) begin
                        if(!when_Spinal1802_l521) begin
                          if(!when_Spinal1802_l523) begin
                            if(!when_Spinal1802_l525) begin
                              if(when_Spinal1802_l527) begin
                                Q <= 1'b0;
                              end else begin
                                if(when_Spinal1802_l529) begin
                                  Q <= 1'b1;
                                end
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
                default : begin
                end
              endcase
            end
            if(!when_Spinal1802_l621) begin
              if(StateCounter_willOverflow) begin
                outN <= 3'b000;
              end
            end
          end
          CoreFMS_enumDef_S2_DMA : begin
            if(!when_Spinal1802_l663) begin
              if(StateCounter_willOverflow) begin
                if(when_Spinal1802_l666) begin
                  if(!when_Spinal1802_l668) begin
                    Idle <= 1'b0;
                  end
                end
              end
            end
          end
          CoreFMS_enumDef_S3_INT : begin
            if(when_Spinal1802_l684) begin
              T <= {X,P};
            end
            if(when_Spinal1802_l688) begin
              IE <= 1'b0;
            end
            if(!when_Spinal1802_l693) begin
              if(StateCounter_willOverflow) begin
                Idle <= 1'b0;
              end
            end
          end
          default : begin
          end
        endcase
      end
    end
  end

  always @(posedge clk) begin
    if(clockedArea_newClockEnable) begin
      Dlast <= D;
      EF <= (~ io_EF_n);
      DFLast <= DF;
      OP <= {I,N};
      if(when_Spinal1802_l129) begin
        Mode <= CPUModes_Load;
      end else begin
        if(when_Spinal1802_l131) begin
          Mode <= CPUModes_Reset;
        end else begin
          if(when_Spinal1802_l133) begin
            Mode <= CPUModes_Pause;
          end else begin
            Mode <= CPUModes_Run;
          end
        end
      end
      if(Reset) begin
        R_0 <= 16'h0;
      end else begin
        if(when_Spinal1802_l160) begin
          if(_zz_2) begin
            R_0 <= _zz_R_0;
          end
          if(_zz_3) begin
            R_1 <= _zz_R_0;
          end
          if(_zz_4) begin
            R_2 <= _zz_R_0;
          end
          if(_zz_5) begin
            R_3 <= _zz_R_0;
          end
          if(_zz_6) begin
            R_4 <= _zz_R_0;
          end
          if(_zz_7) begin
            R_5 <= _zz_R_0;
          end
          if(_zz_8) begin
            R_6 <= _zz_R_0;
          end
          if(_zz_9) begin
            R_7 <= _zz_R_0;
          end
          if(_zz_10) begin
            R_8 <= _zz_R_0;
          end
          if(_zz_11) begin
            R_9 <= _zz_R_0;
          end
          if(_zz_12) begin
            R_10 <= _zz_R_0;
          end
          if(_zz_13) begin
            R_11 <= _zz_R_0;
          end
          if(_zz_14) begin
            R_12 <= _zz_R_0;
          end
          if(_zz_15) begin
            R_13 <= _zz_R_0;
          end
          if(_zz_16) begin
            R_14 <= _zz_R_0;
          end
          if(_zz_17) begin
            R_15 <= _zz_R_0;
          end
        end else begin
          if(when_Spinal1802_l162) begin
            if(_zz_2) begin
              R_0 <= _zz_R_0_1;
            end
            if(_zz_3) begin
              R_1 <= _zz_R_0_1;
            end
            if(_zz_4) begin
              R_2 <= _zz_R_0_1;
            end
            if(_zz_5) begin
              R_3 <= _zz_R_0_1;
            end
            if(_zz_6) begin
              R_4 <= _zz_R_0_1;
            end
            if(_zz_7) begin
              R_5 <= _zz_R_0_1;
            end
            if(_zz_8) begin
              R_6 <= _zz_R_0_1;
            end
            if(_zz_9) begin
              R_7 <= _zz_R_0_1;
            end
            if(_zz_10) begin
              R_8 <= _zz_R_0_1;
            end
            if(_zz_11) begin
              R_9 <= _zz_R_0_1;
            end
            if(_zz_12) begin
              R_10 <= _zz_R_0_1;
            end
            if(_zz_13) begin
              R_11 <= _zz_R_0_1;
            end
            if(_zz_14) begin
              R_12 <= _zz_R_0_1;
            end
            if(_zz_15) begin
              R_13 <= _zz_R_0_1;
            end
            if(_zz_16) begin
              R_14 <= _zz_R_0_1;
            end
            if(_zz_17) begin
              R_15 <= _zz_R_0_1;
            end
          end else begin
            if(when_Spinal1802_l164) begin
              if(_zz_2) begin
                R_0 <= _zz_R_0_2;
              end
              if(_zz_3) begin
                R_1 <= _zz_R_0_2;
              end
              if(_zz_4) begin
                R_2 <= _zz_R_0_2;
              end
              if(_zz_5) begin
                R_3 <= _zz_R_0_2;
              end
              if(_zz_6) begin
                R_4 <= _zz_R_0_2;
              end
              if(_zz_7) begin
                R_5 <= _zz_R_0_2;
              end
              if(_zz_8) begin
                R_6 <= _zz_R_0_2;
              end
              if(_zz_9) begin
                R_7 <= _zz_R_0_2;
              end
              if(_zz_10) begin
                R_8 <= _zz_R_0_2;
              end
              if(_zz_11) begin
                R_9 <= _zz_R_0_2;
              end
              if(_zz_12) begin
                R_10 <= _zz_R_0_2;
              end
              if(_zz_13) begin
                R_11 <= _zz_R_0_2;
              end
              if(_zz_14) begin
                R_12 <= _zz_R_0_2;
              end
              if(_zz_15) begin
                R_13 <= _zz_R_0_2;
              end
              if(_zz_16) begin
                R_14 <= _zz_R_0_2;
              end
              if(_zz_17) begin
                R_15 <= _zz_R_0_2;
              end
            end else begin
              if(when_Spinal1802_l166) begin
                if(_zz_2) begin
                  R_0 <= _zz_R_0_3;
                end
                if(_zz_3) begin
                  R_1 <= _zz_R_0_3;
                end
                if(_zz_4) begin
                  R_2 <= _zz_R_0_3;
                end
                if(_zz_5) begin
                  R_3 <= _zz_R_0_3;
                end
                if(_zz_6) begin
                  R_4 <= _zz_R_0_3;
                end
                if(_zz_7) begin
                  R_5 <= _zz_R_0_3;
                end
                if(_zz_8) begin
                  R_6 <= _zz_R_0_3;
                end
                if(_zz_9) begin
                  R_7 <= _zz_R_0_3;
                end
                if(_zz_10) begin
                  R_8 <= _zz_R_0_3;
                end
                if(_zz_11) begin
                  R_9 <= _zz_R_0_3;
                end
                if(_zz_12) begin
                  R_10 <= _zz_R_0_3;
                end
                if(_zz_13) begin
                  R_11 <= _zz_R_0_3;
                end
                if(_zz_14) begin
                  R_12 <= _zz_R_0_3;
                end
                if(_zz_15) begin
                  R_13 <= _zz_R_0_3;
                end
                if(_zz_16) begin
                  R_14 <= _zz_R_0_3;
                end
                if(_zz_17) begin
                  R_15 <= _zz_R_0_3;
                end
              end else begin
                if(when_Spinal1802_l168) begin
                  TmpUpper <= Bus_1;
                end else begin
                  if(when_Spinal1802_l170) begin
                    if(_zz_2) begin
                      R_0 <= _zz_R_0_4;
                    end
                    if(_zz_3) begin
                      R_1 <= _zz_R_0_4;
                    end
                    if(_zz_4) begin
                      R_2 <= _zz_R_0_4;
                    end
                    if(_zz_5) begin
                      R_3 <= _zz_R_0_4;
                    end
                    if(_zz_6) begin
                      R_4 <= _zz_R_0_4;
                    end
                    if(_zz_7) begin
                      R_5 <= _zz_R_0_4;
                    end
                    if(_zz_8) begin
                      R_6 <= _zz_R_0_4;
                    end
                    if(_zz_9) begin
                      R_7 <= _zz_R_0_4;
                    end
                    if(_zz_10) begin
                      R_8 <= _zz_R_0_4;
                    end
                    if(_zz_11) begin
                      R_9 <= _zz_R_0_4;
                    end
                    if(_zz_12) begin
                      R_10 <= _zz_R_0_4;
                    end
                    if(_zz_13) begin
                      R_11 <= _zz_R_0_4;
                    end
                    if(_zz_14) begin
                      R_12 <= _zz_R_0_4;
                    end
                    if(_zz_15) begin
                      R_13 <= _zz_R_0_4;
                    end
                    if(_zz_16) begin
                      R_14 <= _zz_R_0_4;
                    end
                    if(_zz_17) begin
                      R_15 <= _zz_R_0_4;
                    end
                  end
                end
              end
            end
          end
        end
      end
      case(CoreFMS_stateReg)
        CoreFMS_enumDef_S1_Reset : begin
          SC <= 2'b01;
        end
        CoreFMS_enumDef_S1_Init : begin
          ExeMode <= ExecuteModes_None;
          RegSelMode <= RegSelectModes_PSel;
          RegOpMode <= RegOperationModes_None;
          DRegControl <= DRegControlModes_None;
          BusControl <= BusControlModes_DataIn;
          P <= 4'b0000;
          X <= 4'b0000;
          I <= 4'b0000;
          N <= 4'b0000;
          SC <= 2'b01;
          _zz_when_State_l238 <= (_zz_when_State_l238 - 4'b0001);
        end
        CoreFMS_enumDef_S0_Fetch : begin
          SC <= 2'b00;
          if(when_Spinal1802_l344) begin
            ExeMode <= ExecuteModes_None;
            BusControl <= BusControlModes_DataIn;
            RegSelMode <= RegSelectModes_PSel;
          end
          if(when_Spinal1802_l349) begin
            RegOpMode <= RegOperationModes_Inc;
          end
          if(when_Spinal1802_l352) begin
            RegOpMode <= RegOperationModes_None;
          end
          if(when_Spinal1802_l355) begin
            I <= io_DataIn[7 : 4];
            N <= io_DataIn[3 : 0];
          end
          if(when_Spinal1802_l360) begin
            case(I)
              4'b0000 : begin
                if(when_Spinal1802_l363) begin
                  ExeMode <= ExecuteModes_LoadNoInc;
                  RegSelMode <= RegSelectModes_DMA0;
                end else begin
                  if(when_Spinal1802_l367) begin
                    ExeMode <= ExecuteModes_LoadNoInc;
                    RegSelMode <= RegSelectModes_NSel;
                  end
                end
              end
              4'b0001 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b0010 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b0011 : begin
                ExeMode <= ExecuteModes_Load;
              end
              4'b0100 : begin
                ExeMode <= ExecuteModes_Load;
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b0101 : begin
                ExeMode <= ExecuteModes_WriteNoInc;
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b0110 : begin
                RegSelMode <= RegSelectModes_XSel;
                if(when_Spinal1802_l391) begin
                  ExeMode <= ExecuteModes_Load;
                end else begin
                  if(when_Spinal1802_l393) begin
                    ExeMode <= ExecuteModes_WriteNoInc;
                  end
                end
              end
              4'b0111 : begin
                if(when_Spinal1802_l398) begin
                  RegSelMode <= RegSelectModes_XSel;
                  ExeMode <= ExecuteModes_Load;
                end else begin
                  if(when_Spinal1802_l401) begin
                    RegSelMode <= RegSelectModes_XSel;
                    ExeMode <= ExecuteModes_WriteDec;
                  end else begin
                    if(when_Spinal1802_l404) begin
                      RegSelMode <= RegSelectModes_XSel;
                      ExeMode <= ExecuteModes_LoadNoInc;
                    end else begin
                      if(when_Spinal1802_l407) begin
                        RegSelMode <= RegSelectModes_XSel;
                        ExeMode <= ExecuteModes_WriteNoInc;
                      end else begin
                        if(when_Spinal1802_l410) begin
                          RegSelMode <= RegSelectModes_Stack2;
                          ExeMode <= ExecuteModes_WriteDec;
                        end else begin
                          if(when_Spinal1802_l414) begin
                            ExeMode <= ExecuteModes_Load;
                          end
                        end
                      end
                    end
                  end
                end
              end
              4'b1000 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b1001 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b1010 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b1011 : begin
                RegSelMode <= RegSelectModes_NSel;
              end
              4'b1100 : begin
                ExeMode <= ExecuteModes_Load;
              end
              4'b1111 : begin
                if(when_Spinal1802_l434) begin
                  RegSelMode <= RegSelectModes_XSel;
                  ExeMode <= ExecuteModes_LoadNoInc;
                end else begin
                  if(when_Spinal1802_l437) begin
                    ExeMode <= ExecuteModes_Load;
                  end
                end
              end
              default : begin
              end
            endcase
          end
        end
        CoreFMS_enumDef_S1_Execute : begin
          SC <= 2'b01;
          if(when_Spinal1802_l456) begin
            if(when_Spinal1802_l458) begin
              RegOpMode <= RegOperationModes_Inc;
            end else begin
              if(when_Spinal1802_l460) begin
                RegOpMode <= RegOperationModes_Dec;
              end
            end
          end
          if(when_Spinal1802_l469) begin
            RegOpMode <= RegOperationModes_None;
          end
          if(when_Spinal1802_l473) begin
            case(I)
              4'b0101 : begin
                BusControl <= BusControlModes_DReg;
              end
              4'b0111 : begin
                if(when_Spinal1802_l479) begin
                  BusControl <= BusControlModes_TReg;
                end else begin
                  if(when_Spinal1802_l481) begin
                    BusControl <= BusControlModes_DReg;
                  end
                end
              end
              default : begin
              end
            endcase
          end
          if(when_Spinal1802_l488) begin
            case(I)
              4'b0000 : begin
                if(when_Spinal1802_l491) begin
                  DRegControl <= DRegControlModes_BusIn;
                end
              end
              4'b0001 : begin
                RegOpMode <= RegOperationModes_Inc;
              end
              4'b0010 : begin
                RegOpMode <= RegOperationModes_Dec;
              end
              4'b0011 : begin
                if(Branch) begin
                  RegOpMode <= RegOperationModes_LoadLower;
                end
              end
              4'b0100 : begin
                DRegControl <= DRegControlModes_BusIn;
              end
              4'b0110 : begin
                if(when_Spinal1802_l506) begin
                  RegOpMode <= RegOperationModes_Inc;
                end else begin
                  if(when_Spinal1802_l508) begin
                    DRegControl <= DRegControlModes_BusIn;
                  end
                end
              end
              4'b0111 : begin
                if(when_Spinal1802_l513) begin
                  X <= Bus_1[7 : 4];
                  P <= Bus_1[3 : 0];
                end else begin
                  if(when_Spinal1802_l517) begin
                    DRegControl <= DRegControlModes_BusIn;
                  end else begin
                    if(when_Spinal1802_l519) begin
                      DRegControl <= DRegControlModes_ALU_AddCarry;
                    end else begin
                      if(when_Spinal1802_l521) begin
                        DRegControl <= DRegControlModes_ALU_SubDBorrow;
                      end else begin
                        if(when_Spinal1802_l523) begin
                          DRegControl <= DRegControlModes_ALU_RSHR;
                        end else begin
                          if(when_Spinal1802_l525) begin
                            DRegControl <= DRegControlModes_ALU_SubMBorrow;
                          end else begin
                            if(!when_Spinal1802_l527) begin
                              if(!when_Spinal1802_l529) begin
                                if(when_Spinal1802_l531) begin
                                  DRegControl <= DRegControlModes_ALU_LSHR;
                                end
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
              4'b1000 : begin
                BusControl <= BusControlModes_RLower;
                DRegControl <= DRegControlModes_BusIn;
              end
              4'b1001 : begin
                BusControl <= BusControlModes_RUpper;
                DRegControl <= DRegControlModes_BusIn;
              end
              4'b1010 : begin
                BusControl <= BusControlModes_DReg;
                RegOpMode <= RegOperationModes_LoadLower;
              end
              4'b1011 : begin
                BusControl <= BusControlModes_DReg;
                RegOpMode <= RegOperationModes_LoadUpper;
              end
              4'b1100 : begin
                if(when_Spinal1802_l554) begin
                  if(when_Spinal1802_l555) begin
                    RegOpMode <= RegOperationModes_LoadTemp;
                  end
                end else begin
                  if(when_Spinal1802_l558) begin
                    RegOpMode <= RegOperationModes_LoadJump;
                  end
                end
              end
              4'b1101 : begin
                P <= N;
              end
              4'b1110 : begin
                X <= N;
              end
              4'b1111 : begin
                if(when_Spinal1802_l572) begin
                  DRegControl <= DRegControlModes_BusIn;
                end else begin
                  if(when_Spinal1802_l574) begin
                    DRegControl <= DRegControlModes_ALU_OR;
                  end else begin
                    if(when_Spinal1802_l576) begin
                      DRegControl <= DRegControlModes_ALU_AND;
                    end else begin
                      if(when_Spinal1802_l578) begin
                        DRegControl <= DRegControlModes_ALU_XOR;
                      end else begin
                        if(when_Spinal1802_l580) begin
                          DRegControl <= DRegControlModes_ALU_Add;
                        end else begin
                          if(when_Spinal1802_l582) begin
                            DRegControl <= DRegControlModes_ALU_SubD;
                          end else begin
                            if(when_Spinal1802_l584) begin
                              DRegControl <= DRegControlModes_ALU_RSH;
                            end else begin
                              if(when_Spinal1802_l586) begin
                                DRegControl <= DRegControlModes_ALU_SubM;
                              end else begin
                                if(when_Spinal1802_l588) begin
                                  DRegControl <= DRegControlModes_BusIn;
                                end else begin
                                  if(when_Spinal1802_l590) begin
                                    DRegControl <= DRegControlModes_ALU_LSH;
                                  end
                                end
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
              default : begin
              end
            endcase
          end
          if(when_Spinal1802_l597) begin
            if(when_Spinal1802_l598) begin
              X <= P;
            end
            if(when_Spinal1802_l602) begin
              ExeMode <= ExecuteModes_None;
            end else begin
              if(when_Spinal1802_l604) begin
                ExeMode <= ExecuteModes_LongLoad;
              end else begin
                if(when_Spinal1802_l606) begin
                  ExeMode <= ExecuteModes_LongContinue;
                end else begin
                  if(when_Spinal1802_l608) begin
                    ExeMode <= ExecuteModes_LongLoad;
                  end
                end
              end
            end
            if(Idle) begin
              RegSelMode <= RegSelectModes_DMA0;
            end else begin
              RegSelMode <= RegSelectModes_PSel;
              RegOpMode <= RegOperationModes_None;
              DRegControl <= DRegControlModes_None;
            end
          end
          if(!when_Spinal1802_l621) begin
            if(StateCounter_willOverflow) begin
              if(when_Spinal1802_l625) begin
                RegSelMode <= RegSelectModes_DMA0;
                ExeMode <= ExecuteModes_DMA_In;
              end else begin
                if(when_Spinal1802_l629) begin
                  RegSelMode <= RegSelectModes_DMA0;
                  ExeMode <= ExecuteModes_DMA_Out;
                end else begin
                  if(when_Spinal1802_l633) begin
                    ExeMode <= ExecuteModes_None;
                  end else begin
                    if(when_Spinal1802_l636) begin
                      ExeMode <= ExecuteModes_None;
                    end
                  end
                end
              end
            end
          end
        end
        CoreFMS_enumDef_S2_DMA : begin
          SC <= 2'b10;
          if(when_Spinal1802_l651) begin
            BusControl <= BusControlModes_DataIn;
            RegSelMode <= RegSelectModes_DMA0;
          end
          if(when_Spinal1802_l656) begin
            RegOpMode <= RegOperationModes_Inc;
          end
          if(when_Spinal1802_l660) begin
            RegOpMode <= RegOperationModes_None;
          end
          if(!when_Spinal1802_l663) begin
            if(StateCounter_willOverflow) begin
              if(when_Spinal1802_l666) begin
                ExeMode <= ExecuteModes_None;
                if(!when_Spinal1802_l668) begin
                  RegSelMode <= RegSelectModes_PSel;
                end
              end
            end
          end
        end
        CoreFMS_enumDef_S3_INT : begin
          SC <= 2'b11;
          if(when_Spinal1802_l688) begin
            P <= 4'b0001;
            X <= 4'b0010;
          end
          if(!when_Spinal1802_l693) begin
            if(StateCounter_willOverflow) begin
              if(when_Spinal1802_l697) begin
                ExeMode <= ExecuteModes_DMA_In;
              end else begin
                if(when_Spinal1802_l700) begin
                  ExeMode <= ExecuteModes_DMA_Out;
                end else begin
                  RegSelMode <= RegSelectModes_PSel;
                end
              end
            end
          end
        end
        default : begin
        end
      endcase
      if(when_StateMachine_l249) begin
        _zz_when_State_l238 <= 4'b1001;
      end
    end
  end


endmodule

module CDP1871 (
  input               io_Latch,
  input      [7:0]    io_KeyCode,
  output              io_Ready,
  input               io_TPB,
  input               io_MRD_,
  input               io_N3_,
  output              io_DA_,
  output              io_RPT_,
  output     [7:0]    io_DataOut,
  output              io_KBD_SEL,
  input               clk,
  input               reset
);

  reg                 ready;
  reg        [7:0]    keycode;
  reg                 da;
  wire                rpt;
  wire                kbd_sel;
  wire                when_CDP1871_l33;

  assign rpt = 1'b0;
  assign kbd_sel = ((! io_N3_) && io_MRD_);
  assign when_CDP1871_l33 = (ready && io_Latch);
  assign io_DataOut = (kbd_sel ? keycode : 8'h0);
  assign io_DA_ = (! da);
  assign io_RPT_ = (! rpt);
  assign io_KBD_SEL = kbd_sel;
  assign io_Ready = ready;
  always @(posedge clk) begin
    if(reset) begin
      ready <= 1'b1;
      keycode <= 8'h0;
      da <= 1'b0;
    end else begin
      if(when_CDP1871_l33) begin
        keycode <= io_KeyCode;
        ready <= 1'b0;
        da <= 1'b1;
      end
      if(kbd_sel) begin
        da <= 1'b0;
        ready <= 1'b1;
      end
    end
  end


endmodule

module CDP1870 (
  output              io_HSync_,
  output              io_VSync_,
  output              io_Display_,
  output              io_PreDisplay_,
  output              io_AddSTB_,
  output              io_CPUCLK,
  input      [5:0]    io_CDB_in,
  input      [5:0]    io_CDB_out,
  input      [2:0]    io_CCB_in,
  input      [2:0]    io_CCB_out,
  input      [7:0]    io_DataIn,
  output     [7:0]    io_DataOut,
  input               io_N3_,
  input               io_TPB,
  input               io_MRD,
  input               io_CMSEL,
  input               io_PalOrNTSC,
  output              io_CompSync_,
  output              io_Pixel,
  output     [2:0]    io_Color,
  output              io_Burst,
  input               clk,
  input               reset
);

  wire                when_CDP1870_l36;
  reg        [7:0]    CMD_Reg;
  wire                FresHorz;
  wire       [1:0]    COLB;
  wire                DispOff_Next;
  wire                CFC;
  wire       [2:0]    BKG;
  reg        [8:0]    VerticalCounter;
  reg        [5:0]    HorizontalCounter;
  reg        [3:0]    TimingCounter;
  wire                _zz_when_CDP1870_l48;
  reg                 _zz_when_CDP1870_l48_regNext;
  wire                when_CDP1870_l48;
  reg                 DispOff;
  reg        [5:0]    PixelShifter;
  reg        [2:0]    Color;
  wire                VSync_NTSC;
  wire                VSync_PAL;
  wire                VSync;
  wire                VDisplay_NTSC;
  wire                VDisplay_PAL;
  wire                VDisplay;
  wire                VPreDisplay_NTSC;
  wire                VPreDisplay_PAL;
  wire                VPreDisplay;
  wire                VReset_NTSC;
  wire                VReset_PAL;
  wire                VReset;
  wire                HSync;
  wire                Burst;
  wire                HorizontalBlanking;
  wire                VerticalBlanking;
  wire                HDisplay;
  wire                DotClk6;
  wire                DotClk12;
  wire                DotClk;
  wire                PixelClk;
  wire                AddSTB_;
  reg        [2:0]    ColorOut;
  wire                when_CDP1870_l96;
  wire                when_CDP1870_l103;
  wire                when_CDP1870_l116;
  wire                _zz_io_CPUCLK;
  reg                 _zz_io_CPUCLK_regNext;

  assign when_CDP1870_l36 = (((! io_N3_) && (! io_MRD)) && io_TPB);
  assign FresHorz = CMD_Reg[7];
  assign COLB = CMD_Reg[6 : 5];
  assign DispOff_Next = CMD_Reg[4];
  assign CFC = CMD_Reg[3];
  assign BKG = CMD_Reg[2 : 0];
  assign _zz_when_CDP1870_l48 = (VerticalCounter == 9'h0);
  assign when_CDP1870_l48 = (_zz_when_CDP1870_l48 && (! _zz_when_CDP1870_l48_regNext));
  assign VSync_NTSC = ((9'h102 <= VerticalCounter) && (VerticalCounter <= 9'h106));
  assign VSync_PAL = (9'h134 <= VerticalCounter);
  assign VSync = (io_PalOrNTSC ? VSync_PAL : VSync_NTSC);
  assign VDisplay_NTSC = ((9'h024 <= VerticalCounter) && (VerticalCounter < 9'h0e4));
  assign VDisplay_PAL = ((9'h02c <= VerticalCounter) && (VerticalCounter < 9'h104));
  assign VDisplay = (io_PalOrNTSC ? VDisplay_PAL : VDisplay_NTSC);
  assign VPreDisplay_NTSC = ((9'h023 <= VerticalCounter) && (VerticalCounter < 9'h0e4));
  assign VPreDisplay_PAL = ((9'h02b <= VerticalCounter) && (VerticalCounter < 9'h104));
  assign VPreDisplay = (io_PalOrNTSC ? VPreDisplay_PAL : VPreDisplay_NTSC);
  assign VReset_NTSC = (VerticalCounter == 9'h106);
  assign VReset_PAL = (VerticalCounter == 9'h138);
  assign VReset = (io_PalOrNTSC ? VReset_PAL : VReset_NTSC);
  assign HSync = ((6'h38 <= HorizontalCounter) && (HorizontalCounter <= 6'h3b));
  assign Burst = ((6'h01 <= HorizontalCounter) && (HorizontalCounter <= 6'h04));
  assign HorizontalBlanking = ((HorizontalCounter <= 6'h05) || (6'h36 <= HorizontalCounter));
  assign VerticalBlanking = ((VerticalCounter <= 9'h00a) || (9'h0fc <= VerticalCounter));
  assign HDisplay = ((6'h0a <= HorizontalCounter) && (HorizontalCounter < 6'h32));
  assign DotClk6 = ((TimingCounter == 4'b0000) || (TimingCounter == 4'b0110));
  assign DotClk12 = (TimingCounter == 4'b0000);
  assign DotClk = (FresHorz ? DotClk6 : DotClk12);
  assign PixelClk = (FresHorz ? 1'b1 : (! TimingCounter[0]));
  assign AddSTB_ = (((HDisplay && (! DispOff)) && VDisplay) ? (! DotClk) : 1'b1);
  always @(*) begin
    case(COLB)
      2'b00 : begin
        ColorOut = {{Color[0],Color[1]},Color[2]};
      end
      2'b01 : begin
        ColorOut = {{Color[0],Color[2]},Color[1]};
      end
      2'b10 : begin
        ColorOut = {{Color[2],Color[0]},Color[1]};
      end
      default : begin
        ColorOut = {{Color[2],Color[0]},Color[1]};
      end
    endcase
  end

  assign when_CDP1870_l96 = (TimingCounter == 4'b1011);
  assign when_CDP1870_l103 = (HorizontalCounter == 6'h3b);
  assign when_CDP1870_l116 = (! AddSTB_);
  assign io_Pixel = PixelShifter[5];
  assign io_Color = (PixelShifter[5] ? ColorOut : BKG);
  assign io_Burst = Burst;
  assign io_CompSync_ = (! (HSync ^ VSync));
  assign io_HSync_ = (! HSync);
  assign io_VSync_ = (! VSync);
  assign io_Display_ = ((! DispOff) ? (! VDisplay) : 1'b1);
  assign io_PreDisplay_ = ((! DispOff) ? (! VPreDisplay) : 1'b1);
  assign io_AddSTB_ = AddSTB_;
  assign io_DataOut = 8'h0;
  assign _zz_io_CPUCLK = TimingCounter[0];
  assign io_CPUCLK = (_zz_io_CPUCLK && (! _zz_io_CPUCLK_regNext));
  always @(posedge clk) begin
    if(reset) begin
      CMD_Reg <= 8'h10;
      VerticalCounter <= 9'h0;
      HorizontalCounter <= 6'h3b;
      TimingCounter <= 4'b0000;
      DispOff <= 1'b1;
    end else begin
      if(when_CDP1870_l36) begin
        CMD_Reg <= io_DataIn;
      end
      if(when_CDP1870_l48) begin
        DispOff <= DispOff_Next;
      end
      if(when_CDP1870_l96) begin
        TimingCounter <= 4'b0000;
      end else begin
        TimingCounter <= (TimingCounter + 4'b0001);
      end
      if(DotClk6) begin
        if(when_CDP1870_l103) begin
          HorizontalCounter <= 6'h0;
          if(VReset) begin
            VerticalCounter <= 9'h0;
          end else begin
            VerticalCounter <= (VerticalCounter + 9'h001);
          end
        end else begin
          HorizontalCounter <= (HorizontalCounter + 6'h01);
        end
      end
    end
  end

  always @(posedge clk) begin
    _zz_when_CDP1870_l48_regNext <= _zz_when_CDP1870_l48;
    if(PixelClk) begin
      if(when_CDP1870_l116) begin
        PixelShifter <= io_CDB_in;
        Color <= io_CCB_in;
      end else begin
        PixelShifter <= (PixelShifter <<< 1);
      end
    end
    _zz_io_CPUCLK_regNext <= _zz_io_CPUCLK;
  end


endmodule

module CDP1869 (
  input               io_HSync_,
  input               io_Display_,
  input               io_AddSTB_,
  input      [2:0]    io_N,
  output              io_N3_,
  input               io_TPA,
  input               io_TPB,
  input               io_MRD,
  input               io_MWR,
  input      [7:0]    io_Addr,
  input      [7:0]    io_DataIn,
  output     [7:0]    io_DataOut,
  output              io_CMWR_,
  output              io_CMSEL,
  output     [2:0]    io_CMA,
  output              io_CMA3_PMA10,
  output              io_PMWR_,
  output              io_PMSEL,
  output     [9:0]    io_PMA,
  output     [3:0]    io_Sound,
  input               clk,
  input               reset
);

  wire       [4:0]    _zz_RemapRCA;
  wire       [4:0]    _zz_RemapRCA_1;
  wire       [10:0]   _zz_io_PMA;
  reg                 io_TPA_regNext;
  wire                when_CDP1869_l39;
  reg        [7:0]    UpperAddr;
  wire       [15:0]   Addr16;
  wire                when_CDP1869_l42;
  reg        [8:0]    HMA_Reg;
  wire                when_CDP1869_l43;
  reg        [10:0]   PMA_Reg;
  wire                when_CDP1869_l44;
  reg        [15:0]   WN_Reg;
  wire                when_CDP1869_l45;
  reg        [14:0]   SN_Reg;
  reg        [4:0]    RCA;
  reg        [10:0]   HMA;
  reg        [10:0]   RPA;
  reg        [8:0]    ToneDiv;
  reg        [6:0]    ToneCounter;
  reg                 Tone_FF;
  wire                FresVert;
  wire                DoublePage;
  wire                HiRes16Line;
  wire                NineLine;
  wire                CmemAccessMode;
  wire       [4:0]    RCA_NEXT;
  wire       [10:0]   HMA_NEXT_20;
  wire       [10:0]   HMA_NEXT_40;
  wire       [4:0]    RCA_OUTPUT;
  wire                RCA_15;
  wire                RCA_7;
  wire                RCA_8;
  wire       [3:0]    RemapRCA;
  wire                PMSEL;
  wire                CMSEL;
  wire       [6:0]    Tone;
  wire                Tone_Off;
  wire       [2:0]    Tone_Freq_Sel;
  wire       [3:0]    Tone_Amp;
  reg                 Tone_Clk;
  reg                 io_AddSTB__regNext;
  wire                when_CDP1869_l97;
  wire                when_CDP1869_l98;
  reg                 io_HSync__regNext;
  wire                when_CDP1869_l104;
  wire                when_CDP1869_l105;
  reg                 io_TPA_regNext_1;
  reg                 io_TPB_regNext;
  reg                 io_TPA_regNext_2;
  reg                 io_TPB_regNext_1;
  wire                when_CDP1869_l116;
  reg                 Tone_Clk_regNext;
  wire                when_CDP1869_l120;
  wire                when_CDP1869_l121;

  assign _zz_RemapRCA = RCA;
  assign _zz_RemapRCA_1 = RCA;
  assign _zz_io_PMA = RPA;
  assign when_CDP1869_l39 = (io_TPA && (! io_TPA_regNext));
  assign Addr16 = {UpperAddr,io_Addr};
  assign when_CDP1869_l42 = (((io_N == 3'b111) && (! io_MRD)) && io_TPB);
  assign when_CDP1869_l43 = (((io_N == 3'b110) && (! io_MRD)) && io_TPB);
  assign when_CDP1869_l44 = (((io_N == 3'b101) && (! io_MRD)) && io_TPB);
  assign when_CDP1869_l45 = (((io_N == 3'b100) && (! io_MRD)) && io_TPB);
  assign FresVert = WN_Reg[7];
  assign DoublePage = WN_Reg[6];
  assign HiRes16Line = WN_Reg[5];
  assign NineLine = WN_Reg[3];
  assign CmemAccessMode = WN_Reg[0];
  assign RCA_NEXT = (RCA + 5'h01);
  assign HMA_NEXT_20 = (HMA + 11'h014);
  assign HMA_NEXT_40 = (HMA + 11'h028);
  assign RCA_OUTPUT = ((FresVert ? RCA : RCA) >>> 1);
  assign RCA_15 = (((RCA < 5'h0f) && NineLine) && (HiRes16Line || (! FresVert)));
  assign RCA_7 = ((RCA < 5'h07) && NineLine);
  assign RCA_8 = ((RCA < 5'h08) && (! NineLine));
  assign RemapRCA = (FresVert ? _zz_RemapRCA[3 : 0] : _zz_RemapRCA_1[4 : 1]);
  assign PMSEL = ((8'hf8 <= UpperAddr) && io_Display_);
  assign CMSEL = (((8'hf4 <= UpperAddr) && (UpperAddr <= 8'hf7)) && io_Display_);
  assign Tone = SN_Reg[14 : 8];
  assign Tone_Off = SN_Reg[7];
  assign Tone_Freq_Sel = SN_Reg[6 : 4];
  assign Tone_Amp = SN_Reg[3 : 0];
  always @(*) begin
    case(Tone_Freq_Sel)
      3'b000 : begin
        Tone_Clk = ToneDiv[8];
      end
      3'b001 : begin
        Tone_Clk = ToneDiv[7];
      end
      3'b010 : begin
        Tone_Clk = ToneDiv[6];
      end
      3'b011 : begin
        Tone_Clk = ToneDiv[5];
      end
      3'b100 : begin
        Tone_Clk = ToneDiv[4];
      end
      3'b101 : begin
        Tone_Clk = ToneDiv[3];
      end
      3'b110 : begin
        Tone_Clk = ToneDiv[2];
      end
      default : begin
        Tone_Clk = ToneDiv[1];
      end
    endcase
  end

  assign when_CDP1869_l97 = ((! io_AddSTB_) && io_AddSTB__regNext);
  assign when_CDP1869_l98 = (11'h3bf <= RPA);
  assign when_CDP1869_l104 = ((! io_HSync_) && io_HSync__regNext);
  assign when_CDP1869_l105 = ((RCA_15 || RCA_7) || RCA_8);
  assign when_CDP1869_l116 = ((((io_TPA && (! io_TPA_regNext_1)) || (io_TPB && (! io_TPB_regNext))) || ((! io_TPA) && io_TPA_regNext_2)) || ((! io_TPB) && io_TPB_regNext_1));
  assign when_CDP1869_l120 = (Tone_Clk && (! Tone_Clk_regNext));
  assign when_CDP1869_l121 = (ToneCounter < Tone);
  assign io_N3_ = (io_N != 3'b011);
  assign io_DataOut = 8'h0;
  assign io_PMSEL = PMSEL;
  assign io_PMA = (io_Display_ ? (CmemAccessMode ? PMA_Reg[9 : 0] : (PMSEL ? Addr16[9 : 0] : 10'h0)) : _zz_io_PMA[9 : 0]);
  assign io_PMWR_ = ((io_Display_ && PMSEL) ? io_MWR : 1'b1);
  assign io_CMSEL = CMSEL;
  assign io_CMWR_ = ((io_Display_ && CMSEL) ? io_MWR : 1'b1);
  assign io_CMA = (io_Display_ ? (CMSEL ? Addr16[2 : 0] : 3'b000) : RemapRCA[2 : 0]);
  assign io_CMA3_PMA10 = (DoublePage ? RPA[10] : RemapRCA[3]);
  assign io_Sound = (Tone_FF ? Tone_Amp : 4'b0000);
  always @(posedge clk) begin
    io_TPA_regNext <= io_TPA;
    io_TPA_regNext_1 <= io_TPA;
    io_TPB_regNext <= io_TPB;
    io_TPA_regNext_2 <= io_TPA;
    io_TPB_regNext_1 <= io_TPB;
    Tone_Clk_regNext <= Tone_Clk;
  end

  always @(posedge clk) begin
    if(reset) begin
      UpperAddr <= 8'h0;
      HMA_Reg <= 9'h0;
      PMA_Reg <= 11'h0;
      WN_Reg <= 16'h0;
      SN_Reg <= 15'h0080;
      RCA <= 5'h0;
      HMA <= 11'h0;
      RPA <= 11'h0;
      ToneDiv <= 9'h0;
      ToneCounter <= 7'h0;
      Tone_FF <= 1'b0;
    end else begin
      if(when_CDP1869_l39) begin
        UpperAddr <= io_Addr;
      end
      if(when_CDP1869_l42) begin
        HMA_Reg <= Addr16[10 : 2];
      end
      if(when_CDP1869_l43) begin
        PMA_Reg <= Addr16[10 : 0];
      end
      if(when_CDP1869_l44) begin
        WN_Reg <= Addr16;
      end
      if(when_CDP1869_l45) begin
        SN_Reg <= Addr16[14 : 0];
      end
      if(io_Display_) begin
        RCA <= 5'h0;
        RPA <= {HMA_Reg,2'b00};
        HMA <= {HMA_Reg,2'b00};
      end else begin
        if(when_CDP1869_l97) begin
          if(when_CDP1869_l98) begin
            RPA <= 11'h0;
          end else begin
            RPA <= (RPA + 11'h001);
          end
        end
        if(when_CDP1869_l104) begin
          if(when_CDP1869_l105) begin
            RCA <= RCA_NEXT;
            RPA <= HMA;
          end else begin
            RCA <= 5'h0;
            HMA <= RPA;
          end
        end
      end
      if(when_CDP1869_l116) begin
        ToneDiv <= (ToneDiv + 9'h001);
      end
      if(when_CDP1869_l120) begin
        if(when_CDP1869_l121) begin
          ToneCounter <= (ToneCounter + 7'h01);
        end else begin
          ToneCounter <= 7'h0;
          Tone_FF <= (! Tone_FF);
        end
      end
    end
  end

  always @(posedge clk) begin
    io_AddSTB__regNext <= io_AddSTB_;
    io_HSync__regNext <= io_HSync_;
  end


endmodule
