#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <termios.h>
#include <unistd.h>

struct termios options;

int set_interface_attribs (int fd, int speed)
{
        struct termios tty;
        memset (&tty, 0, sizeof tty);
        if (tcgetattr (fd, &tty) != 0)
        {
                //error_message ("error %d from tcgetattr", errno);
                return -1;
        }
        
        tcgetattr(fd, &options);

        cfsetispeed(&options, speed);
        cfsetospeed(&options, speed);
		/* 8N1 Mode */
		options.c_cflag &= ~PARENB;   /* Disables the Parity Enable bit(PARENB),So No Parity   */
		options.c_cflag &= ~CSTOPB;   /* CSTOPB = 2 Stop bits,here it is cleared so 1 Stop bit */
		options.c_cflag &= ~CSIZE;	 /* Clears the mask for setting the data size             */
		options.c_cflag |=  CS8;      /* Set the data bits = 8                                 */
		
		options.c_cflag &= ~CRTSCTS;       /* No Hardware flow Control                         */
		options.c_cflag |= CREAD | CLOCAL; /* Enable receiver,Ignore Modem Control lines       */ 
		
		
		options.c_iflag &= ~(IXON | IXOFF | IXANY);          /* Disable XON/XOFF flow control both i/p and o/p */
		options.c_iflag &= ~(ICANON | ECHO | ECHOE | ISIG);  /* Non Cannonical mode                            */

		options.c_oflag &= ~OPOST;/*No Output Processing*/
		
		/* Setting Time outs */
		options.c_cc[VMIN] = 3; /* Read at least 10 characters */
		options.c_cc[VTIME] = 5; /* Wait indefinetly   */

		if((tcsetattr(fd,TCSANOW,&options)) != 0){ /* Set the attributes to the termios structure*/
		    printf("\nERROR ! in Setting attributes");
            return -1;
        }

        fcntl(fd, F_SETFL, FNDELAY);
        return 0;
}

size_t readport(char *buff, size_t len, int fd)
{
    size_t pos=0, l;
    char c[2];
    while (1)
    {
        l = read(fd, &c, 1);

        if(l < 0){
            printf("There was a error.\r\n");
            //tcflush(fd, TCIFLUSH);
            //tcflush(fd, TCIOFLUSH);
            return -1;
        }

        buff[pos++] = c[0];

        if(pos >= len || l == -1 || c[0] == '\n'){
            //tcflush(fd, TCIFLUSH);
            //tcflush(fd, TCIOFLUSH);
            buff[pos] = '\0';
            return pos;
        }
    }
}

int main(int argc, char **argv)
{
    int fd = open(argv[1], O_RDWR | O_NOCTTY);
    if (fd < 0) {
        printf("There was a error opening %s, %i\r\n\r\n", portname, fd);
        return 0;
    }

    if (set_interface_attribs (fd, B57600) < 0) {
        printf("There was a error setting up the port\r\n\r\n");
        return 0;
    }


    size_t len=0;
    char ch[256], c[2];

    strcpy(ch, "?f04");
    write (fd, ch, 4); //Send to serial port
    tcflush(fd, TCIOFLUSH); //flush serial buffer

    while (strncmp(ch, "Ok.", 3) != 0)
    {
        len = readport(ch, 255, fd); // Read serial port
    }

    close(fd);
    return 1;
}
