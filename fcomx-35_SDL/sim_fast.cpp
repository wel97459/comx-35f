// sim_fast.cpp — emulated-VIS backend for COMX-35 simulation.
//
// Drives the CPU-only comx35_fast Verilator DUT (CPU + FDC + keyboard, no VIS).
// Decodes the VIS register writes off the bus, owns pram/cram in software, and
// renders the framebuffer directly from pram/cram. The RGB framebuffer is then
// encoded to NTSC via crt_modulate(), and the drawCRT callback (passed in from
// main.cpp) does crt_demodulate() + SDL presentation for the CRT look.
//
// Interface: fast_init / fast_keyevent / fast_run / fast_end (selected by --fast).

#include <SDL2/SDL.h>
#include <memory>
#include <vector>
#include <cstring>
#include <cstdio>
#include "sim.h"
#include "crt_core.h"
#include "comx_loader.h"
#include <verilated_fst_c.h>
#include "Vcomx35_fast.h"

// ---- globals (static to avoid colliding with sim.cpp) ----
static void (*sim_draw)();
static SDL_Texture *screen;
static unsigned char *sim_video;
static struct CRT *sim_crt;

static Vcomx35_fast *comx;
static Uint64 main_time = 0;

// ---- memory ----
static Uint8 rom[0x4000];
static Uint8 ram[0x8000];
static Uint8 fdc[0x2000];

// ---- keyboard ----
static char basicStr[] = "\rdoos cat\r";
static char *keyInput = &basicStr[0];
static Uint16 FrameCount = 0;
static Uint16 FrameCurent = 0;
static Uint8 Ready_Edge = 0;

static char ComxKeyboard(char keyCode) {
    Uint8 keyboardCode_ = keyCode;
    switch (keyboardCode_) {
        case '\r': keyboardCode_ = 0x80; break;
        case '@': keyboardCode_ = 0x20; break;
        case '#': keyboardCode_ = 0x23; break;
        case '\'': keyboardCode_ = 0x27; break;
        case '[': keyboardCode_ = 0x28; break;
        case ']': keyboardCode_ = 0x29; break;
        case ':': keyboardCode_ = 0x2a; break;
        case ';': keyboardCode_ = 0x2b; break;
        case '<': keyboardCode_ = 0x2c; break;
        case '=': keyboardCode_ = 0x2d; break;
        case '>': keyboardCode_ = 0x2e; break;
        case '\\': keyboardCode_ = 0x2f; break;
        case '.': keyboardCode_ = 0x3a; break;
        case ',': keyboardCode_ = 0x3b; break;
        case '(': keyboardCode_ = 0x3c; break;
        case '^': keyboardCode_ = 0x3d; break;
        case ')': keyboardCode_ = 0x3e; break;
        case '_': keyboardCode_ = 0x3f; break;
        case '?': keyboardCode_ = 0x40; break;
        case '+': keyboardCode_ = 0x5b; break;
        case '-': keyboardCode_ = 0x5c; break;
        case '*': keyboardCode_ = 0x5d; break;
        case '/': keyboardCode_ = 0x5e; break;
        case ' ': keyboardCode_ = 0x5f; break;
        case '\b': keyboardCode_ = 0x86; break;
        case 0x1: keyboardCode_ = 0x82; break;
        case 0x2: keyboardCode_ = 0x84; break;
        case 0x3: keyboardCode_ = 0x85; break;
        case 0x4: keyboardCode_ = 0x83; break;
    }
    if (keyboardCode_ >= 0x90) keyboardCode_ &= 0x7f;
    return keyboardCode_;
}

static int loadFile(const char *filename, Uint8 *pointer, const Uint32 len) {
    FILE *fp = fopen(filename, "r");
    if (!fp) { printf("Could not open file\n"); return -1; }
    fseek(fp, 0L, SEEK_END);
    Uint32 fsize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    if (fsize > len) { printf("File is too big!\n"); fclose(fp); return -2; }
    fread(pointer, 1, fsize, fp);
    fclose(fp);
    return 0;
}

