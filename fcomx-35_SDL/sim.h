#ifndef SIM_CLASS_H
#define SIM_CLASS_H
    #define WINDOW_WIDTH 640
    #define WINDOW_HEIGHT 480

    //Bit functions
    #define BIT_SET(X, Y) 				*(X) |= (1<<Y)
    #define BIT_CLEAR(X, Y) 			*(X) &= ~(1<<Y)
    #define BIT_CHECK(X, Y)				(*(X) & (1<<Y))
    #define BIT_TOGGLE(X, Y)			*(X) ^= (1<<Y)
    #define SHIFT(X)                    (1<<X)
    #define SHIFT_MSB(X)                (0x8000>>X)

    void screenshot(const char filename[]);

    void sim_init(Uint32 *p,SDL_Texture *td ,void (*d)());
    void sim_keyevent(const uint8_t *keystates);
    void sim_run();
    void sim_end();
#endif