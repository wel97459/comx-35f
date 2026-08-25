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
#include "CMakeFiles/dt_lib_fast.dir/Vcomx35_fast.dir/Vcomx35_fast___024root.h"

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

// ---- FDC disk image (raw sector dump: track-interleaved, 35t x 2s x 16sec x 128B) ----
#define DISK_TRACKS     35
#define DISK_SIDES      2
#define DISK_SECTORS    16
#define DISK_SECTOR_LEN 128
#define DISK_SIZE       (DISK_TRACKS * DISK_SIDES * DISK_SECTORS * DISK_SECTOR_LEN)

static Uint8  diskImage[DISK_SIZE];
static bool   diskLoaded = false;
static Uint32 diskReadCount = 0;
static Uint32 diskWriteCount = 0;

// Byte offset in the raw image for (track, side, sector, byte-index).
static Uint32 diskOffset(Uint8 track, Uint8 side, Uint8 sector, Uint8 byteIdx) {
    Uint32 o = ((Uint32)track * DISK_SIDES + side);   // head-interleaved per track
    o = o * DISK_SECTORS + sector;
    o = o * DISK_SECTOR_LEN + byteIdx;
    return o;
}

// ---- keyboard ----
static Uint8 keyValid = 0;
static char keyInput = 0x00;
static int autoTypeIdx = 0;      // auto-"DOS CAT" typewriter index
static int autoStage = 0;        // 0=logo ENTER pending, 1=typing DOS CAT, 2=done
static Uint16 lastKeyFrame = 0;
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
    settings.hue      = 180;
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
    diskLoaded = (loadFile("../data/dos.img", diskImage, DISK_SIZE) == 0);
    printf("disk image: %s\n", diskLoaded ? "loaded" : "NOT FOUND");

    comx = new Vcomx35_fast();
    updateDisplayParams();

    printf("CRT_INPUT_SIZE: %i\n", CRT_INPUT_SIZE);
}

