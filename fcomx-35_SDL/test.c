#include<stdio.h>
#include<stdint.h>
#include<math.h>
#define WHITE_LEVEL 100
int cc1[4] = {0, 1, 0, -1};

int main(int argc, char *argv[])
{
    uint32_t colorsRGB[]={
        0x00000000,
        0x0000FF00,
        0x000000FF,
        0x0000FFFF,
        0x00FF0000,
        0x00FFFF00,
        0x00FF00FF,
        0x00FFFFFF,
    };
    int Color = 2;
    float fi, fq, fy;
    int pA;
    float rA, gA, bA;
    for(uint32_t ii = 0; ii < 8; ii++)
    {
        printf("\n************%u**************\n", ii);
        for (uint32_t i = 0; i < 4; i++)
        {   

            pA = colorsRGB[ii];
            bA = ((pA >> 16) & 0xff)/255;
            gA = ((pA >>  8) & 0xff)/255;
            rA = ((pA >>  0) & 0xff)/255;

            fy = 0.299*rA + 0.587*gA + 0.114*bA;
            fi = 0.596*rA - 0.274*gA - 0.322*bA;
            fq = 0.211*rA - 0.523*gA + 0.312*bA;

            fy = fy;
            fi = fi * cc1[(i + 0) & 3];
            fq = fq * cc1[(i + 3) & 3];
            // ire += (fy + fi + fq) * (WHITE_LEVEL * 100 / 100) >> 10;;
            // if (ire < 0)   ire = 0;
            // if (ire > 110) ire = 110;
            printf("I:%u, iq:%f, y:%f\n", i, floor((0.6 + fi + fq)*6.5), floor(fy*3)); 
        }
    }
}