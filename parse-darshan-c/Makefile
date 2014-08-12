CC=gcc
CFLAGS=-I. -L/usr/local/lib -ldarshan-util -lbz2 -lz -g -fno-stack-protector

%o: %.c
	$(CC) -c -o $@ $< $(CFLAGS)

trans: translate-darshan-log.o
	$(CC) -o trans translate-darshan-log.o $(CFLAGS)

clean:
	rm *.o
	rm trans

