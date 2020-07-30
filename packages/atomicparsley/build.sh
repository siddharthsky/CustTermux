TERMUX_PKG_HOMEPAGE=https://bitbucket.org/wez/atomicparsley
TERMUX_PKG_DESCRIPTION="Read, parse and set metadata of MPEG-4 and 3gp files"
TERMUX_PKG_LICENSE="GPL-2.0"
TERMUX_PKG_VERSION=20200701.154658.b0d6223
TERMUX_PKG_REVISION=6
TERMUX_PKG_SRCURL=https://github.com/wez/atomicparsley/archive/${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=52f11dc0cbd8964fcdaf019bfada2102f9ee716a1d480cd43ae5925b4361c834
TERMUX_PKG_DEPENDS="libc++, zlib"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="ac_cv_func_lroundf=yes"

termux_step_pre_configure() {
	./autogen.sh

	# Avoid the configure script from using g++ for linking:
	export OBJCXX=$CXX
}
