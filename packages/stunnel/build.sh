TERMUX_PKG_HOMEPAGE=https://www.stunnel.org/
TERMUX_PKG_DESCRIPTION="Socket wrapper which can provide TLS support to ordinary applications"
TERMUX_PKG_VERSION=5.44
TERMUX_PKG_SHA256=990a325dbb47d77d88772dd02fbbd27d91b1fea3ece76c9ff4461eca93f12299
TERMUX_PKG_SRCURL=https://www.stunnel.org/downloads/stunnel-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_DEPENDS="openssl, libutil"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--disable-shared --with-ssl=$TERMUX_PREFIX --disable-fips"
TERMUX_PKG_RM_AFTER_INSTALL="bin/stunnel3 share/man/man8/stunnel.*.8"

termux_step_pre_configure() {
	LDFLAGS+=" -llog"
}
