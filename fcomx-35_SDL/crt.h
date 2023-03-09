/*****************************************************************************/
/*
 * NTSC/CRT - integer-only NTSC video signal encoding / decoding emulation
 *
 *   by EMMIR 2018-2023
 *
 *   YouTube: https://www.youtube.com/@EMMIR_KC/videos
 *   Discord: https://discord.com/invite/hdYctSmyQJ
 */
/*****************************************************************************/
#ifndef _CRT_H_
#define _CRT_H_

#ifdef __cplusplus
extern "C" {
#endif

/* crt.h
 *
 * An interface to convert a digital image to an analog NTSC signal
 * and decode the NTSC signal back into a digital image.
 * Can easily be integrated into real-time applications
 * or be used as a command-line tool.
 *
 */

/* do bloom emulation (side effect: makes screen have black borders) */
#define CRT_DO_BLOOM    0
#define CRT_DO_VSYNC    1  /* look for VSYNC */
#define CRT_DO_HSYNC    1  /* look for HSYNC */
/* 0 = vertical  chroma (228 chroma clocks per line) */
/* 1 = checkered chroma (227.5 chroma clocks per line) */
#define CRT_DO_CHK_C    1

/* chroma clocks (subcarrier cycles) per line */
#if CRT_DO_CHK_C
#define CRT_CC_LINE 2275
#else
/* this will give the 'rainbow' effect in the famous waterfall scene */
#define CRT_CC_LINE 2280
#endif

#define CRT_CB_FREQ     4 /* carrier frequency relative to sample rate */
#define CRT_HRES        ((CRT_CC_LINE * CRT_CB_FREQ)/8) /* horizontal res */
#define CRT_VRES        262                       /* vertical resolution */
#define CRT_INPUT_SIZE  (CRT_HRES * CRT_VRES)

#define CRT_TOP         21     /* first line with active video */
#define CRT_BOT         261    /* final line with active video */
#define CRT_LINES       (CRT_BOT - CRT_TOP) /* number of active video lines */

/*
 *                      FULL HORIZONTAL LINE SIGNAL (~63500 ns)
 * |---------------------------------------------------------------------------|
 *   HBLANK (~10900 ns)                 ACTIVE VIDEO (~52600 ns)
 * |-------------------||------------------------------------------------------|
 *   
 *   
 *   WITHIN HBLANK PERIOD:
 *   
 *   FP (~1500 ns)  SYNC (~4700 ns)  BW (~600 ns)  CB (~2500 ns)  BP (~1600 ns)
 * |--------------||---------------||------------||-------------||-------------|
 *      BLANK            SYNC           BLANK          BLANK          BLANK
 * 
 */
#define DOT_ns 176UL
#define DOTx6_ns 1058UL
#define LINE_BEG         0
#define FP_ns            (1*DOTx6_ns)      /* front porch */
#define SYNC_ns          (4*DOTx6_ns)      /* sync tip */
#define BW_ns            (1*DOTx6_ns)       /* breezeway */
#define CB_ns            (3*DOTx6_ns)      /* color burst */
#define BP_ns            (1*DOTx6_ns)      /* back porch */
#define AV_ns            (50*DOTx6_ns)     /* active video */
#define HB_ns            (FP_ns + SYNC_ns + BW_ns + CB_ns + BP_ns) /* h blank */
/* line duration should be ~63500 ns */
#define LINE_ns          (FP_ns + SYNC_ns + BW_ns + CB_ns + BP_ns + AV_ns)

/* convert nanosecond offset to its corresponding point on the sampled line */
#define ns2pos(ns)       ((ns) * CRT_HRES / LINE_ns)
/* starting points for all the different pulses */
#define FP_BEG           ns2pos(0)
#define SYNC_BEG         ns2pos(FP_ns)
#define BW_BEG           ns2pos(FP_ns + SYNC_ns)
#define CB_BEG           ns2pos(FP_ns + SYNC_ns + BW_ns)
#define BP_BEG           ns2pos(FP_ns + SYNC_ns + BW_ns + CB_ns)
#define AV_BEG           ns2pos(HB_ns)
#define AV_LEN           ns2pos(AV_ns)

/* somewhere between 7 and 12 cycles */
#define CB_CYCLES   10

/* frequencies for bandlimiting */
#define L_FREQ           1431818 /* full line */
#define Y_FREQ           420000  /* Luma   (Y) 4.2  MHz of the 14.31818 MHz */
#define I_FREQ           150000  /* Chroma (I) 1.5  MHz of the 14.31818 MHz */
#define Q_FREQ           55000   /* Chroma (Q) 0.55 MHz of the 14.31818 MHz */

/* IRE units (100 = 1.0V, -40 = 0.0V) */
#define WHITE_LEVEL      100
#define BURST_LEVEL      20
#define BLACK_LEVEL      7
#define BLANK_LEVEL      0
#define SYNC_LEVEL      -40

#define MIN(a,b) (((a)<(b))?(a):(b))
#define MAX(a,b) (((a)>(b))?(a):(b))

struct CRT {
    signed char analog[CRT_INPUT_SIZE]; /* sampled at 14.31818 MHz */
    signed char inp[CRT_INPUT_SIZE]; /* CRT input, can be noisy */
    int hsync, vsync; /* used internally to keep track of sync over frames */
    int brightness, contrast, saturation; /* common monitor settings */
    int black_point, white_point; /* user-adjustable */
    int outw, outh; /* output width/height */
    int *out; /* output image */
};

/* Initializes the library. Sets up filters.
 *   w   - width of the output image
 *   h   - height of the output image
 *   out - pointer to output image data 32-bit RGB packed as 0xXXRRGGBB
 */
extern void crt_init(struct CRT *v, int w, int h, int *out);

/* Updates the output image parameters
 *   w   - width of the output image
 *   h   - height of the output image
 *   out - pointer to output image data 32-bit RGB packed as 0xXXRRGGBB
 */
extern void crt_resize(struct CRT *v, int w, int h, int *out);

/* Resets the CRT settings back to their defaults */
extern void crt_reset(struct CRT *v);

struct NTSC_SETTINGS {
    const int *rgb; /* 32-bit RGB image data (packed as 0xXXRRGGBB) */
    int w, h;       /* width and height of image */
    int raw;        /* 0 = scale image to fit monitor, 1 = don't scale */
    int as_color;   /* 0 = monochrome, 1 = full color */
    int field;      /* 0 = even, 1 = odd */
    /* color carrier sine wave.
     * ex: { 0, 1, 0, -1 }
     * ex: { 1, 0, -1, 0 }
     */
    int cc[4];      
};

/* Convert RGB image to analog NTSC signal
 *   s - struct containing settings to apply to this field
 */
extern void crt_2ntsc(struct CRT *v, struct NTSC_SETTINGS *s);

/* Decodes the NTSC signal generated by crt_2ntsc()
 *   noise - the amount of noise added to the signal (0 - inf)
 */
extern void crt_draw(struct CRT *v, int noise, int roll, int vs, int hs);

#ifdef __cplusplus
}
#endif

#endif
