#include <SDL2/SDL.h>
#include <memory>
#include <vector>
#include <cstring>
#include <cstdio>
#include "sim_tv.h"
#include "crt_core.h"

static void (*sim_draw)();
static SDL_Texture *screen;
static unsigned char *sim_video;
static struct CRT *sim_crt;

int loadFile(const char *filename, void *pointer, const Uint32 len)
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

void tv_init(unsigned char *v, SDL_Texture *td, void (*d)(), struct CRT *c, int argc, char **argv) {
    sim_draw  = d;
    screen    = td;
    sim_video = v;
    sim_crt   = c;

    char *filename = NULL;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--tv") == 0) filename = argv[i + 1];
    }

    if (filename) {
        if (loadFile(filename, sim_crt, CRT_INPUT_SIZE) == 0) {
            printf("Loaded TV image from %s\n", filename);
        } else {
            printf("Failed to load TV image from %s\n", filename);
        }
    }

    SDL_UpdateTexture(screen, NULL, sim_video, 240 * sizeof(Uint32));
    sim_draw();
}

void tv_keyevent(int key){
    if (key == SDLK_o) {
        sim_crt->saturation-=1;
        printf("Saturation: %d\n", sim_crt->saturation);
    }
    if (key == SDLK_p) {
        sim_crt->saturation+=1;
        printf("Saturation: %d\n", sim_crt->saturation);
    }
    if (key == SDLK_k) {
        sim_crt->contrast-=10;
        printf("Contrast: %d\n", sim_crt->contrast);
    }
    if (key == SDLK_l) {
        sim_crt->contrast+=10;
        printf("Contrast: %d\n", sim_crt->contrast);
    }
    if (key == SDLK_n) {
        sim_crt->brightness-=10;
        printf("Brightness: %d\n", sim_crt->brightness);
    }
    if (key == SDLK_m) {
        sim_crt->brightness+=10;
        printf("Brightness: %d\n", sim_crt->brightness);
    }
}

void tv_run() {
    SDL_Delay(50);
    sim_draw();
}

void tv_end() {

}