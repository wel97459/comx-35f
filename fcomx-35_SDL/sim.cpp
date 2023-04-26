#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>
#include <memory>
#include <vector>
#include <pthread.h>
#include <thread>
#include "sim.h"
#include "crt_core.h"
#include <verilated_fst_c.h>
#include "Vcomx35_test__Syms.h"


#define COLOR_LEVEL (WHITE_LEVEL - 20)
int ccmodI[CRT_CC_SAMPLES]; /* color phase for mod */
int ccmodQ[CRT_CC_SAMPLES]; /* color phase for mod */
int ccburst[CRT_CC_SAMPLES]; /* color phase for burst */

void (*sim_draw)();
Uint32 *screenPixels;
SDL_Texture *screen;
unsigned char *sim_video;
struct CRT *sim_crt;
static uint64_t vidTime = 0;
static int PhaseOffset = 1;
struct COLOR_SETTINGS {
    char *text[8];
    uint8_t index;
    int Amplitude[8];
    int Phase[8];
    int PhaseAmp[8];
};

static COLOR_SETTINGS colors;

VerilatedFstC* m_trace;
Vcomx35_test__Syms *comx_Syms;
Vcomx35_test *comx;

Uint64 main_time=0;
Uint64 main_trace=0;
Uint8 trace=1;

Uint8 rom[0x4000];
Uint8 ram[0x8000];
Uint8 pram[0x400];
Uint8 cram[0x400];

Uint8 Display_Edge=0;
Uint8 HSync_Edge=0;
Uint8 VSync_Edge=0;
Uint8 Ready_Edge=0;
Uint8 Burst_Edge=0;
Uint8 Video_Last=0;
Uint8 MW_Last=0;
Uint8 MR_Last=0;
Uint8 P_Last=0;
Uint16 R3_Last=0;
Uint16 ADDR_Last=0;
Uint8 colorBurst=1;

Uint16 drawX, drawY, scanX;

Uint16 FrameCount = 0;
Uint16 FrameCurent = 0;

Uint64 ticksLast = 0 ;
char tmpstr[64];

//char basicStr[]="\r5 i=0\r10 cpos(3,0)\r20 pr i;\r30 i=i+1\r40 goto 10\rrun\r";
//char basicStr[]="\rcaall(@4401)\r";
char basicStr[]="\rtoone(2,2,8)\r";
//char basicStr[]="\rshhape(20, \"00000000dfffffdf00\")\r";
char *keyInput = &basicStr[0];

char ComxKeyboard(char keyCode)
{
    Uint8 keyboardCode_ = keyCode;

    switch(keyboardCode_)
    {
        case '\r':
            keyboardCode_ = 0x80;
        break;

        case '@':keyboardCode_ = 0x20; break;
        case '#':keyboardCode_ = 0x23; break;
        case '\'': keyboardCode_ = 0x27; break;
        case '[':keyboardCode_ = 0x28; break;
        case ']':keyboardCode_ = 0x29; break;
        case ':':keyboardCode_ = 0x2a; break;
        case ';':keyboardCode_ = 0x2b; break;
        case '<':keyboardCode_ = 0x2c; break;
        case '=':keyboardCode_ = 0x2d; break;
        case '>':keyboardCode_ = 0x2e; break;
        case '\\':keyboardCode_ = 0x2f; break;
        case '.':keyboardCode_ = 0x3a; break;
        case ',':keyboardCode_ = 0x3b; break;
        case '(':keyboardCode_ = 0x3c; break;
        case '^':keyboardCode_ = 0x3d; break;
        case ')':keyboardCode_ = 0x3e; break;
        case '_':keyboardCode_ = 0x3f; break;
        case '?':keyboardCode_ = 0x40; break;
        case '+':keyboardCode_ = 0x5b; break;
        case '-':keyboardCode_ = 0x5c; break;
        case '*':keyboardCode_ = 0x5d; break;
        case '/':keyboardCode_ = 0x5e; break;
        case ' ':keyboardCode_ = 0x5f; break;
        case '\b':keyboardCode_ = 0x86; break;
    }
    if (keyboardCode_ >= 0x90)  keyboardCode_ &= 0x7f;
    return keyboardCode_;
}

