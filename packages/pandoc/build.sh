TERMUX_PKG_HOMEPAGE=https://pandoc.org/
TERMUX_PKG_DESCRIPTION="Universal markup converter"
TERMUX_PKG_LICENSE="GPL-2.0"
TERMUX_PKG_MAINTAINER="Aditya Alok <alok@termux.dev>"
TERMUX_PKG_VERSION=3.1
TERMUX_PKG_SRCURL="https://hackage.haskell.org/package/pandoc-$TERMUX_PKG_VERSION/pandoc-$TERMUX_PKG_VERSION.tar.gz"
TERMUX_PKG_SHA256=f97956087c61b0f3fef618ea35da9f24bcd90aa409559bcda727631de7a696b7
TERMUX_PKG_DEPENDS="libffi"
TERMUX_PKG_BUILD_DEPENDS="ghc-libs"
TERMUX_PKG_BUILD_IN_SRC=true
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
--ghc-option=-fexternal-interpreter
--ghc-option=-pgmi=$TERMUX_PKG_CACHEDIR/bin/iserv-proxy
--ghc-options='-opti159.223.171.199 -opti41407'"

termux_step_post_get_source(){
	local tar_file="$TERMUX_PKG_CACHEDIR/iserv-proxy.tar.xz"
	termux_download "https://github.com/MrAdityaAlok/ghc-cross-tools/releases/download/iserv-proxy-v9.2.5/iserv-proxy-9.2.5.tar.xz" \
		"$tar_file" \
		121a20990447e37fbd32ff37e2b809513e6b31824ae435038703e953a2fdadc5
	mkdir -p "$TERMUX_PKG_CACHEDIR/bin"
	tar xf "$tar_file" -C "$TERMUX_PKG_CACHEDIR/bin"
}

termux_step_post_make_install() {
	# Will be compressed in massage step.
	install -Dm600 ./man/pandoc.1 "$TERMUX_PREFIX"/share/man/man1/pandoc.1
	# Create empty completions file so that it is removed on uninstalling the package.
	install -Dm644 /dev/null "$TERMUX_PREFIX"/share/bash-completion/completions/pandoc
}

termux_step_create_debscripts() {
	cat <<-EOF >./postinst
		#!$TERMUX_PREFIX/bin/sh
		pandoc --bash-completion > $TERMUX_PREFIX/share/bash-completion/completions/pandoc
	EOF
}