// ---- VIS emulation state ----
static Uint16 reg3 = 0x10; // CDP1870 CMD_Reg (FresHorz/COLB/DispOff/CFC/BKG)
static Uint16 reg4 = 0x80; // SN_Reg (sound — unused for rendering)
static Uint16 reg5 = 0;    // WN_Reg (FresVert/DoublePage/HiRes16Line/NineLine/CmemAccessMode)
static Uint16 reg6 = 0;    // PMA_Reg
static Uint16 reg7 = 0;    // HMA_Reg

#define PRAM_SIZE 0x400
#define CRAM_SIZE 0x800
static Uint8 pram[PRAM_SIZE];
static Uint8 cram[CRAM_SIZE];

static int pixelWidth = 2, pixelHeight = 2;
static int charactersPerRow = 20, rowsPerScreen = 12;
static int shownLinesPerChar = 9, linesPerChar = 9;
static int pageMemoryMask = 0x3ff;
static int cmaMask = 0xf;
static int backgroundColour = 0;
static int colourFormatControl = 0;
static bool displayOff = false;

#define PCB_MASK        0x7f
#define MAX_CHAR_LINES  16

static void updateDisplayParams() {
    pixelWidth        = (reg3 & 0x80) ? 1 : 2;
    displayOff        = (reg3 & 0x10) != 0;
    charactersPerRow  = (reg3 & 0x80) ? 40 : 20;
    backgroundColour  = reg3 & 0x7;
    colourFormatControl = (reg3 & 0x8) != 0;
    pixelHeight       = (reg5 & 0x80) ? 1 : 2;
    shownLinesPerChar = (reg5 & 0x8) ? 8 : 9;
    linesPerChar      = (reg5 & 0x20) ? MAX_CHAR_LINES : shownLinesPerChar;
    pageMemoryMask    = (reg5 & 0x40) ? 0x7ff : 0x3ff;
    rowsPerScreen     = (reg5 & 0x80) ? 24 : 12;
    cmaMask           = (linesPerChar == 16 || linesPerChar == 9) ? 0xf : 0x7;
}

// Detect a VIS register write from the bus. Returns true if display params changed.
static void decodeVisRegister(Uint8 N, bool MRD, bool TPB, Uint16 Addr16, Uint8 DataOut) {
    if (MRD || !TPB) return;                 // MRD LOW (asserted), TPB HIGH
    bool changed = false;
    if (N == 3) {
        changed = (reg3 != (Uint16)DataOut);
        reg3 = DataOut;
    } else if (N == 4) {
        reg4 = Addr16 & 0x7fff;
    } else if (N == 5) {
        changed = ((reg5 & 0xe8) != (Addr16 & 0xe8));
        reg5 = Addr16;
        if (reg5 & 1) reg6 = Addr16 & 0x7ff;   // cmemAccessMode
    } else if (N == 6) {
        reg6 = Addr16 & 0x7ff;
    } else if (N == 7) {
        changed = (reg7 != (Addr16 & pageMemoryMask & 0x7fc));
        reg7 = Addr16 & pageMemoryMask & 0x7fc;
    } else {
        return;
    }
    if (changed) updateDisplayParams();
}

// ---- color palette (bit2=R, bit1=B, bit0=G) ----
static const Uint8 pal[8][3] = {
    {0,0,0},     {0,255,0},   {0,0,255},   {0,255,255},
    {255,0,0},   {255,255,0}, {255,0,255}, {255,255,255}
};

// ---- framebuffer ----
static unsigned char *fb = NULL;
static int fbW = 0, fbH = 0;

static void putPixel(int x, int y, int c) {
    if (x < 0 || x >= fbW || y < 0 || y >= fbH) return;
    int o = (y * fbW + x) * 3;
    fb[o + 0] = pal[c][0];
    fb[o + 1] = pal[c][1];
    fb[o + 2] = pal[c][2];
}

