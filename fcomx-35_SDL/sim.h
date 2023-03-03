#ifndef SIM_CLASS_H
#define SIM_CLASS_H
    #define WINDOW_WIDTH 832
    #define WINDOW_HEIGHT 624

    //Bit functions
    #define BIT_SET(X, Y) 				*(X) |= (1<<Y)
    #define BIT_CLEAR(X, Y) 			*(X) &= ~(1<<Y)
    #define BIT_CHECK(X, Y)				(*(X) & (1<<Y))
    #define BIT_TOGGLE(X, Y)			*(X) ^= (1<<Y)
    #define SHIFT(X)                    (1<<X)
    #define SHIFT_MSB(X)                (0x8000>>X)

    void screenshot(const char filename[]);

    void sim_init(int *v, SDL_Texture *td ,void (*d)(), struct CRT *c);
    void sim_keyevent(int key);
    void sim_run();
    void sim_end();
#endif