TERMUX_PKG_HOMEPAGE=https://github.com/xiph/rav1e/
TERMUX_PKG_DESCRIPTION="An AV1 encoder focused on speed and safety"
TERMUX_PKG_LICENSE="BSD 2-Clause"
TERMUX_PKG_MAINTAINER="@DLC01"
TERMUX_PKG_VERSION=0.4.1
TERMUX_PKG_SRCURL=https://github.com/xiph/rav1e/archive/refs/tags/v${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=b0be59435a40e03b973ecc551ca7e632e03190b5a20f944818afa3c2ecf4852d
TERMUX_PKG_BUILD_IN_SRC=true

termux_step_pre_configure() {
        termux_setup_rust
}

termux_step_make() {
        cargo build --jobs $TERMUX_MAKE_PROCESSES --target $CARGO_TARGET_NAME --release
}

termux_step_make_install() {
        install -Dm755 -t $TERMUX_PREFIX/bin target/${CARGO_TARGET_NAME}/release/rav1e
}