int loadFile(const char *filename, Uint8 *pointer, const Uint32 len)
{
    FILE *fp = fopen(filename, "r");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }

    fseek(fp, 0L, SEEK_END);
    Uint32 fsize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);

    if(fsize > len){
        printf("File is to big!\n");
        fclose(fp);
        return -2;
    }

    size_t s = fread(pointer, 1, fsize, fp);
    fclose(fp);

    return 0;
}

int saveFile(const char *filename, Uint8 *pointer, const Uint32 len)
{
    FILE *fp = fopen(filename, "w+");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }
    size_t s = fwrite(pointer, 1, len, fp);
    fclose(fp);

    return 0;
}

void genIQ()
{
    int sn, cs, n;
    for (int x = 0; x < CRT_CC_SAMPLES; x++) {
        n = x * (360 / CRT_CC_SAMPLES);
        crt_sincos14(&sn, &cs, (n + 33) * 8192 / 180);
        ccburst[x] = sn >> 10;
        crt_sincos14(&sn, &cs, n * 8192 / 180);
        ccmodI[x] = sn >> 10;
        crt_sincos14(&sn, &cs, (n - 90) * 8192 / 180);
        ccmodQ[x] = sn >> 10;
    }
}

void sim_init(unsigned char *v, SDL_Texture *td, void (*d)(), struct CRT *c){
    //screenPixels = p;
    sim_draw = d;
    screen = td;
    sim_video = v;
    sim_crt = c;

    SDL_UpdateTexture(screen, NULL, screenPixels, 240 * sizeof(Uint32));
    sim_draw();

    printf("Started.\n");
    genIQ();
    loadFile("../data/comx35.1.3.bin", rom, 0x4000);
    comx = new Vcomx35_test();
    comx_Syms = comx->vlSymsp;

	#ifdef TRACE
		Verilated::traceEverOn(true);
		m_trace = new VerilatedFstC;
		comx->trace(m_trace, 99);
		m_trace->open ("simx.fst");
	#endif

    printf("CRT_INPUT_SIZE: %i\n", CRT_INPUT_SIZE);
}

void sim_keyevent(int key){
    if (key == SDLK_9 && colors.index > 0) {
        colors.index -= 1;
        printf("Index:%i\n", colors.index);
    }
    if (key == SDLK_0 && colors.index < 8) {
        colors.index += 1;
        printf("Index:%i\n", colors.index);
    }
    if (key == SDLK_o) {
        colors.Amplitude[colors.index] -= 1000;
        printf("Amplitude[%u]:%i\n",colors.index, colors.Amplitude[colors.index]);
    }
    if (key == SDLK_p) {
        colors.Amplitude[colors.index] += 1000;
        printf("Amplitude[%u]:%i\n",colors.index, colors.Amplitude[colors.index]);
    }
    if (key == SDLK_k) {
        colors.Phase[colors.index]-= 50;
        printf("Phase[%u]:%i\n",colors.index, colors.Phase[colors.index]);
    }
    if (key == SDLK_l) {
        colors.Phase[colors.index]+= 50;
        printf("Phase[%u]:%i\n",colors.index, colors.Phase[colors.index]);
    }
    if (key == SDLK_n) {
        colors.PhaseAmp[colors.index]-= 50;
        printf("PhaseAmp[%u]:%i\n",colors.index, colors.PhaseAmp[colors.index]);
    }
    if (key == SDLK_m) {
        colors.PhaseAmp[colors.index]+= 50;
        printf("PhaseAmp[%u]:%i\n",colors.index, colors.PhaseAmp[colors.index]);
    }
}

