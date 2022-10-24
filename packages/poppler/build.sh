TERMUX_PKG_HOMEPAGE=https://poppler.freedesktop.org/
TERMUX_PKG_DESCRIPTION="PDF rendering library"
TERMUX_PKG_LICENSE="GPL-2.0"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=22.04.0
TERMUX_PKG_REVISION=2
<<<<<<< HEAD
=======
# Do not forget to bump revision of reverse dependencies and rebuild them
# when SOVERSION is changed.
_POPPLER_SOVERSION=120
>>>>>>> parent of 2ce76be09 (luarocks: bump to `3.9.1`)
TERMUX_PKG_SRCURL=https://poppler.freedesktop.org/poppler-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=813fb4b90e7bda63df53205c548602bae728887a60f4048aae4dbd9b1927deff
TERMUX_PKG_DEPENDS="fontconfig, freetype, glib, libc++, libcairo, libcurl, libiconv, libjpeg-turbo, libpng, libtiff, littlecms, openjpeg, openjpeg-tools, zlib"
TERMUX_PKG_BUILD_DEPENDS="boost, boost-headers"
TERMUX_PKG_BREAKS="poppler-dev"
TERMUX_PKG_REPLACES="poppler-dev"
#texlive needs the xpdf headers
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
-DENABLE_GLIB=ON
-DENABLE_GOBJECT_INTROSPECTION=OFF
-DENABLE_UNSTABLE_API_ABI_HEADERS=ON
-DENABLE_QT5=OFF
-DFONT_CONFIGURATION=fontconfig
"

termux_step_pre_configure() {
<<<<<<< HEAD
=======
	if ! test "${_POPPLER_SOVERSION}"; then
		termux_error_exit "Please set _POPPLER_SOVERSION variable."
	fi
	local sover_x11=$(. $TERMUX_SCRIPTDIR/x11-packages/poppler-qt/build.sh; echo $_POPPLER_SOVERSION)
	if [ "${sover_x11}" != "${_POPPLER_SOVERSION}" ]; then
		termux_error_exit "SOVERSION mismatch with \"poppler-qt\" package."
	fi
	local sover_cmake=$(sed -En 's/^.*set_target_properties\(poppler PROPERTIES .* SOVERSION ([0-9]+).*$/\1/p' CMakeLists.txt)
	if [ "${sover_cmake}" != "${_POPPLER_SOVERSION}" ]; then
		termux_error_exit "SOVERSION guard check failed (CMakeLists.txt: \"${sover_cmake}\")."
	fi

>>>>>>> parent of 2ce76be09 (luarocks: bump to `3.9.1`)
	CPPFLAGS+=" -DCMS_NO_REGISTER_KEYWORD"
}
