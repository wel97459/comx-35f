#include <SDL2/SDL.h>
//#include <SDL2/SDL_ttf.h>
#include <memory>
#include <vector>
#include "sim.h"
#include "crt.h"
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "stb_image_write.h"

using namespace std;
//SDL renderer and single font (leaving global for simplicity)
SDL_Renderer *renderer;
SDL_Window *window;
// TTF_Font *font;
SDL_Texture *texDisplay;
SDL_Rect texDisplayDest;

Uint32 * pixels = new Uint32[240 * 240];

int *video = NULL;
static struct CRT crt;
static int roll;
void SetAlpha(void *pixels){
    union p
    {
        void *p;
        char *c;
    };
    union p pix;
    pix.p = pixels;
    for (size_t i = 3; i < (WINDOW_HEIGHT*WINDOW_WIDTH)*4; i+=4)
    {
        pix.c[i] = pix.c[i-3];
        pix.c[i-3] = pix.c[i-1];
        pix.c[i-1] = pix.c[i];
        pix.c[i] = 0xff;
    }
}

void screenshot(const char filename[])
{
	// Create an empty RGB surface that will be used to create the screenshot bmp file
	SDL_Surface* pScreenShot = SDL_CreateRGBSurface(0, WINDOW_WIDTH, WINDOW_HEIGHT, 32, 0x000000FF, 0x0000FF00, 0x00FF0000, 0x00000000);
	
	// Read the pixels from the current render target and save them onto the surface
	SDL_RenderReadPixels(renderer, NULL, SDL_GetWindowPixelFormat(window), pScreenShot->pixels, pScreenShot->pitch);
    
	// Create the bmp screenshot file
	//SDL_SaveBMP(pScreenShot, filename);
    SetAlpha(pScreenShot->pixels);
    
    stbi_write_png(filename, WINDOW_WIDTH, WINDOW_HEIGHT, 4, pScreenShot->pixels, WINDOW_WIDTH*4);
	// Destroy the screenshot surface
	SDL_FreeSurface(pScreenShot);
}

int handleInput()
{
	SDL_Event event;
	//event handling, check for close window, escape key and mouse clicks
	//return -1 when exit requested
	while (SDL_PollEvent(&event)) {
		switch (event.type) {
		case SDL_QUIT:
			return -1;

		case SDL_KEYDOWN:
			if (event.key.keysym.sym == SDLK_ESCAPE)
				return -1;
            sim_keyevent(event.key.keysym.sym);

		// case SDL_MOUSEBUTTONDOWN:
		// 	handleMouse(event.button.x, event.button.y);
		// 	break;
		}
    }
    return 0;
}

int initVideo()
{
    //setup SDL with title, 640x480, and load font
	if (SDL_Init(SDL_INIT_VIDEO)) {
		printf("Unable to initialize SDL: %s\n", SDL_GetError());
		return 0;
	}

	window = SDL_CreateWindow("SDL2 - Verilator - SpinalHDL", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, WINDOW_WIDTH, WINDOW_HEIGHT, 0);
	if (!window) {
		printf("Can't create window: %s\n", SDL_GetError());
		return 0;
	}

    renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);

    // /* Initialize the TTF library */
    // if (TTF_Init() < 0) {
    //         fprintf(stderr, "Couldn't initialize TTF: %s\n",SDL_GetError());
    //         SDL_Quit();
    //         return 0;
    // }

    // font = TTF_OpenFont("OpenSans-Regular.ttf", 24);
    // if(!font) {
    //     printf("TTF_OpenFont: %s\n", TTF_GetError());
    //     SDL_Quit();
    //     return 0;
    //     // handle error
    // }

	texDisplayDest.x=(WINDOW_WIDTH/2) - (240*2)/2;
	texDisplayDest.y=WINDOW_HEIGHT/2 - (240*2)/2;
	texDisplayDest.w=240*2;
	texDisplayDest.h=240*2;

	texDisplay = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGB888, SDL_TEXTUREACCESS_STREAMING, WINDOW_WIDTH, WINDOW_HEIGHT);

	return 1;
}

static void draw()
{
    //clear screen, draw each element, then flip the buffer
	SDL_RenderCopy(renderer, texDisplay, NULL, &texDisplayDest);
    SDL_RenderPresent(renderer);
}

void drawCRT()
{
	crt_draw(&crt, 0, roll, 180, 4);
	SDL_UpdateTexture(texDisplay, NULL, video, WINDOW_WIDTH * sizeof(Uint32));

	SDL_RenderCopy(renderer, texDisplay, NULL, NULL);

    SDL_RenderPresent(renderer);
    roll+=10;
}

int main(int argc, char *argv[])
{
    roll=0;
    if(initVideo()==0) return -1;

    video = (int *) malloc(WINDOW_WIDTH * sizeof(int) * WINDOW_HEIGHT);
    crt_init(&crt, WINDOW_WIDTH, WINDOW_HEIGHT, video);

    sim_init(video, texDisplay, drawCRT, &crt);
    drawCRT();

    do{
        sim_run();
    } while (handleInput() >= 0); //run until exit requested

done:
    sim_end();
    // Create an empty RGB surface that will be used to create the screenshot bmp file
    SDL_Surface* pScreenShot = SDL_CreateRGBSurface(0, WINDOW_WIDTH, WINDOW_HEIGHT, 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);

    if(pScreenShot)
    {
        // Read the pixels from the current render target and save them onto the surface
        SDL_RenderReadPixels(renderer, NULL, SDL_GetWindowPixelFormat(window), pScreenShot->pixels, pScreenShot->pitch);

        // Create the bmp screenshot file
        SDL_SaveBMP(pScreenShot, "Screenshot.bmp");

        // Destroy the screenshot surface
        SDL_FreeSurface(pScreenShot);
    }

    return 0;
}