Uint32 colorsRGB[]={
    0x00000000,
    0x0000FF00,
    0x000000FF,
    0x0000FFFF,
    0x00FF0000,
    0x00FFFF00,
    0x00FF00FF,
    0x00FFFFFF,
};

void doNTSC(int CompSync, int Video, int Burst, int Color)
{	
    int ire = -40, fi, fq, fy;
    int pA;
    int rA, gA, bA;
    int rB = 127, gB = 127, bB = 127;
	if(CompSync) ire=BLANK_LEVEL;
	if(Video) ire=WHITE_LEVEL;

    uint32_t i;
    int xoff;
    for (i = ns2pos(vidTime); i < ns2pos(vidTime+DOT_ns); i++)
    {
        xoff = i % CRT_CC_SAMPLES;
        if(Burst) ire = ccburst[(i + 0) & 3];
        
        if(Color > 0) {
            ire = BLACK_LEVEL ;

            pA = colorsRGB[Color];
            bA = (pA >> 16) & 0xff;
            gA = (pA >>  8) & 0xff;
            rA = (pA >>  0) & 0xff;

            fy = (19595 * rA + 38470 * gA +  7471 * bA) >> 14;
            fi = (39059 * rA - 18022 * gA - 21103 * bA) >> 14;
            fq = (13894 * rA - 34275 * gA + 20382 * bA) >> 14;

            fy = fy;
            fi = fi * ccmodI[xoff] >> 4;
            fq = fq * ccmodQ[xoff] >> 4;
            ire += (fy + fi + fq) * (WHITE_LEVEL * 100 / 100) >> 10;
            if (ire < 0)   ire = 0;
            if (ire > 110) ire = 110;
        }
        sim_crt->analog[i] = ire;
    }

    vidTime+=DOT_ns;
	return;
}

