TERMUX_PKG_HOMEPAGE=http://cscope.sourceforge.net/
TERMUX_PKG_DESCRIPTION="A developers tool for browsing program code"
TERMUX_PKG_VERSION=15.8b
TERMUX_PKG_SRCURL=https://downloads.sourceforge.net/project/cscope/cscope/${TERMUX_PKG_VERSION}/cscope-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=4889d091f05aa0845384b1e4965aa31d2b20911fb2c001b2cdcffbcb7212d3af
TERMUX_PKG_DEPENDS="ncurses"
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--with-ncurses=$TERMUX_PREFIX"

termux_step_post_extract_package () {
    ls $TERMUX_PREFIX
    read
}
