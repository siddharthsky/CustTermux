TERMUX_PKG_HOMEPAGE=https://ocaml.org/
TERMUX_PKG_DESCRIPTION="OCaml Core"
TERMUX_PKG_LICENSE="custom"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=4.12.0
TERMUX_PKG_LICENSE_FILE="LICENSE"
TERMUX_PKG_SRCURL=https://github.com/ocaml/ocaml/archive/${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=adc07a3995362403f3cb11085a86354de08e5a7f9eb3c09be7bbcc38a3a26744
TERMUX_PKG_DEPENDS="libc++"
TERMUX_PKG_BUILD_IN_SRC=true
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--perfix=$PREFIX"