void sim_run(){
    comx->reset = !(main_time>10);
    comx->io_Start = (main_time>15);
    comx->io_Wait = true;

    if (comx->io_MRD == false && comx->io_Addr16 < 0x4000) {
        comx->io_DataIn = rom[comx->io_Addr16 & 0x3fff];
    } else if (comx->io_MRD == false && comx->io_Addr16 >= 0x4000 && comx->io_Addr16 < 0xC000) {
        comx->io_DataIn = ram[(comx->io_Addr16 & 0x7fff) - 0x4000];
    } else {
        comx->io_DataIn = 0x00;
    }
    
    if (comx->io_MWR == false && comx->io_Addr16 > 0x3fff && comx->io_Addr16 < 0xC000) {
        ram[(comx->io_Addr16 & 0x7fff) - 0x4000] = comx->io_DataOut;
    }

    if(comx->io_PMWR_== false){
        pram[comx->io_PMA] = comx->io_PMD_Out;
    }
    
    if(comx->io_CMWR_ == false){
        cram[comx->io_CMA] = comx->io_CMD_Out;
    }

    comx->io_PMD_In = pram[comx->io_PMA];
    comx->io_CMD_In = cram[comx->io_CMA];

    if(!comx->io_KBD_Ready){
        comx->io_KBD_Latch = false;
        comx->io_KBD_KeyCode = 0x00;
        FrameCurent = FrameCount + 4;
    }

    if(FrameCount == 10 && comx->io_KBD_Ready){
            comx->io_KBD_Latch = true;
            comx->io_KBD_KeyCode = ComxKeyboard(*(keyInput));
    } 

    if(FrameCount >= 84 && FrameCount > FrameCurent && comx->io_KBD_Ready && *keyInput != 0x00){
            comx->io_KBD_Latch = true;
            comx->io_KBD_KeyCode = ComxKeyboard(*(keyInput));
    }

    if(Ready_Edge && !comx->io_KBD_Ready) keyInput++;


    if(!comx->io_VSync_ && VSync_Edge){
        sim_draw();
        sprintf(tmpstr,"Frames/Frame%04i.png",FrameCount++);
        Uint64 ticks = SDL_GetTicks64();
        //printf("Frame: %i\n", FrameCount);
        ticksLast = ticks;
        screenshot(tmpstr);
        vidTime = 0;
        memset(sim_crt->analog, 0, CRT_INPUT_SIZE);

        if(FrameCount == 84){
            loadFile("/home/winston/Projects/C/RCA1802Toolkit/comx_testing/tetris/main.comx", &ram[0x401], 0x8000);
            saveFile("../data/debug_ram.bin", ram, 0x8000);
        } 
        if(FrameCount == 160){
            saveFile("../data/debug_ram_CA.bin", cram, 0x400);
            saveFile("../data/debug_ram_PA.bin", pram, 0x400);
        } 
    }
    doNTSC(comx->io_Sync, comx->io_Pixel, comx->io_Burst, comx->io_Color);
    if(trace && 0){
        if(P_Last != comx_Syms->TOP__comx35_test__clockedArea_CPU.__Vdly__P)
        {
            if(comx_Syms->TOP__comx35_test__clockedArea_CPU.P == 5 || comx_Syms->TOP__comx35_test__clockedArea_CPU.P == 4) R3_Last=comx_Syms->TOP__comx35_test__clockedArea_CPU.R_3;
            if(P_Last == 5)printf("Cret: 0x%04x->0x%04x\n", R3_Last, comx_Syms->TOP__comx35_test__clockedArea_CPU.R_3);
            if(P_Last == 4)printf("Call: 0x%04x->0x%04x\n",R3_Last, comx_Syms->TOP__comx35_test__clockedArea_CPU.R_3);
            if(P_Last == 4 || P_Last == 5)printf("\tR2: 0x%04x\n\n", comx_Syms->TOP__comx35_test__clockedArea_CPU.R_2);
        }
        if(MW_Last < comx_Syms->TOP__comx35_test__clockedArea_CPU.io_MWR){
            if(comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16 > 0xbd00 && comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16 <= 0xbdff){
                printf("0x%04X: Write: 0x%04x = 0x%02x\n", ADDR_Last, comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16, comx_Syms->TOP__comx35_test__clockedArea_CPU.D);
            }
        }
        if(MR_Last < comx_Syms->TOP__comx35_test__clockedArea_CPU.io_MRD){
            if(comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16 > 0xbd00 && comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16 <= 0xbdff){
                printf("0x%04X: Read: 0x%04x = 0x%02x\n", ADDR_Last, comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16, comx_Syms->TOP__comx35_test__clockedArea_CPU.D);
            }
            ADDR_Last = comx_Syms->TOP__comx35_test__clockedArea_CPU.io_Addr16;
        }
    }
    Display_Edge = comx->io_Display_;
    HSync_Edge = comx->io_HSync_;
    VSync_Edge = comx->io_VSync_;
    Ready_Edge = comx->io_KBD_Ready;
    Burst_Edge = comx->io_Burst;
    Video_Last = comx->io_Video;
    P_Last = comx_Syms->TOP__comx35_test__clockedArea_CPU.P;
    MW_Last = comx_Syms->TOP__comx35_test__clockedArea_CPU.io_MWR;
    MR_Last = comx_Syms->TOP__comx35_test__clockedArea_CPU.io_MRD;
    main_time++;
    comx->clk = 1;
    comx->eval();

    #ifdef TRACE
        if(trace){
            main_trace++;
            m_trace->dump (main_trace);
        }
    #endif

    main_time++;
    comx->clk = 0;
    comx->eval();

    #ifdef TRACE
        if(trace){
            main_trace++;
            m_trace->dump (main_trace);
        }
    #endif
}

void sim_end()
{
    printf("Ended.\n");
    comx->final();

    #ifdef TRACE
        m_trace->close();
    #endif
}