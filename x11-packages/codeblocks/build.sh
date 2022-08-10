TERMUX_PKG_HOMEPAGE=https://www.codeblocks.org/
TERMUX_PKG_DESCRIPTION="Code::Blocks is the Integrated Development Environment (IDE)."
TERMUX_PKG_LICENSE="GPL-3.0"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=20.03
TERMUX_PKG_SRCURL=https://sourceforge.net/projects/codeblocks/files/Sources/${TERMUX_PKG_VERSION}/codeblocks-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=15eeb3e28aea054e1f38b0c7f4671b4d4d1116fd05f63c07aa95a91db89eaac5
TERMUX_PKG_DEPENDS="libbz2, wxwidgets, zip"
TERMUX_PKG_HOSTBUILD=true
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--without-contrib-plugins --disable-compiler"

termux_step_host_build() {
	"${TERMUX_PKG_SRCDIR}/configure"
	make -j $TERMUX_MAKE_PROCESSES -C src/base
	make -j $TERMUX_MAKE_PROCESSES -C src/build_tools
}

termux_step_pre_configure() {
	sed "s/-lpthread//g" -i configure
	LDFLAGS+=" $($CC -print-libgcc-file-name)"
}

termux_step_post_configure() {
	cp -r $TERMUX_PKG_HOSTBUILD_DIR/src/build_tools $TERMUX_PKG_BUILDDIR/src
	sed -i 's/ -shared / -Wl,-O1,--as-needed\0/g' $TERMUX_PKG_BUILDDIR/libtool
	sed "s/-lpthread//g; s/-pthread//g" -i $TERMUX_PKG_BUILDDIR/Makefile $TERMUX_PKG_BUILDDIR/{*/*/*/*/*/*,*/*/*/*/*,*/*/*/*,*/*/*,*/*,*}/Makefile
}
