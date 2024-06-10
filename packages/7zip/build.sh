TERMUX_PKG_HOMEPAGE=https://www.7-zip.org
TERMUX_PKG_DESCRIPTION="7-Zip file archiver with a high compression ratio"
TERMUX_PKG_LICENSE="LGPL-2.1, BSD 3-Clause"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=24.06
TERMUX_PKG_REVISION=1
TERMUX_PKG_SRCURL=(https://www.7-zip.org/a/7z${TERMUX_PKG_VERSION//./}-src.tar.xz
https://www.7-zip.org/a/7z${TERMUX_PKG_VERSION//./}-linux-arm.tar.xz) # for manual, arm is smallest
TERMUX_PKG_SHA256=(2aa1660c773525b2ed84d6cd7ff0680c786ec0893b87e4db44654dcb7f5ac8b5
52f260fe2f396a0d0804f5af1b45ce0d0db45e607ed0acff8100039a5dd3dd32)
TERMUX_PKG_AUTO_UPDATE=false
TERMUX_PKG_BUILD_IN_SRC=true

# The original "termux_extract_src_archive" always strips the first components
# but the source of 7zip is directly under the root directory of the tar file
termux_extract_src_archive() {
	local file="$TERMUX_PKG_CACHEDIR/$(basename "$TERMUX_PKG_SRCURL")"
	mkdir -p "$TERMUX_PKG_SRCDIR"
	tar -xf "$file" -C "$TERMUX_PKG_SRCDIR"
}

termux_step_pre_configure() {
	if [ "$TERMUX_ARCH" = 'aarch64' ]; then
		CFLAGS+=' -march=armv8.1-a+crypto'
		CXXFLAGS+=' -march=armv8.1-a+crypto'
	fi
	# from https://build.opensuse.org/package/view_file/openSUSE:Factory/7zip/7zip.spec?rev=5
	# Remove carriage returns from docs
	sed -i -e 's/\r$//g' DOC/*.txt
	# Remove executable perms from docs
	chmod -x DOC/*.txt
	# Remove -Werror to make build succeed
	sed -i -e 's/-Werror//' CPP/7zip/7zip_gcc.mak
}

termux_step_make() {
	# from https://git.alpinelinux.org/aports/tree/community/7zip/APKBUILD?id=b4601c88f608662c75422311b7ca3c26fab4b1f4
	cd CPP/7zip/Bundles/Alone2
	mkdir -p b/c
	# TODO: enable asm
	# DISABLE_RAR: RAR codec is non-free
	# -D_GNU_SOURCE: broken sched.h defines
	make \
		CC="$CC $CFLAGS $LDFLAGS -D_GNU_SOURCE" \
		CXX="$CXX $CXXFLAGS $LDFLAGS -D_GNU_SOURCE" \
		DISABLE_RAR=1 \
		--file ../../cmpl_clang.mak \
		--jobs "$TERMUX_MAKE_PROCESSES"
}

termux_step_make_install() {
	install -Dm0755 \
		-t "$TERMUX_PREFIX"/bin \
		"$TERMUX_PKG_BUILDDIR"/CPP/7zip/Bundles/Alone2/b/c/7zz
	install -Dm0644 \
		-t "$TERMUX_PREFIX"/share/doc/"$TERMUX_PKG_NAME" \
		"$TERMUX_PKG_BUILDDIR"/DOC/{7zC,7zFormat,copying,License,lzma,Methods,readme,src-history}.txt
	tar -C "$TERMUX_PREFIX"/share/doc/"$TERMUX_PKG_NAME" \
		-xf "$TERMUX_PKG_CACHEDIR/$(basename "$TERMUX_PKG_SRCURL[1]")" MANUAL
}