void fast_keyevent(int key) {
    // (no per-key color adjustment in fast mode for now)
    keyInput = key;
    keyValid = 1;
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
    } else if (comx->io_MRD == 1 && comx->io_N == 2 && !comx->io_KBD_SEL) {
        // INP 2: MRD stays HIGH during an 1802 input instruction; the card
        // drives the bus via its register mux.
        memData = comx->io_Card_DataOut;
    } else if (comx->io_MRD == 0 && comx->io_N == 2) {
        memData = comx->io_Card_DataOut;
    }
    comx->io_DataIn = memData;

    // --- FDC port activity logger (env FDC_LOG=1) ---
    // Spinal1802 bus polarity (verified in Spinal1802.scala):
    //   OUT n (0x60-67) -> ExeMode=Load       -> MRD pulses LOW  (CPU writes port)
    //   INP n (0x68-6F) -> ExeMode=WriteNoInc -> MWR pulses LOW  (CPU reads port)
    // So: MRD low + TPB = OUT (write); MWR low + TPB = INP (read).
    if (getenv("FDC_LOG")) {
        static Uint8 selLatch = 0;
        static Uint64 logCnt = 0;
        const Uint64 CAP = (getenv("FDC_LOG_ALL")) ? 100000 : 400;
        // current program counter = R[P] (so each event is tied to DOS source line)
        auto* rr = comx->rootp;
        const Uint16* Rrr[16] = {
            &rr->comx35_fast__DOT__CPU__DOT__R_0,  &rr->comx35_fast__DOT__CPU__DOT__R_1,
            &rr->comx35_fast__DOT__CPU__DOT__R_2,  &rr->comx35_fast__DOT__CPU__DOT__R_3,
            &rr->comx35_fast__DOT__CPU__DOT__R_4,  &rr->comx35_fast__DOT__CPU__DOT__R_5,
            &rr->comx35_fast__DOT__CPU__DOT__R_6,  &rr->comx35_fast__DOT__CPU__DOT__R_7,
            &rr->comx35_fast__DOT__CPU__DOT__R_8,  &rr->comx35_fast__DOT__CPU__DOT__R_9,
            &rr->comx35_fast__DOT__CPU__DOT__R_10, &rr->comx35_fast__DOT__CPU__DOT__R_11,
            &rr->comx35_fast__DOT__CPU__DOT__R_12, &rr->comx35_fast__DOT__CPU__DOT__R_13,
            &rr->comx35_fast__DOT__CPU__DOT__R_14, &rr->comx35_fast__DOT__CPU__DOT__R_15 };
        Uint16 pc = *Rrr[rr->comx35_fast__DOT__CPU__DOT__P];
        if (comx->io_MRD == 0 && comx->io_N == 2 && comx->io_TPB) {   // OUT 2
            if (comx->io_Q) {
                if (logCnt < CAP) {
                    Uint16 r2sel = *Rrr[2];
                    Uint8 m2 = (r2sel >= 0x4000 && r2sel <= 0xBFFF) ? ram[r2sel - 0x4000] : (r2sel <= 0x3FFF) ? rom[r2sel] : 0xEE;
                    fprintf(stderr, "[fdc] f=%u pc=%04x OUT2 SELECT=%02X (drive=%d side=%d gate=%d) [M[R2=%04x]=%02X]\n",
                            FrameCount, pc, comx->io_DataOut,
                            (comx->io_DataOut >> 2) & 3,   // bits 2-3: drive
                            (comx->io_DataOut >> 5) & 1,   // bit 5: side
                            (comx->io_DataOut >> 4) & 1,   // bit 4: gate
                            r2sel, m2);
                    ++logCnt;
                } else if (logCnt == CAP) { fprintf(stderr, "[fdc] ...suppressing\n"); ++logCnt; }
                selLatch = comx->io_DataOut;
            } else if (logCnt < CAP) {
                if ((selLatch & 3) == 0) {
                    const char* cmd = "?";
                    switch (comx->io_DataOut & 0xF0) {
                        case 0x00: cmd = "RESTORE"; break;
                        case 0x10: cmd = "SEEK"; break;
                        case 0x20: case 0x30: cmd = "STEP"; break;
                        case 0x80: cmd = "READ_SEC"; break;
                        case 0x90: cmd = "READ_MULTI"; break;
                        case 0xA0: cmd = "WRITE_SEC"; break;
                        case 0xB0: cmd = "WRITE_MULTI"; break;
                        case 0xC0: cmd = "READ_ADDR"; break;
                        case 0xD0: cmd = "FORCE_INT"; break;
                        case 0xE0: cmd = "READ_TRACK"; break;
                        case 0xF0: cmd = "WRITE_TRACK"; break;
                    }
                    fprintf(stderr, "[fdc] f=%u pc=%04x OUT2 CMD=%s (%02X)\n", FrameCount, pc, cmd, comx->io_DataOut);
                } else {
                    fprintf(stderr, "[fdc] f=%u pc=%04x OUT2 reg=%d data=%02X\n",
                            FrameCount, pc, selLatch & 3, comx->io_DataOut);
                }
                ++logCnt;
            } else if (logCnt == CAP) { fprintf(stderr, "[fdc] ...suppressing\n"); ++logCnt; }
        }
        if (comx->io_MWR == 0 && comx->io_N == 2 && comx->io_TPB && logCnt < CAP) {   // INP 2
            static const char* const regn[4] = {"STATUS","TRACK","SECTOR","DATA"};
            const char* what = comx->io_Q ? "INTRQ" : regn[selLatch & 3];
            Uint16 r2 = *Rrr[2], r4 = *Rrr[4], r8 = *Rrr[8], r10 = *Rrr[10], r14 = *Rrr[14];
            Uint8 memr2 = (r2 >= 0x4000 && r2 <= 0xBFFF) ? ram[r2 - 0x4000]
                        : (r2 <= 0x3FFF) ? rom[r2] : 0xEE;
            auto peekmem = [&](Uint16 a){ return (a>=0x4000&&a<=0xBFFF)? ram[a-0x4000] : (a<=0x3FFF)? rom[a] : 0xEE; };
            fprintf(stderr, "[fdc] f=%u pc=%04x INP2 %s -> %02X (st=%02X | R2=%04x [-2..+8]=%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X | RA=%04x)\n",
                    FrameCount, pc, what, comx->io_Card_DataOut,
                    comx->rootp->comx35_fast__DOT__fdc__DOT__FDC_Status,
                    r2,
                    peekmem(r2-2), peekmem(r2-1), peekmem(r2), peekmem(r2+1), peekmem(r2+2),
                    peekmem(r2+3), peekmem(r2+4), peekmem(r2+5), peekmem(r2+6), peekmem(r2+7),
                    peekmem(r2+8), r10);
            ++logCnt;
        } else if (logCnt == CAP) { fprintf(stderr, "[fdc] ...suppressing\n"); ++logCnt; }
    }

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

    // --- FDC disk byte interface (serve/capture one byte) ---
    if (comx->io_DiskReadReq) {
        Uint32 off = diskOffset(comx->io_DiskTrack, comx->io_DiskSide,
                                comx->io_DiskSector, comx->io_DiskByte);
        comx->io_DiskDataIn = (off < DISK_SIZE) ? diskImage[off] : 0;
        diskReadCount++;
    } else {
        comx->io_DiskDataIn = 0;
    }
    if (comx->io_DiskWriteReq) {
        Uint32 off = diskOffset(comx->io_DiskTrack, comx->io_DiskSide,
                                comx->io_DiskSector, comx->io_DiskByte);
        if (off < DISK_SIZE) diskImage[off] = comx->io_DiskDataOut;
        diskWriteCount++;
    }

    // --- keyboard ---
    // Auto-type key sequence so DOS boots headlessly:
    //   1) ENTER to get past the COMX-35 logo landing screen
    //   2) wait for BASIC READY
    //   3) "DOS CAT" + Enter (lists the disk directory and loads the DOS system)
    if (!getenv("NO_AUTO_DOS")) {
        static const char stage2[] = "dos cat\r";
        if (autoStage == 0 && FrameCount >= 60) {          // logo screen -> ENTER
            if (!keyValid && comx->io_KBD_Ready && FrameCount > FrameCurent &&
                FrameCount > lastKeyFrame + 6) {
                keyInput = '\r'; keyValid = 1;
                lastKeyFrame = FrameCount; autoStage = 1;
                printf("[auto-dos] ENTER for logo\n");
            }
        } else if (autoStage == 1 && FrameCount >= 160) {   // BASIC READY
            if (autoTypeIdx < (int)sizeof(stage2) - 1) {
                if (!keyValid && comx->io_KBD_Ready && FrameCount > FrameCurent &&
                    FrameCount > lastKeyFrame + 6) {
                    keyInput = stage2[autoTypeIdx++]; keyValid = 1;
                    lastKeyFrame = FrameCount;
                    printf("[auto-dos] typing '%c'\n", keyInput);
                }
            } else {
                autoStage = 2;
            }
        }
    }
    if (!comx->io_KBD_Ready) {
        comx->io_KBD_Latch = 0;
        comx->io_KBD_KeyCode = 0x00;
        FrameCurent = FrameCount + 4;
    }
    if (FrameCount == 10 && comx->io_KBD_Ready) {
        comx->io_KBD_Latch = 1;
        comx->io_KBD_KeyCode = ComxKeyboard(keyInput);
    }
    if (((FrameCount >= 84 && FrameCount <= 105) || FrameCount >= 142) &&
        FrameCount > FrameCurent && comx->io_KBD_Ready && keyValid) {
        comx->io_KBD_Latch = 1;
        comx->io_KBD_KeyCode = ComxKeyboard(keyInput);
        keyValid = 0;
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

        if (getenv("FDC_TRACE")) {
            auto* r = comx->rootp;
            static const Uint16* const Rtab[16] = {
                &r->comx35_fast__DOT__CPU__DOT__R_0,  &r->comx35_fast__DOT__CPU__DOT__R_1,
                &r->comx35_fast__DOT__CPU__DOT__R_2,  &r->comx35_fast__DOT__CPU__DOT__R_3,
                &r->comx35_fast__DOT__CPU__DOT__R_4,  &r->comx35_fast__DOT__CPU__DOT__R_5,
                &r->comx35_fast__DOT__CPU__DOT__R_6,  &r->comx35_fast__DOT__CPU__DOT__R_7,
                &r->comx35_fast__DOT__CPU__DOT__R_8,  &r->comx35_fast__DOT__CPU__DOT__R_9,
                &r->comx35_fast__DOT__CPU__DOT__R_10, &r->comx35_fast__DOT__CPU__DOT__R_11,
                &r->comx35_fast__DOT__CPU__DOT__R_12, &r->comx35_fast__DOT__CPU__DOT__R_13,
                &r->comx35_fast__DOT__CPU__DOT__R_14, &r->comx35_fast__DOT__CPU__DOT__R_15 };
            static Uint16 lastPC = 0xFFFF;
            Uint16 pc = *Rtab[r->comx35_fast__DOT__CPU__DOT__P];
            if (pc != lastPC) {
                fprintf(stderr, "[pc] f=%u PC=%04x D=%02x Q=%d\n",
                        FrameCount, pc, r->comx35_fast__DOT__CPU__DOT__D, comx->io_Q);
                lastPC = pc;
            }
        }

        // Encode RGB → NTSC analog; the draw callback (drawCRT) demodulates + presents.
        renderToCRT();
        sim_draw();
    }
}

void fast_end() {
    printf("Ended (fast).\n");
    printf("FDC disk: %u bytes read, %u bytes written\n", diskReadCount, diskWriteCount);
    comx->final();
    delete[] fb;
}