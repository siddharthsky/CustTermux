TERMUX_PKG_HOMEPAGE=https://www.gtk.org
TERMUX_PKG_DESCRIPTION="The interface definitions of accessibility infrastructure"
TERMUX_PKG_LICENSE="LGPL-2.0"
TERMUX_PKG_MAINTAINER="@termux"
_MAJOR_VERSION=2.38
TERMUX_PKG_VERSION=${_MAJOR_VERSION}.0
TERMUX_PKG_REVISION=0
TERMUX_PKG_SRCURL=https://ftp.gnome.org/pub/gnome/sources/atk/${_MAJOR_VERSION}/atk-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=ac4de2a4ef4bd5665052952fe169657e65e895c5057dffb3c2a810f6191a0c36
TERMUX_PKG_DEPENDS="glib"
TERMUX_PKG_CONFLICTS="libatk"
TERMUX_PKG_REPLACES="libatk"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="-Dintrospection=false"
