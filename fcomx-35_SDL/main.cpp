#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>
#include <memory>
#include <vector>
#include "sim.h"

using namespace std;
//SDL renderer and single font (leaving global for simplicity)
SDL_Renderer *renderer;
SDL_Window *window;
TTF_Font *font;
SDL_Texture *texDisplay;
SDL_Rect texDisplayDest;

Uint32 * pixels = new Uint32[240 * 240];

void screenshot(const char filename[])
{
	// Create an empty RGB surface that will be used to create the screenshot bmp file
	SDL_Surface* pScreenShot = SDL_CreateRGBSurface(0, WINDOW_WIDTH, WINDOW_HEIGHT, 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
	
	// Read the pixels from the current render target and save them onto the surface
	SDL_RenderReadPixels(renderer, NULL, SDL_GetWindowPixelFormat(window), pScreenShot->pixels, pScreenShot->pitch);

	// Create the bmp screenshot file
	SDL_SaveBMP(pScreenShot, filename);

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

		// case SDL_MOUSEBUTTONDOWN:
		// 	handleMouse(event.button.x, event.button.y);
		// 	break;
		}
    }
    
    const Uint8* keystates = SDL_GetKeyboardState(NULL);
    sim_keyevent(keystates);
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

    renderer = SDL_CreateRenderer(window, -1, 0);

    /* Initialize the TTF library */
    if (TTF_Init() < 0) {
            fprintf(stderr, "Couldn't initialize TTF: %s\n",SDL_GetError());
            SDL_Quit();
            return 0;
    }

    font = TTF_OpenFont("OpenSans-Regular.ttf", 24);
    if(!font) {
        printf("TTF_OpenFont: %s\n", TTF_GetError());
        SDL_Quit();
        return 0;
        // handle error
    }

	texDisplayDest.x=(WINDOW_WIDTH/2) - (240*2)/2;
	texDisplayDest.y=WINDOW_HEIGHT/2 - (240*2)/2;
	texDisplayDest.w=240*2;
	texDisplayDest.h=240*2;

	texDisplay = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, 240, 240);

	return 1;
}

void draw()
{
    //clear screen, draw each element, then flip the buffer
    SDL_SetRenderDrawColor(renderer, 100, 100, 100, 255);
    SDL_RenderClear(renderer);

	SDL_RenderCopy(renderer, texDisplay, NULL, &texDisplayDest);

    SDL_RenderPresent(renderer);
}

int main(int argc, char *argv[])
{
    if(initVideo()==0) return -1;

    sim_init(pixels, texDisplay, draw);
    draw();

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
