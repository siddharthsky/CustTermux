TERMUX_PKG_HOMEPAGE=https://dns.lookup.dog/
TERMUX_PKG_DESCRIPTION="A command-line DNS client."
TERMUX_PKG_LICENSE="EUPL-1.2"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=0.1.0
TERMUX_PKG_SRCURL=https://github.com/ogham/dog/archive/refs/tags/v${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=82387d38727bac7fcdb080970e84b36de80bfe7923ce83f993a77d9ac7847858
#TERMUX_PKG_DEPENDS=""
TERMUX_PKG_BUILD_IN_SRC=true

termux_step_pre_configure() {
	termux_setup_rust
}

termux_step_make() {
	cargo build --jobs $TERMUX_MAKE_PROCESSES --target $CARGO_TARGET_NAME --release
}

termux_step_make_install() {
	install -Dm755 -t $TERMUX_PREFIX/bin target/${CARGO_TARGET_NAME}/release/dog
}
