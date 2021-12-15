TERMUX_PKG_HOMEPAGE=https://www.gnu.org/software/wdiff/
TERMUX_PKG_DESCRIPTION="Display word differences between text files"
TERMUX_PKG_LICENSE="GPL-3"
TERMUX_PKG_MAINTAINER="@harieamjari"
TERMUX_PKG_VERSION=1.2.2
TERMUX_PKG_SRCURL=http://ftp.gnu.org/gnu/wdiff/wdiff-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=34ff698c870c87e6e47a838eeaaae729fa73349139fc8db12211d2a22b78af6b
TERMUX_PKG_DEPENDS="libiconv"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
--prefix=$TERMUX_PREFIX
--disable-nls
--disable-rpath
--disable-threads
--disable-dependency-tracking"
