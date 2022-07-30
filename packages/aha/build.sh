TERMUX_PKG_HOMEPAGE=https://github.com/theZiz/aha
TERMUX_PKG_DESCRIPTION="Converts ANSI escape sequences of a unix terminal to HTML code"
# License: MPL-1.1 or LGPL-2.0-or-later
TERMUX_PKG_LICENSE="custom"
TERMUX_PKG_LICENSE_FILE="LICENSE.MPL1.1, LICENSE.LGPLv2+"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=0.5.1
TERMUX_PKG_REVISION=0
TERMUX_PKG_SRCURL=https://github.com/theZiz/aha/archive/refs/tags/${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=6aea13487f6b5c3e453a447a67345f8095282f5acd97344466816b05ebd0b3b1
TERMUX_PKG_BUILD_IN_SRC=true
TERMUX_PKG_EXTRA_MAKE_ARGS="PREFIX=$TERMUX_PREFIX"
