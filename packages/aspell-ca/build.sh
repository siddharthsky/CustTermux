TERMUX_PKG_HOMEPAGE=http://aspell.net
TERMUX_PKG_DESCRIPTION="Catalan dictionary for GNU Aspell"
TERMUX_PKG_VERSION=2.1.5-1
TERMUX_PKG_PLATFORM_INDEPENDENT=yes
TERMUX_PKG_SRCURL=ftp://ftp.gnu.org/gnu/aspell/dict/ca/aspell6-ca-${TERMUX_PKG_VERSION}.tar.bz2
TERMUX_PKG_SHA256=ebdae47edf87357a4df137dd754737e6417452540cb1ed34b545ccfd66f165b9
TERMUX_PKG_BUILD_IN_SRC=yes

termux_step_configure () {
    # aspell configure doesn't play nicely with cross-compile but it's so trivial
    # we can easily replace it.
    cat > $TERMUX_PKG_SRCDIR/Makefile <<EOF
ASPELL = `which aspell`
ASPELL_FLAGS = 
PREZIP = `which prezip`
DESTDIR =
dictdir = $TERMUX_PREFIX/lib/aspell-0.60
datadir = $TERMUX_PREFIX/lib/aspell-0.60

EOF
    cat $TERMUX_PKG_SRCDIR/Makefile.pre >> $TERMUX_PKG_SRCDIR/Makefile
}