static void renderCharacter(int cx, int cy, Uint8 v) {
    int pcb = (v & 0x80) ? 1 : 0;
    v &= PCB_MASK;   // charCode

    for (int line = 0; line < linesPerChar; line++) {
        // Font stored as cram[charCode * 16 + line] (maxCharLines = 16).
        Uint8 fontbyte = cram[(v * 16 + line) & (CRAM_SIZE - 1)];

        int clr = 0;
        switch (reg3 & 0x60) {
            case 0x00: if (fontbyte & 0x40) clr += 4; if (fontbyte & 0x80) clr += 2; if (pcb) clr += 1; break;
            case 0x20: if (fontbyte & 0x40) clr += 4; if (pcb)          clr += 2; if (fontbyte & 0x80) clr += 1; break;
            case 0x40:
            case 0x60: if (pcb)          clr += 4; if (fontbyte & 0x40) clr += 2; if (fontbyte & 0x80) clr += 1; break;
        }
        int fg = clr & 0x7;
        int bg = backgroundColour & 0x7;

        int px = cx * 6;
        int py = cy * linesPerChar + line;
        for (int bit = 5; bit >= 0; bit--) {
            int c = (fontbyte >> bit) & 1 ? fg : bg;
            int bx = px * pixelWidth  + (5 - bit) * pixelWidth;
            int by = py * pixelHeight;
            for (int dy = 0; dy < pixelHeight; dy++)
                for (int dx = 0; dx < pixelWidth; dx++)
                    putPixel(bx + dx, by + dy, c);
        }
    }
}

static void renderFrame() {
    for (int y = 0; y < fbH; y++)
        for (int x = 0; x < fbW; x++)
            putPixel(x, y, backgroundColour & 7);

    if (displayOff) return;

    int n = charactersPerRow * rowsPerScreen;
    int addr = reg7 & pageMemoryMask;
    for (int i = 0; i < n; i++) {
        renderCharacter(i % charactersPerRow, i / charactersPerRow,
                        pram[addr & (PRAM_SIZE - 1)]);
        addr++;
        if (addr > pageMemoryMask) addr = 0;
    }
}

// Render pram/cram to RGB and encode to NTSC analog. The drawCRT callback
// (called after this) does crt_demodulate + SDL presentation.
static void renderToCRT() {
    int w = charactersPerRow * 6 * pixelWidth;
    int h = rowsPerScreen * linesPerChar * pixelHeight;
    if (w < 1) w = 240;
    if (h < 1) h = 216;

    if (fbW != w || fbH != h || !fb) {
        fbW = w; fbH = h;
        delete[] fb;
        fb = new unsigned char[fbW * fbH * 3];
    }

    renderFrame();

    struct NTSC_SETTINGS settings;
    memset(&settings, 0, sizeof(settings));
    settings.data     = fb;
    settings.format   = CRT_PIX_FORMAT_RGB;
    settings.w        = w;
    settings.h        = h;
    settings.raw      = 0;
    settings.as_color = 1;

    crt_modulate(sim_crt, &settings);
}

// ---- interface ----

void fast_init(unsigned char *v, SDL_Texture *td, void (*d)(), struct CRT *c) {
    sim_draw  = d;
    screen    = td;
    sim_video = v;
    sim_crt   = c;

    SDL_UpdateTexture(screen, NULL, sim_video, 240 * sizeof(Uint32));
    sim_draw();

    printf("Started (fast / emulated VIS mode).\n");
    loadFile("../data/comx35.1.3.bin", rom, 0x4000);
    loadFile("../data/fdc.bin", fdc, 0x2000);

    comx = new Vcomx35_fast();
    updateDisplayParams();

    printf("CRT_INPUT_SIZE: %i\n", CRT_INPUT_SIZE);
}

void fast_keyevent(int key) {
    // (no per-key color adjustment in fast mode for now)
}

#define CPU_CLOCKS_PER_FRAME 47250
static Uint64 frameClock = 0;

