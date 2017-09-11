TERMUX_PKG_HOMEPAGE=https://www.winehq.org
TERMUX_PKG_DESCRIPTION="Windows API implementation"
TERMUX_PKG_VERSION=2.0
TERMUX_PKG_SRCURL=https://dl.winehq.org/wine/source/2.0/wine-${TERMUX_PKG_VERSION}.tar.bz2
TERMUX_PKG_SHA256=9756f5a2129b6a83ba701e546173cbff86caa671b0af73eb8f72c03b20c066c6
TERMUX_PKG_BUILD_IN_SRC=yes
TERMUX_PKG_FOLDERNAME=wine-${TERMUX_PKG_VERSION}
TERMUX_PKG_DEPENDS="ncurses, libpng"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--with-wine-tools=${TERMUX_PKG_HOSTBUILD_DIR} --without-x --without-freetype --without-pcap"
TERMUX_PKG_HOSTBUILD="yes"
TERMUX_PKG_EXTRA_HOSTBUILD_CONFIGURE_ARGS="--without-x --without-alsa --without-capi --without-cms --without-coreaudio --without-cups --without-curses --without-dbus --without-fontconfig --without-freetype --without-gettext --without-gphoto --without-glu --without-gnutls --without-gsm --without-gstreamer --without-hal --without-jpeg --without-ldap --without-mpg123 --without-netapi --without-openal --without-opencl --without-opengl --without-osmesa --without-oss --without-pcap --without-png --without-pthread --without-pulse --without-sane --without-tiff --without-v4l --without-xcomposite --without-xcursor --without-xinerama --without-xinput --without-xinput2 --without-xml --without-xrandr --without-xrender --without-xshape --without-xshm --without-xslt --without-xxf86vm --without-zlib --without-mingw32"
 
termux_step_host_build () {
   $TERMUX_PKG_SRCDIR/configure ${TERMUX_PKG_EXTRA_HOSTBUILD_CONFIGURE_ARGS}
   make tools
}
