TERMUX_PKG_HOMEPAGE=https://libtins.github.io
TERMUX_PKG_DESCRIPTION="High-level, multiplatform C++ network packet sniffing and crafting library."
TERMUX_PKG_LICENSE="BSD 2-Clause"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=4.4
TERMUX_PKG_REVISION=1
TERMUX_PKG_SRCURL=https://github.com/mfontanini/libtins/archive/refs/tags/v$TERMUX_PKG_VERSION.tar.gz
TERMUX_PKG_SHA256=ff0121b4ec070407e29720c801b7e1a972042300d37560a62c57abadc9635634
TRRMUX_PKG_AUTO_UPDATE=true
TERMUX_PKG_DEPENDS="libc++, libpcap, openssl"
TERMUX_PKG_BUILD_DEPENDS="boost, boost-headers"
TERMUX_PKG_BREAKS="libtins-dev"
TERMUX_PKG_REPLACES="libtins-dev"
