all: darshan-parser darshan-convert darshan-diff darshan-analyzer darshan-log-params darshan-util-lib

DESTDIR =
srcdir = .
prefix = /usr/local
exec_prefix = ${prefix}
datarootdir = $(DESTDIR)${prefix}/share
includedir = $(DESTDIR)${prefix}/include
mandir = $(DESTDIR)${datarootdir}/man
sbindir = $(DESTDIR)${exec_prefix}/sbin
bindir = $(DESTDIR)${exec_prefix}/bin
libdir = $(DESTDIR)${exec_prefix}/lib
pkgconfigdir = $(DESTDIR)$(libdir)/pkgconfig

DARSHAN_LOG_FORMAT = $(srcdir)/../darshan-log-format.h
DARSHAN_ENABLE_SHARED=0



ifeq ($(DARSHAN_ENABLE_SHARED),1)
all: libdarshan-util.so
endif
cp_zlib_link_flags = 
cp_zlib_include_flags = 

# deliberately avoid large file support for host side utilities to avoid
# potentially buggy libz 64 bit offset support
CFLAGS = -I . -I $(srcdir) -I $(srcdir)/../ -DDARSHAN_CONFIG_H=\"darshan-util-config.h\" -g -O2  -I/usr/include 
CFLAGS_SHARED = $(CFLAGS) -shared -fpic -DPIC 
LDFLAGS= -L/usr/lib

CC=gcc
LD=@LD@
AR=@AR@

LIBS = -lz -lbz2

mktestdir::
	mkdir -p test

uthash-1.9.2:
	tar xjvf $(srcdir)/extern/uthash-1.9.2.tar.bz2

darshan-parser: darshan-parser.c $(DARSHAN_LOG_FORMAT) darshan-logutils.h darshan-logutils.o | uthash-1.9.2
	$(CC) $(CFLAGS) $(LDFLAGS) $< darshan-logutils.o -o $@ $(LIBS) 

darshan-convert: darshan-convert.c $(DARSHAN_LOG_FORMAT) darshan-logutils.h darshan-logutils.o lookup3.o
	$(CC) $(CFLAGS)  $(LDFLAGS) $< darshan-logutils.o lookup3.o -o $@ $(LIBS)

darshan-analyzer: darshan-analyzer.c $(DARSHAN_LOG_FORMAT) darshan-logutils.h darshan-logutils.o
	$(CC) $(CFLAGS)  $(LDFLAGS) $< darshan-logutils.o -o $@ $(LIBS)

darshan-log-params: darshan-log-params.c $(DARSHAN_LOG_FORMAT)
	$(CC) $(CFLAGS)  $(LDFLAGS) $< -o $@ $(LIBS)

jenkins: util/bin/jenkins.o lookup3.o
	$(CC) $(CFLAGS)  $(LDFLAGS) $< -o $@ lookup3.o $(LIBS)

darshan-diff: darshan-diff.o $(DARSHAN_LOG_FORMAT) darshan-logutils.o darshan-logutils.h
	$(CC) $(CFLAGS)  $(LDFLAGS) $< darshan-logutils.o -o $@ $(LIBS)
darshan-diff.o: darshan-diff.c
	$(CC) $(CFLAGS) -c  $< -o $@
darshan-logutils.o: darshan-logutils.c
	$(CC) $(CFLAGS) -c  $< -o $@
darshan-logutils.po: darshan-logutils.c
	$(CC) $(CFLAGS_SHARED) -c  $< -o $@

libdarshan-util.so: darshan-logutils.po
	$(CC) $(CFLAGS_SHARED) $(LDFLAGS) -o $@ $^ $(LIBS)
	
darshan-util-lib: darshan-logutils.o
	ar rcs libdarshan-util.a $<

test/gztest: test/gztest.c mktestdir
	$(CC) $(CFLAGS)  $(LDFLAGS) -lz $< -o $@

test/gz-bench: test/gz-bench.c mktestdir
	$(CC) $(CFLAGS)  $(LDFLAGS) -lz $< -o $@

lookup3.o: lookup3.c
	$(CC) $(CFLAGS) -c $< -o $@

install:: all
	install -d $(bindir)
	install -d $(libdir)
	install -d $(includedir)
	install -d $(pkgconfigdir)
	install -m 755 darshan-parser $(bindir)
	install -m 755 darshan-convert $(bindir)
	install -m 755 darshan-diff $(bindir)
	install -m 755 darshan-analyzer $(bindir)
	install -m 755 $(srcdir)/darshan-summary-per-file.sh $(bindir)
	install -m 755 libdarshan-util.a $(libdir)
ifeq ($(DARSHAN_ENABLE_SHARED),1)
	install -m 755 libdarshan-util.so $(libdir)
endif
	install -m 644 $(srcdir)/darshan-logutils.h $(includedir)
	install -m 644 $(DARSHAN_LOG_FORMAT) $(includedir)
	install -m 755 darshan-job-summary/bin/darshan-job-summary.pl $(bindir)
	install -d $(libdir)/TeX
	install -m 644 $(srcdir)/darshan-job-summary/lib/TeX/Encode.pm $(libdir)/TeX/
	install -d $(libdir)/Number
	install -d $(libdir)/Number/Bytes
	install -m 644 $(srcdir)/darshan-job-summary/lib/Number/Bytes/Human.pm $(libdir)/Number/Bytes
	install -d $(datarootdir)
	install -m 644 $(srcdir)/darshan-job-summary/share/* $(datarootdir)
	install -m 644 maint/darshan-util.pc $(pkgconfigdir)


clean::
	rm -f *.o *.a darshan-parser darshan-convert darshan-diff darshan-analyzer darshan-log-params

distclean:: clean
	rm -f darshan-runtime-config.h aclocal.m4 autom4te.cache/* config.status config.log Makefile util/bin/darshan-job-summary.pl
	rm -rf uthash-1.9.2
	rm -rf autom4te.cache
