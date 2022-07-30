TERMUX_PKG_HOMEPAGE=https://git.causal.agency/pounce
TERMUX_PKG_DESCRIPTION="A multi-client, TLS-only IRC bouncer"
TERMUX_PKG_LICENSE="GPL-3.0"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=3.0
TERMUX_PKG_REVISION=0
TERMUX_PKG_SRCURL=https://git.causal.agency/pounce/snapshot/pounce-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=f776f7f170493697a97923e7dce9597dff5577fd40ba756e9a1bcfab17199df0
TERMUX_PKG_DEPENDS="libcrypt, libretls"
TERMUX_PKG_BUILD_IN_SRC=true
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
--mandir=$TERMUX_PREFIX/share/man
"
TERMUX_PKG_EXTRA_MAKE_ARGS="all"
