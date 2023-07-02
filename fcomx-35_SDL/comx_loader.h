#ifndef _COMX_LOADER_H_
#define _COMX_LOADER_H_
    #define FLIP_SHORT(s)  *(s) = ((*(s) << 8) | (*(s) >> 8))
    #define OFFSET_SHORT(s, f)  *(s) = (((*(s) & 0xff00) >> 8) + *(f)) << 8 | (*(s) & 0x00ff)
    #define DEFUS_ADDR          0x4281
    #define EOP_ADDR            0x4283
    #define STRING_ADDR         0x4292
    #define ARRAY_VALUE_ADDR    0x4294
    #define EOD_ADDR            0x4299
    #define BASIC_RAM_ADDR      0x4400

    struct comxHeader
    {
        char type; //0
        char name[4]; //1-4
        unsigned short defus; //5-6
        unsigned short eop; //7-8
        unsigned short eod; //9-10
        unsigned short array; //11-12
        unsigned short nu1; //13-14
        unsigned short address_start;
        size_t len;
        char *data;
    };

    struct comxHeader * LoadComx(const char *filename);
    void comxFree(struct comxHeader * cxh);
#endif