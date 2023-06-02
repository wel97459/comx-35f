#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <termios.h>
#include <unistd.h>

struct termios options;

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
};


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

void waitOk(int fd)
{
    size_t len;
    char ch[5];
    while (strncmp(ch, "Ok.", 3) != 0)
    {
        len = readport(ch, 5, fd);
    }
    tcflush(fd, TCIOFLUSH);
}

void loadDefus(int fd, struct comxHeader *cxh){
    char cs[32];
    size_t len;
    unsigned char offset = 0x00;
    if(cxh->type == 6) offset = 0x44;
    //if(cxh->type == 6) offset = 0x28;

    write (fd, "a4281", 5); //Set Defus Address
    waitOk(fd);

    len = sprintf(cs, "w%02xw%02x", ((cxh->defus & 0xff00) >> 8)+offset, cxh->defus & 0xff);
    printf("%s %lu\n", cs, len);
    write (fd, cs, len); //Set Defus
    waitOk(fd);


    write (fd, "a4283", 5); //Set EOP Address
    waitOk(fd);

    len = sprintf(cs, "w%02xw%02x", ((cxh->eop & 0xff00) >> 8)+offset, cxh->eop & 0xff);
    printf("%s %lu\n", cs, len);
    write (fd, cs, len); //Set EOP
    waitOk(fd);

    if(cxh->type == 1) return;

    write (fd, "a4299", 5); //Set EOD Address
    waitOk(fd);

    len = sprintf(cs, "w%02xw%02x", ((cxh->eod & 0xff00) >> 8)+offset, cxh->eod & 0xff);
    printf("%s %lu\n", cs, len);
    write (fd, cs, len); //Set EOD
    waitOk(fd);

    write (fd, "a4292", 5); //Set String Address
    waitOk(fd);

    len = sprintf(cs, "w%02xw%02x", ((cxh->eod & 0xff00) >> 8)+offset, cxh->eod & 0xff);
    printf("%s %lu\n", cs, len);
    write (fd, cs, len); //Set String
    waitOk(fd);

    len = sprintf(cs, "w%02xw%02x", ((cxh->array & 0xff00) >> 8)+offset, cxh->array & 0xff);
    printf("%s %lu\n", cs, len);
    write (fd, cs, len); //Set Array
    waitOk(fd);

}
void flipShort(unsigned short *s){
    *s = (*s << 8) | (*s >> 8);
}

int main(int argc, char **argv)
{
    FILE *fp;
    char portname[32];
    strcat(portname, argv[1]);

    fp = fopen(argv[2], "r");
    int fd;
    fd = open(portname, O_RDWR | O_NOCTTY);
    if (fd < 0) {
        printf("There was a error opening %s, %i\r\n\r\n", portname, fd);
        return 0;
    }

    if (set_interface_attribs (fd, B57600) < 0) {
        printf("There was a error setting up the port\r\n\r\n");
        return 0;
    }

    fseek( fp , 0L , SEEK_END);
    size_t data_len = ftell( fp );
    rewind(fp);

    size_t len=0;
    char ch[256], c[16];
    unsigned short address_start = BASIC_RAM_ADDR;
    struct comxHeader cxh;

    fread(&cxh, 1, 1, fp);
    fread(&cxh.name, 1, 4, fp);

    fread(&cxh.defus, 1, 2, fp);
    flipShort(&cxh.defus);

    fread(&cxh.eop, 1, 2, fp);
    flipShort(&cxh.eop);

    fread(&cxh.eod, 1, 2, fp);
    flipShort(&cxh.eod);
    
    if(cxh.type != 1){
        fread(&cxh.array, 1, 2, fp);
        flipShort(&cxh.array);
        fread(&cxh.nu1, 1, 2, fp);
        flipShort(&cxh.nu1);    
    }else{
        address_start = cxh.defus;
    }
    
    printf("Type: %u, Str:%.4s, defus:%04X, eop:%04X, eod:%04X, array:%04X  \n", cxh.type, cxh.name, cxh.defus, cxh.eop, cxh.eod, cxh.array);

    printf("Resetting System!\r\n");
    write (fd, "f01", 3);
    waitOk(fd);
    sleep(1);

    write (fd, "f00tt", 5);
    waitOk(fd);
    sleep(2);

    printf("Halting system!\r\n");
    write (fd, "f04", 3);
    waitOk(fd);

    len = sprintf(ch, "a%04x", address_start);
    printf("%s %lu\n", ch, len);
    write (fd, ch, len); //Set address
    waitOk(fd);

    printf("file starting at:%lu\n", ftell(fp));
    int i = 0;
    while(data_len>0){
        data_len = fread(&ch, 1, 255, fp);

        if(data_len > 0){
            c[0] = '`';
            c[1] = data_len;
            write (fd, &c, 2);
            
            printf("%c", data_len == 255 ? 'O' : '.');

            write (fd, ch, data_len);
            waitOk(fd);
        }
    }

    printf("\r\nLoading Defus\r\n");
    loadDefus(fd, &cxh);

    printf("Put system in run mod.\r\n");
    write (fd, "f00", 3);
    waitOk(fd);

    write (fd, "t\n", 2);
    sleep(1);
 
    goto done;
    len = sprintf(ch, "t\ntctatltlt(t@t4t4t0t1t)t\n");
    for(int i = 0;i<len;i+=2){
        write (fd, ch+i, 2);
        waitOk(fd);
    }

done:
    printf("Done.\r\n");
    fclose(fp);
    close(fd);
    return 1;
}