void fast_run() {
    comx->reset    = (main_time > 10) ? 0 : 1;
    comx->io_Start = (main_time > 15) ? 1 : 0;
    comx->io_Wait   = 1;
    comx->io_Tape_in = 1;
    comx->io_FDCRom_DataIn = fdc[comx->io_FDCRom_Addr];

    // --- memory read mux (full mux in software, incl. pram/cram) ---
    Uint8 memData = 0x00;
    Uint16 a16 = comx->io_Addr16;
    if (comx->io_KBD_SEL) {
        memData = comx->io_KBD_DataOut;
    } else if (comx->io_MRD == 0 && a16 >= 0xF800) {
        memData = pram[a16 & 0x3FF];                          // pixel memory read
    } else if (comx->io_MRD == 0 && a16 >= 0xF400 && a16 <= 0xF7FF) {
        int line = a16 & 0xF;                              // CmaMask
        int charCode = pram[a16 & 0x3FF] & 0x7F;
        memData = cram[(charCode * 16 + line) & (CRAM_SIZE - 1)];  // color/font memory read
    } else if (comx->io_MRD == 0 && comx->io_ExtRom) {
        memData = comx->io_Card_DataOut;
    } else if (comx->io_MRD == 0 && a16 <= 0x3FFF) {
        memData = rom[a16];
    } else if (comx->io_MRD == 0 && a16 >= 0x4000 && a16 <= 0xBFFF) {
        memData = ram[a16 - 0x4000];
    } else if (comx->io_MRD == 0 && a16 >= 0xC000 && a16 <= 0xDFFF) {
        memData = comx->io_Card_DataOut;
    } else if (comx->io_MRD == 0 && comx->io_N == 2) {
        memData = comx->io_Card_DataOut;
    }
    comx->io_DataIn = memData;

    // --- memory write ---
    if (comx->io_MWR == 0 && comx->io_Addr16 >= 0x4000 && comx->io_Addr16 < 0xC000) {
        ram[comx->io_Addr16 - 0x4000] = comx->io_DataOut;
    }

    // --- VIS register decode ---
    decodeVisRegister(comx->io_N, comx->io_MRD, comx->io_TPB,
                      comx->io_Addr16, comx->io_DataOut);

    // --- pram/cram writes ---
    if (comx->io_MWR == 0) {
        Uint16 a = comx->io_Addr16;
        if (a >= 0xF800) {
            pram[a & 0x3FF] = comx->io_DataOut;
        } else if (a >= 0xF400 && a <= 0xF7FF) {
            int pma = (reg5 & 1) ? (reg6 & 0x3FF) : (a & 0x3FF);
            int line = a & 0xF;                              // CmaMask
            int charCode = pram[pma & 0x3FF] & 0x7F;
            int ca = (charCode * 16 + line) & (CRAM_SIZE - 1);
            cram[ca] = comx->io_DataOut;
        }
    }

    // --- keyboard ---
    if (!comx->io_KBD_Ready) {
        comx->io_KBD_Latch = 0;
        comx->io_KBD_KeyCode = 0x00;
        FrameCurent = FrameCount + 4;
    }
    if (FrameCount == 10 && comx->io_KBD_Ready) {
        comx->io_KBD_Latch = 1;
        comx->io_KBD_KeyCode = ComxKeyboard(*(keyInput));
    }
    if (((FrameCount >= 84 && FrameCount <= 105) || FrameCount >= 142) &&
        FrameCount > FrameCurent && comx->io_KBD_Ready && *keyInput != 0x00) {
        comx->io_KBD_Latch = 1;
        comx->io_KBD_KeyCode = ComxKeyboard(*(keyInput));
    }
    if (Ready_Edge && !comx->io_KBD_Ready) keyInput++;
    Ready_Edge = comx->io_KBD_Ready;

    // --- PreDisplay timing ---
    // PreDisplay_ (CPU EF1) is HIGH during the blanking interval, LOW during the
    // active display. The real CDP1870 keeps it high ~27% of the frame; the CPU
    // writes display memory (pram/cram) only while it is high. The interrupt
    // fires on its rising edge (end of the display period).
    comx->io_PreDisplay_ = (frameClock > (CPU_CLOCKS_PER_FRAME * 7 / 10)) ? 1 : 0;

    // --- clock the CPU (one full cycle) ---
    main_time++;
    comx->clk = 1;
    comx->eval();
    main_time++;
    comx->clk = 0;
    comx->eval();

    // --- frame boundary ---
    frameClock++;
    if (frameClock >= CPU_CLOCKS_PER_FRAME) {
        frameClock = 0;
        FrameCount++;

        // Encode RGB → NTSC analog; the draw callback (drawCRT) demodulates + presents.
        renderToCRT();
        sim_draw();
    }
}

void fast_end() {
    printf("Ended (fast).\n");
    comx->final();
    delete[] fb;
}