CC=gcc
CFLAGS=-I.

%o: %.c
	$(CC) -c -o $@ $< $(CFLAGS) -g

trans: translate-darshan-log.o
	$(CC) -o trans translate-darshan-log.o -I. -ldarshan-util -lbz2 -lz -g

clean:
	rm *.o
	rm trans

