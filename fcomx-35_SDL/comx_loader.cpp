#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "comx_loader.h"

struct comxHeader * LoadComx(const char *filename)
{
    size_t len;
    struct comxHeader *cxh = (struct comxHeader *) malloc(sizeof(struct comxHeader));
    FILE *fp = fopen(filename, "r");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return NULL;
    }

    fseek(fp, 0L, SEEK_END);
    cxh->len = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    
    
    cxh->address_start = BASIC_RAM_ADDR;
    fread(&cxh->type, 1, 1, fp);
    fread(&cxh->name, 1, 4, fp);

    unsigned short offset = 0x00;
    if(cxh->type == 6) offset = 0x44;
    //if(cxh->type == 6) offset = 0x28;

    fread(&cxh->defus, 1, 2, fp);
    FLIP_SHORT(&cxh->defus);
    OFFSET_SHORT(&cxh->defus, &offset);

    fread(&cxh->eop, 1, 2, fp);
    FLIP_SHORT(&cxh->eop);
    OFFSET_SHORT(&cxh->eop, &offset);

    fread(&cxh->eod, 1, 2, fp);
    FLIP_SHORT(&cxh->eod);
    OFFSET_SHORT(&cxh->eod, &offset);

    if(cxh->type != 1){
        fread(&cxh->array, 1, 2, fp);
        FLIP_SHORT(&cxh->array);
        OFFSET_SHORT(&cxh->array, &offset);

        fread(&cxh->nu1, 1, 2, fp);
        FLIP_SHORT(&cxh->nu1);
    }else{
        cxh->address_start = cxh->defus;
    }

    cxh->len = cxh->len - ftell(fp);
    cxh->data = (char *) malloc(cxh->len+1); 
    fread(&cxh->data[0], 1, cxh->len, fp);

    printf("Type: %u, Str:%.4s, defus:%04X, eop:%04X, eod:%04X, array:%04X Len:%lu Len:%lu \n", cxh->type, cxh->name, cxh->defus, cxh->eop, cxh->eod, cxh->array, cxh->len, ftell(fp));
    FLIP_SHORT(&cxh->defus);
    FLIP_SHORT(&cxh->eop);
    FLIP_SHORT(&cxh->eod);
    FLIP_SHORT(&cxh->array);

    fclose(fp);
    return cxh;
}

void comxFree(struct comxHeader * cxh)
{
    free(cxh->data);
    free(cxh);
}